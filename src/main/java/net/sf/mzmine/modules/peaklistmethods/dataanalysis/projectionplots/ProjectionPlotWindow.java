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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.PeakMeasurementType;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

public class ProjectionPlotWindow extends JFrame implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ProjectionPlotToolbar toolbar;
    private ProjectionPlotPanel plot;

    public ProjectionPlotWindow(PeakList peakList,
	    ProjectionPlotDataset dataset, ParameterSet parameters) {

	toolbar = new ProjectionPlotToolbar(this);
	add(toolbar, BorderLayout.EAST);

	plot = new ProjectionPlotPanel(this, dataset, parameters);
	add(plot, BorderLayout.CENTER);

	String title = peakList.getName();
	title = title.concat(" : ");
	title = title.concat(dataset.toString());
	if (parameters.getParameter(
		ProjectionPlotParameters.peakMeasurementType).getValue() == PeakMeasurementType.HEIGHT)
	    title = title.concat(" (using peak heights)");
	else
	    title = title.concat(" (using peak areas)");

	this.setTitle(title);

	pack();

    }

    public void actionPerformed(ActionEvent event) {

	String command = event.getActionCommand();

	if (command.equals("SETUP_AXES")) {
	    AxesSetupDialog dialog = new AxesSetupDialog(this, plot.getChart()
		    .getXYPlot());
	    dialog.setVisible(true);
	}

	if (command.equals("TOGGLE_LABELS")) {
	    /*
	     * XYItemRenderer rend = plot.getChart().getXYPlot().getRenderer();
	     * rend.setBaseItemLabelsVisible(!rend.getBaseItemLabelsVisible());
	     */
	    plot.cycleItemLabelMode();
	}

    }

}
