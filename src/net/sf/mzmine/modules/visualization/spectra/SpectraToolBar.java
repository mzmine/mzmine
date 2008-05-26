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

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

/**
 * Spectra visualizer's toolbar class
 */
public class SpectraToolBar extends JToolBar {

    static final Icon centroidIcon = new ImageIcon("icons/centroidicon.png");
    static final Icon continuousIcon = new ImageIcon("icons/continuousicon.png");
    static final Icon dataPointsIcon = new ImageIcon("icons/datapointsicon.png");
    static final Icon annotationsIcon = new ImageIcon("icons/annotationsicon.png");
    static final Icon pickedPeakIcon = new ImageIcon("icons/pickedpeakicon.png");
    static final Icon axesIcon = new ImageIcon("icons/axesicon.png");
    
    private JButton centroidContinuousButton, dataPointsButton, peaksButton, axesButton;

    public SpectraToolBar(SpectraPlot plot) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        centroidContinuousButton = GUIUtils.addButton(this, null, centroidIcon,
        		plot, "TOGGLE_PLOT_MODE",
                "Toggle centroid/continuous mode");

        addSeparator();

        dataPointsButton = GUIUtils.addButton(this, null, dataPointsIcon,
        		plot, "SHOW_DATA_POINTS",
                "Toggle displaying of data points  in continuous mode");

        addSeparator();

        GUIUtils.addButton(this, null, annotationsIcon, plot,
                "SHOW_ANNOTATIONS", "Toggle displaying of peak values");

        addSeparator();

        peaksButton = GUIUtils.addButton(this, null, pickedPeakIcon, plot,
                "SHOW_PICKED_PEAKS", "Toggle displaying of picked peaks");
        
        addSeparator();
        
        axesButton = GUIUtils.addButton(this, null, axesIcon, plot,
                "SETUP_AXES", "Setup ranges for axes");
        

    }

    public void setCentroidButton(boolean centroid) {
        if (centroid) {
            centroidContinuousButton.setIcon(centroidIcon);
            dataPointsButton.setEnabled(true);
        } else {
            centroidContinuousButton.setIcon(continuousIcon);
            dataPointsButton.setEnabled(false);
        }
    }
    
    public void setPeaksButtonEnabled(boolean enabled) {
        peaksButton.setEnabled(enabled);
    }

    public void setAxesButtonEnabled(boolean enabled) {
    	axesButton.setEnabled(enabled);
    }

}
