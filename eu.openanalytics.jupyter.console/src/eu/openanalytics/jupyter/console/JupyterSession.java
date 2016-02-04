package eu.openanalytics.jupyter.console;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;

import eu.openanalytics.jupyter.console.io.EventMonitor;
import eu.openanalytics.jupyter.console.io.EventType;
import eu.openanalytics.jupyter.console.io.SessionEvent;
import eu.openanalytics.jupyter.console.io.Signal;
import eu.openanalytics.jupyter.console.io.SimpleStreamMonitor;
import eu.openanalytics.jupyter.wsclient.API;
import eu.openanalytics.jupyter.wsclient.WebSocketChannel;
import eu.openanalytics.jupyter.wsclient.WebSocketChannel.EvalResponse;

public class JupyterSession {

	public static final String CONNECTION_METHOD = "CONNECTION_METHOD";
	public static final String CONNECTION_URL = "CONNECTION_URL";
	public static final String KERNEL_NAME = "KERNEL_NAME";
	
	public static final String OUTPUT_STDOUT = "stdout";
	public static final String OUTPUT_STDERR = "stderr";
	public static final String OUTPUT_CONTROL = "control";
	
	private ILaunchConfiguration configuration;
	private SimpleStreamMonitor[] outputMonitors;
	private EventMonitor eventMonitor;
	private ExecutorService asyncResponseHandler;
	
	private String nbUrl;
	private String kernelId;
	private WebSocketChannel channel;
	
	public JupyterSession(ILaunchConfiguration configuration) {
		this.configuration = configuration;
		this.outputMonitors = new SimpleStreamMonitor[] {
				new SimpleStreamMonitor(OUTPUT_STDOUT),
				new SimpleStreamMonitor(OUTPUT_STDERR),
				new SimpleStreamMonitor(OUTPUT_CONTROL)
		};
		this.eventMonitor = new EventMonitor();
		this.asyncResponseHandler = Executors.newFixedThreadPool(1);
	}
	
	public void connect() throws IOException, CoreException {
		String baseUrl = configuration.getAttribute(CONNECTION_URL, "");
		String kernelName = configuration.getAttribute(KERNEL_NAME, "");
		
		nbUrl = API.getNotebookService().spawn(baseUrl);
		outputMonitors[2].append("Notebook server spawned: " + nbUrl);
		
		kernelId = API.getKernelService().launchKernel(nbUrl, kernelName);
		outputMonitors[2].append("Kernel launched: " + kernelName + ", id: " + kernelId);
		
		channel = API.getKernelService().createChannel(nbUrl, kernelId);
		channel.connect();
		outputMonitors[2].append("Session started. Enjoy!");
		
		eventMonitor.post(new SessionEvent(EventType.SessionStarted, null));
	}
	
	public void write(String input) throws IOException {
		Future<EvalResponse> response = channel.evalAsync(input);
		asyncResponseHandler.submit(new ResponseReceiver(response));
	}
	
	public void signal(Signal sig) throws IOException {
		if (sig == Signal.StopSession) stopSession();
	}
	
	public void addStreamListener(IStreamListener listener) {
		for (SimpleStreamMonitor outputMonitor: outputMonitors) outputMonitor.addListener(listener);
	}
	
	public void removeStreamListener(IStreamListener listener) {
		for (SimpleStreamMonitor outputMonitor: outputMonitors) outputMonitor.removeListener(listener);
	}
	
	public EventMonitor getEventMonitor() {
		return eventMonitor;
	}
	
	private void stopSession() throws IOException {
		channel.close();
		API.getKernelService().stopKernel(nbUrl, kernelId);
		outputMonitors[2].append("Session stopped.");
		for (SimpleStreamMonitor outputMonitor: outputMonitors) outputMonitor.dispose();
		eventMonitor.post(new SessionEvent(EventType.SessionStopped, null));
		eventMonitor.dispose();
		asyncResponseHandler.shutdown();
	}
	
	private class ResponseReceiver implements Runnable {
		
		private Future<EvalResponse> futureResponse;
		
		public ResponseReceiver(Future<EvalResponse> futureResponse) {
			this.futureResponse = futureResponse;
		}
		
		@Override
		public void run() {
			try {
				EvalResponse response = futureResponse.get();
				String data = (response.data == null) ? "" : response.data;
				if (response.isError) outputMonitors[1].append(data);
				else outputMonitors[0].append(data);
			} catch (Exception e) {
				outputMonitors[1].append("Socket I/O error: " + e.getMessage());
			}
		}
	}
}
