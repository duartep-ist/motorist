package pt.ulisboa.tecnico.motorist.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.MalformedJsonException;

import pt.ulisboa.tecnico.motorist.common.JSONStreamReader;
import pt.ulisboa.tecnico.motorist.common.JSONStreamWriter;
import pt.ulisboa.tecnico.motorist.common.SecureDocument;
import pt.ulisboa.tecnico.motorist.common.UserKeyFile;

public class Main {
	private static final Base64.Encoder base64Encoder = Base64.getEncoder();
	private static final Base64.Decoder base64Decoder = Base64.getDecoder();
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final Pattern usernamePattern = Pattern.compile("^(?:\\w|-)+$");

	private static final JsonObject defaultUserConfig = JsonParser.parseString("{\"ac\":[{\"out1\":\"1000\"},{\"out2\":\"1000\"}],\"seat\":[{\"pos1\":\"0\"},{\"pos2\":\"0\"}]}").getAsJsonObject();
	private static final String carID = "1234XYZ";

	private static String databaseDirPath;

	public static void main(String[] args) throws IOException {
		databaseDirPath = args.length > 0 ? args[0] : "./server-db";
		new File(Paths.get(databaseDirPath, "users").toString()).mkdirs();

		try {
            UpdateDaemon daemon = new UpdateDaemon(databaseDirPath);
			Thread daemonThread = new Thread(daemon); 
            daemonThread.start(); 
        } catch (Exception e) {
            System.err.println("SERVER: Failed to start UpdateDaemon: " + e.getMessage());
            e.printStackTrace();
        }

		try (ServerSocket serverSocket = new ServerSocket(5000)) {
			System.out.println("Listening on port 5000.");
			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Accepted connection from client.");

				JSONStreamReader reader = null;
				JSONStreamWriter writer = null;
				try {
					reader = new JSONStreamReader(clientSocket.getInputStream());
					writer = new JSONStreamWriter(clientSocket.getOutputStream());
				} catch (IOException e) {
					continue;
				}

				ConnectionState state = ConnectionState.AWAITING_AUTH_REQUEST;
				SecretKey userKey = null;
				String username = null;
				String password = null;
				byte[] challenge = new byte[32];

				// Handle messages from this connection
				receive_loop:
				while (true) {
					try {
						JsonObject receivedMessage = reader.read();
						String type = receivedMessage.get("type").getAsString();
						switch (type) {
							case "AUTH_REQUEST": {
								if (state != ConnectionState.AWAITING_AUTH_REQUEST)
									throw new Exception("Unexpected auth request message");

								username = receivedMessage.get("username").getAsString();
								if (!usernamePattern.matcher(username.subSequence(0, username.length())).matches())
									throw new Exception("Invalid username");
								password = receivedMessage.get("password").getAsString();

								secureRandom.nextBytes(challenge);

								JsonObject authChallenge = new JsonObject();
								authChallenge.addProperty("type", "AUTH_CHALLENGE");
								authChallenge.addProperty("challenge", new String(base64Encoder.encode(challenge)));
								writer.write(authChallenge);

								state = ConnectionState.AWAITING_AUTH_PROOF;
								break;
							}

							case "AUTH_PROOF": {
								if (state != ConnectionState.AWAITING_AUTH_PROOF)
									throw new Exception("Unexpected auth proof message");

								JsonObject response = new JsonObject();
								userKey = validateAuth(username, password, challenge, base64Decoder.decode(receivedMessage.get("mac").getAsString()));
								if (userKey != null) {
									state = ConnectionState.AUTHENTICATED;
									response.addProperty("type", "AUTH_CONFIRMATION");
									writer.write(response);
								} else {
									response.addProperty("type", "AUTH_FAILURE");
									writer.write(response);
									break receive_loop;
								}
								break;
							}

							case "USER_CONFIG_READ_REQUEST": {
								if (state != ConnectionState.AUTHENTICATED)
									throw new Exception("The client isn't authenticated yet");

								JsonObject response = new JsonObject();
								response.addProperty("type", "USER_CONFIG_READ_RESPONSE");
								try {
									response.add("configuration", SecureDocument.unprotect(userKey, Files.readAllBytes(Paths.get(databaseDirPath, "users", username, "user-config.json.prot"))));
								} catch (IOException e) {
									response.add("configuration", defaultUserConfig);
								}
								writer.write(response);
								break;
							}

							case "USER_CONFIG_WRITE_REQUEST": {
								if (state != ConnectionState.AUTHENTICATED)
									throw new Exception("The client isn't authenticated yet");

								Files.write(Paths.get(databaseDirPath, "users", username, "user-config.json.prot"), SecureDocument.protect(userKey, receivedMessage.get("configuration").getAsJsonObject()));

								JsonObject confirmation = new JsonObject();
								confirmation.addProperty("type", "USER_CONFIG_WRITE_CONFIRMATION");
								writer.write(confirmation);
								break;
							}

							case "CAR_INFO_READ_REQUEST": {
								if (state != ConnectionState.AUTHENTICATED)
									throw new Exception("The client isn't authenticated yet");

								JsonObject response = new JsonObject();
								response.addProperty("type", "CAR_INFO_READ_RESPONSE");

								JsonObject info = new JsonObject();
								info.addProperty("carID", carID);
								info.addProperty("batteryLevel", 75);

								response.add("info", info);
								writer.write(response);
								break;
							}

							default:
								throw new Exception("Unrecognized message type \"" + type + "\".");
						}
					} catch (MalformedJsonException e) {
						System.out.print("Client sent a malformed message: ");
						System.out.println(e);
						break;
					} catch (IllegalStateException e) {
						System.out.print("Client sent a malformed message: ");
						System.out.println(e);
						break;
					} catch (IOException e) {
						System.out.print("Connection closed or errored out: ");
						System.out.println(e);
						break;
					} catch (Exception e) {
						System.out.println(e);
						break;
					}
				}
				clientSocket.close();
			}
		}
	}

	private static SecretKey validateAuth(String username, String password, byte[] challenge, byte[] receivedMac) throws IOException {
		if (!new File(Paths.get(databaseDirPath, "users", username).toString()).exists()) {
			System.out.println("Authentication failure: User \"" + username + "\" does not exist.");
			return null;
		}

		UserKeyFile keyFile = new UserKeyFile(new File(Paths.get(databaseDirPath, "users", username, "key.p12").toString()));
		SecretKey userKey = keyFile.loadKey(password);
		if (userKey == null) {
			System.out.println("Authentication failure: Wrong password for user \"" + username + "\".");
			return null;
		}

		byte[] realMac;
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(userKey);
			realMac = mac.doFinal(challenge);
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		} catch (InvalidKeyException e) {
			throw new Error(e);
		}

		if (Arrays.equals(receivedMac, realMac)) {
			System.out.println("User \"" + username + "\" successfully authenticated.");
			return userKey;
		} else {
			System.out.println("Authentication failure: Invalid MAC for user \"" + username + "\".");
			return null;
		}
	}
}
