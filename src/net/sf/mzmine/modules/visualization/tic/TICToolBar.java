/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.tic;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

/**
 * TIC visualizer's toolbar class
 */
public class TICToolBar extends JToolBar {

    static final Icon showSpectrumIcon = new ImageIcon("icons/spectrumicon.png");
    static final Icon dataPointsIcon = new ImageIcon("icons/datapointsicon.png");
    static final Icon annotationsIcon = new ImageIcon(
            "icons/annotationsicon.png");
    static final Icon axesIcon = new ImageIcon("icons/axesicon.png");

    public TICToolBar(ActionListener masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        GUIUtils.addButton(this, null, showSpectrumIcon, masterFrame,
                "SHOW_SPECTRUM", "Show spectrum of selected scan");

        addSeparator();

        GUIUtils.addButton(this, null, dataPointsIcon, masterFrame,
                "SHOW_DATA_POINTS", "Toggle displaying of data points");

        addSeparator();

        GUIUtils.addButton(this, null, annotationsIcon, masterFrame,
                "SHOW_ANNOTATIONS", "Toggle displaying of peak values");

        addSeparator();

        GUIUtils.addButton(this, null, axesIcon, masterFrame, "SETUP_AXES",
                "Setup ranges for axes");

    }

}
