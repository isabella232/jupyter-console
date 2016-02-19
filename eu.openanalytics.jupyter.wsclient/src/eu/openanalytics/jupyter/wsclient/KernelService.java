package eu.openanalytics.jupyter.wsclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.openanalytics.jupyter.wsclient.util.HTTPUtil;
import eu.openanalytics.jupyter.wsclient.util.JSONUtil;

public class KernelService {

	private static final String KERNEL_SERVICE_URL = "api/kernels";
	private static final String KERNELSPEC_SERVICE_URL = "api/kernelspecs";
	
	public String[] listRunningKernels(String baseUrl) throws IOException {
		String url = HTTPUtil.concat(baseUrl, KERNEL_SERVICE_URL);
		String res = HTTPUtil.get(url, 200);
		List<String> kernelIds = new ArrayList<>();
		for (Map<String, Object> kernel: JSONUtil.toList(res)) {
			kernelIds.add(kernel.get("id").toString());
		}
		return kernelIds.toArray(new String[kernelIds.size()]);
	}
	
	@SuppressWarnings("unchecked")
	public KernelSpec[] listAvailableKernels(String baseUrl) throws IOException {
		String url = HTTPUtil.concat(baseUrl, KERNELSPEC_SERVICE_URL);
		String res = HTTPUtil.get(url, 200);
		Map<String, Object> kernelSpecs = (Map<String, Object>) JSONUtil.toMap(res).get("kernelspecs");
		List<KernelSpec> specs = new ArrayList<>();
		for (String s: kernelSpecs.keySet()) {
			Map<String, Object> specJson = (Map<String, Object>) kernelSpecs.get(s);
			KernelSpec spec = new KernelSpec();
			spec.name = specJson.get("name").toString();
			specJson = (Map<String, Object>) specJson.get("spec");
			spec.displayName = specJson.get("display_name").toString();
			spec.language = specJson.get("language").toString();
			specs.add(spec);
		}
		return specs.toArray(new KernelSpec[specs.size()]);
	}
	
	public KernelSpec getKernelSpec(String baseUrl, String kernelName) throws IOException {
		KernelSpec[] specs = listAvailableKernels(baseUrl);
		for (KernelSpec spec: specs) {
			if (spec.name.equals(kernelName)) return spec;
		}
		return null;
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
	
	public static class KernelSpec {
		public String name;
		public String displayName;
		public String language;
	}
}
