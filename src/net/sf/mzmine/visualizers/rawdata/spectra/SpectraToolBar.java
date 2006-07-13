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

package net.sf.mzmine.visualizers.rawdata.spectra;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

/**
 * Spectra visualizer's toolbar class
 */
class SpectraToolBar extends JToolBar {

    static final Icon centroidIcon = new ImageIcon("dist/centroidicon.png");
    static final Icon continuousIcon = new ImageIcon("dist/continuousicon.png");
    static final Icon dataPointsIcon = new ImageIcon("dist/datapointsicon.png");
    static final Icon annotationsIcon = new ImageIcon("dist/annotationsicon.png");
    static final Icon pickedPeakIcon = new ImageIcon("dist/pickedpeakicon.png");

    private JButton centroidContinuousButton, dataPointsButton;

    SpectraToolBar(SpectraVisualizerWindow masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        centroidContinuousButton = GUIUtils.addButton(this, null, centroidIcon,
                masterFrame, "TOGGLE_PLOT_MODE",
                "Toggle centroid/continuous mode");

        addSeparator();

        dataPointsButton = GUIUtils.addButton(this, null, dataPointsIcon,
                masterFrame, "SHOW_DATA_POINTS",
                "Toggle displaying of data points  in continuous mode");

        addSeparator();

        GUIUtils.addButton(this, null, annotationsIcon, masterFrame,
                "SHOW_ANNOTATIONS", "Toggle displaying of peak values");

        addSeparator();

        GUIUtils.addButton(this, null, pickedPeakIcon, masterFrame,
                "SHOW_PICKED_PEAKS", "Toggle displaying of picked peaks");

    }

    void setCentroidButton(boolean centroid) {
        if (centroid) {
            centroidContinuousButton.setIcon(centroidIcon);
            dataPointsButton.setEnabled(true);
        } else {
            centroidContinuousButton.setIcon(continuousIcon);
            dataPointsButton.setEnabled(false);
        }
    }

}
