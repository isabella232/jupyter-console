package eu.openanalytics.jupyter.wsclient;

import static eu.openanalytics.japyter.Japyter.JSON_OBJECT_MAPPER;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import eu.openanalytics.japyter.Japyter;
import eu.openanalytics.japyter.client.Protocol;
import eu.openanalytics.japyter.client.Protocol.BroadcastType;
import eu.openanalytics.japyter.client.Protocol.RequestMessageType;
import eu.openanalytics.japyter.model.gen.Broadcast;
import eu.openanalytics.japyter.model.gen.Data_;
import eu.openanalytics.japyter.model.gen.Error;
import eu.openanalytics.japyter.model.gen.ExecuteReply;
import eu.openanalytics.japyter.model.gen.ExecuteRequest;
import eu.openanalytics.japyter.model.gen.ExecuteResult;
import eu.openanalytics.japyter.model.gen.Reply;
import eu.openanalytics.japyter.model.gen.Request;
import eu.openanalytics.jupyter.wsclient.internal.Channel;
import eu.openanalytics.jupyter.wsclient.internal.Message;

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

	public Future<EvalResponse> evalAsync(String code) throws IOException {
		if (socketIO == null) throw new IOException("Websocket is not ready");
		return socketIO.submit(code);
	}
	
	public static class EvalResponse {
		
		public String data;
		public boolean isError;
		
		public EvalResponse(String data, boolean isError) {
			this.data = data;
			this.isError = isError;
		}
	}
	
	//TODO Proper support for mixed/queued submissions
	private class WebSocketIO implements WebSocketListener {

		private Session session;
		private String sessionId;
		private String userName;
		private Semaphore locker;
		private EvalResponse response;
		
		public WebSocketIO() {
			locker = new Semaphore(0);
			// These must not be null, or JSON parsing will fail!
			sessionId = UUID.randomUUID().toString();
			userName = "username";
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

		@Override
		public void onWebSocketText(String message) {
			try {
				Message msg = Japyter.JSON_OBJECT_MAPPER.readValue(message, Message.class);
				
				if (msg.getChannel() == Channel.IOPub) {
					Class<? extends Broadcast> bcClass = BroadcastType.classFromValue(msg.getHeader().getMsgType());
					Broadcast bc = JSON_OBJECT_MAPPER.convertValue(msg.getContent(), bcClass);
					if (bc instanceof ExecuteResult) {
						Data_ data = ((ExecuteResult) bc).getData();
						Object retVal = data.getAdditionalProperties().get("text/plain");
						response = new EvalResponse(retVal == null ? "" : retVal.toString(), false);
					} else if (bc instanceof Error) {
						response = new EvalResponse(((Error) bc).getEvalue(), true);
					}
				} else if (msg.getChannel() == Channel.Shell) {
					Class<? extends Reply> replyClass = null;
					for (RequestMessageType t: RequestMessageType.values()) {
						if (t.getReplyMessageType().toString().equals(msg.getHeader().getMsgType())) replyClass = t.getReplyContentClass();
					}
					Reply reply = JSON_OBJECT_MAPPER.convertValue(msg.getContent(), replyClass);
					if (reply instanceof ExecuteReply) {
						if (response == null) {
							ExecuteReply.Status status = ((ExecuteReply) reply).getStatus();
							if (status == ExecuteReply.Status.OK) {
								response = new EvalResponse(null, false);
							} else {
								response = new EvalResponse("Status: " + status, true);
							}
						}
						locker.release();
					}
				}
			} catch (IOException e) {
				response = new EvalResponse("Failed to read reply: " + e.getMessage(), true);
			}
		}
		
		public Future<EvalResponse> submit(String code) throws IOException {
			Request request = new ExecuteRequest().withCode(code);
			RequestMessageType requestMessageType = RequestMessageType.fromRequestContentClass(request.getClass());
			
			Message message = new Message(Channel.Shell, requestMessageType).withContent(request);
			if (sessionId != null) message.getHeader().setSession(sessionId);
			if (userName != null) message.getHeader().setUsername(userName);
			message.getHeader().setVersion(Protocol.VERSION);
			
			String msg = Japyter.JSON_OBJECT_MAPPER.writeValueAsString(message);
			session.getRemote().sendString(msg);
			return new ResponseFuture();
		}
		
		private class ResponseFuture implements Future<EvalResponse> {
			
			private boolean done = false;
			
			@Override
			public boolean isDone() {
				return done;
			}
			
			@Override
			public boolean isCancelled() {
				return false;
			}
			
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}
			
			@Override
			public EvalResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				if (locker.tryAcquire(timeout, unit)) return doGet();
				else return null;
			}
			
			@Override
			public EvalResponse get() throws InterruptedException, ExecutionException {
				locker.acquire();
				return doGet();
			}
			
			private EvalResponse doGet() {
				done = true;
				EvalResponse res = response;
				response = null;
				return res;
			}
		}
	}
}
