/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.internal.console.IOConsolePage;

import eu.openanalytics.jupyter.console.action.RemoveLaunchAction;
import eu.openanalytics.jupyter.console.action.StopSessionAction;

@SuppressWarnings("restriction")
public class JupyterConsolePage extends IOConsolePage {

	private StopSessionAction stopSessionAction;
	private RemoveLaunchAction removeLaunchAction;
	
	public JupyterConsolePage(TextConsole console, IConsoleView view) {
		super(console, view);
	}

	@Override
	protected void createActions() {
		super.createActions();
		stopSessionAction = new StopSessionAction(getConsole());
		removeLaunchAction = new RemoveLaunchAction(getConsole());
	}
	
	@Override
	protected void configureToolBar(IToolBarManager mgr) {
		super.configureToolBar(mgr);
        String groupId = "JUPYTER";
        mgr.insertBefore(IConsoleConstants.OUTPUT_GROUP, new Separator(groupId));
        mgr.appendToGroup(groupId, stopSessionAction);
        mgr.appendToGroup(groupId, removeLaunchAction);
    }
	
	@Override
	public void dispose() {
		stopSessionAction.dispose();
		removeLaunchAction.dispose();
		super.dispose();
	}
}
