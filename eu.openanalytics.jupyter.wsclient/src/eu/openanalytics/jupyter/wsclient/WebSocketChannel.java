package eu.openanalytics.jupyter.wsclient;

import static eu.openanalytics.japyter.Japyter.JSON_OBJECT_MAPPER;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;

import eu.openanalytics.japyter.Japyter;
import eu.openanalytics.japyter.client.Protocol;
import eu.openanalytics.japyter.client.Protocol.RequestMessageType;
import eu.openanalytics.japyter.model.gen.ExecuteRequest;
import eu.openanalytics.japyter.model.gen.Request;
import eu.openanalytics.japyter.model.gen.UserExpressions;
import eu.openanalytics.jupyter.wsclient.internal.Channel;
import eu.openanalytics.jupyter.wsclient.internal.Message;
import eu.openanalytics.jupyter.wsclient.internal.ReplyProcessor;

public class WebSocketChannel implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketChannel.class);
	
	private String url;
	private WebSocketClient client;
	private WebSocketIO socketIO;
	
	static {
		JSON_OBJECT_MAPPER
			.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
			.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
	}
	
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
			socketIO = null;
		} catch (Exception e) {
			throw new IOException("Failed to close websocket", e);
		}
	}

	public Future<EvalResponse> eval(String code) throws IOException {
		if (socketIO == null) throw new IOException("Websocket is not ready");
		return socketIO.submit(code);
	}
	
	private class WebSocketIO implements WebSocketListener {

		private Session session;
		private String sessionId;
		private String userName;
		
		private Map<String, ReplyProcessor> replyProcessors;
		
		public WebSocketIO() {
			replyProcessors = new ConcurrentHashMap<>();
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
			this.replyProcessors.clear();
		}

		@Override
		public void onWebSocketConnect(Session session) {
			this.session = session;
		}

		@Override
		public void onWebSocketError(Throwable cause) {
			LOGGER.error("Websocket closed unexpectedly", cause);
			for (ReplyProcessor processor: replyProcessors.values()) {
				processor.handleError(cause);
			}
			replyProcessors.clear();
		}

		@Override
		public void onWebSocketText(String message) {
			//TODO
			System.out.println(message);
			try {
				Message msg = Japyter.JSON_OBJECT_MAPPER.readValue(message, Message.class);
				String msgId = msg.getParentHeader().getMsgId();
				ReplyProcessor processor = replyProcessors.get(msgId);
				if (processor == null) {
					LOGGER.warn("Ignored reply to unknown message id " + msgId + ": " + msg.toString());
					return;
				} else {
					processor.handle(msg);
					if (processor.isComplete()) replyProcessors.remove(msgId);
				}
			} catch (IOException e) {
				LOGGER.error("Failed to process message", e);
				for (ReplyProcessor processor: replyProcessors.values()) {
					processor.handleError(e);
				}
				replyProcessors.clear();
			}
		}
		
		public Future<EvalResponse> submit(String code) throws IOException {
			Request request = new ExecuteRequest().withCode(code).withUserExpressions(new UserExpressions());
			RequestMessageType requestMessageType = RequestMessageType.fromRequestContentClass(request.getClass());
			
			Message message = new Message(Channel.Shell, requestMessageType).withContent(request);
			if (sessionId != null) message.getHeader().setSession(sessionId);
			if (userName != null) message.getHeader().setUsername(userName);
			message.getHeader().setVersion(Protocol.VERSION);
			
			String msg = Japyter.JSON_OBJECT_MAPPER.writeValueAsString(message);
			session.getRemote().sendString(msg);
			
			ReplyProcessor replyProcessor = new ReplyProcessor(message, request);
			replyProcessors.put(message.getHeader().getMsgId(), replyProcessor);
			return replyProcessor.getFuture();
		}
	}
}
