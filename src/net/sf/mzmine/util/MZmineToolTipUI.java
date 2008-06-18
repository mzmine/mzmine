/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicToolTipUI;

public class MZmineToolTipUI extends BasicToolTipUI {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private String[] strs;

	private int maxWidth = 0;

	public void paint(Graphics g, JComponent c) {
		Graphics2D g2 = (Graphics2D) g;

		RenderingHints qualityHints = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		qualityHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		qualityHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		qualityHints.put(RenderingHints.KEY_COLOR_RENDERING,
				RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		qualityHints.put(RenderingHints.KEY_DITHERING,
				RenderingHints.VALUE_DITHER_ENABLE);
		qualityHints.put(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		qualityHints.put(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		qualityHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		qualityHints.put(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_NORMALIZE);

		g2.setRenderingHints(qualityHints);

		Font defaultFont = g2.getFont();
		FontMetrics metrics = g2.getFontMetrics(g.getFont());
		Dimension size = c.getSize();
		g2.setColor(c.getBackground());
		g2.fillRect(0, 0, size.width, size.height);
		g2.setColor(c.getForeground());
		if (strs != null) {
			int yPosition = 0;
			for (int i = 0; i < strs.length; i++) {
				if ((i == 0) && (strs.length > 1)) {
					Font newFont = new Font("SansSerif.bold", Font.PLAIN,
							defaultFont.getSize() + 1);
					g2.setFont(newFont);
					g2.drawString(strs[i], 3, (metrics.getHeight())
							* (yPosition + 1));
					g2.setFont(defaultFont);
					yPosition++;
					continue;
				}
				if (strs[i].equals("IMAGE")) {
					try {
						File input = new File(System.getProperty("user.dir")
								+ File.separator + "help" + File.separator + "tipTool"
								+ File.separator + "images" + File.separator + strs[i + 1]);
						
						BufferedImage image = ImageIO.read(input);

						// determine thumbnail size
						int thumbWidth = size.width - 40;
						int thumbHeight = 250;
						double thumbRatio = (double) thumbWidth
								/ (double) thumbHeight;
						int imageWidth = image.getWidth(null);
						int imageHeight = image.getHeight(null);
						double imageRatio = (double) imageWidth
								/ (double) imageHeight;
						if (thumbRatio < imageRatio) {
							thumbHeight = (int) (thumbWidth / imageRatio);
						} else {
							thumbWidth = (int) (thumbHeight * imageRatio);
						}

						g2.drawImage(image, 20, (metrics.getHeight())
								* (yPosition + 1), thumbWidth, thumbHeight, c);

						i++;
						yPosition += (250 / metrics.getHeight()) + 2;

					} catch (FileNotFoundException e) {
						logger.finest("Error:" + e.getMessage());
					} catch (IOException e) {
						logger.finest("Error:" + e.getMessage());
					}
					continue;
				}

				g2.drawString(strs[i], 3, (metrics.getHeight())
						* (yPosition + 1));
				yPosition++;
			}
		}
	}

	public Dimension getPreferredSize(JComponent c) {

		int height = 0;
		String line;
		int maxWidth = 0;
		int numberOfImages = 0;
		Vector<String> lineValues = new Vector<String>();

		FontMetrics metrics = c.getFontMetrics(c.getFont());
		String tipText = ((JToolTip) c).getTipText();

		if (tipText == null) {
			return new Dimension(0, 0);
		}

		try {
			File file = new File(System.getProperty("user.dir")
					+ File.separator + "help" + File.separator + "tipTool"
					+ File.separator + "texts" + File.separator + tipText);
			FileInputStream fis = new FileInputStream(file);

			// Here BufferedInputStream is added for fast reading.
			BufferedInputStream bis = new BufferedInputStream(fis);
			BufferedReader ris = new BufferedReader(new InputStreamReader(bis));

			while ((line = ris.readLine()) != null) {
				int width = SwingUtilities.computeStringWidth(metrics, line);
				maxWidth = (maxWidth < width) ? width : maxWidth;
				lineValues.add(line);
				if (line.equals("IMAGE"))
					numberOfImages++;
			}

			// dispose all the resources after using them.
			fis.close();
			bis.close();
			ris.close();

			int numberOfLines = lineValues.size();
			if (numberOfLines < 1) {
				strs = null;
				numberOfLines = 1;
			} else {
				strs = new String[numberOfLines];
				int i = 0;
				for (String lineString : lineValues) {
					strs[i] = lineString;
					i++;
				}
			}
			height = (metrics.getHeight() * numberOfLines)
					+ (numberOfImages * 250);
			this.maxWidth = maxWidth;
			return new Dimension(maxWidth + 6, height + 4);

		} catch (FileNotFoundException e) {

			int dot = tipText.lastIndexOf('.');
			if ((dot != tipText.length()) && (dot > -1))
				if (tipText.substring(dot).equals(".txt"))
					return null;

			maxWidth = SwingUtilities.computeStringWidth(metrics, tipText);
			height = metrics.getHeight();
			strs = new String[] { tipText };
			return new Dimension(maxWidth + 6, height + 4);

		} catch (IOException e) {
			logger.finest("Error:" + e.getMessage());
		}

		return new Dimension(maxWidth + 6, height + 4);
	}
}
