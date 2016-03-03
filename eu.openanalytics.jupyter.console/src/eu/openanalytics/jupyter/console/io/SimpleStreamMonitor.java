/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console.io;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;

public class SimpleStreamMonitor implements IStreamMonitor {

	private String name;
	private ListenerList listeners;
	
	public SimpleStreamMonitor(String name) {
		this.name = name;
		this.listeners = new ListenerList();
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public void addListener(IStreamListener listener) {
		listeners.add(listener);
	}

	@Override
	public String getContents() {
		// Method not supported
		return "";
	}

	@Override
	public void removeListener(IStreamListener listener) {
		listeners.remove(listener);
	}

	public void append(String contents) {
		for (Object listener: listeners.getListeners()) {
			((IStreamListener) listener).streamAppended(contents, this);
		}
	}
	
	public void dispose() {
		listeners.clear();
	}
}
