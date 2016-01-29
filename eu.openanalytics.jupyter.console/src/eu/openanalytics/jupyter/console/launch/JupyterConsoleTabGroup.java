package eu.openanalytics.jupyter.console.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class JupyterConsoleTabGroup extends AbstractLaunchConfigurationTabGroup {

	public JupyterConsoleTabGroup() {
		// Default constructor
	}

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		setTabs(new ILaunchConfigurationTab[] { new JupyterConsoleTab() });
	}

}
