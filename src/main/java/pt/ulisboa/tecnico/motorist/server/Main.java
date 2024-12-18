package pt.ulisboa.tecnico.motorist.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import pt.ulisboa.tecnico.motorist.common.JSONStreamReader;
import pt.ulisboa.tecnico.motorist.common.JSONStreamWriter;

public class Main {
	public static void main(String[] args) throws IOException {
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
				while (true) {
					try {
						JSONObject receivedMessage = reader.read();
						System.out.print("Received ");
						System.out.println(receivedMessage.toString());
						String type = receivedMessage.getString("type");
						switch (type) {
							case "echo":
								JSONObject response = new JSONObject();
								response.put("message", receivedMessage);
								System.out.print("Sending ");
								System.out.println(response.toString());
								writer.write(response);
								break;
						
							default:
								System.out.println("Unrecognized message type \"" + type + "\".");
								clientSocket.close();
								break;
						}
					} catch (IOException e) {
						System.out.print("Connection closed or errored out: ");
						System.out.println(e);
						break;
					} catch (JSONException e) {
						System.out.print("Client sent a malformed message: ");
						System.out.println(e);
						clientSocket.close();
						break;
					}
				}
			}
		}
	}
}
