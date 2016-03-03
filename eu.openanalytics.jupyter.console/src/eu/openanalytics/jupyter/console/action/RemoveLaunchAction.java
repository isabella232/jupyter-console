package eu.openanalytics.jupyter.console.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;

import eu.openanalytics.jupyter.console.JupyterConsole;
import eu.openanalytics.jupyter.console.io.EventType;
import eu.openanalytics.jupyter.console.io.IEventListener;
import eu.openanalytics.jupyter.console.io.SessionEvent;

public class RemoveLaunchAction extends Action {

	private static ImageDescriptor imgEnabled = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE);
	private static ImageDescriptor imgDisabled = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE_DISABLED);
	
	private JupyterConsole console;
	private IEventListener sessionListener;
	
	public RemoveLaunchAction(IConsole console) {
		super();
		setImageDescriptor(imgDisabled);
		setToolTipText("Remove Launch");
		setEnabled(false);
		
		this.console = (JupyterConsole) console;
		sessionListener = new IEventListener() {
			@Override
			public void handle(SessionEvent event) {
				if (event.type == EventType.SessionStopped) {
					setImageDescriptor(imgEnabled);
					setEnabled(true);
				}
			}
		};
		this.console.getSession().getEventMonitor().addListener(sessionListener);
	}

	@Override
	public void run() {
		IConsole[] consoles = { console };
		ConsolePlugin.getDefault().getConsoleManager().removeConsoles(consoles);
	}

	public void dispose() {
		console.getSession().getEventMonitor().removeListener(sessionListener);
		console = null;
	}
}

