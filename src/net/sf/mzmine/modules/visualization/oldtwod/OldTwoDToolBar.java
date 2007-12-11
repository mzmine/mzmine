/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.oldtwod;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

/**
 * 2D visualizer's toolbar class
 */
public class OldTwoDToolBar extends JToolBar {

    static final Icon centroidIcon = new ImageIcon("icons/centroidicon.png");
    static final Icon continuousIcon = new ImageIcon("icons/continuousicon.png");
    static final Icon dataPointsIcon = new ImageIcon("icons/datapointsicon.png");
    static final Icon annotationsIcon = new ImageIcon("icons/annotationsicon.png");
    static final Icon paletteIcon = new ImageIcon("icons/colorbaricon.png");
    static final Icon zoomIcon = new ImageIcon("icons/zoomouticon.png");
    static final Icon peakEditIcon = new ImageIcon("icons/xicicon.png");
    
    JButton centroidContinuousButton;
    JButton showPeakButton;
    JButton paletteButton;
    JButton zoomPeakEditModeButton;

    public OldTwoDToolBar(OldTwoDVisualizerWindow masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        centroidContinuousButton = GUIUtils.addButton(this, null, centroidIcon, masterFrame,
                "TOGGLE_PLOT_MODE", "Switch between centroid/continuous modes");
        
        addSeparator();
             
        showPeakButton = GUIUtils.addButton(this, null, annotationsIcon, masterFrame,
                "SHOW_ANNOTATIONS", "Toggle displaying of peaks");
        
        
        addSeparator();
        
        paletteButton = GUIUtils.addButton(this, null, paletteIcon, masterFrame,
                "SWITCH_PALETTE", "Switch between grayscale/rainbow palette");
        
        addSeparator();

        zoomPeakEditModeButton = GUIUtils.addButton(this, null, peakEditIcon, masterFrame,
                "SWITCH_ZOOMPEAKDETECTION", "Switch between zoom / peak edit modes");        
    }

}
