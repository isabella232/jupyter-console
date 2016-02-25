package eu.openanalytics.jupyter.console.view;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.jupyter.console.util.ImageUtil;
import eu.openanalytics.jupyter.console.util.LogUtil;

public class GraphicsView extends ViewPart {

	private Canvas graphicsCanvas;
	private ImageData imageData;
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);
		graphicsCanvas = new Canvas(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(graphicsCanvas);
		
		graphicsCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				repaintGraphics(e.gc);
			}
		});
	}

	@Override
	public void setFocus() {
		graphicsCanvas.setFocus();
	}

	private void repaintGraphics(GC gc) {
		if (imageData == null) return;
		Image image = new Image(gc.getDevice(), imageData);
		Point size = graphicsCanvas.getSize();
		image = ImageUtil.scaleByAspectRatio(image, size.x, size.y, true);
		try {
			gc.drawImage(image, 0, 0);
		} finally {
			image.dispose();
		}
	}
	
	private void setImage(ImageData imageData) {
		this.imageData = imageData;
		graphicsCanvas.redraw();
	}
	
	public static void openWith(final ImageData imageData) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					GraphicsView view = (GraphicsView) page.showView(GraphicsView.class.getName(), null, IWorkbenchPage.VIEW_VISIBLE);
					view.setImage(imageData);
				} catch (PartInitException e) {
					LogUtil.error("Failed to open graphics view", e);
				}
			}
		});
	}
}
