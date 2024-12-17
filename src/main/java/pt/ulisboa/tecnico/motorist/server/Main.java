package pt.ulisboa.tecnico.motorist.server;

<<<<<<< HEAD
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class Main {
	public static void startServer(int port) throws IOException {

        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        try (SSLServerSocket listener = (SSLServerSocket) factory.createServerSocket(port)) {
            listener.setNeedClientAuth(true);
            listener.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256" });
            listener.setEnabledProtocols(new String[] { "TLSv1.3" });
            System.out.println("listening for messages...");
            String message = "";
            InputStream is = null;
            OutputStream os = null;
            try (Socket socket = listener.accept()) {

                while (!message.equals("Exit")) {
                    try {
                        is = new BufferedInputStream(socket.getInputStream());
                        byte[] data = new byte[2048];
                        int len = is.read(data);

                        message = new String(data, 0, len);
                        os = new BufferedOutputStream(socket.getOutputStream());
                        System.out.printf("server received %d bytes: %s%n", len, message);
                        String response = message + " processed by server";
                        os.write(response.getBytes(), 0, response.getBytes().length);
                        os.flush();
                    } catch (IOException i) {
                        System.out.println(i);
                        return;
                    }
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
    }

	public static void main(String[] args) throws IOException {
		System.setProperty("javax.net.ssl.keyStore", "server.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeme");
        System.setProperty("javax.net.ssl.trustStore", "servertruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeme");
        startServer(5000);
	}
}

/* Main do duarte, resultado do merge
 * Quando fiz o merge não tive ver o que já tinhas feito e adaptar, por isso fiz esta
 * monstruosidade, quando voltar a pegar nisto tento adaptar o que tu já fizeste.
 * Deixei o que eu tinha feito para que poderem testar a comunicação entre duas VMs.
 * Eu testei e está a funcionar, se vocês não conseguirem mandem msg.
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
				} catch (IOException err) {
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
					} catch (IOException err) {
						System.out.println("Connection closed or errored out:");
						System.out.println(err);
						break;
					} catch (JSONException err) {
						System.out.println("Client sent a malformed message:");
						System.out.println(err);
						clientSocket.close();
						break;
					}
				}
			}
		}
		*/
