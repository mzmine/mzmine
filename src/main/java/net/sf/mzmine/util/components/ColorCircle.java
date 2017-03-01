/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.util.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * 
 */
public class ColorCircle extends JComponent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_MARGIN = 5;

    private Color circleColor;
    private int margin;

    /**
     */
    public ColorCircle(Color circleColor) {
	this(circleColor, DEFAULT_MARGIN);
    }

    /**
     */
    public ColorCircle(Color circleColor, int margin) {
	this.circleColor = circleColor;
	this.margin = margin;
    }

    public void paint(Graphics g) {
	super.paint(g);
	Dimension size = getSize();
	g.setColor(circleColor);
	int diameter = Math.min(size.width, size.height) - (2 * margin);
	g.fillOval((size.width - diameter) / 2, (size.height - diameter) / 2,
		diameter, diameter);
    }

}
