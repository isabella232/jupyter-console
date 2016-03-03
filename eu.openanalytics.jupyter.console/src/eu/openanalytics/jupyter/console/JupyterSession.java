/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.swt.graphics.ImageData;

import eu.openanalytics.jupyter.console.io.EventMonitor;
import eu.openanalytics.jupyter.console.io.EventType;
import eu.openanalytics.jupyter.console.io.SessionEvent;
import eu.openanalytics.jupyter.console.io.SimpleStreamMonitor;
import eu.openanalytics.jupyter.console.util.ImageUtil;
import eu.openanalytics.jupyter.console.util.LogUtil;
import eu.openanalytics.jupyter.console.view.GraphicsView;
import eu.openanalytics.jupyter.wsclient.API;
import eu.openanalytics.jupyter.wsclient.EvalResponse;
import eu.openanalytics.jupyter.wsclient.KernelService.KernelSpec;
import eu.openanalytics.jupyter.wsclient.WebSocketChannel;

public class JupyterSession {

	public static final String CONNECTION_METHOD = "CONNECTION_METHOD";
	public static final String CONNECTION_URL = "CONNECTION_URL";
	public static final String KERNEL_NAME = "KERNEL_NAME";
	
	public static final String DEFAULT_CONNECTION_METHOD = "tmpnb";
	public static final String DEFAULT_CONNECTION_URL = "http://tmpnb.openanalytics.eu:8000";
	public static final String DEFAULT_KERNEL_NAME = "python3";
	
	public static final String OUTPUT_STDOUT = "stdout";
	public static final String OUTPUT_STDERR = "stderr";
	public static final String OUTPUT_CONTROL = "control";
	public static final String OUTPUT_ECHO = "inputEcho";
	
	private ILaunchConfiguration configuration;
	private SimpleStreamMonitor[] outputMonitors;
	private EventMonitor eventMonitor;
	private ExecutorService asyncResponseHandler;
	
	private String nbUrl;
	private String kernelId;
	private KernelSpec kernelSpec;
	private WebSocketChannel channel;
	
	public JupyterSession(ILaunchConfiguration configuration) {
		this.configuration = configuration;
		this.outputMonitors = new SimpleStreamMonitor[] {
				new SimpleStreamMonitor(OUTPUT_STDOUT),
				new SimpleStreamMonitor(OUTPUT_STDERR),
				new SimpleStreamMonitor(OUTPUT_CONTROL),
				new SimpleStreamMonitor(OUTPUT_ECHO)
		};
		this.eventMonitor = new EventMonitor();
		this.asyncResponseHandler = Executors.newFixedThreadPool(1);
	}
	
	public void connect() throws IOException, CoreException {
		String baseUrl = configuration.getAttribute(CONNECTION_URL, "");
		String kernelName = configuration.getAttribute(KERNEL_NAME, "");
		
		nbUrl = API.getNotebookService().getNotebook(baseUrl);
		outputMonitors[2].append("Notebook server spawned: " + nbUrl);
		
		kernelSpec = API.getKernelService().getKernelSpec(nbUrl, kernelName);
		kernelId = API.getKernelService().launchKernel(nbUrl, kernelName);
		outputMonitors[2].append(kernelSpec.displayName + " kernel launched, id: " + kernelId);
		
		channel = API.getKernelService().createChannel(nbUrl, kernelId);
		channel.connect();
		outputMonitors[2].append("Session started. Enjoy!");
		
		eventMonitor.post(new SessionEvent(EventType.SessionStarted, null));
	}
	
	public KernelSpec getKernelSpec() {
		return kernelSpec;
	}
	
	public void write(String input) throws IOException {
		write(input, false);
	}
	
	public void write(String input, boolean echo) throws IOException {
		if (echo) outputMonitors[3].append(input);
		eventMonitor.post(new SessionEvent(EventType.SessionBusy, null));
		Future<EvalResponse> response = channel.eval(input);
		asyncResponseHandler.submit(new ResponseReceiver(response));
	}
	
	public void stop() throws IOException {
		stopSession();
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
	
	/*
	 * Non-public
	 * **********
	 */
	
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
				ImageData imageData = ImageUtil.getImage(response);
				if (imageData != null) GraphicsView.openWith(imageData);
				else {
					String text = (String) response.getValue("text/plain");
					if (text != null) outputMonitors[(response.isError()) ? 1 : 0].append(text);
				}
			} catch (Exception e) {
				String msg = "Error handling response: " + e.getMessage();
				outputMonitors[1].append(msg);
				LogUtil.error(msg, e);
			}
			eventMonitor.post(new SessionEvent(EventType.SessionIdle, null));
		}
	}
}
