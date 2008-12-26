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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.jfree.chart.plot.DefaultDrawingSupplier;

public class ColorCellRenderer extends JLabel implements ListCellRenderer {

	// This is the only method defined by ListCellRenderer.
	// We just reconfigure the JLabel each time we're called.
	private DefaultDrawingSupplier drawing;

	public ColorCellRenderer() {
		super();
		drawing = new DefaultDrawingSupplier();
		drawing.getNextPaint();
	}

	public Component getListCellRendererComponent(JList list, Object value, // value
																			// to
																			// display
			int index, // cell index
			boolean isSelected, // is the cell selected
			boolean cellHasFocus) // the list and the cell have the focus
	{
		Color color = Color.BLUE;
		float[] compBlueArray = null;
		compBlueArray = color.getRGBColorComponents(compBlueArray);

		if (!((ListSelectionItem) value).hasColor()) {

			color = (Color) drawing.getNextPaint();

			float[] compArray = null;
			compArray = color.getRGBColorComponents(compArray);
			
			float r = Math.abs(compArray[0] - compBlueArray[0]);
			float g = Math.abs(compArray[1] - compBlueArray[1]);
			float b = Math.abs(compArray[2] - compBlueArray[2]);
			
			// Color blue is reserved for data points without selection
			if ((r < 50) && (g < 50) && (b < 50))
				color = (Color) drawing.getNextPaint();

			((ListSelectionItem) value).setColor(color);

		} else {
			color = ((ListSelectionItem) value).getColor();
		}

		BufferedImage image = new BufferedImage(20, 20,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(Color.BLACK);
		g2d.drawRect(5, 5, 10, 10);
		g2d.setColor(color);
		g2d.fillRect(5, 5, 10, 10);
		ImageIcon icon = new ImageIcon(image);

		String s = value.toString();
		setText(s);
		setIcon(icon);
		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		} else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		setEnabled(list.isEnabled());
		setFont(list.getFont());
		setOpaque(true);
		return this;
	}

}
