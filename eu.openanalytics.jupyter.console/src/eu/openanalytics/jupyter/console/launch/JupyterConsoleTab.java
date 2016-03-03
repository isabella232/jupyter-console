/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console.launch;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.jupyter.console.JupyterSession;
import eu.openanalytics.jupyter.console.util.LogUtil;
import eu.openanalytics.jupyter.console.util.SelectKernelDialog;
import eu.openanalytics.jupyter.wsclient.API;
import eu.openanalytics.jupyter.wsclient.KernelService.KernelSpec;

public class JupyterConsoleTab extends AbstractLaunchConfigurationTab {

	private Combo connectionMethodCmb;
	private Text connectionUrlTxt;
	private Text kernelNameTxt;
	
	private String connectionMethod;
	private String connectionUrl;
	private String kernelName;
	
	@Override
	public void createControl(Composite parent) {
		Composite  area= new Composite(parent, SWT.NONE);
		setControl(area);
		GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).applyTo(area);
		
		ModifyListener dirtyAdapter = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		};
		
		new Label(area, SWT.NONE).setText("Connection method:");		
		connectionMethodCmb = new Combo(area, SWT.READ_ONLY);
		connectionMethodCmb.addModifyListener(dirtyAdapter);
		GridDataFactory.fillDefaults().hint(250, SWT.DEFAULT).applyTo(connectionMethodCmb);
		
		new Label(area, SWT.NONE).setText("URL:");
		connectionUrlTxt = new Text(area, SWT.BORDER);
		connectionUrlTxt.addModifyListener(dirtyAdapter);
		GridDataFactory.fillDefaults().hint(250, SWT.DEFAULT).applyTo(connectionUrlTxt);
		
		new Label(area, SWT.NONE).setText("Kernel spec:");
		Composite kernelCmp = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(kernelCmp);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(kernelCmp);
		
		kernelNameTxt = new Text(kernelCmp, SWT.BORDER);
		kernelNameTxt.setEditable(false);
		kernelNameTxt.addModifyListener(dirtyAdapter);
		GridDataFactory.fillDefaults().hint(250, SWT.DEFAULT).applyTo(kernelNameTxt);
		
		Button selectKernelBtn = new Button(kernelCmp, SWT.PUSH);
		selectKernelBtn.setText("Select...");
		selectKernelBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectKernel();
			}
		});
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			connectionMethod = configuration.getAttribute(JupyterSession.CONNECTION_METHOD, JupyterSession.DEFAULT_CONNECTION_METHOD);
			connectionUrl = configuration.getAttribute(JupyterSession.CONNECTION_URL, JupyterSession.DEFAULT_CONNECTION_URL);
			kernelName = configuration.getAttribute(JupyterSession.KERNEL_NAME, JupyterSession.DEFAULT_KERNEL_NAME);
			
			String[] methods = { JupyterSession.DEFAULT_CONNECTION_METHOD };
			connectionMethodCmb.setItems(methods);
			connectionMethodCmb.select(Arrays.binarySearch(methods, connectionMethod));
			
			connectionUrlTxt.setText(connectionUrl);
			kernelNameTxt.setText(kernelName);
		} catch (CoreException e) {
			LogUtil.showError("Error initializing page", e.getMessage(), e);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(JupyterSession.CONNECTION_METHOD, connectionMethod);
		configuration.setAttribute(JupyterSession.CONNECTION_URL, connectionUrl);
		configuration.setAttribute(JupyterSession.KERNEL_NAME, kernelName);
	}
	
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(JupyterSession.CONNECTION_METHOD, JupyterSession.DEFAULT_CONNECTION_METHOD);
		configuration.setAttribute(JupyterSession.CONNECTION_URL, JupyterSession.DEFAULT_CONNECTION_URL);
		configuration.setAttribute(JupyterSession.KERNEL_NAME, JupyterSession.DEFAULT_KERNEL_NAME);
	}

	@Override
	public String getName() {
		return "Server Connection";
	}

	private void selectKernel() {
		try {
			String nbUrl = API.getNotebookService().getNotebook(connectionUrl);
			KernelSpec[] specs = API.getKernelService().listAvailableKernels(nbUrl);
			SelectKernelDialog dialog = new SelectKernelDialog(getShell(), specs);
			if (dialog.open() == Window.OK) {
				kernelName = dialog.getSelectedSpec().name;
				kernelNameTxt.setText(kernelName);
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		} catch (IOException e) {
			LogUtil.showError("Error selecting kernel spec", e.getMessage(), e);
		}
	}
}
