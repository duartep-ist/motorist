package pt.ulisboa.tecnico.motorist.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSchException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ulisboa.tecnico.motorist.common.JSONStreamReader;
import pt.ulisboa.tecnico.motorist.common.JSONStreamWriter;
import pt.ulisboa.tecnico.motorist.common.UserKeyFile;

public class Main {
	private static final Base64.Encoder base64Encoder = Base64.getEncoder();
	private static final Base64.Decoder base64Decoder = Base64.getDecoder();

	private static final BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));

	private static String prompt(String promptText) throws IOException {
		System.out.print(promptText);
		return stdinReader.readLine();
	}

	public static void main(String[] args) throws Exception {
		System.err.println("Usage: app <server address> <server TCP port> <key file path> <username> <password> <subcommand> <arguments>...");

		String serverAddress = args.length > 0 ? args[0] : "localhost";
		int serverPort = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

		String keyFilePath = args.length > 2 ? args[2] : "./app-key.p12";
		String username = args.length > 3 ? args[3] : prompt("Username: ");
		String password = args.length > 4 ? args[4] : prompt("Password: ");

		UserKeyFile keyFile = new UserKeyFile(new File(keyFilePath));
		SecretKey userKey;

		if (!keyFile.exists()) {
			System.out.println("Key file \"" + keyFilePath + "\" not found. The file will be created with a newly-generated key.");
			userKey = UserKeyFile.generateKey();
			keyFile.storeKey(userKey, password);
		}

		while (true) {
			userKey = keyFile.loadKey(password);
			if (userKey != null) break;
			System.out.println("Wrong password or corrupted key file.");
			password = prompt("Password: ");
		}

		Session sshSession = null;
		try {
			JSch jsch = new JSch();
            sshSession = jsch.getSession("app", serverAddress, serverPort);
            sshSession.setConfig("StrictHostKeyChecking", "no"); 
            sshSession.connect();

			try (Socket socket = new Socket(serverAddress, serverPort)) {
				System.out.println("Connected to the server!");

				JSONStreamReader reader = new JSONStreamReader(socket.getInputStream());
				JSONStreamWriter writer = new JSONStreamWriter(socket.getOutputStream());

				{
					JsonObject authRequest = new JsonObject();
					authRequest.addProperty("type", "AUTH_REQUEST");
					authRequest.addProperty("username", username);
					authRequest.addProperty("password", password);
					writer.write(authRequest);
				}
				{
					JsonObject authChallenge = reader.read();
					if (!authChallenge.get("type").getAsString().equals("AUTH_CHALLENGE")) {
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
					if (authResponse.get("type").getAsString().equals("AUTH_FAILURE")) {
						System.out.println("Wrong username or key.");
						return;
					} else if (!authResponse.get("type").getAsString().equals("AUTH_CONFIRMATION")) {
						throw new Exception("Expected to receive an AUTH_CONFIRMATION or AUTH_FAILURE message");
					}
				}

				// At this point we are successfully authenticated.

				input_loop:
				while (true) {
					String[] arguments;
					if (args.length > 5) {
						arguments = Arrays.copyOfRange(args, 5, args.length);
					} else {
						String input = prompt("app> ");
						arguments = input.split(" ");
					}

					switch (arguments[0]) {
						case "exit":
							break input_loop;

						case "help":
							System.out.println(
								"Available commands:\n" +
								"  help\n" +
								"  get-user-config\n" +
								"  set-user-config <JSON>\n" +
								"  get-car-info"
							);
							break;

						case "get-user-config": {
							JsonObject request = new JsonObject();
							request.addProperty("type", "USER_CONFIG_READ_REQUEST");
							writer.write(request);

							JsonObject response = reader.read();
							if (!response.get("type").getAsString().equals("USER_CONFIG_READ_RESPONSE"))
								throw new Exception("Expected to receive a USER_CONFIG_READ_RESPONSE message");

							Gson prettyPrinter = new GsonBuilder().setPrettyPrinting().create();
							System.out.println("Current user configuration:\n" + prettyPrinter.toJson(response.get("configuration")));
							break;
						}

						case "set-user-config": {
							JsonObject request = new JsonObject();
							request.addProperty("type", "USER_CONFIG_WRITE_REQUEST");
							request.add("configuration", JsonParser.parseString(arguments[1]).getAsJsonObject());
							writer.write(request);

							JsonObject response = reader.read();
							if (!response.get("type").getAsString().equals("USER_CONFIG_WRITE_CONFIRMATION"))
								throw new Exception("Expected to receive a USER_CONFIG_WRITE_CONFIRMATION message");
							break;
						}

						case "get-car-info": {
							JsonObject request = new JsonObject();
							request.addProperty("type", "CAR_INFO_READ_REQUEST");
							writer.write(request);

							JsonObject response = reader.read();
							if (!response.get("type").getAsString().equals("CAR_INFO_READ_RESPONSE"))
								throw new Exception("Expected to receive a CAR_INFO_READ_RESPONSE message");

							JsonObject info = response.get("info").getAsJsonObject();

							System.out.printf(
								"Current car information:\n" +
								"  Car ID: " + info.get("carID").getAsString() + "\n" +
								"  Battery level: " + info.get("batteryLevel").getAsNumber().toString() + "\n"
							);
							break;
						}

						default:
							System.out.println("Unrecognized command \"" + arguments[0] + "\".");
							break;
					}

					if (args.length > 5) break;
				}
			} catch (IOException e) {
				System.out.println("Connection error: " + e.getMessage());
			}
		} catch (JSchException e) {
			System.out.println("SSH tunneling session error: " + e.getMessage());
		}
	}
}
