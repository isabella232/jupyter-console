package eu.openanalytics.jupyter.console;

import java.io.IOException;

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
		EvalResponse response = channel.eval(input);
		String data = (response.data == null) ? "" : response.data;
		if (response.isError) outputMonitors[1].append(data);
		else outputMonitors[0].append(data);
	}
	
	public void signal(Signal sig) throws IOException {
		if (sig == Signal.StopSession) {
			channel.close();
			API.getKernelService().stopKernel(nbUrl, kernelId);
			outputMonitors[2].append("Session stopped.");
			for (SimpleStreamMonitor outputMonitor: outputMonitors) outputMonitor.dispose();
			eventMonitor.post(new SessionEvent(EventType.SessionStopped, null));
			eventMonitor.dispose();
		}
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
}
