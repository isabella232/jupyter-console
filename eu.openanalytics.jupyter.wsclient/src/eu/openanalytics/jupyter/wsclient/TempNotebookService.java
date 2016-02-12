package eu.openanalytics.jupyter.wsclient;

import java.io.IOException;

import eu.openanalytics.jupyter.wsclient.util.HTTPUtil;
import eu.openanalytics.jupyter.wsclient.util.JSONUtil;

public class TempNotebookService {

	private static final String SPAWN_SERVER_URL = "api/spawn";
	
	private String activeNotebookURL;
	
	public String spawn(String baseUrl) throws IOException {
		String url = HTTPUtil.concat(baseUrl, SPAWN_SERVER_URL);
		String res = HTTPUtil.post(url, null, 200);
		String path = JSONUtil.toMap(res).get("url").toString();
		activeNotebookURL = HTTPUtil.concat(baseUrl, path);
		return activeNotebookURL;
	}
	
	public String getNotebook(String baseUrl) throws IOException {
		if (activeNotebookURL == null) spawn(baseUrl);
		return activeNotebookURL;
	}
}
