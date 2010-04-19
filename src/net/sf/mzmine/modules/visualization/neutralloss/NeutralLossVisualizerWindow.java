/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.util.Range;

/**
 * Neutral loss visualizer using JFreeChart library
 */
public class NeutralLossVisualizerWindow extends JInternalFrame implements
		ActionListener {

	private NeutralLossToolBar toolBar;
	private NeutralLossPlot neutralLossPlot;

	private NeutralLossDataSet dataset;

	private RawDataFile dataFile;

	private Desktop desktop;

	public NeutralLossVisualizerWindow(RawDataFile dataFile, Object xAxisType,
			Range rtRange, Range mzRange, int numOfFragments) {

		super(dataFile.toString(), true, true, true, true);

		this.desktop = MZmineCore.getDesktop();

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setBackground(Color.white);

		this.dataFile = dataFile;

		dataset = new NeutralLossDataSet(dataFile, xAxisType, rtRange, mzRange,
				numOfFragments, this);

		toolBar = new NeutralLossToolBar(this);
		add(toolBar, BorderLayout.EAST);

		neutralLossPlot = new NeutralLossPlot(this, dataset, xAxisType);
		add(neutralLossPlot, BorderLayout.CENTER);

		if (xAxisType == NeutralLossParameters.xAxisRT)
			setDomainRange(rtRange);
		else
			setDomainRange(mzRange);

		updateTitle();

		pack();

	}

	/**
     */
	public void setRangeRange(Range rng) {
		neutralLossPlot.getXYPlot().getRangeAxis().setRange(rng.getMin(),
				rng.getMax());
	}

	/**
     */
	public void setDomainRange(Range rng) {
		neutralLossPlot.getXYPlot().getDomainAxis().setRange(rng.getMin(),
				rng.getMax());
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

		if (command.equals("HIGHLIGHT")) {
			JDialog dialog = new NeutralLossSetHighlightDialog(desktop,
					neutralLossPlot);
			dialog.setVisible(true);
		}

		if (command.equals("SHOW_SPECTRUM")) {
			NeutralLossDataPoint pos = getCursorPosition();
			if (pos != null) {
				SpectraVisualizer.showNewSpectrumWindow(dataFile, pos
						.getScanNumber());
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
