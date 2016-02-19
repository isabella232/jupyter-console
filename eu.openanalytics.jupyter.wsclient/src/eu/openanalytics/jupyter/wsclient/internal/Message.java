package eu.openanalytics.jupyter.wsclient.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import eu.openanalytics.japyter.Japyter;
import eu.openanalytics.japyter.client.Protocol.RequestMessageType;
import eu.openanalytics.japyter.model.gen.Header;
import eu.openanalytics.japyter.model.gen.Reply;
import eu.openanalytics.japyter.model.gen.Request;

@JsonPropertyOrder({
    "channel",
    "header",
    "parent_header",
    "content",
    "metadata"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

	@JsonProperty("channel")
	private Channel channel;
	@JsonProperty("header")
    private Header header;
	@JsonProperty("parent_header")
    private Header parentHeader;
	@JsonProperty("metadata")
    private Map<String, Object> metadata;
	@JsonProperty("content")
    private Map<String, Object> content;
    
    public Message() {
        header = new Header();
        parentHeader = new Header();
        metadata = new HashMap<>();
        content = new HashMap<>();
    }

    /**
     * Creates a new empty message with a random ID and the specified message type.
     */
    public Message(final Channel channel, final RequestMessageType type) {
        this();
        withChannel(channel);
        getHeader().withMsgType(type.toString()).withMsgId(randomId());
    }
    
    public Message withChannel(Channel channel) {
    	this.channel = channel;
    	return this;
    }
    
    public Message withHeader(final Header header) {
        this.header = header;
        return this;
    }

    public Message withParentHeader(final Header parentHeader) {
        this.parentHeader = parentHeader;
        return this;
    }

    public Message withMetadata(final Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Message withContent(final Map<String, Object> content) {
        this.content = content;
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public Message withContent(final Request content) {
        this.content = Japyter.JSON_OBJECT_MAPPER.convertValue(content, Map.class);
        return this;
    }

    @SuppressWarnings("unchecked")
    public Message withContent(final Reply content) {
        this.content = Japyter.JSON_OBJECT_MAPPER.convertValue(content, Map.class);
        return this;
    }
    
    @JsonProperty("channel")
    public Channel getChannel() {
		return channel;
	}
    
    @JsonProperty("channel")
    public void setChannel(Channel channel) {
		this.channel = channel;
	}
    
    @JsonProperty("header")
    public Header getHeader() {
		return header;
	}
    
    @JsonProperty("header")
    public void setHeader(Header header) {
		this.header = header;
	}
    
    @JsonProperty("parent_header")
    public Header getParentHeader() {
		return parentHeader;
	}
    
    @JsonProperty("parent_header")
    public void setParentHeader(Header parentHeader) {
		this.parentHeader = parentHeader;
	}
    
    @JsonProperty("metadata")
    public Map<String, Object> getMetadata() {
		return metadata;
	}
    
    @JsonProperty("metadata")
    public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}
    
    @JsonProperty("content")
    public Map<String, Object> getContent() {
		return content;
	}
    
    @JsonProperty("content")
    public void setContent(Map<String, Object> content) {
		this.content = content;
	}
    
    private String randomId() {
    	return UUID.randomUUID().toString();
    }
}
