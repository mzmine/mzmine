/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.tooltip;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicToolTipUI;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakListAppliedMethod;

public class MZmineToolTipUI extends BasicToolTipUI {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private String[] strs;

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

		Font newFont = new Font("SansSerif", Font.PLAIN,
				g.getFont().getSize() + 1);
		g2.setFont(newFont);


		FontMetrics metrics = g2.getFontMetrics(new Font("SansSerif", Font.PLAIN,
				g.getFont().getSize() + 2));
		Dimension size = c.getSize();
		
		if (size == null)
			return;
		
		g2.setColor(new Color(255, 250, 205));//c.getBackground());
		g2.fillRect(0, 0, size.width, size.height);
		g2.setColor(c.getForeground());
		if (strs != null) {
			int yPosition = 0;
			for (int i = 0; i < strs.length; i++) {
				g2.drawString(strs[i], 3, (metrics.getHeight())
						* (yPosition + 1));
				yPosition++;
			}
		}
	}

	public Dimension getPreferredSize(JComponent c) {

		int height = 0;
		int maxWidth = 0;
		Vector<String> lineValues = new Vector<String>();

		FontMetrics metrics = c.getFontMetrics(new Font("SansSerif", Font.PLAIN,
				c.getFont().getSize() + 3));
		JComponent component = ((JToolTip) c).getComponent();
		ParameterSet parameters = ((PeakListAppliedMethod) component).getParameterSet();
		
		if (parameters == null)
			return null;
		
		Parameter[] param = parameters.getParameters();
		
		if (param == null)
			return null;
		
		//logger.finest(param.toString());
			
			
		for (Parameter p: param) {
			String units="";
			String value="";
			if(p.getUnits()!=null)
				units = p.getUnits();
			if(parameters.getParameterValue(p)!=null)
				value = parameters.getParameterValue(p).toString();
			String line = p.getName() + " = " + value + " " + units;
			int width = SwingUtilities.computeStringWidth(metrics, line);
			maxWidth = (maxWidth < width) ? width : maxWidth;
			lineValues.add(line);
		}

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
		height = (metrics.getHeight() * numberOfLines);
		return new Dimension(maxWidth + 3, height + 10);

	}
	
}
