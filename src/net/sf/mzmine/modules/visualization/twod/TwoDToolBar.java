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

package net.sf.mzmine.modules.visualization.twod;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

/**
 * 2D visualizer's toolbar class
 */
class TwoDToolBar extends JToolBar {

    static final Icon paletteIcon = new ImageIcon("icons/colorbaricon.png");
    static final Icon dataPointsIcon = new ImageIcon("icons/datapointsicon.png");
    static final Icon annotationsIcon = new ImageIcon("icons/annotationsicon.png");
    static final Icon axesIcon = new ImageIcon("icons/axesicon.png");
    static final Icon centroidIcon = new ImageIcon("icons/centroidicon.png");
    
    TwoDToolBar(TwoDVisualizerWindow masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        GUIUtils.addButton(this, null, paletteIcon, masterFrame,
                "SWITCH_PALETTE", "Switch palette");
        
        addSeparator();
        
        GUIUtils.addButton(this, null, dataPointsIcon, masterFrame,
                "SHOW_DATA_POINTS",
                "Toggle displaying of data points in continuous mode");
        
        addSeparator();
        
        GUIUtils.addButton(this, null, annotationsIcon, masterFrame,
                "SHOW_ANNOTATIONS", "Toggle displaying of peak values");

        addSeparator();

        GUIUtils.addButton(this, null, axesIcon, masterFrame, "SETUP_AXES",
                "Setup ranges for axes");
        
        addSeparator();
        
        GUIUtils.addButton(this, null, centroidIcon, masterFrame, "SWITCH_CENTROID",
        "Switch between continuous and centroided mode");        
        
    }

}
