/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.dataanalysis.rtmzplots;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;

import org.jfree.data.xy.AbstractXYZDataset;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;
import net.sf.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScaleSetupDialog;

public class RTMZAnalyzerWindow extends JInternalFrame implements ActionListener {

	private Desktop desktop;
	private RTMZToolbar toolbar;
	private RTMZPlot plot;
	
	public RTMZAnalyzerWindow(Desktop desktop, AbstractXYZDataset dataset, PeakList peakList, SimpleParameterSet parameters, InterpolatingLookupPaintScale paintScale) {
		super(null, true, true, true, true);
		
		this.desktop = desktop;
		
        toolbar = new RTMZToolbar(this);
        add(toolbar, BorderLayout.EAST);
        
        plot = new RTMZPlot(this, dataset, paintScale);
        add(plot, BorderLayout.CENTER);
        
        String title = peakList.toString();
        title = title.concat(" : ");
        title = title.concat(dataset.toString());
        this.setTitle(title);
        
        pack();

        desktop.addInternalFrame(this);
		
	}
	
	public void actionPerformed(ActionEvent event) {
		
        String command = event.getActionCommand();
        
        if (command.equals("SETUP_AXES")) {
        	AxesSetupDialog dialog = new AxesSetupDialog(plot.getChart().getXYPlot());
        	dialog.setVisible(true);
        }
        
        if (command.equals("SETUP_COLORS")) {
        	InterpolatingLookupPaintScaleSetupDialog colorDialog = new InterpolatingLookupPaintScaleSetupDialog((Frame)desktop, plot.getPaintScale());
            colorDialog.setVisible(true);
            	
        	if (colorDialog.getExitCode()==ExitCode.OK)
        		plot.setPaintScale(colorDialog.getPaintScale());
        }
        
	}

}
