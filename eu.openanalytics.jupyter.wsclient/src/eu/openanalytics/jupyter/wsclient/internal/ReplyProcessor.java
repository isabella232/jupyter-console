/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.wsclient.internal;

import static eu.openanalytics.japyter.Japyter.JSON_OBJECT_MAPPER;

import java.util.Map;
import java.util.concurrent.Future;

import eu.openanalytics.japyter.client.Protocol.BroadcastType;
import eu.openanalytics.japyter.client.Protocol.RequestMessageType;
import eu.openanalytics.japyter.model.gen.Broadcast;
import eu.openanalytics.japyter.model.gen.Data;
import eu.openanalytics.japyter.model.gen.Data_;
import eu.openanalytics.japyter.model.gen.DisplayData;
import eu.openanalytics.japyter.model.gen.Error;
import eu.openanalytics.japyter.model.gen.ExecuteReply;
import eu.openanalytics.japyter.model.gen.ExecuteResult;
import eu.openanalytics.japyter.model.gen.Reply;
import eu.openanalytics.japyter.model.gen.Request;
import eu.openanalytics.japyter.model.gen.Stream;
import eu.openanalytics.jupyter.wsclient.EvalResponse;

public class ReplyProcessor {

	private ResponseFuture future;
	private StringBuilder streamingText;
	private EvalResponse response;
	private boolean complete;
	
	private static final String MIMETYPE_TEXT = "text/plain";

	public ReplyProcessor(Message requestMessage, Request request) {
		this.future = new ResponseFuture();
		this.streamingText = new StringBuilder();
		this.response = new EvalResponse();
		this.complete = false;
	}
	
	public void handle(Message msg) {
		if (msg.getChannel() == Channel.IOPub) {
			Class<? extends Broadcast> bcClass = BroadcastType.classFromValue(msg.getHeader().getMsgType());
			Broadcast bc = JSON_OBJECT_MAPPER.convertValue(msg.getContent(), bcClass);
			if (bc instanceof ExecuteResult) {
				Data_ data = ((ExecuteResult) bc).getData();
				addValues(data.getAdditionalProperties());
			} else if (bc instanceof DisplayData) {
				Data data = ((DisplayData) bc).getData();
				addValues(data.getAdditionalProperties());
			} else if (bc instanceof Stream) {
				String text = ((Stream) bc).getText();
				streamingText.append(text);
			} else if (bc instanceof Error) {
				addError(((Error) bc).getEvalue());
			}
		} else if (msg.getChannel() == Channel.Shell) {
			Class<? extends Reply> replyClass = null;
			for (RequestMessageType t: RequestMessageType.values()) {
				if (t.getReplyMessageType().toString().equals(msg.getHeader().getMsgType())) replyClass = t.getReplyContentClass();
			}
			Reply reply = JSON_OBJECT_MAPPER.convertValue(msg.getContent(), replyClass);
			if (reply instanceof ExecuteReply) {
				ExecuteReply.Status status = ((ExecuteReply) reply).getStatus();
				if (status == ExecuteReply.Status.OK) {
					if (streamingText.length() > 0) response.addValue(MIMETYPE_TEXT, streamingText.toString());
				} else {
					addError(((ExecuteReply) reply).getEvalue());
				}
				future.setResponse(response);
				complete = true;
			}
		}
	}
	
	public void handleError(Throwable cause) {
		future.setResponse(new EvalResponse("Unexpected error: " + cause.getMessage(), MIMETYPE_TEXT, true));
		complete = true;
	}
	
	public Future<EvalResponse> getFuture() {
		return future;
	}
	
	public boolean isComplete() {
		return complete;
	}
	
	private void addValues(Map<String, Object> map) {
		for (String key: map.keySet()) {
			response.addValue(key, map.get(key));
		}
	}
	
	private void addError(String evalue) {
		if (evalue != null && evalue.endsWith("\n")) evalue = evalue.substring(0, evalue.length() - 1);
		response.addValue(MIMETYPE_TEXT, evalue);
		response.setError(true);
	}
}
