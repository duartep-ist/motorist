package pt.ulisboa.tecnico.motorist.secure_document;

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
					SecureDocument.protect(args[1], args[2], args[3]);
					break;
				case "check":
					if (args.length != 3) throw new IllegalArgumentException("Invalid arguments for check.");
					SecureDocument.check(args[1], args[2]);
					break;
				case "unprotect":
					if (args.length != 4) throw new IllegalArgumentException("Invalid arguments for unprotect.");
					SecureDocument.unprotect(args[1], args[2], args[3]);
					break;
				case "generate-key":
					if (args.length != 2) throw new IllegalArgumentException("Invalid arguments for generate-key.");
					SecureDocument.generateDummyKey(args[1]);
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
