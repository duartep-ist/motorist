package pt.ulisboa.tecnico.motorist.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.google.gson.JsonObject;
import com.google.gson.stream.MalformedJsonException;

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

				// Handle messages from this connection
				while (true) {
					try {
						JsonObject receivedMessage = reader.read();
						System.out.print("Received ");
						System.out.println(receivedMessage.toString());
						String type = receivedMessage.get("type").getAsString();
						switch (type) {
							case "echo":
								JsonObject response = new JsonObject();
								response.add("message", receivedMessage);
								System.out.print("Sending ");
								System.out.println(response.toString());
								writer.write(response);
								break;
						
							default:
								System.out.println("Unrecognized message type \"" + type + "\".");
								clientSocket.close();
								break;
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
}
