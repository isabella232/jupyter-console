package eu.openanalytics.jupyter.wsclient.internal;

import static eu.openanalytics.japyter.Japyter.JSON_OBJECT_MAPPER;

import java.util.concurrent.Future;

import eu.openanalytics.japyter.client.Protocol.BroadcastType;
import eu.openanalytics.japyter.client.Protocol.RequestMessageType;
import eu.openanalytics.japyter.model.gen.Broadcast;
import eu.openanalytics.japyter.model.gen.Data_;
import eu.openanalytics.japyter.model.gen.Error;
import eu.openanalytics.japyter.model.gen.ExecuteReply;
import eu.openanalytics.japyter.model.gen.ExecuteResult;
import eu.openanalytics.japyter.model.gen.Reply;
import eu.openanalytics.japyter.model.gen.Request;
import eu.openanalytics.jupyter.wsclient.EvalResponse;

public class ReplyProcessor {

	private Message requestMessage;
	private Request request;
	private ResponseFuture future;
	
	private EvalResponse response;
	private boolean complete;
	
	public ReplyProcessor(Message requestMessage, Request request) {
		this.requestMessage = requestMessage;
		this.request = request;
		this.future = new ResponseFuture();
		this.complete = false;
	}
	
	public void handle(Message msg) {
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
				future.setResponse(response);
				complete = true;
			}
		}
	}
	
	public void handleError(Throwable cause) {
		future.setResponse(new EvalResponse("Unexpected error: " + cause.getMessage(), true));
		complete = true;
	}
	
	public Future<EvalResponse> getFuture() {
		return future;
	}
	
	public boolean isComplete() {
		return complete;
	}
}
