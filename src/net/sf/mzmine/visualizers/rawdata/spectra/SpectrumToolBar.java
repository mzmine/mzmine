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

/**
 * 
 */
class SpectrumToolBar extends JToolBar {

    static final Icon zoomOutIcon = new ImageIcon("zoomouticon.png");
    static final Icon centroidIcon = new ImageIcon("centroidicon.png");
    static final Icon continuousIcon = new ImageIcon("continuousicon.png");
    static final Icon dataPointsIcon = new ImageIcon("datapointsicon.png");
    static final Icon annotationsIcon = new ImageIcon("annotationsicon.png");

    private JButton zoomOutButton, centroidContinuousButton, dataPointsButton,
            annotationsButton;

    SpectrumToolBar(SpectrumVisualizer masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        zoomOutButton = new JButton(zoomOutIcon);
        zoomOutButton.setEnabled(false);
        zoomOutButton.setActionCommand("ZOOM_OUT");
        zoomOutButton.setToolTipText("Zoom out");
        zoomOutButton.addActionListener(masterFrame);

        centroidContinuousButton = new JButton(centroidIcon);
        centroidContinuousButton.setActionCommand("SET_PLOT_MODE");
        centroidContinuousButton.setToolTipText("Show as centroid");
        centroidContinuousButton.addActionListener(masterFrame);

        dataPointsButton = new JButton(dataPointsIcon);
        dataPointsButton.setActionCommand("SHOW_DATA_POINTS");
        dataPointsButton.setToolTipText("Toggle data points display");
        dataPointsButton.addActionListener(masterFrame);

        annotationsButton = new JButton(annotationsIcon);
        annotationsButton.setActionCommand("SHOW_ANNOTATIONS");
        annotationsButton.setToolTipText("Toggle displaying of peak values");
        annotationsButton.addActionListener(masterFrame);

        add(zoomOutButton);
        addSeparator();
        add(centroidContinuousButton);
        addSeparator();
        add(dataPointsButton);
        addSeparator();
        add(annotationsButton);

    }

    void setZoomOutButtonEnabled(boolean enabled) {
        zoomOutButton.setEnabled(enabled);
    }

    void setCentroidButton(boolean centroid) {
        if (centroid) {
            centroidContinuousButton.setIcon(centroidIcon);
            centroidContinuousButton.setToolTipText("Show as centroid");
        } else {
            centroidContinuousButton.setIcon(continuousIcon);
            centroidContinuousButton.setToolTipText("Show as continuous");
        }
    }

}
