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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.internal.console.IOConsolePage;

import eu.openanalytics.jupyter.console.cmd.RemoveLaunchCmd;
import eu.openanalytics.jupyter.console.cmd.RestartKernelCmd;
import eu.openanalytics.jupyter.console.cmd.StopSessionCmd;
import eu.openanalytics.jupyter.console.cmd.SwitchKernelCmd;
import eu.openanalytics.jupyter.console.util.CmdDelegatingAction;

@SuppressWarnings("restriction")
public class JupyterConsolePage extends IOConsolePage {

	private CmdDelegatingAction[] actions;
	
	public JupyterConsolePage(TextConsole console, IConsoleView view) {
		super(console, view);
	}

	@Override
	protected void createActions() {
		super.createActions();
		actions = new CmdDelegatingAction[4];
		actions[0] = new CmdDelegatingAction(getConsole(), StopSessionCmd.class.getName(),
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_STOP),
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_STOP_DISABLED),
				"Stop session", true);
		actions[1] = new CmdDelegatingAction(getConsole(), RestartKernelCmd.class.getName(),
				Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/restart.png"),
				Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/restart.png"),
				"Restart kernel", true);
		actions[2] = new CmdDelegatingAction(getConsole(), SwitchKernelCmd.class.getName(),
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED),
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED),
				"Switch kernel", true);
		actions[3] = new CmdDelegatingAction(getConsole(), RemoveLaunchCmd.class.getName(),
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE_DISABLED),
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE),
				"Remove Launch", false);
	}
	
	@Override
	protected void configureToolBar(IToolBarManager mgr) {
		super.configureToolBar(mgr);
        String groupId = "JUPYTER";
        mgr.insertBefore(IConsoleConstants.OUTPUT_GROUP, new Separator(groupId));
        for (int i = 0; i < actions.length; i++) {
        	mgr.appendToGroup(groupId, actions[i]);
		}
    }
	
	@Override
	public void dispose() {
		for (int i = 0; i < actions.length; i++) {
        	actions[i].dispose();
		}
		super.dispose();
	}
}
