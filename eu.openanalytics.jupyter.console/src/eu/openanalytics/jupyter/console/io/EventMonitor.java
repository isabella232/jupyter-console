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

