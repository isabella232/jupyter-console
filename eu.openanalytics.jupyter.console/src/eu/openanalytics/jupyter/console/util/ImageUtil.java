/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.console.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;

import eu.openanalytics.jupyter.wsclient.EvalResponse;

@SuppressWarnings("restriction")
public class ImageUtil {

	public static ImageData getImage(EvalResponse response) throws IOException {
		String value = (String) response.getValue("image/svg+xml");
		if (value != null) return convertSVG(value);
		
		value = (String) response.getValue("image/png");
		if (value != null) {
			byte[] bytes = Base64.decode(value.getBytes("UTF-8"));
			ImageData[] datas = new ImageLoader().load(new ByteArrayInputStream(bytes));
			if (datas != null && datas.length > 0) return datas[0];
		}
		return null;
	}
	
	public static ImageData convertSVG(String svg) throws IOException {
		try {
			SVGCache.getSVGUniverse().clear();
			URI xmlURI = SVGCache.getSVGUniverse().loadSVG(new ByteArrayInputStream(svg.getBytes()), "image.svg");
			SVGDiagram diagram = SVGCache.getSVGUniverse().getDiagram(xmlURI);
			BufferedImage awtImage = new BufferedImage((int) diagram.getWidth(), (int) diagram.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = (Graphics2D) awtImage.getGraphics();
			diagram.render(g2);
			return convert(awtImage);
		} catch (SVGException e) {
			throw new IOException("Failed to convert SVG image");
		}
	}
	
	public static Image convert(Display display, java.awt.Image awtImage) {
		Image swtImage = new Image(display, convert(awtImage));
		return swtImage;
	}

	public static ImageData convert(java.awt.Image awtImage) {

		int width = awtImage.getWidth(null);
		int height = awtImage.getHeight(null);

		// Convert to a BufferedImage if needed, for direct pixel buffer access.
		BufferedImage bufferedImage = null;
		if (awtImage instanceof BufferedImage) {
			bufferedImage = (BufferedImage)awtImage;
		} else {
			bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bufferedImage.createGraphics();
			g2d.drawImage(awtImage, 0, 0, null);
			g2d.dispose();
		}

		// Transfer the RGB values.
		int[] data = ((DataBufferInt)bufferedImage.getData().getDataBuffer()).getData();
		ImageData imageData = new ImageData(width, height, 24,
				new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
		imageData.setPixels(0, 0, data.length, data, 0);

		// If alpha is present, transfer that as well.
		if (bufferedImage.getColorModel().hasAlpha()) {
			int[] alpha = ((DataBufferInt)bufferedImage.getAlphaRaster().getDataBuffer()).getData();
			byte[] alphaBytes = new byte[alpha.length];
			for (int i=0; i<alpha.length; i++) {
				alphaBytes[i] = (byte)((alpha[i] >> 24) & 0xFF);
			}
			imageData.setAlphas(0, 0, alphaBytes.length, alphaBytes, 0);
		}

		return imageData;
	}

	public static Image scaleByAspectRatio(Image image, int w, int h, boolean disposeOld) {
		if (image == null) return null;
		Image scaled = new Image(Display.getDefault(), w, h);
		GC gc = new GC(scaled);
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);

		int oldWidth = image.getBounds().width;
		int oldHeight = image.getBounds().height;

		// Calculate resize ratios for resizing
		float ratioW = (float) w / (float) oldWidth;
		float ratioH = (float) h / (float) oldHeight;

		// Smaller ratio will ensure that the image fits in the view
		float ratio = ratioW < ratioH ? ratioW : ratioH;

		int newWidth = (int) (oldWidth * ratio);
		int newHeight = (int) (oldHeight * ratio);

		gc.drawImage(image, 0, 0, oldWidth, oldHeight, 0, 0, newWidth, newHeight);
		gc.dispose();
		if (disposeOld) image.dispose();

		return scaled;
	}
}
