package eu.openanalytics.jupyter.wsclient;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import eu.openanalytics.jupyter.wsclient.util.JSONUtil;

public class WebSocketChannel implements Closeable {

	private String url;
	private WebSocketClient client;
	private WebSocketIO socketIO;
	
	public WebSocketChannel(String url) {
		this.url = url;
	}

	public void connect() throws IOException {
		client = new WebSocketClient();
		socketIO = new WebSocketIO();
		try {
			client.start();
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			client.connect(socketIO, new URI(url), request).get();
		} catch (Exception e) {
			throw new IOException("Failed to start websocket", e);
		}
	}
	
	@Override
	public void close() throws IOException {
		try {
			client.stop();
		} catch (Exception e) {
			throw new IOException("Failed to close websocket", e);
		}
	}
	
	public EvalResponse eval(String code) throws IOException {
		if (socketIO == null) throw new IOException("Websocket is not ready");
		return socketIO.submitSync(code);
	}
	
	public static class EvalResponse {
		
		public String data;
		public boolean isError;
		
		public EvalResponse(String data, boolean isError) {
			this.data = data;
			this.isError = isError;
		}
	}
	
	private class WebSocketIO implements WebSocketListener {

		private Session session;
		
		private Semaphore locker;
		private EvalResponse response;
		
		public WebSocketIO() {
			locker = new Semaphore(0);
		}
		
		@Override
		public void onWebSocketBinary(byte[] payload, int offset, int len) {
			// Ignore binary messages.
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			this.session = null;
		}

		@Override
		public void onWebSocketConnect(Session session) {
			this.session = session;
		}

		@Override
		public void onWebSocketError(Throwable cause) {
			cause.printStackTrace(System.err);
		}

		//TODO Reuse the Japyter protocol here
		
		@SuppressWarnings("unchecked")
		@Override
		public void onWebSocketText(String message) {
			Map<String, Object> map = JSONUtil.toMap(message);
			String channel = map.get("channel").toString();
			String msgType =  map.get("msg_type").toString();
			if (channel.equals("iopub") && msgType.equals("execute_result")) {
				Map<String, Object> data = (Map<String, Object>) ((Map<String, Object>) map.get("content")).get("data");
				response = new EvalResponse(data.get("text/plain").toString(), false);
			} else if (channel.equals("iopub") && msgType.equals("error")) {
				response = new EvalResponse(((Map<String, Object>) map.get("content")).get("evalue").toString(), true);
			} else if (channel.equals("shell") && msgType.equals("execute_reply")) {
				String status = ((Map<String, Object>) map.get("content")).get("status").toString();
				if (response == null) {
					if (status.equals("ok")) response = new EvalResponse(null, false);
					else response = new EvalResponse("Status: " + status, true);
				}
				locker.release();
			}
		}
		
		public EvalResponse submitSync(String code) throws IOException {
			String msg = "{\"metadata\":{},"
					+ "\"content\":{\"code\":\"" + code + "\",\"silent\":false},"
					+ "\"header\":{\"msg_id\":\"japyter." + UUID.randomUUID().toString() + "\",\"msg_type\":\"execute_request\"},"
					+ "\"parent_header\":{}}";
			session.getRemote().sendString(msg);
			
			try { locker.acquire(); } catch (InterruptedException e) {}
			EvalResponse res = response;
			response = null;
			return res;
		}
	}
}
