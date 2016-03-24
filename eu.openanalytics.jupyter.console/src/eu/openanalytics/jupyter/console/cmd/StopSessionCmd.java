/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console.cmd;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eu.openanalytics.jupyter.console.JupyterConsole;
import eu.openanalytics.jupyter.console.util.ConsoleUtil;
import eu.openanalytics.jupyter.console.util.LogUtil;

public class StopSessionCmd extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		JupyterConsole console = ConsoleUtil.getActiveJupyterConsole();
		if (console != null) {
			try {
				console.getSession().stop();
			} catch (IOException e) {
				LogUtil.error("Failed to stop session", e);
			}
		}
		return null;
	}

}
