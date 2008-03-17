/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;

import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

import org.jfree.chart.renderer.xy.XYItemRenderer;

public class ProjectionPlotWindow extends JInternalFrame implements
		ActionListener {

	private ProjectionPlotToolbar toolbar;
	private ProjectionPlotPanel plot;

	public ProjectionPlotWindow(Desktop desktop, ProjectionPlotDataset dataset,
			ProjectionPlotParameters parameters) {
		super(null, true, true, true, true);

		toolbar = new ProjectionPlotToolbar(this);
		add(toolbar, BorderLayout.EAST);

		plot = new ProjectionPlotPanel(this, dataset);
		add(plot, BorderLayout.CENTER);

		String title = parameters.getSourcePeakList().toString();
		title = title.concat(" : ");
		title = title.concat(dataset.toString());
		if (parameters
				.getParameterValue(ProjectionPlotParameters.peakMeasurementType) == ProjectionPlotParameters.PeakMeasurementTypeHeight)
			title = title.concat(" (using peak heights)");
		if (parameters
				.getParameterValue(ProjectionPlotParameters.peakMeasurementType) == ProjectionPlotParameters.PeakMeasurementTypeArea)
			title = title.concat(" (using peak areas)");
		this.setTitle(title);

		pack();

	}

	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();

		if (command.equals("SETUP_AXES")) {
			AxesSetupDialog dialog = new AxesSetupDialog(plot.getChart()
					.getXYPlot());
			dialog.setVisible(true);
		}

		if (command.equals("TOGGLE_LABELS")) {
			XYItemRenderer rend = plot.getChart().getXYPlot().getRenderer();
			rend.setBaseItemLabelsVisible(!rend.getBaseItemLabelsVisible());
		}

	}

}
