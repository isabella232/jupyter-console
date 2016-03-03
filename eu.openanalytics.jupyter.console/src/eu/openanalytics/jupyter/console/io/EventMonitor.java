/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console.io;

import org.eclipse.core.runtime.ListenerList;

public class EventMonitor {
	
	private ListenerList listeners;
	
	public EventMonitor() {
		listeners = new ListenerList();
	}
	
	public void addListener(IEventListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IEventListener listener) {
		listeners.remove(listener);
	}

	public void post(SessionEvent event) {
		for (Object l: listeners.getListeners()) {
			((IEventListener) l).handle(event);
		}
	}
	
	public void dispose() {
		listeners.clear();
	}
}

