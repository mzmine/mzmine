/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.tic;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.swing.IconUtil;

/**
 * TIC visualizer's toolbar.
 */
public class TICToolBar extends JToolBar {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // Margins.
    private static final Insets MARGIN = new Insets(5, 5, 5, 5);

    // Icons.
    private static final Icon SHOW_SPECTRUM_ICON = IconUtil
            .loadIconFromResources("icons/spectrumicon.png");
    private static final Icon DATA_POINTS_ICON = IconUtil
            .loadIconFromResources("icons/datapointsicon.png");
    private static final Icon ANNOTATIONS_ICON = IconUtil
            .loadIconFromResources("icons/annotationsicon.png");
    private static final Icon AXES_ICON = IconUtil
            .loadIconFromResources("icons/axesicon.png");
    private static final Icon LEGEND_ICON = IconUtil
            .loadIconFromResources("icons/legendkey.png");
    private static final Icon BACKGROUND_ICON = IconUtil
            .loadIconFromResources("icons/bgicon.png");

    public TICToolBar(final ActionListener listener) {

        super(SwingConstants.VERTICAL);
        setFloatable(false);
        setMargin(MARGIN);
        setBackground(Color.white);

        GUIUtils.addButton(this, null, SHOW_SPECTRUM_ICON, listener,
                "SHOW_SPECTRUM", "Show spectrum of selected scan");

        addSeparator();

        GUIUtils.addButton(this, null, DATA_POINTS_ICON, listener,
                "SHOW_DATA_POINTS", "Toggle displaying of data points");

        addSeparator();

        GUIUtils.addButton(this, null, ANNOTATIONS_ICON, listener,
                "SHOW_ANNOTATIONS", "Toggle displaying of peak labels");

        addSeparator();

        GUIUtils.addButton(this, null, AXES_ICON, listener, "SETUP_AXES",
                "Setup ranges for axes");

        addSeparator();

        GUIUtils.addButton(this, null, LEGEND_ICON, listener, "SHOW_LEGEND",
                "Toggle display of the legend");

        addSeparator();

        GUIUtils.addButton(this, null, BACKGROUND_ICON, listener,
                "GRAY_BACKGROUND",
                "Toggle between white or gray background color");
    }
}
