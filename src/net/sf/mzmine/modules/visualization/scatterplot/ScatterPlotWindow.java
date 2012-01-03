/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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
import javax.swing.JInternalFrame;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.modules.visualization.scatterplot.scatterplotchart.ScatterPlotChart;

/**
 * Main window of the scatter plot visualizer.
 * 
 */
public class ScatterPlotWindow extends JInternalFrame {

	private ScatterPlotToolBar toolbar;
	private ScatterPlotChart chart;
	private ScatterPlotTopPanel topPanel;
	private ScatterPlotBottomPanel bottomPanel;

	public ScatterPlotWindow(PeakList peakList) {

		super("Scatter plot of " + peakList, true, true, true, true);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		topPanel = new ScatterPlotTopPanel();
		add(topPanel, BorderLayout.NORTH);

		chart = new ScatterPlotChart(topPanel, peakList);
		Border border = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		chart.setBorder(border);
		chart.setBackground(Color.white);
		add(chart, BorderLayout.CENTER);

		toolbar = new ScatterPlotToolBar(chart);
		add(toolbar, BorderLayout.EAST);

		JComponent leftMargin = (JComponent) Box.createRigidArea(new Dimension(10,10));
		leftMargin.setOpaque(false);
		add(leftMargin, BorderLayout.WEST);
		
		bottomPanel = new ScatterPlotBottomPanel(chart, peakList);
		add(bottomPanel, BorderLayout.SOUTH);

		pack();

	}

}
