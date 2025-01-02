package pt.ulisboa.tecnico.motorist.manufacturer;

import java.net.Socket;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Base64;


public class Main {

    private static String serverHost;
    private static int    serverPort;
    private static String keystorePath;
    private static final String PRIVATE_KEY_PATH = "manufacturer_private.pem";
    private static String manufacturerFirmwarePath = "./firmware";

    public Main(String serverHost, int serverPort, String keystorePath, String keystorePassword) {
        Main.serverHost = serverHost;
        Main.serverPort = serverPort;
        Main.keystorePath = keystorePath;
        

        System.setProperty("javax.net.ssl.keyStore", "manufacturer.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "manufacturertruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");

        
    }




    // sign firmware
    public static String signFirmware(byte[] firmwareData ) throws Exception {
        // Load private key
        PrivateKey privateKey = loadPrivateKey();

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(firmwareData);
        byte[] digitalSignature = signature.sign();

        // signature as a Base64 string
        return Base64.getEncoder().encodeToString(digitalSignature);
    }

    // Load the private key from PEM file
    private static PrivateKey loadPrivateKey() throws Exception {
        byte[] keyBytes = Files.readAllBytes(new File(PRIVATE_KEY_PATH).toPath());

        String key = new String(keyBytes);
        key = key.replace("-----BEGIN PRIVATE KEY-----", "")
                 .replace("-----END PRIVATE KEY-----", "")
                 .replaceAll("\\s", "");

        byte[] decodedKey = Base64.getDecoder().decode(key);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
    }

    
    private static byte[] readFirmwareFile(File firmwareFile) throws IOException {
        return Files.readAllBytes(firmwareFile.toPath());
    }

    /**
     * Sends a file over the socket
     */
    private static void sendFile(OutputStream os, File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            System.out.printf("Manufacturer sent file: %s%n", file.getName());
        } catch(IOException e) {
            System.out.println("Failed to send file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendMessage(OutputStream os, String message) throws IOException {
        os.write(message.getBytes());
        os.flush();
        System.out.printf("Manufacturer sent: %s%n", message);
    }

    private static String rcvdMessage(InputStream is) throws IOException {
        byte[] data = new byte[2048];
        int len = is.read(data);
        String msg = new String(data, 0, len);
        System.out.printf("Manufacturer received %d bytes: %s%n", len, msg);
        return msg;
    }

   

    public static String[] getLatestUpdateFromDB(String version) throws Exception {
        Connection connection = DriverManager.getConnection(
            "jdbc:mariadb://localhost:3306/firmware_db",
            "manufacturer_user", "password"
        );
        try(PreparedStatement stmt = connection.prepareStatement("SELECT * FROM firmware_updates WHERE version > ? ORDER BY version DESC LIMIT 1")) {
            stmt.setString(1, version);
            ResultSet rs = stmt.executeQuery();
            String update = null;
            String version_update = null;
            while(rs.next()){
                version_update = rs.getString("version");
                System.out.println("Version: " + rs.getString("version"));
                byte[] data = rs.getBytes("firmware_data");
                update = new String(data);
                System.out.println("Description: " + rs.getString("description"));
                System.out.println("Data: " + update);
            }
            connection.close(); 
            if(update == null){
                return new String[]{null,null};
            }
            return new String[]{update,version_update};
        } catch (Exception e) {
            System.out.println("Failed to get latest update from DB: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

   
    public static void main(String[] args) throws Exception {
        
        int port;
        new File(Paths.get(manufacturerFirmwarePath).toString()).mkdirs();

        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
            //firmwareFilePath = args[1];             
        } else{
            System.out.println("Usage: Manufacturer <serverPort>");
            return;
        }

        System.setProperty("javax.net.ssl.keyStore", "manufacturer.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "manufacturertruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");



        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        while (true) {
            try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port)) {
                listener.setNeedClientAuth(true);
                listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
                listener.setEnabledProtocols(new String[] { "TLSv1.3" });
                System.out.println("Waiting for connections on port " + port );
                
                InputStream is = null;
                OutputStream os = null;
                try (Socket socket = listener.accept()) {
                    try {

                    os = new BufferedOutputStream(socket.getOutputStream());
                    is = new BufferedInputStream(socket.getInputStream());

                    sendMessage(os, "This is a secure channel!");

                    //mudar para receber versao mais recente do carro
                    String latest_version = rcvdMessage(is);

                    String queryResults[] = getLatestUpdateFromDB(latest_version);
                    String update = queryResults[0];
                    if (update == null) {
                        sendMessage(os, "No update available");
                        continue;
                    }
                    String version = queryResults[1]; 
                    try {
                        //String signature = signFirmware(Files.readAllBytes(update.toPath()));
                        String signature = signFirmware(update.getBytes());
                        System.out.println("Firmware signed successfully.");
                        //change the firmware name 
                        sendMessage(os, "firmware_" + version );
                        System.out.println("Firmware name sent successfully.");
                        sendMessage(os, update);
                        System.out.println("Firmware sent successfully.");
                        sendMessage(os, signature);
                        System.out.println("Signature sent successfully.");
                    } catch (Exception e) {
                        System.out.println("Failed to send firmware and signature" + e.getMessage());
                        e.printStackTrace();
                    }
                    } catch (IOException i) {
                        System.out.println(i);
                        return;
                    }
                    try {
                        is.close();
                        os.close();
                        socket.close();
                    } catch (IOException i) {
                        System.out.println(i);
                        return;
                    }
                }
            }
        }

    }
}
