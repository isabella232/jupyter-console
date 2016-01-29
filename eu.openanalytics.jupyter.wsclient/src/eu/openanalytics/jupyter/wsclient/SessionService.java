package eu.openanalytics.jupyter.wsclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.openanalytics.jupyter.wsclient.util.HTTPUtil;
import eu.openanalytics.jupyter.wsclient.util.JSONUtil;

public class SessionService {

	private static final String SESSION_SERVICE_URL = "api/sessions";
	
	public String[] listOpenSessions(String baseUrl) throws IOException {
		String url = HTTPUtil.concat(baseUrl, SESSION_SERVICE_URL);
		String res = HTTPUtil.get(url, 200);
		List<String> sessionIds = new ArrayList<>();
		for (Map<String, Object> session: JSONUtil.toList(res)) {
			sessionIds.add(session.get("id").toString());
		}
		return sessionIds.toArray(new String[sessionIds.size()]);
	}
	
	public String startSession(String baseUrl, String kernelName, String notebookPath) throws IOException {
		String url = HTTPUtil.concat(baseUrl, SESSION_SERVICE_URL);
		String body = "{ \"notebook\": { \"path\": \"" + notebookPath + "\" }, \"kernel\": { \"name\": \"" + kernelName + "\" } }";
		String res = HTTPUtil.post(url, body, 201);
		return JSONUtil.toMap(res).get("id").toString();
	}
	
	public void stopSession(String baseUrl, String sessionId) throws IOException {
		String url = HTTPUtil.concat(baseUrl, SESSION_SERVICE_URL, sessionId);
		HTTPUtil.delete(url, 204);
	}
}
