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

package net.sf.mzmine.modules.visualization.threed;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;
import javax.swing.plaf.metal.MetalIconFactory;

import net.sf.mzmine.util.GUIUtils;

/**
 * 3D visualizer's toolbar
 */
class ThreeDToolBar extends JToolBar {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    static final Icon propertiesIcon = MetalIconFactory.getTreeComputerIcon();
    static final Icon annotationsIcon = new ImageIcon(
	    "icons/annotationsicon.png");

    ThreeDToolBar(ThreeDVisualizerWindow masterFrame) {

	super(JToolBar.VERTICAL);

	setFloatable(false);
	setFocusable(false);
	setMargin(new Insets(5, 5, 5, 5));
	setBackground(Color.white);

	GUIUtils.addButton(this, null, propertiesIcon, masterFrame,
		"PROPERTIES", "Set properties");

	addSeparator();

	GUIUtils.addButton(this, null, annotationsIcon, masterFrame,
		"SHOW_ANNOTATIONS", "Toggle displaying of peak values");

    }

}
