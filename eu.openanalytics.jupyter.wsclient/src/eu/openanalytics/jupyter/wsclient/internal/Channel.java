package eu.openanalytics.jupyter.wsclient.internal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Channel {
	IOPub("iopub"),
	Shell("shell"),
	Control("control"),
	Stdin("stdin");

	private String value;

	private Channel(String value) {
		this.value = value;
	}

	@JsonValue
	@Override
	public String toString() {
		return this.value;
	}

	@JsonCreator
	public static Channel fromValue(String value) {
		for (Channel c: values()) {
			if (c.value.equals(value)) return c;
		}
		throw new IllegalArgumentException(value);
	}
}
