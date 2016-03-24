/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console.cmd;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.jupyter.console.JupyterConsole;
import eu.openanalytics.jupyter.console.util.ConsoleUtil;
import eu.openanalytics.jupyter.console.util.LogUtil;
import eu.openanalytics.jupyter.console.util.ReflectionUtils;

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
			TextSelection textSelection = (TextSelection) selection;
			if (textSelection.getLength() == 0) {
				IDocument doc = (IDocument) ReflectionUtils.invoke("getDocument", textSelection);
				if (doc != null) {
					try {
						int lineNr = textSelection.getStartLine();
						int offset = doc.getLineOffset(lineNr);
						text = doc.get(offset, doc.getLineLength(lineNr));
						if (text.endsWith("\n")) text = text.substring(0, text.length() - 1);
					} catch (BadLocationException e) {
						// Failed to retrieve the document's line.
					}
				}
			} else {
				text = textSelection.getText();
			}
		}

		// Submit the text to the active console
		if (text != null && !text.isEmpty()) {
			JupyterConsole console = ConsoleUtil.getActiveJupyterConsole();
			if (console != null) {
				try {
					text = text.replace("\r", "");
					console.getSession().write(text, true);
				} catch (IOException e) {
					LogUtil.error("Failed to submit text to console", e);
				}
			}
		}

		return null;
	}
	
}
