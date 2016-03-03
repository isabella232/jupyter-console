package eu.openanalytics.jupyter.console;

import java.io.IOException;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.part.IPageBookViewPage;

import eu.openanalytics.jupyter.console.io.EventType;
import eu.openanalytics.jupyter.console.io.IEventListener;
import eu.openanalytics.jupyter.console.io.SessionEvent;
import eu.openanalytics.jupyter.console.io.SimpleStreamMonitor;
import eu.openanalytics.jupyter.console.util.ConsoleUtil;

public class JupyterConsole extends IOConsole {

	private static final String LINE_SEP = System.getProperty("line.separator");
	private static final String CONSOLE_NAME = "Jupyter";
	
	private JupyterSession session;
	private boolean running;
	private boolean busy;
	
	private IDocumentListener inputListener;
	private IOConsoleOutputStream[] outputStreams;
	
	public JupyterConsole(JupyterSession session) {
		super(CONSOLE_NAME, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "/icons/jupyter-sq-text-left-16.png"));
		this.session = session;
		
		inputListener = new IDocumentListener() {
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				// Do nothing.
			}
			
			@Override
			public void documentChanged(DocumentEvent event) {
				String text = event.getText();
				if (text.equals(LINE_SEP)) {
					String input = null;
					try {
						int lineCount = event.getDocument().getNumberOfLines();
						IRegion region = event.getDocument().getLineInformation(lineCount-2);
						input = event.getDocument().get(region.getOffset(), region.getLength());
						if (input != null && !input.isEmpty()) {
							JupyterConsole.this.session.write(input);
						}
					} catch (IOException e) {
						printErr(e.getMessage());
					} catch (Exception e) {
						 ConsolePlugin.log(e);
					}
				}
			}
		};
		
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				outputStreams = new IOConsoleOutputStream[] {
					JupyterConsole.this.newOutputStream(),
					JupyterConsole.this.newOutputStream(),
					JupyterConsole.this.newOutputStream()
				};
				outputStreams[1].setColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
				outputStreams[2].setColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
				
				JupyterConsole.this.getInputStream().setColor(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
				getDocument().addDocumentListener(inputListener);
			};
		});
		
		session.addStreamListener(new IStreamListener() {
			@Override
			public void streamAppended(String text, IStreamMonitor monitor) {
				if (text.isEmpty()) return;
				String streamName = ((SimpleStreamMonitor) monitor).getName();
				if (streamName.equals(JupyterSession.OUTPUT_STDERR)) printErr(text);
				else if (streamName.equals(JupyterSession.OUTPUT_CONTROL)) printControl(text);
				else print(text);
			}
		});
		
		session.getEventMonitor().addListener(new IEventListener() {
			@Override
			public void handle(SessionEvent event) {
				final EventType type = event.type;
				if (type == EventType.SessionStarted) running = true;
				else if (type == EventType.SessionStopped) running = false;
				else if (type == EventType.SessionBusy) busy = true;
				else if (type == EventType.SessionIdle) busy = false;
				updateConsoleName();
			}
		});
	}
	
	public JupyterSession getSession() {
		return session;
	}
	
	public void print(String message) {
		try {
			if (PlatformUI.isWorkbenchRunning()) outputStreams[0].write(message + LINE_SEP);
        } catch (IOException e) {
            ConsolePlugin.log(e);
        }
	}
	
	public void printErr(String message) {
		try {
			if (PlatformUI.isWorkbenchRunning()) outputStreams[1].write(message + LINE_SEP);
        } catch (IOException e) {
            ConsolePlugin.log(e);
        }
	}
	
	public void printControl(String message) {
		try {
			if (PlatformUI.isWorkbenchRunning()) outputStreams[2].write(message + LINE_SEP);
        } catch (IOException e) {
            ConsolePlugin.log(e);
        }
	}
	
	@Override
	public IPageBookViewPage createPage(IConsoleView view) {
		return new JupyterConsolePage(this, view);
	}
	
	private void updateConsoleName() {
		ConsoleUtil.inUIThread(new Runnable() {
			public void run() {
				if (running) {
					String state = busy ? "working" : "ready";
					String kernelName = JupyterConsole.this.session.getKernelSpec().displayName;
					setName(CONSOLE_NAME + " <" + kernelName + "> <" + state + ">");
				} else {
					setName(CONSOLE_NAME + " <terminated>");
				}						
			}
		});
	}
}
