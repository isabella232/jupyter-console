package eu.openanalytics.jupyter.console.cmd;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;

import eu.openanalytics.jupyter.console.JupyterConsole;
import eu.openanalytics.jupyter.console.util.ConsoleUtil;
import eu.openanalytics.jupyter.console.util.LogUtil;

public class SubmitSelectionCmd extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		// Get the selected text in the active editor
		ISelection selection = null;
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = page.getActiveEditor();
		if (editor != null) {
			ISelectionProvider provider = editor.getEditorSite().getSelectionProvider();
			if (provider != null) selection = provider.getSelection();
		}
		String text = null;
		if (selection instanceof TextSelection) {
			text = ((TextSelection) selection).getText();
		}

		// Submit the text to the active console
		if (text != null && !text.isEmpty()) {
			IConsole console = ConsoleUtil.getActiveConsole();
			if (console instanceof JupyterConsole) {
				try {
					text = text.replace("\r", "");
					((JupyterConsole) console).getSession().write(text, true);
				} catch (IOException e) {
					LogUtil.error("Failed to submit text to console", e);
				}
			}
		}

		return null;
	}
	
}
