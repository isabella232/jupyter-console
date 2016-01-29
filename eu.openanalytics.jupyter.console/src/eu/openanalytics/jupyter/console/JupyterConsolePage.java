package eu.openanalytics.jupyter.console;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.internal.console.IOConsolePage;

import eu.openanalytics.jupyter.console.action.StopSessionAction;

@SuppressWarnings("restriction")
public class JupyterConsolePage extends IOConsolePage {

	private StopSessionAction stopSessionAction;
	
	public JupyterConsolePage(TextConsole console, IConsoleView view) {
		super(console, view);
	}

	@Override
	protected void createActions() {
		super.createActions();
		stopSessionAction = new StopSessionAction(getConsole());
	}
	
	@Override
	protected void configureToolBar(IToolBarManager mgr) {
		super.configureToolBar(mgr);
        String groupId = "JUPYTER";
        mgr.insertBefore(IConsoleConstants.OUTPUT_GROUP, new Separator(groupId));
        mgr.appendToGroup(groupId, stopSessionAction);
    }
	
	@Override
	public void dispose() {
		stopSessionAction.dispose();
		super.dispose();
	}
}
