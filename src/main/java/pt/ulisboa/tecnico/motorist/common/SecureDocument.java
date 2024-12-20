package pt.ulisboa.tecnico.motorist.common;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * SecureDocument class provides methods to protect, check, and unprotect a document.
 * The document is protected by encrypting it with AES and adding an HMAC for integrity verification.
 */
public class SecureDocument {

    // AES encryption and HMAC algos 
    private static final String AES_ALGORITHM = "AES";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Protect a document: Encrypts and adds HMAC
     * @param inputFile Path to the input JSON file
     * @param keyFile Path to the key file
     * @param outputFile Path to the output file
     * @throws Exception
     * @throws IllegalArgumentException if the input file is not a valid JSON
    */
    public static void protect(String inputFile, String keyFile, String outputFile) throws Exception {
        // Load the key from a file
        byte[] keyBytes = Files.readAllBytes(Paths.get(keyFile));
        SecretKey secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);

        // Validate the JSON document
        String documentContent = new String(Files.readAllBytes(Paths.get(inputFile)), StandardCharsets.UTF_8);
        if (!isValidJSON(documentContent)) {
            throw new IllegalArgumentException("The input file is not a valid JSON.");
        }
        byte[] document = documentContent.getBytes(StandardCharsets.UTF_8);

        // Cipher the document
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedDocument = cipher.doFinal(document);

        // Generate HMAC for the ciphered document
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(secretKey);
        byte[] hmac = mac.doFinal(encryptedDocument);

        // Write hmac and cipher to the output file
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(hmac);
            fos.write(encryptedDocument);
        }
        System.out.println("Document protected successfully.");
    }


    /**
     * Converts a byte array to a hexadecimal string    
     * For debugging purposes
     * @param bytes The byte array to convert
     * @return The hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    
    /**
     * Check a document: Verifies HMAC
     * @param inputFile
     * @param keyFile
     * @throws Exception
     */
    public static void check(String inputFile, String keyFile) throws Exception {
        // Load the key from the file
        byte[] keyBytes = Files.readAllBytes(Paths.get(keyFile));
        SecretKey secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);

        // Read input file (HMAC + encrypted document)
        byte[] fileContent = Files.readAllBytes(Paths.get(inputFile));
        byte[] hmac = Arrays.copyOfRange(fileContent, 0, 32); // First 32 bytes for HMAC
        byte[] encryptedDocument = Arrays.copyOfRange(fileContent, 32, fileContent.length);

        // Generate HMAC for the encrypted document
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(secretKey);
        byte[] computedHmac = mac.doFinal(encryptedDocument);

        // debugging
        System.out.println("Stored HMAC: " + bytesToHex(hmac));
        System.out.println("Computed HMAC: " + bytesToHex(computedHmac));

        // Compare HMACs
        if (Arrays.equals(hmac, computedHmac)) {
            System.out.println("Document integrity verified successfully.");
        } else {
            System.err.println("Document integrity verification failed.");
        }
    }


    /**
     * Unprotect a document: Decrypts and validates JSON
     * @param inputFile
     * @param keyFile
     * @param outputFile
     * @throws Exception
     */
    public static void unprotect(String inputFile, String keyFile, String outputFile) throws Exception {
        // Load the key from the file
        byte[] keyBytes = Files.readAllBytes(Paths.get(keyFile));
        SecretKey secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);

        // Read HMAC + cipher
        byte[] fileContent = Files.readAllBytes(Paths.get(inputFile));
        byte[] hmac = Arrays.copyOfRange(fileContent, 0, 32); // First 32 bytes is the HMAC
        byte[] encryptedDocument = Arrays.copyOfRange(fileContent, 32, fileContent.length);

        // Verify HMAC by computing HMAC of cipher and comparing with received HMAC
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(secretKey);
        byte[] computedHmac = mac.doFinal(encryptedDocument);
        if (!Arrays.equals(hmac, computedHmac)) {
            throw new SecurityException("Document integrity verification failed. Cannot unprotect.");
        }

        // Decipher the document
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedDocument = cipher.doFinal(encryptedDocument);

        // Convert deciphered bytes to JSON 
        String decryptedContent = new String(decryptedDocument, StandardCharsets.UTF_8);
        if (!isValidJSON(decryptedContent)) {
            throw new IllegalArgumentException("The decrypted file is not a valid JSON.");
        }

        // Pretty-format the JSON
        //decryptedContent = prettyFormatJson(decryptedContent);

        Files.write(Paths.get(outputFile), decryptedContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("Document unprotected successfully.");
    }


    /**
     * Generate a dummy key and write it to a file
     * @param keyFile
     * @throws Exception
     */
    public static void generateDummyKey(String keyFile) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(256, new SecureRandom());
        SecretKey secretKey = keyGen.generateKey();

        // Write the key to the file
        Files.write(Paths.get(keyFile), secretKey.getEncoded());
        System.out.println("Dummy key generated successfully: " + keyFile);
    }

    /**
     * Check if a string is a valid JSON
     * @param content
     * @return
     */
    public static boolean isValidJSON(String content) {
        try {
            JsonParser.parseString(content); // Will throw JsonSyntaxException if invalid
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * Pretty-format a JSON string
     * @param json
     * @return
     */
    public static String prettyFormatJson(String json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(JsonParser.parseString(json));
    }
}
