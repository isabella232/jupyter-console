package eu.openanalytics.jupyter.console.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.jupyter.console.Activator;

public class LogUtil {

	public static void showError(String title, String message, Exception e) {
		Shell shell = Display.getDefault().getActiveShell();
		ErrorDialog.openError(shell, title, message, new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, e));
	}
	
	public static void info(String message) {
		Activator.getDefault().getLog().log(new Status(IStatus.INFO, Activator.PLUGIN_ID, message));
	}
	
	public static void warn(String message) {
		Activator.getDefault().getLog().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, message));
	}
	
	public static void error(String message, Throwable cause) {
		Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, cause));
	}
}
