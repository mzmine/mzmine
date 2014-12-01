/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.parameters.parametertypes;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JFrame;

/**
 * This class records a position and size of a window.
 */
public class WindowSettings implements ComponentListener {

    private Point position;
    private Dimension dimension;

    public Point getPosition() {
	return position;
    }

    public void setPosition(Point position) {
	this.position = position;
    }

    public Dimension getDimension() {
	return dimension;
    }

    public void setDimension(Dimension dimension) {
	this.dimension = dimension;
    }

    /**
     * Set window size and position according to the values in this instance
     */
    public void applySettingsToWindow(JFrame window) {
	if (position != null) {
	    position.translate(20, 20);
	    window.setLocation(position);
	}
	if (dimension != null) {
	    window.setSize(dimension);
	}
    }

    @Override
    public void componentMoved(ComponentEvent e) {
	position = e.getComponent().getLocation();

    }

    @Override
    public void componentResized(ComponentEvent e) {
	dimension = e.getComponent().getSize();

    }

    @Override
    public void componentHidden(ComponentEvent e) {
	// ignore
    }

    @Override
    public void componentShown(ComponentEvent e) {
	// ignore
    }

}
