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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.scatterplot.scatterplotchart.ScatterPlotChart;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;

/**
 * Main window of the scatter plot visualizer.
 * 
 */
public class ScatterPlotWindow extends JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ScatterPlotToolBar toolbar;
    private ScatterPlotChart chart;
    private ScatterPlotTopPanel topPanel;
    private ScatterPlotBottomPanel bottomPanel;

    public ScatterPlotWindow(PeakList peakList) {

	super("Scatter plot of " + peakList);

	setDefaultCloseOperation(DISPOSE_ON_CLOSE);

	topPanel = new ScatterPlotTopPanel();
	add(topPanel, BorderLayout.NORTH);

	chart = new ScatterPlotChart(this, topPanel, peakList);
	Border border = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
	chart.setBorder(border);
	chart.setBackground(Color.white);
	add(chart, BorderLayout.CENTER);

	toolbar = new ScatterPlotToolBar(chart);
	add(toolbar, BorderLayout.EAST);

	JComponent leftMargin = (JComponent) Box.createRigidArea(new Dimension(
		10, 10));
	leftMargin.setOpaque(false);
	add(leftMargin, BorderLayout.WEST);

	bottomPanel = new ScatterPlotBottomPanel(this, chart, peakList);
	add(bottomPanel, BorderLayout.SOUTH);

	// Add the Windows menu
	JMenuBar menuBar = new JMenuBar();
	menuBar.add(new WindowsMenu());
	setJMenuBar(menuBar);

	pack();

	// get the window settings parameter
	ParameterSet paramSet = MZmineCore.getConfiguration()
		.getModuleParameters(ScatterPlotVisualizerModule.class);
	WindowSettingsParameter settings = paramSet
		.getParameter(ScatterPlotParameters.windowSettings);

	// update the window and listen for changes
	settings.applySettingsToWindow(this);
	this.addComponentListener(settings);

    }

}
