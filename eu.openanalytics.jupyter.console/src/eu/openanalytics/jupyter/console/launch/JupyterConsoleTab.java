package eu.openanalytics.jupyter.console.launch;

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.jupyter.console.JupyterSession;

public class JupyterConsoleTab extends AbstractLaunchConfigurationTab {

	private Combo connectionMethodCmb;
	private Text connectionUrlTxt;
	private Combo kernelNameCmb;
	
	private String connectionMethod;
	private String connectionUrl;
	private String kernelName;
	
	@Override
	public void createControl(Composite parent) {
		Composite  area= new Composite(parent, SWT.NONE);
		setControl(area);
		GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).applyTo(area);
		
		new Label(area, SWT.NONE).setText("Connection method:");		
		connectionMethodCmb = new Combo(area, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().hint(250, SWT.DEFAULT).applyTo(connectionMethodCmb);
		
		new Label(area, SWT.NONE).setText("URL:");
		connectionUrlTxt = new Text(area, SWT.BORDER);
		GridDataFactory.fillDefaults().hint(250, SWT.DEFAULT).applyTo(connectionUrlTxt);
		
		new Label(area, SWT.NONE).setText("Kernel spec:");
		kernelNameCmb = new Combo(area, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().hint(250, SWT.DEFAULT).applyTo(kernelNameCmb);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			connectionMethod = configuration.getAttribute(JupyterSession.CONNECTION_METHOD, "tmpnb");
			connectionUrl = configuration.getAttribute(JupyterSession.CONNECTION_URL, "http://tmpnb.openanalytics.eu:8000");
			kernelName = configuration.getAttribute(JupyterSession.KERNEL_NAME, "python3");
			
			String[] methods = { "tmpnb" };
			connectionMethodCmb.setItems(methods);
			connectionMethodCmb.select(Arrays.binarySearch(methods, connectionMethod));
			
			connectionUrlTxt.setText(connectionUrl);
			
			String[] kernels = { "python3" };
			kernelNameCmb.setItems(kernels);
			kernelNameCmb.select(Arrays.binarySearch(kernels, kernelName));
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(JupyterSession.CONNECTION_METHOD, connectionMethod);
		configuration.setAttribute(JupyterSession.CONNECTION_URL, connectionUrl);
		configuration.setAttribute(JupyterSession.KERNEL_NAME, kernelName);
	}
	
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(JupyterSession.CONNECTION_METHOD, "tmpnb");
		configuration.setAttribute(JupyterSession.CONNECTION_URL, "http://tmpnb.openanalytics.eu:8000");
		configuration.setAttribute(JupyterSession.KERNEL_NAME, "python3");
	}

	@Override
	public String getName() {
		return "Server Connection";
	}

}
