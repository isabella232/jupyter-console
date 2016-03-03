/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.wsclient;

public class API {

	private static KernelService kernelService;
	private static SessionService sessionService;
	private static TempNotebookService notebookService;
	
	static {
		kernelService = new KernelService();
		sessionService = new SessionService();
		notebookService = new TempNotebookService();
	}
	
	public static KernelService getKernelService() {
		return kernelService;
	}
	
	public static SessionService getSessionService() {
		return sessionService;
	}
	
	public static TempNotebookService getNotebookService() {
		return notebookService;
	}
	
}
