/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.neutralloss;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JInternalFrame;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.util.Range;

import org.jfree.chart.axis.ValueAxis;

/**
 * Neutral loss visualizer using JFreeChart library
 */
public class NeutralLossVisualizerWindow extends JInternalFrame implements
		ActionListener {

	private NeutralLossToolBar toolBar;
	private NeutralLossPlot neutralLossPlot;

	private NeutralLossDataSet dataset;

	private RawDataFile dataFile;

	public NeutralLossVisualizerWindow(RawDataFile dataFile,
			NeutralLossParameters parameters) {

		super(dataFile.toString(), true, true, true, true);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setBackground(Color.white);

		this.dataFile = dataFile;

		// Retrieve parameter's values
		Range rtRange = parameters.getParameter(
				NeutralLossParameters.retentionTimeRange).getValue();
		Range mzRange = parameters.getParameter(NeutralLossParameters.mzRange)
				.getValue();
		int numOfFragments = parameters.getParameter(
				NeutralLossParameters.numOfFragments).getInt();

		Object xAxisType = parameters
				.getParameter(NeutralLossParameters.xAxisType);

		// Set window components
		dataset = new NeutralLossDataSet(dataFile, xAxisType, rtRange, mzRange,
				numOfFragments, this);

		neutralLossPlot = new NeutralLossPlot(this, dataset, xAxisType);
		add(neutralLossPlot, BorderLayout.CENTER);

		toolBar = new NeutralLossToolBar(this);
		add(toolBar, BorderLayout.EAST);

		// Set range for Y axis
		ValueAxis yAxis = neutralLossPlot.getXYPlot().getDomainAxis();

		if (xAxisType == NeutralLossParameters.xAxisRT)
			yAxis.setRange(rtRange.getMin(), rtRange.getMax());
		else
			yAxis.setRange(mzRange.getMin(), mzRange.getMax());

		updateTitle();

		pack();

	}

	void updateTitle() {

		StringBuffer title = new StringBuffer();
		title.append("[");
		title.append(dataFile.toString());
		title.append("]: neutral loss");

		setTitle(title.toString());

		NeutralLossDataPoint pos = getCursorPosition();

		if (pos != null) {
			title.append(", ");
			title.append(pos.toString());
		}

		neutralLossPlot.setTitle(title.toString());

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();

		if (command.equals("HIGHLIGHT_PRECURSOR")
				|| command.equals("HIGHLIGHT_NEUTRALLOSS")) {
			JDialog dialog = new NeutralLossSetHighlightDialog(neutralLossPlot,
					command);
			dialog.setVisible(true);
		}

		if (command.equals("SHOW_SPECTRUM")) {
			NeutralLossDataPoint pos = getCursorPosition();
			if (pos != null) {
				SpectraVisualizer.showNewSpectrumWindow(dataFile,
						pos.getScanNumber());
			}
		}

	}

	/**
     * 
     */
	public NeutralLossDataPoint getCursorPosition() {
		double xValue = (double) neutralLossPlot.getXYPlot()
				.getDomainCrosshairValue();
		double yValue = (double) neutralLossPlot.getXYPlot()
				.getRangeCrosshairValue();

		NeutralLossDataPoint point = dataset.getDataPoint(xValue, yValue);

		return point;

	}

	NeutralLossPlot getPlot() {
		return neutralLossPlot;
	}

}
