package pt.ulisboa.tecnico.motorist.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.motorist.common.JSONStreamReader;
import pt.ulisboa.tecnico.motorist.common.JSONStreamWriter;
import pt.ulisboa.tecnico.motorist.common.UserKeyFile;
import pt.ulisboa.tecnico.motorist.common.UserKeyFile.WrongPasswordException;

public class Main {
	private static final Base64.Encoder base64Encoder = Base64.getEncoder();
	private static final Base64.Decoder base64Decoder = Base64.getDecoder();

	private static final BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));

	private static String prompt(String promptText) throws IOException {
		System.out.print(promptText);
		return stdinReader.readLine();
	}

	public static void main(String[] args) throws Exception {
		System.err.println("Usage: app <server address> <server TCP port> <key file path> <user ID> <password>");

		String serverAddress = args.length > 0 ? args[0] : "localhost";
		int serverPort = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

		String keyFilePath = args.length > 2 ? args[0] : "./key.p12";
		String userID = args.length > 3 ? args[2] : prompt("User ID: ");
		String password = args.length > 4 ? args[3] : prompt("Password: ");

		UserKeyFile keyFile;
		SecretKey userKey;
		while (true) {
			keyFile = new UserKeyFile(new File(keyFilePath), password);
			try {
				userKey = keyFile.loadKey();
				break;
			} catch (WrongPasswordException e) {
				System.out.println("Wrong password or corrupted key file.");
				password = prompt("Password: ");
			} catch (IOException e) {
				System.out.println(e);
				System.out.println("Key file \"" + keyFilePath + "\" not found. The file will be created with a newly-generated key.");
				userKey = UserKeyFile.generateKey();
				keyFile.storeKey(userKey);
				break;
			}
		}

		// TODO(Duarte): The code below is untested!

		try (Socket socket = new Socket(serverAddress, serverPort)) {
			System.out.println("Connected to the server!");

			JSONStreamReader reader = new JSONStreamReader(socket.getInputStream());
			JSONStreamWriter writer = new JSONStreamWriter(socket.getOutputStream());

			{
				JsonObject authRequest = new JsonObject();
				authRequest.addProperty("type", "AUTH_REQUEST");
				authRequest.addProperty("id", userID);
				authRequest.addProperty("password", password);
				writer.write(authRequest);
			}
			{
				JsonObject authChallenge = reader.read();
				if (authChallenge.get(keyFilePath).getAsString().equals("AUTH_CHALLENGE")) {
					throw new Exception("Expected to receive an AUTH_CHALLENGE message.");
				}
				byte[] challenge = base64Decoder.decode(authChallenge.get("challenge").getAsString());

				Mac mac = Mac.getInstance("HmacSHA256");
				mac.init(userKey);
				byte[] macResult = mac.doFinal(challenge);

				JsonObject authProof = new JsonObject();
				authProof.addProperty("type", "AUTH_PROOF");
				authProof.addProperty("mac", new String(base64Encoder.encode(macResult)));
				writer.write(authProof);
			}
			{
				JsonObject authResponse = reader.read();
				if (authResponse.get(keyFilePath).getAsString().equals("AUTH_FAILURE")) {
					System.out.println("Wrong user ID or password.");
				} else if (!authResponse.get(keyFilePath).getAsString().equals("AUTH_CONFIRMATION")) {
					throw new Exception("Expected to receive an AUTH_CONFIRMATION or AUTH_FAILURE message.");
				}
			}

			// At this point we are successfully authenticated.
		} catch (IOException e) {
			System.out.println("Couldn't connect to the server: " + e.getMessage());
		}
	}
}
