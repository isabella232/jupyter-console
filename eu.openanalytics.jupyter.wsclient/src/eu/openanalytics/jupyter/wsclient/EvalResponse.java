package eu.openanalytics.jupyter.wsclient;

public class EvalResponse {
	
	public String data;
	public boolean isError;
	
	public EvalResponse(String data, boolean isError) {
		this.data = data;
		this.isError = isError;
	}
}