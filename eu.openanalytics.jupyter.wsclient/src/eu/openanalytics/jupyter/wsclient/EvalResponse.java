package eu.openanalytics.jupyter.wsclient;

import java.util.HashMap;
import java.util.Map;

public class EvalResponse {
	
	private Map<String, Object> values;
	private boolean isError;
	
	public EvalResponse() {
		values = new HashMap<>();
	}
	
	public EvalResponse(Object value, String mimetype, boolean isError) {
		this();
		this.values.put(mimetype, value);
		this.isError = isError;
	}
	
	public void addValue(String mimetype, Object value) {
		values.put(mimetype, value);
	}
	
	public void setError(boolean isError) {
		this.isError = isError;
	}
	
	public Object getValue(String mimetype) {
		return values.get(mimetype);
	}
	
	public boolean isError() {
		return isError;
	}
}