package eu.openanalytics.jupyter.wsclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.openanalytics.jupyter.wsclient.util.HTTPUtil;
import eu.openanalytics.jupyter.wsclient.util.JSONUtil;

public class KernelService {

	private static final String KERNEL_SERVICE_URL = "api/kernels";
	
	public String[] listRunningKernels(String baseUrl) throws IOException {
		String url = HTTPUtil.concat(baseUrl, KERNEL_SERVICE_URL);
		String res = HTTPUtil.get(url, 200);
		List<String> kernelIds = new ArrayList<>();
		for (Map<String, Object> kernel: JSONUtil.toList(res)) {
			kernelIds.add(kernel.get("id").toString());
		}
		return kernelIds.toArray(new String[kernelIds.size()]);
	}
	
	public String launchKernel(String baseUrl, String kernelName) throws IOException {
		String url = HTTPUtil.concat(baseUrl, KERNEL_SERVICE_URL);
		String body = (kernelName == null) ? null : JSONUtil.toJSON("name", kernelName);
		String res = HTTPUtil.post(url, body, 201);
		return JSONUtil.toMap(res).get("id").toString();
	}
	
	public void stopKernel(String baseUrl, String kernelId) throws IOException {
		String url = HTTPUtil.concat(baseUrl, KERNEL_SERVICE_URL, kernelId);
		HTTPUtil.delete(url, 204);
	}
	
	public WebSocketChannel createChannel(String baseUrl, String kernelId) {
		String url = HTTPUtil.concat(baseUrl, KERNEL_SERVICE_URL, kernelId, "/channels");
		url = url.replace("http://", "ws://");
		WebSocketChannel channel = new WebSocketChannel(url);
		return channel;
	}
}
