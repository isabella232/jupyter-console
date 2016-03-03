package eu.openanalytics.jupyter.console.util;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;

public class ConsoleUtil {

	public static void inUIThread(Runnable r) {
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(r);
		} else {
			r.run();
		}
	}
	
	public static IConsole getActiveConsole() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
            	IViewPart view = page.findView("org.eclipse.ui.console.ConsoleView");
            	if (view instanceof IConsoleView) return ((IConsoleView) view).getConsole();
            }
        }
        return null;
	}
}
