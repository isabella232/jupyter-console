/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console.util;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.console.IConsole;

import eu.openanalytics.jupyter.console.Activator;
import eu.openanalytics.jupyter.console.JupyterConsole;
import eu.openanalytics.jupyter.console.io.EventType;
import eu.openanalytics.jupyter.console.io.IEventListener;
import eu.openanalytics.jupyter.console.io.SessionEvent;

public class CmdDelegatingAction extends Action {

	private JupyterConsole console;
	private String commandId;
	private IEventListener sessionListener;
	
	public CmdDelegatingAction(IConsole console,
			final String commandId,
			final ImageDescriptor img,
			final ImageDescriptor imgConsoleStopped,
			final String tooltip,
			final boolean requireRunningConsole) {
		
		super();
		this.console = (JupyterConsole) console;
		this.commandId = commandId;
		
		setImageDescriptor(img);
		setToolTipText(tooltip);
		setEnabled(requireRunningConsole);
		
		sessionListener = new IEventListener() {
			@Override
			public void handle(SessionEvent event) {
				if (event.type == EventType.SessionStopped) {
					setImageDescriptor(imgConsoleStopped);
					setEnabled(!requireRunningConsole);
				}
			}
		};
		this.console.getSession().addEventListener(sessionListener);
	}

	@Override
	public void run() {
		ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		final Command cmd = service.getCommand(commandId);
		if (cmd == null) {
			LogUtil.warn("Command not found: " + commandId);
			return;
		}
		String jobName = commandId;
		try { jobName = cmd.getName(); } catch (NotDefinedException e) {}
		
		Job cmdJob = new Job(jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				try {
					cmd.executeWithChecks(new ExecutionEvent());
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to execute command " + commandId, e);
				}
				return Status.OK_STATUS;
			}
		};
		cmdJob.schedule();
	}

	public void dispose() {
		console.getSession().removeEventListener(sessionListener);
		console = null;
	}
}
