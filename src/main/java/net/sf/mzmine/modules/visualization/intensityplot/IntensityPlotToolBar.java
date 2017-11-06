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

package net.sf.mzmine.modules.visualization.intensityplot;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 * Intensity plot toolbar class
 */
class IntensityPlotToolBar extends JToolBar implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    static final Icon pointsIcon = new ImageIcon("icons/pointsicon.png");
    static final Icon linesIcon = new ImageIcon("icons/linesicon.png");
    static final Icon axesIcon = new ImageIcon("icons/axesicon.png");

    private IntensityPlotWindow window;
    private JButton linesVisibleButton, setupAxesButton;

    IntensityPlotToolBar(IntensityPlotWindow window) {

	super(JToolBar.VERTICAL);

	setFloatable(false);
	setMargin(new Insets(5, 5, 5, 5));
	setBackground(Color.white);

	this.window = window;

	linesVisibleButton = GUIUtils.addButton(this, null, linesIcon, this,
		null, "Switch lines on/off");

	if (window.getChart().getPlot() instanceof XYPlot) {
	    addSeparator();
	    setupAxesButton = GUIUtils.addButton(this, null, axesIcon, this,
		    "SETUP_AXES", "Setup ranges for axes");
	}

    }

    public void actionPerformed(ActionEvent e) {

	Object src = e.getSource();

	if (src == linesVisibleButton) {

	    Plot plot = window.getChart().getPlot();

	    Boolean linesVisible;

	    if (plot instanceof CategoryPlot) {
		LineAndShapeRenderer renderer = (LineAndShapeRenderer) ((CategoryPlot) plot)
			.getRenderer();
		linesVisible = renderer.getDefaultLinesVisible();
	    } else {
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) ((XYPlot) plot)
			.getRenderer();
		linesVisible = renderer.getDefaultLinesVisible();
		renderer.setDrawSeriesLineAsPath(true);
	    }

	    // check for null value
	    if (linesVisible == null)
		linesVisible = false;

	    // update the icon
	    if (linesVisible) {
		linesVisibleButton.setIcon(linesIcon);
	    } else {
		linesVisibleButton.setIcon(pointsIcon);
	    }

	    // switch the button
	    linesVisible = !linesVisible;

	    if (plot instanceof CategoryPlot) {
		LineAndShapeRenderer renderer = (LineAndShapeRenderer) ((CategoryPlot) plot)
			.getRenderer();
		renderer.setDefaultLinesVisible(linesVisible);
	    } else {
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) ((XYPlot) plot)
			.getRenderer();
		renderer.setDefaultLinesVisible(linesVisible);
		renderer.setDrawSeriesLineAsPath(true);
	    }

	}

	if (src == setupAxesButton) {
	    AxesSetupDialog dialog = new AxesSetupDialog(window, window
		    .getChart().getXYPlot());
	    dialog.setVisible(true);
	}

    }

}
