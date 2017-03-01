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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;

import net.sf.mzmine.modules.visualization.scatterplot.scatterplotchart.ScatterPlotChart;
import net.sf.mzmine.util.GUIUtils;

public class ScatterPlotToolBar extends JToolBar {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    static final Icon axesIcon = new ImageIcon("icons/axesicon.png");

    public ScatterPlotToolBar(ScatterPlotChart chart) {

	super(JToolBar.VERTICAL);

	setFloatable(false);
	setFocusable(false);
	setOpaque(true);
	setMargin(new Insets(5, 5, 5, 5));

	GUIUtils.addButton(this, null, axesIcon, chart, "SETUP_AXES",
		"Setup ranges for axes");

    }

}