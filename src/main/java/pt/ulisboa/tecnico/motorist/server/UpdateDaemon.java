package pt.ulisboa.tecnico.motorist.server;

import java.io.*;

import java.net.Socket;
import java.nio.file.Files;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.security.PublicKey;
import java.util.*;


public class UpdateDaemon implements Runnable {

    private static String databaseDirPath;
    private static String firmwareDirPath = "/firmware"; 
    
    private final int port = 5001; // Port for SSL connections

    public UpdateDaemon(String databaseDirPath) {
        UpdateDaemon.databaseDirPath = databaseDirPath;

        System.setProperty("javax.net.ssl.keyStore", "server.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "servertruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");
    }

    @Override
    public void run() {
        try {
            startSecureServer();
        } catch (Exception e) {
            System.err.println("Failed to start UpdateDaemon: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startSecureServer() throws Exception {

        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        while (true) {
            try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port)) {
                listener.setNeedClientAuth(true);
                listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
                listener.setEnabledProtocols(new String[] { "TLSv1.3" });
                System.out.println("Waiting for Updates on port " + port );
                String message = "";
                InputStream is = null;
                OutputStream os = null;
                try (Socket socket = listener.accept()) {
                    try {
                        is = new BufferedInputStream(socket.getInputStream());
                        byte[] data = new byte[2048];
                        int len = is.read(data);

                        message = new String(data, 0, len);
                        os = new BufferedOutputStream(socket.getOutputStream());
                        System.out.printf("server received %d bytes: %s%n", len, message);
                        String response = message + " processed by server";
                        os.write(response.getBytes(), 0, response.getBytes().length);
                        os.flush();

                        String firmwareName = rcvdMessage(is);
                        System.out.println("Firmware name received: " + firmwareName);
                        String firmware = rcvdMessage(is);
                        System.out.println("Firmware received: " + firmware);
                        String signature = rcvdMessage(is);
                        System.out.println("Signature received: " + signature);
                        
                        PublicKey publicKey = loadPublicKey("manufacturer_public.pem");
                        //System.out.println("Public key loaded: " + publicKey);
                        if(!verifySignature(firmware, signature, publicKey)) {
                            System.out.println("Invalid signature. Firmware update rejected.");
                            return;
                        } 
                        System.out.println("Signature verified. Firmware update accepted.");
                        // save the firmware to a file and also the signature
                        File firmwareDir = new File(databaseDirPath + firmwareDirPath + "/" + firmwareName);
                        if (!firmwareDir.exists()) {
                            firmwareDir.mkdirs();
                        }

                        File firmwareFile = new File(firmwareDir, firmwareName + ".bin");
                        try (FileOutputStream fos = new FileOutputStream(firmwareFile)) {
                            fos.write(firmware.getBytes());
                        }

                        File signatureFile = new File(firmwareDir, "signature.sig");
                        try (FileOutputStream fos = new FileOutputStream(signatureFile)) {
                            fos.write(signature.getBytes());
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


    // Load the public key from PEM file
    public static PublicKey loadPublicKey(String publicKeyPath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(new File(publicKeyPath).toPath());

        String key = new String(keyBytes);
        key = key.replace("-----BEGIN PUBLIC KEY-----", "")
                 .replace("-----END PUBLIC KEY-----", "")
                 .replaceAll("\\s", "");

        byte[] decodedKey = Base64.getDecoder().decode(key);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
    }

    
    // Verify the signature of the firmware using the public key
    public static boolean verifySignature(String firmwareString, String signatureString, PublicKey publicKey) throws Exception {
         
        byte[] firmwareBytes = firmwareString.getBytes();
        // Decode the signature string (base64) into bytes
        byte[] signatureBytes = Base64.getDecoder().decode(signatureString);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(firmwareBytes);

   
        return signature.verify(signatureBytes);
    }

    // Load the signature from a file for later non repudiation checks
    private static byte[] loadSignature(String signaturePath) throws Exception {
        byte[] signatureBytes = Files.readAllBytes(new File(signaturePath).toPath());
        return signatureBytes;
    }

    
    private static String rcvdMessage(InputStream is) throws IOException {
        byte[] data = new byte[2048];
        int len = is.read(data);
        String msg = new String(data, 0, len);
        System.out.printf("Manufacturer received %d bytes: %s%n", len, msg);
        return msg;
    } 


    public static void main(String[] args) {
        databaseDirPath = args.length > 0 ? args[0] : "./server-db";
        UpdateDaemon daemon = new UpdateDaemon(databaseDirPath);
        try {
            daemon.startSecureServer();
        } catch (Exception e) {
            System.err.println("Failed to start UpdateDaemon: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
