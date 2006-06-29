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

package net.sf.mzmine.visualizers.rawdata.tic;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

/**
 * 
 */
class TICToolBar extends JToolBar {

    private JButton showSpectraButton, dataPointsButton, annotationsButton;

    static final Icon showSpectrumIcon = new ImageIcon("spectrumicon.png");
    static final Icon dataPointsIcon = new ImageIcon("datapointsicon.png");
    static final Icon annotationsIcon = new ImageIcon("annotationsicon.png");

    TICToolBar(TICVisualizer masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        showSpectraButton = new JButton(showSpectrumIcon);
        showSpectraButton.setActionCommand("SHOW_SPECTRUM");
        showSpectraButton.setToolTipText("Show spectrum of selected scan");
        showSpectraButton.addActionListener(masterFrame);

        dataPointsButton = new JButton(dataPointsIcon);
        dataPointsButton.setActionCommand("SHOW_DATA_POINTS");
        dataPointsButton.setToolTipText("Toggle displaying of data points");
        dataPointsButton.addActionListener(masterFrame);

        annotationsButton = new JButton(annotationsIcon);
        annotationsButton.setActionCommand("SHOW_ANNOTATIONS");
        annotationsButton.setToolTipText("Toggle displaying of peak values");
        annotationsButton.addActionListener(masterFrame);

        add(showSpectraButton);
        addSeparator();
        add(dataPointsButton);
        addSeparator();
        add(annotationsButton);

    }

}
