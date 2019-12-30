/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.msms;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JToolBar;
import io.github.mzmine.util.GUIUtils;

/**
 * MS/MS visualizer's toolbar class
 */
class MsMsToolBar extends JToolBar {

  private static final long serialVersionUID = 1L;

  private JButton toggleContinuousModeButton, toggleTooltipButton;

  MsMsToolBar(MsMsVisualizerWindow masterFrame) {

    super(JToolBar.VERTICAL);

    setFloatable(false);
    setFocusable(false);
    setMargin(new Insets(5, 5, 5, 5));
    setBackground(Color.white);

    GUIUtils.addButton(this, null, axesIcon, masterFrame, "SETUP_AXES", "Setup ranges for axes");

    addSeparator();

    toggleContinuousModeButton = GUIUtils.addButton(this, null, dataPointsIcon, masterFrame,
        "SHOW_DATA_POINTS", "Toggle displaying of data points for the peaks");

    addSeparator();

    toggleTooltipButton = GUIUtils.addButton(this, null, tooltipsIcon, masterFrame,
        "SWITCH_TOOLTIPS", "Toggle displaying of tool tips on the peaks");

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
