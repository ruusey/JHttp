package com.test.net.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class SocketListener extends Thread {

	private static final transient String OUTPUT_HEADERS = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n"
			+ "Content-Length: ";
	private static final transient String OUTPUT_END_OF_HEADERS = "\r\n\r\n";
	private ServerSocket serverSocket;
	private boolean shutdown = false;

	public SocketListener(int port) {
		Logger.info("Creating local HTTP server at port " + port);
		try {
			this.serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			Logger.error("Failed to create server socket. Reason: " + e.getMessage());
		}
	}

	public void run() {
		Logger.info("Starting HTTP server...");
		while (!this.shutdown) {
			final Map<String, String> request = new HashMap<>();
			Socket clientSocket = null;
			PrintWriter out = null;
			try {
				clientSocket = this.serverSocket.accept();
				out = new PrintWriter(clientSocket.getOutputStream(), true);
				final BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String inputLine = null;
				if (in != null) {
					while (!(inputLine = in.readLine()).equals("")) {
						final String[] split = inputLine.split(":");
						if (split.length >= 2) {
							request.put(split[0], split[1].trim());
						} else {
							try {
								final String[] requestResourceParts = inputLine.split("HTTP");
								final String method = requestResourceParts[0].split(" ")[0].trim();
								final String resource = requestResourceParts[0].split(" ")[1].trim();
								final String httpVersion = requestResourceParts[1].substring(1).trim();
								request.put("Method", method);
								request.put("Resource", resource);
								request.put("Version", httpVersion);
							} catch (Exception e) {
								Logger.error(
										"Failed to extract some metadata from HTTP request. Reason: " + e.getMessage());
							}

						}
					}
					final StringBuilder payload = new StringBuilder();
					while (in.ready()) {
						payload.append((char) in.read());
					}
					request.put("Body", payload.toString());
				}

			} catch (Exception e) {
				Logger.error("Failed to get any information from HTTP request. Reason: " + e.getMessage());
			}
			// If the request had data
			if (request.size() > 0) {
				Logger.info("Recieved valid HTTP Request: " + request.get("Resource"));
				this.respondToRequest(out, request);
			}
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				Logger.error(e.getMessage());
			}
		}
	}

	private void respondToRequest(final PrintWriter out, final Map<String, String> request) {
		if (out == null)
			return;
		try {
			final String contentToWrite = this.getFormattedContent(request);
			final String fullMsg = OUTPUT_HEADERS + contentToWrite.length() + OUTPUT_END_OF_HEADERS + contentToWrite;
			out.write(fullMsg);
			out.flush();
			Logger.info("Succesfully wrote HTTP response.");
		} catch (Exception e) {
			Logger.error("Failed to send response to client. Reason: " + e.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private String getFormattedContent(final Map<String, String> request) {
		final StringBuilder builder = new StringBuilder();
		builder.append("<html>");
		builder.append("<head>");
		builder.append("<title>");
		builder.append("Basic Java Web Server");
		builder.append("</title>");
		builder.append("</head>");
		builder.append("<body>");
		for (final Map.Entry<String, String> entry : request.entrySet()) {
			builder.append("<p>" + entry.getKey() + "=" + entry.getValue() + "</p>");
		}

		builder.append("</body>");
		builder.append("</html>");

		return builder.toString();
	}

	private static class Logger {
		public static void error(final String msg) {
			final Timestamp now = new Timestamp(System.currentTimeMillis());
			System.err.println("[" + now.toString() + "] ERROR: " + msg);
		}

		public static void info(final String msg) {
			final Timestamp now = new Timestamp(System.currentTimeMillis());
			System.out.println("[" + now.toString() + "] INFO: " + msg);
		}

		public static void info(final Map<String, String> msg) {
			final Timestamp now = new Timestamp(System.currentTimeMillis());
			for (Map.Entry<String, String> entry : msg.entrySet()) {
				System.out.println("[" + now.toString() + "] INFO: " + entry.getKey() + "=" + entry.getValue());
			}
		}
	}
}
