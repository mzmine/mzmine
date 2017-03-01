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

package net.sf.mzmine.modules.visualization.msms;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

/**
 * MS/MS visualizer's toolbar class
 */
class MsMsToolBar extends JToolBar {

    private static final long serialVersionUID = 1L;
    static final Icon axesIcon = new ImageIcon("icons/axesicon.png");
    static final Icon dataPointsIcon = new ImageIcon("icons/datapointsicon.png");
    static final Icon tooltipsIcon = new ImageIcon(
	    "icons/tooltips2dploticon.png");
    static final Icon notooltipsIcon = new ImageIcon(
	    "icons/notooltips2dploticon.png");
    static final Icon findIcon = new ImageIcon("icons/search.png");

    private JButton toggleContinuousModeButton, toggleTooltipButton;

    MsMsToolBar(MsMsVisualizerWindow masterFrame) {

	super(JToolBar.VERTICAL);

	setFloatable(false);
	setFocusable(false);
	setMargin(new Insets(5, 5, 5, 5));
	setBackground(Color.white);

	GUIUtils.addButton(this, null, axesIcon, masterFrame, "SETUP_AXES",
		"Setup ranges for axes");

	addSeparator();

	toggleContinuousModeButton = GUIUtils.addButton(this, null,
		dataPointsIcon, masterFrame, "SHOW_DATA_POINTS",
		"Toggle displaying of data points for the peaks");

	addSeparator();

	toggleTooltipButton = GUIUtils.addButton(this, null, tooltipsIcon,
		masterFrame, "SWITCH_TOOLTIPS",
		"Toggle displaying of tool tips on the peaks");

	addSeparator();

	GUIUtils.addButton(this, null, findIcon, masterFrame, "FIND_SPECTRA",
		"Search for MS/MS spectra with specific ions");

    }

    void toggleContinuousModeButtonSetEnable(boolean enable) {
	toggleContinuousModeButton.setEnabled(enable);
    }

    void setTooltipButton(boolean tooltip) {
	if (tooltip) {
	    toggleTooltipButton.setIcon(tooltipsIcon);
	} else {
	    toggleTooltipButton.setIcon(notooltipsIcon);
	}
    }

}
