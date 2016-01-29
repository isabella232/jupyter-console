package eu.openanalytics.jupyter.console.util;

import org.eclipse.swt.widgets.Display;

public class ConsoleUtil {

	public static void inUIThread(Runnable r) {
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(r);
		} else {
			r.run();
		}
	}
}
