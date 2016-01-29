package eu.openanalytics.jupyter.console.launch;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;

import eu.openanalytics.jupyter.console.Activator;
import eu.openanalytics.jupyter.console.JupyterConsole;
import eu.openanalytics.jupyter.console.JupyterSession;

public class JupyterConsoleLaunchDelegate extends LaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		IProgressMonitor m = SubMonitor.convert(monitor, "Starting Jupyter session", 3);
		m.worked(1);
		
		JupyterSession session = new JupyterSession(configuration);
		m.worked(1);
		
		JupyterConsole console = new JupyterConsole(session);
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
		m.worked(1);
		
		try {
			session.connect();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to create session", e));
		}
		
		m.done();
	}

	@Override
	protected boolean saveBeforeLaunch(final ILaunchConfiguration configuration, final String mode, final IProgressMonitor monitor) throws CoreException {
		return true;
	}
	
	@Override
	public boolean buildForLaunch(final ILaunchConfiguration configuration, final String mode, final IProgressMonitor monitor) throws CoreException {
		return false;
	}
}
