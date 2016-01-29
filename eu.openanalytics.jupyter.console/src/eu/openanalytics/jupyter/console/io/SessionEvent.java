package eu.openanalytics.jupyter.console.io;

public class SessionEvent {

	public EventType type;
	public Object data;

	public SessionEvent() {
		// Default constructor
	}

	public SessionEvent(EventType type, Object data) {
		this.type = type;
		this.data = data;
	}
}
