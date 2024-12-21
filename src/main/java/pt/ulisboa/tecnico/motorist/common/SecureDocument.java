package pt.ulisboa.tecnico.motorist.common;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * SecureDocument class provides methods to protect, check, and unprotect a document.
 * The document is protected by encrypting it with AES and adding an HMAC for integrity verification.
 */
public class SecureDocument {
	// AES encryption and HMAC algos 
	public static final String ENCRYPTION_ALGORITHM = "AES";
	public static final String MAC_ALGORITHM = "HmacSHA256";

	/**
	 * Protect a document: Encrypts and adds a MAC to a JSON object
	 * @param secretKey The secret key
	 * @param document The JSON object to encrypt
	 */
	public static byte[] protect(SecretKey secretKey, JsonObject document) throws InvalidKeyException {
		try {
			byte[] unencryptedDocument = document.toString().getBytes(StandardCharsets.UTF_8);

			// Cipher the document
			Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] encryptedDocument = cipher.doFinal(unencryptedDocument);

			// Generate HMAC for the ciphered document
			Mac mac = Mac.getInstance(MAC_ALGORITHM);
			mac.init(secretKey);
			byte[] hmac = mac.doFinal(encryptedDocument);

			byte[] output = new byte[hmac.length + encryptedDocument.length];
			System.arraycopy(hmac, 0, output, 0, hmac.length);
			System.arraycopy(encryptedDocument, 0, output, hmac.length, encryptedDocument.length);
			return output;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
			// Rethrow missing algorithm and similar exceptions as errors. This should never happen.
			throw new Error(e);
		}
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
	 * @return `true` if the protected document is valid, `false` otherwise
	 */
	public static boolean check(SecretKey secretKey, byte[] protectedDocument) throws InvalidKeyException {
		try {
			// Split protected document into HMAC + encrypted document
			byte[] hmac = Arrays.copyOfRange(protectedDocument, 0, 32); // First 32 bytes for HMAC
			byte[] encryptedDocument = Arrays.copyOfRange(protectedDocument, 32, protectedDocument.length);

			// Generate HMAC for the encrypted document
			Mac mac = Mac.getInstance(MAC_ALGORITHM);
			mac.init(secretKey);
			byte[] computedHmac = mac.doFinal(encryptedDocument);

			// debugging
			if (Debug.ENABLED) {
				System.out.println("SecureDocument.check(): Stored HMAC: " + bytesToHex(hmac));
				System.out.println("SecureDocument.check(): Computed HMAC: " + bytesToHex(computedHmac));
			}

			return Arrays.equals(hmac, computedHmac);
		} catch (NoSuchAlgorithmException e) {
			// Rethrow missing algorithm exceptions as errors. This should never happen.
			throw new Error(e);
		}
	}


	/**
	 * Unprotect a document: Decrypts and validates JSON
	 * @param inputFile
	 * @param keyFile
	 * @param outputFile
	 * @throws Exception
	 */
	public static JsonObject unprotect(SecretKey secretKey, byte[] protectedDocument) throws InvalidKeyException, SecurityException, JsonSyntaxException {
		try {
			// Extract the encrypted document
			byte[] encryptedDocument = Arrays.copyOfRange(protectedDocument, 32, protectedDocument.length);

			if (!check(secretKey, protectedDocument)) {
				throw new SecurityException("Document integrity verification failed. Cannot unprotect.");
			}

			// Decipher the document
			Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] decryptedDocument = cipher.doFinal(encryptedDocument);

			// Convert deciphered bytes to JSON 
			return JsonParser.parseString(new String(decryptedDocument, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
			// Rethrow missing algorithm and similar exceptions as errors. This should never happen.
			throw new Error(e);
		}
	}


	/**
	 * Securely generate a new secret key
	 */
	public static SecretKey generateKey() {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
			keyGen.init(256, new SecureRandom());
			return keyGen.generateKey();
		} catch (NoSuchAlgorithmException e) {
			// Rethrow missing algorithm exceptions as errors. This should never happen.
			throw new Error(e);
		}
	}

	/**
	 * Pretty-format a JSON object
	 * @param json
	 * @return
	 */
	public static String prettyFormatJson(JsonObject json) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(json);
	}
}
