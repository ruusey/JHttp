package com.test;

import com.test.net.server.SocketListener;

public class ServerTest {
	public static final transient int SERVER_PORT = 81;

	public static void main(String[] args) {
		final SocketListener listener = new SocketListener(SERVER_PORT);
		listener.start();
	}
}
