package pt.ulisboa.tecnico.motorist.secure_document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import pt.ulisboa.tecnico.motorist.common.SecureDocument;

public class Main {
	// Usage example for testing:
	// Help:
	// ./run secure_document help
	// Generate Dummy key:
	// ./run secure_document generate-key keyfile.key
	// Protect:
	// ./run secure_document protect example.json keyfile.key protected_example.dat
	// Check:
	// ./run secure_document check protected_example.dat keyfile.key
	// Unprotect:
	// ./run secure_document unprotect protected_example.dat keyfile.key unprotected_example.json

	public static byte[] loadFromFile(String filePath) throws IOException {
		return Files.readAllBytes(Paths.get(filePath));
	}
	public static JsonObject loadDocumentFromFile(String filePath) throws IOException, IllegalStateException {
		return JsonParser.parseString(new String(loadFromFile(filePath), StandardCharsets.UTF_8)).getAsJsonObject();
	}
	public static SecretKey loadKeyFromFile(String keyFilePath) throws IOException {
		byte[] keyBytes = loadFromFile(keyFilePath);
		return new SecretKeySpec(keyBytes, SecureDocument.ENCRYPTION_ALGORITHM);
	}

	public static void storeInFile(String filePath, byte[] content) throws IOException {
		Files.write(Paths.get(filePath), content);
	}
	public static void storeDocumentInFile(String filePath, JsonObject document) throws IOException {
		storeInFile(filePath, document.toString().getBytes(StandardCharsets.UTF_8));
	}
	public static void storeKeyInFile(String filePath, SecretKey secretKey) throws IOException {
		storeInFile(filePath, secretKey.getEncoded());
	}

	/**
	 * Protect a document: Encrypts and adds HMAC
	 * @param inputFile Path to the input JSON file
	 * @param keyFile Path to the key file
	 * @param outputFile Path to the output file
	 * @throws Exception
	 * @throws IllegalArgumentException if the input file is not a valid JSON
	*/
	public static void protectCommand(String inputFile, String keyFile, String outputFile) throws Exception {
		storeInFile(outputFile, SecureDocument.protect(loadKeyFromFile(keyFile), loadDocumentFromFile(inputFile)));
		System.out.println("Document protected successfully.");
	}

	/**
	 * Check a document: Verifies HMAC
	 * @param inputFile
	 * @param keyFile
	 * @throws Exception
	 */
    public static void checkCommand(String inputFile, String keyFile) throws Exception {
		if (SecureDocument.check(loadKeyFromFile(keyFile), loadFromFile(inputFile))) {
			System.out.println("The protected document is valid.");
		} else {
			System.err.println("The protected document is not valid (i.e. was tampered with).");
		}
	}

	/**
	 * Unprotect a document: Decrypts and decodes JSON
	 * @param inputFile
	 * @param keyFile
	 * @param outputFile
	 * @throws Exception
	 */
	public static void unprotectCommand(String inputFile, String keyFile, String outputFile) throws Exception {
		try {
			storeDocumentInFile(outputFile, SecureDocument.unprotect(loadKeyFromFile(keyFile), loadFromFile(inputFile)));
			System.out.println("Document unprotected successfully.");
		} catch (SecurityException e) {
			throw new IllegalArgumentException("The protected document is not valid (i.e. was tampered with)");
		} catch (JsonSyntaxException e) {
			throw new IllegalArgumentException("The protected document does not contain valid JSON");
		}
	}

	public static void generateKeyCommand(String keyFile) throws Exception {
		storeKeyInFile(keyFile, SecureDocument.generateKey());
		System.out.println("Dummy key generated successfully: " + keyFile);
	}

	/**
	 * Command-line interface
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			printHelp();
			return;
		}

		String command = args[0];
		try {
			switch (command) {
				case "help":
					printHelp();
					break;
				case "protect":
					if (args.length != 4) throw new IllegalArgumentException("Invalid arguments for protect.");
					protectCommand(args[1], args[2], args[3]);
					break;
				case "check":
					if (args.length != 3) throw new IllegalArgumentException("Invalid arguments for check.");
					checkCommand(args[1], args[2]);
					break;
				case "unprotect":
					if (args.length != 4) throw new IllegalArgumentException("Invalid arguments for unprotect.");
					unprotectCommand(args[1], args[2], args[3]);
					break;
				case "generate-key":
					if (args.length != 2) throw new IllegalArgumentException("Invalid arguments for generate-key.");
					generateKeyCommand(args[1]);
					break;
				default:
					throw new IllegalArgumentException("Unknown command: " + command);
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}


	/**
	* Usage instructions
	*/
	private static void printHelp() {
		System.out.println("Usage:");
		System.out.println(" help                             - Print this help message");
		System.out.println(" protect <input> <key> <output>   - Protect a document");
		System.out.println(" check <input> <key>              - Check the integrity of a document");
		System.out.println(" unprotect <input> <key> <output> - Unprotect a document");
		System.out.println(" generate-key <keyfile>           - Generate a dummy key");
	}
}
