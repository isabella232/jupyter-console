/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console.util;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;

import eu.openanalytics.jupyter.console.JupyterConsole;

public class ConsoleUtil {

	public static void inUIThread(Runnable r) {
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(r);
		} else {
			r.run();
		}
	}
	
	public static <T> T inUIThread(final Callable<T> c) throws Exception {
		final AtomicReference<T> retValRef = new AtomicReference<>();
		final AtomicReference<Exception> exceptionRef = new AtomicReference<>();
		final Runnable wrapper = new Runnable() {
			@Override
			public void run() {
				try {
					retValRef.set(c.call());
				} catch (Exception e) {
					exceptionRef.set(e);
				}
			}
		};
		inUIThread(wrapper);
		if (exceptionRef.get() == null) return retValRef.get();
		else throw exceptionRef.get();
	}
	
	public static IConsole getActiveConsole() {
		try {
			return inUIThread(new Callable<IConsole>() {
				@Override
				public IConsole call() throws Exception {
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			        if (window != null && window.getActivePage() != null) {
			            IViewPart view = window.getActivePage().findView("org.eclipse.ui.console.ConsoleView");
			            if (view instanceof IConsoleView) return ((IConsoleView) view).getConsole();
			        }
			        return null;
				}
			});
		} catch (Exception e) {
			return null;
		}
	}
	
	public static JupyterConsole getActiveJupyterConsole() {
		IConsole activeConsole = getActiveConsole();
		if (activeConsole instanceof JupyterConsole) return (JupyterConsole) activeConsole;
		return null;
	}
}
