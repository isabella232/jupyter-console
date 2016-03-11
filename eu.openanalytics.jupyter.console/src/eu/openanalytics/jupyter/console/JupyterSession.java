/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.swt.graphics.ImageData;

import eu.openanalytics.japyter.model.gen.Broadcast;
import eu.openanalytics.japyter.model.gen.Data;
import eu.openanalytics.japyter.model.gen.Data_;
import eu.openanalytics.japyter.model.gen.DisplayData;
import eu.openanalytics.japyter.model.gen.Error;
import eu.openanalytics.japyter.model.gen.ExecuteResult;
import eu.openanalytics.japyter.model.gen.Status;
import eu.openanalytics.japyter.model.gen.Status.ExecutionState;
import eu.openanalytics.japyter.model.gen.Stream;
import eu.openanalytics.jupyter.console.io.EventMonitor;
import eu.openanalytics.jupyter.console.io.EventType;
import eu.openanalytics.jupyter.console.io.IEventListener;
import eu.openanalytics.jupyter.console.io.SessionEvent;
import eu.openanalytics.jupyter.console.io.SimpleStreamMonitor;
import eu.openanalytics.jupyter.console.util.ImageUtil;
import eu.openanalytics.jupyter.console.view.GraphicsView;
import eu.openanalytics.jupyter.wsclient.API;
import eu.openanalytics.jupyter.wsclient.KernelService.KernelSpec;
import eu.openanalytics.jupyter.wsclient.KernelService.SessionSpec;
import eu.openanalytics.jupyter.wsclient.WebSocketChannel;
import eu.openanalytics.jupyter.wsclient.response.BaseMessageCallback;

public class JupyterSession {

	public static final String CONNECTION_METHOD = "CONNECTION_METHOD";
	public static final String CONNECTION_URL = "CONNECTION_URL";
	public static final String KERNEL_NAME = "KERNEL_NAME";
	
	public static final String DEFAULT_CONNECTION_METHOD = "tmpnb";
	public static final String DEFAULT_CONNECTION_URL = "https://tmpnb.org";
	public static final String DEFAULT_KERNEL_NAME = "python3";
	
	public static final String OUTPUT_STDOUT = "stdout";
	public static final String OUTPUT_STDERR = "stderr";
	public static final String OUTPUT_CONTROL = "control";
	public static final String OUTPUT_ECHO = "inputEcho";
	
	private ILaunchConfiguration configuration;
	private SimpleStreamMonitor[] outputMonitors;
	private EventMonitor eventMonitor;
	
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
	}
	
	public void connect() throws IOException, CoreException {
		String baseUrl = configuration.getAttribute(CONNECTION_URL, "");
		String kernelName = configuration.getAttribute(KERNEL_NAME, "");
		
		nbUrl = API.getNotebookService().getNotebook(baseUrl);
		outputMonitors[2].append("Notebook server spawned: " + nbUrl);
		
		kernelSpec = API.getKernelService().getKernelSpec(nbUrl, kernelName);
		kernelId = API.getKernelService().launchKernel(nbUrl, kernelName);
		outputMonitors[2].append(kernelSpec.displayName + " kernel launched, id: " + kernelId);
		
		eventMonitor.post(new SessionEvent(EventType.SessionStarting, null));
		SessionSpec spec = new SessionSpec();
		spec.kernelId = kernelId;
		channel = API.getKernelService().createChannel(nbUrl, spec, new MessageCallback());
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
		try {
			channel.submit(input);
		} catch (IOException e) {
			eventMonitor.post(new SessionEvent(EventType.SessionIdle, null));
			throw e;
		}
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
	
	public void addEventListener(IEventListener listener) {
		eventMonitor.addListener(listener);
	}
	
	public void removeEventListener(IEventListener listener) {
		eventMonitor.removeListener(listener);
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
	}
	
	private class MessageCallback extends BaseMessageCallback {
		
		@Override
		public void onPubResult(Broadcast bc) {
			if (bc instanceof Status) {
				ExecutionState state = ((Status) bc).getExecutionState();
				if (state == ExecutionState.BUSY) eventMonitor.post(new SessionEvent(EventType.SessionBusy, null));
				if (state == ExecutionState.IDLE) eventMonitor.post(new SessionEvent(EventType.SessionIdle, null));
			} else if (bc instanceof ExecuteResult) {
				Data_ data = ((ExecuteResult) bc).getData();
				processTextOrImage(data.getAdditionalProperties());
			} else if (bc instanceof DisplayData) {
				Data data = ((DisplayData) bc).getData();
				processTextOrImage(data.getAdditionalProperties());
			} else if (bc instanceof Stream) {
				String text = ((Stream) bc).getText();
				if (text != null) outputMonitors[0].append(text);
			} else if (bc instanceof Error) {
				String text = ((Error) bc).getEvalue();
				if (text != null) outputMonitors[1].append(text);
			}
		}
		
		@Override
		public void onChannelError(Throwable cause) {
			outputMonitors[1].append("Websocket channel error: " + cause.getMessage());
			try { stopSession(); } catch (IOException e) {}
		}
		
		private void processTextOrImage(Map<String, Object> response) {
			try {
				ImageData imageData = ImageUtil.getImage(response);
				if (imageData != null) {
					GraphicsView.openWith(imageData);
					return;
				}
			} catch (IOException e) {
				outputMonitors[1].append("Failed to render image: " + e.getMessage());
			}
			String text = (String) response.get("text/plain");
			if (text != null) outputMonitors[0].append(text);
		}
	}
}
