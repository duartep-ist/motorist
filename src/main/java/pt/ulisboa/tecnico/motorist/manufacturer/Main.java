package pt.ulisboa.tecnico.motorist.manufacturer;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
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
        

        System.setProperty("javax.net.ssl.keyStore", "user.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "usertruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");
    }

    // sign firmware
    public static String signFirmware(File firmwareFile) throws Exception {
        // Load private key
        PrivateKey privateKey = loadPrivateKey();

        byte[] firmwareData = readFirmwareFile(firmwareFile);

        
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

    private static File createFirmwareUpdateFile(String firmwareName) {
        
        File firmwareFile = new File(manufacturerFirmwarePath + "/" + firmwareName);
        if (!firmwareFile.exists()) {
            try {
            firmwareFile.createNewFile();
            try (FileOutputStream fos = new FileOutputStream(firmwareFile)) {
                fos.write("Initial firmware content".getBytes());
            }
            } catch (IOException e) {
            System.out.println("Failed to create firmware file: " + e.getMessage());
            e.printStackTrace();
            return null;
            }
        }
        return firmwareFile;
    }

   
    public static void main(String[] args) throws IOException {
        
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

        

        SocketFactory factory = SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", port)) {

            socket.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            socket.setEnabledProtocols(new String[] { "TLSv1.3" });

            OutputStream os = new BufferedOutputStream(socket.getOutputStream());
            InputStream is = new BufferedInputStream(socket.getInputStream());

            sendMessage(os, "This is a secure channel!");
            rcvdMessage(is);

            String firmwareName = "update_v1";
            File update = createFirmwareUpdateFile(firmwareName + ".bin");
            if (update == null) {
                System.out.println("Firmware file not found.");
                return;
            }
            try {
                String signature = signFirmware(update);
                System.out.println("Firmware signed successfully.");
                sendMessage(os, firmwareName);
                System.out.println("Firmware name sent successfully.");
                sendFile(os, update);
                System.out.println("Firmware sent successfully.");
                sendMessage(os, signature);
                System.out.println("Signature sent successfully.");
            } catch (Exception e) {
                System.out.println("Failed to send firmware and signature" + e.getMessage());
                e.printStackTrace();
            }
            
            // os.write("Exit".getBytes());
            // os.flush();

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
