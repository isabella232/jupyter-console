package eu.openanalytics.jupyter.wsclient.util;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HTTPUtil {

	private static CloseableHttpClient client;
	static {
		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		client = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
	}

	public static String concat(String baseUrl, String... path) {
		StringBuilder sb = new StringBuilder(baseUrl);
		if (!baseUrl.endsWith("/") && path.length > 0) sb.append("/");
		for (int i = 0; i < path.length; i++) {
			String p = path[i];
			if (p.startsWith("/")) sb.append(p.substring(1));
			else sb.append(p);
			if (i < path.length-1) sb.append("/");
		}
		return sb.toString();
	}
	
	public static String get(String url, Integer expectedStatus) throws IOException {
		final AtomicReference<String> returnValue = new AtomicReference<>();
		get(url, expectedStatus, new IResponseConsumer() {
			@Override
			public void consume(HttpResponse response, String body) throws IOException {
				returnValue.set(body);
			}
		});
		return returnValue.get();
	}
	
	public static void get(String url, Integer expectedStatus, IResponseConsumer consumer) throws IOException {
		submit(new HttpGet(url), expectedStatus, consumer);
	}

	public static String post(String url, String data, Integer expectedStatus) throws IOException {
		final AtomicReference<String> returnValue = new AtomicReference<>();
		post(url, data, expectedStatus, new IResponseConsumer() {
			@Override
			public void consume(HttpResponse response, String body) throws IOException {
				returnValue.set(body);
			}
		});
		return returnValue.get();
	}
	
	public static void post(String url, String data, Integer expectedStatus, IResponseConsumer consumer) throws IOException {
		HttpPost post = new HttpPost(url);
		if (data != null) post.setEntity(new StringEntity(data));
		submit(post, expectedStatus, consumer);
	}
	
	public static String delete(String url, Integer expectedStatus) throws IOException {
		final AtomicReference<String> returnValue = new AtomicReference<>();
		delete(url, expectedStatus, new IResponseConsumer() {
			@Override
			public void consume(HttpResponse response, String body) throws IOException {
				returnValue.set(body);
			}
		});
		return returnValue.get();
	}
	
	public static void delete(String url, Integer expectedStatus, IResponseConsumer consumer) throws IOException {
		HttpDelete del = new HttpDelete(url);
		submit(del, null, consumer);
	}
	
	public static interface IResponseConsumer {
		public void consume(HttpResponse response, String body) throws IOException;
	}
	
	private static void submit(HttpRequestBase req, Integer expectedStatus, IResponseConsumer consumer) throws IOException {
		CloseableHttpResponse response = null;
		try {
			response = client.execute(req);
			int status = response.getStatusLine().getStatusCode();
			if (expectedStatus != null && status != expectedStatus.intValue()) {
				throw new IOException("Unexpected response from " + req.getURI() + ": " + response.getStatusLine().toString());
			}
			
			HttpEntity entity = response.getEntity();
			if (consumer == null) {
				if (entity != null) EntityUtils.consume(entity);
			} else {
				String body = (entity == null) ? null : EntityUtils.toString(entity);
				consumer.consume(response, body);
			}
		} finally {
			if (response != null) try { response.close(); } catch (IOException e) {}
		}
	}
}
