/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.wsclient;

import static eu.openanalytics.japyter.Japyter.JSON_OBJECT_MAPPER;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;

import eu.openanalytics.japyter.Japyter;
import eu.openanalytics.japyter.client.Protocol;
import eu.openanalytics.japyter.client.Protocol.BroadcastType;
import eu.openanalytics.japyter.client.Protocol.RequestMessageType;
import eu.openanalytics.japyter.model.gen.Broadcast;
import eu.openanalytics.japyter.model.gen.ExecuteRequest;
import eu.openanalytics.japyter.model.gen.Reply;
import eu.openanalytics.japyter.model.gen.Request;
import eu.openanalytics.japyter.model.gen.UserExpressions;
import eu.openanalytics.jupyter.wsclient.KernelService.SessionSpec;
import eu.openanalytics.jupyter.wsclient.internal.Channel;
import eu.openanalytics.jupyter.wsclient.internal.Message;
import eu.openanalytics.jupyter.wsclient.response.BaseMessageCallback;
import eu.openanalytics.jupyter.wsclient.response.IMessageCallback;

public class WebSocketChannel implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketChannel.class);
	
	private static final int MAX_MSG_BUFFER_SIZE = 1024 * 1024;
	private static final int MAX_MSG_SIZE = 2 * MAX_MSG_BUFFER_SIZE;
	
	private String url;
	
	private WebSocketClient client;
	private WebSocketIO socketIO;
	
	private SessionSpec sessionSpec;
	private IMessageCallback callback;
	
	static {
		JSON_OBJECT_MAPPER
			.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
			.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
	}
	
	public WebSocketChannel(String url, SessionSpec sessionSpec, IMessageCallback callback) {
		this.url = url;
		this.sessionSpec = sessionSpec;
		this.callback = callback;
		if (this.callback == null) this.callback = new NullMessageCallback();
	}

	public void connect() throws IOException {
		SslContextFactory ssl = new SslContextFactory();
		client = new WebSocketClient(ssl);
		client.getPolicy().setMaxBinaryMessageBufferSize(MAX_MSG_BUFFER_SIZE);
		client.getPolicy().setMaxBinaryMessageSize(MAX_MSG_SIZE);
		client.getPolicy().setMaxTextMessageBufferSize(MAX_MSG_BUFFER_SIZE);
		client.getPolicy().setMaxTextMessageSize(MAX_MSG_SIZE);
		try {
			client.start();
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			socketIO = new WebSocketIO();
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

	public String submit(String code) throws IOException {
		if (socketIO == null || socketIO.session == null || socketIO.session.getRemote() == null) throw new IOException("Websocket is not ready");
		return socketIO.submit(code);
	}
	
	private class WebSocketIO implements WebSocketListener {

		private Session session;
		
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
			LOGGER.error("Websocket closed unexpectedly", cause);
			callback.onChannelError(cause);
		}

		@Override
		public void onWebSocketText(String message) {
			if (LOGGER.isDebugEnabled()) LOGGER.debug("[Message recv]" + message);
			try {
				Message msg = Japyter.JSON_OBJECT_MAPPER.readValue(message, Message.class);
				
				if (msg.getChannel() == Channel.IOPub) {
					Class<? extends Broadcast> bcClass = BroadcastType.classFromValue(msg.getHeader().getMsgType());
					Broadcast broadcast = JSON_OBJECT_MAPPER.convertValue(msg.getContent(), bcClass);
					callback.onPubResult(broadcast);
				} else if (msg.getChannel() == Channel.Shell) {
					Class<? extends Reply> replyClass = null;
					for (RequestMessageType t: RequestMessageType.values()) {
						if (t.getReplyMessageType().toString().equals(msg.getHeader().getMsgType())) replyClass = t.getReplyContentClass();
					}
					Reply reply = JSON_OBJECT_MAPPER.convertValue(msg.getContent(), replyClass);
					callback.onShellReply(reply);
				}
			} catch (Exception e) {
				LOGGER.error("Failed to process message", e);
			}
		}
		
		public String submit(String code) throws IOException {
			Request request = new ExecuteRequest()
					.withCode(code)
					.withAllowStdin(true)
					.withStoreHistory(true)
					.withUserExpressions(new UserExpressions());
			RequestMessageType requestMessageType = RequestMessageType.fromRequestContentClass(request.getClass());
			
			Message message = new Message(Channel.Shell, requestMessageType).withContent(request);
			message.getHeader().setSession(sessionSpec.sessionId);
			message.getHeader().setUsername(sessionSpec.userName);
			message.getHeader().setVersion(Protocol.VERSION);
			
			String msg = Japyter.JSON_OBJECT_MAPPER.writeValueAsString(message);
			session.getRemote().sendString(msg);
			
			if (LOGGER.isDebugEnabled()) LOGGER.debug("[Message sent]" + msg);
			return message.getHeader().getMsgId();
		}
	}
	
	private static class NullMessageCallback extends BaseMessageCallback {
		
	}
}
