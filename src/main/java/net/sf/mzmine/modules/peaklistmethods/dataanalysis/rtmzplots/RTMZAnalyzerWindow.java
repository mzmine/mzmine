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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;
import net.sf.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;
import net.sf.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScaleSetupDialog;

import org.jfree.data.xy.AbstractXYZDataset;

public class RTMZAnalyzerWindow extends JFrame implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private RTMZToolbar toolbar;
    private RTMZPlot plot;

    public RTMZAnalyzerWindow(AbstractXYZDataset dataset, PeakList peakList,
	    InterpolatingLookupPaintScale paintScale) {
	super("");

	toolbar = new RTMZToolbar(this);
	add(toolbar, BorderLayout.EAST);

	plot = new RTMZPlot(this, dataset, paintScale);
	add(plot, BorderLayout.CENTER);

	String title = peakList.getName();
	title = title.concat(" : ");
	title = title.concat(dataset.toString());
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

	if (command.equals("SETUP_COLORS")) {
	    InterpolatingLookupPaintScaleSetupDialog colorDialog = new InterpolatingLookupPaintScaleSetupDialog(
		    this, plot.getPaintScale());
	    colorDialog.setVisible(true);

	    if (colorDialog.getExitCode() == ExitCode.OK)
		plot.setPaintScale(colorDialog.getPaintScale());
	}

    }

}
