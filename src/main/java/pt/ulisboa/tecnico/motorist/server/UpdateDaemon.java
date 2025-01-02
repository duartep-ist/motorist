package pt.ulisboa.tecnico.motorist.server;

import java.io.*;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.file.Files;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.security.PublicKey;
import java.util.*;


public class UpdateDaemon implements Runnable {

    private static String databaseDirPath;
    private static String firmwareDirPath = "/firmware"; 
    private static final BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));
    
    private final int port = 5001; // Port for SSL connections (manufacturer)
    private final String manufacturerHost = "192.168.1.2"; // Host for SSL connections (manufacturer)

    public UpdateDaemon(String databaseDirPath) {
        UpdateDaemon.databaseDirPath = databaseDirPath;

        System.setProperty("javax.net.ssl.keyStore", "daemon.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "daemontruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");


    }

    private static String prompt(String promptText) throws IOException {
		System.out.print(promptText);
		return stdinReader.readLine();
	}

    /*
     * The run method is the main loop of the UpdateDaemon. It reads commands from the user
     */

    @Override
    public void run() {
        
        while(true){
            String[] arguments;
            try {
                String input = prompt("car_updates> ");
                arguments = input.split(" ");
                switch (arguments[0]) {
                    case "get_updates":
                        try {
                            startSecureConnection();
                        }catch (ConnectException ce){
                            System.out.println("Manufacturer Server unreachable. Please try again later.");
                        }catch(SocketException se){
                            System.out.println("Socket error. Check truststores configuration.");
                        }catch(KeyManagementException ke){
                            System.out.println("Key Management error. Check truststores configuration.");
                        }catch ( NoSuchAlgorithmException ne) {
                            System.out.println("Algorithm to be used in TLS not found. Check truststores configuration ");
                        } catch (Exception e) {
                            System.err.println("StartSecureConnection error: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "verify_updates":
                        verifyUpdates();
                        break;
                    case "help":
                        System.out.println(
                            "Available commands:\n" +
                            "  help\n" +
                            "  get_updates\n" +
                            "  verify_updates\n"
                        );
                        break;
                    default:
                        break;
                }
               
            } catch (IOException e) {
                System.out.println("Error reading input: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            
            
        }
    }

    private void verifyUpdates() {
        File firmwareDir = new File(databaseDirPath + firmwareDirPath);
        if (!firmwareDir.exists() || !firmwareDir.isDirectory()) {
            System.out.println("No firmware directory found.");
            return;
        }

        File[] directories = firmwareDir.listFiles(File::isDirectory);
        if (directories == null || directories.length == 0) {
            System.out.println("No firmware updates found.");
            return;
        }

        for (File dir : directories) {
            String dirName = dir.getName();
            File firmwareFile = new File(dir, dirName + ".bin");
            File signatureFile = new File(dir, "signature.sig");

            if (!firmwareFile.exists() || !signatureFile.exists()) {
                System.out.println(dirName + ": Missing firmware or signature file.");
                continue;
            }

            try {
                String firmwareData = new String(Files.readAllBytes(firmwareFile.toPath()));
                String signatureData = new String(Files.readAllBytes(signatureFile.toPath()));
                PublicKey publicKey = loadPublicKey("manufacturer_public.pem");

                if (verifySignature(firmwareData, signatureData, publicKey)) {
                    System.out.println(dirName + ": Signature verified successfully.");
                } else {
                    System.out.println(dirName + ": Signature verification failed.");
                }
            } catch (Exception e) {
                System.out.println(dirName + ": Error verifying signature - " + e.getMessage());
            }
        }
    }




    /**
     * Get the latest update version from the firmware directory
     * @return the latest version
     */
    public static String getLatestUpdateVersion(){
        File firmwareDir = new File(databaseDirPath + firmwareDirPath);
        if (!firmwareDir.exists() || !firmwareDir.isDirectory()) {
            return "0.0.0";
        }

        File[] directories = firmwareDir.listFiles(File::isDirectory);
        if (directories == null || directories.length == 0) {
            return "0.0.0";
        }

        String latestVersion = "";
        for (File dir : directories) {
            String dirName = dir.getName();
            int underscoreIndex = dirName.lastIndexOf('_');
            if (underscoreIndex != -1) {
                String version = dirName.substring(underscoreIndex + 1);
                if (version.compareTo(latestVersion) > 0) {
                    latestVersion = version;
                }
            }
        }

        return latestVersion.isEmpty() ? "0.0.0" : latestVersion;
    }

    /**
     * Establish a secure tls connection with the manufacturer updates server
     * @throws ConnectException
     * @throws Exception
     */
    public void startSecureConnection() throws ConnectException,NoSuchAlgorithmException,KeyManagementException,SocketException, Exception {

        SocketFactory factory = SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(manufacturerHost, port)) {
            socket.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            socket.setEnabledProtocols(new String[] { "TLSv1.3" });

            OutputStream os = new BufferedOutputStream(socket.getOutputStream());
            InputStream is = new BufferedInputStream(socket.getInputStream());
            
            //gets and sends the latest version in the car
            String version = getLatestUpdateVersion();
            System.out.println("Sending latest version: " + version);
            sendMessage(os, version);

            // receives the firmware info along with the signature
            String firmwareName = rcvdMessage(is);
            if (firmwareName.equals("No update available")) {
                return;
            }
            String firmware_data = rcvdMessage(is);
            String signature = rcvdMessage(is);
            
            // checks firmware signature using manufacturer public key
            PublicKey publicKey = loadPublicKey("manufacturer_public.pem");
            //System.out.println("Public key loaded: " + publicKey);
            if(!verifySignature(firmware_data, signature, publicKey)) {
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
                fos.write(firmware_data.getBytes());
            }catch (IOException i) {
                System.out.println(i);
                return;
            }

            File signatureFile = new File(firmwareDir, "signature.sig");
            try (FileOutputStream fos = new FileOutputStream(signatureFile)) {
                fos.write(signature.getBytes());
            }catch (IOException i) {
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
    


    /**
     * Load the public key from a file
     * @param publicKeyPath
     * @return the public key
     * @throws Exception
     */
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

    
    /**
     * Verify the signature of the firmware with manufacturers public key
     * @param firmwareString
     * @param signatureString
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static boolean verifySignature(String firmwareString, String signatureString, PublicKey publicKey) throws Exception {
         
        byte[] firmwareBytes = firmwareString.getBytes();
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

    /**
     * Sends a message over the socket
     * @param os
     * @param message
     * @throws IOException
     */
    private static void sendMessage(OutputStream os, String message) throws IOException {
        os.write(message.getBytes());
        os.flush();
        System.out.printf("Sent: %s%n", message);
    }

    /**
     * Receives a message from the socket
     * @param is
     * @return
     * @throws IOException
     */
    private static String rcvdMessage(InputStream is) throws IOException {
        byte[] data = new byte[2048];
        int len = is.read(data);
        String msg = new String(data, 0, len);
        System.out.printf("Received %d bytes: %s%n", len, msg);
        return msg;
    } 


}
