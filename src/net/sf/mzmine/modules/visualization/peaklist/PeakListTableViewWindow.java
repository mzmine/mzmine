/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.modules.visualization.spectra.SpectraSetupDialog;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.modules.visualization.tic.TICSetupDialog;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;


/**
 *
 */
public class PeakListTableViewWindow extends JInternalFrame implements ActionListener {

	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private PeakListTable table;

	private TaskController taskController;
	private Desktop desktop;
	
	private OpenedRawDataFile rawData;
	
	public PeakListTableViewWindow(TaskController taskController, Desktop desktop, OpenedRawDataFile rawData) {

		super(rawData.toString() + " Peak list", true, true, true, true);
		
		this.taskController = taskController;
		this.desktop = desktop;
		this.rawData = rawData;

		
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

		// Build toolbar
        PeakListTableToolBar toolBar = new PeakListTableToolBar(this);

		// Build table
		table = new PeakListTable(this, rawData);
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		
		JScrollPane tableScroll = new JScrollPane(table);

		add(toolBar, BorderLayout.EAST);
		add(tableScroll, BorderLayout.CENTER);

		pack();
	}


	public void actionPerformed(java.awt.event.ActionEvent event) {

        String command = event.getActionCommand();


        if (command.equals("SHOW_SPECTRUM_FOR_PEAK")) {

        	Peak[] peaks = getSelectedPeaks();
        	if (peaks==null) return;
        	
        	for (Peak p : peaks) {	
        		SpectraSetupDialog dialog = new SpectraSetupDialog(taskController, desktop, rawData, 1, p.getScanNumbers(), 0.01);
        		dialog.setVisible(true);
        	}
			
		}

        if (command.equals("SHOW_XIC_FOR_PEAK")) {
        	
			// Open a new chromatographic visualizer showing XIC with all selected peaks shaded
        	
        	Peak[] peaks = getSelectedPeaks();
        	if (peaks==null) return;
        	
        	// Determine m/z range for XIC (use minimum and maximum m/z of all peaks)
			// TODO: Use mass accuracy to define XIC width
        	double minMZ = Double.MAX_VALUE;
        	double maxMZ = Double.MIN_VALUE;
        	for (Peak p : peaks) {
        		if (p.getMinMZ()<minMZ) 
        			minMZ = p.getMinMZ();
        		if (p.getMaxMZ()>maxMZ) 
        			maxMZ = p.getMaxMZ();
        	}
        		
			TICSetupDialog setupDialog = new TICSetupDialog(taskController, desktop, rawData, minMZ, maxMZ, peaks);
			setupDialog.setVisible(true);	
     	       	
		}
        
        if (command.equals("SHOW_ALIGNMENTS_FOR_PEAK")) {
        	Peak[] peaks = getSelectedPeaks();
        	if (peaks != null) {
        		
        		for (Peak p : peaks) {
	        		MZmineProject project = MZmineProject.getCurrentProject();      		
	        		AlignmentResult[] alignmentResults = project.getAlignmentResults();
	        		for (AlignmentResult alignmentResult : alignmentResults) {
	        			// TODO: Must implement function to AlignmentResult for checking if peak is there 
	        			//if (alignmentResult.containsPeak(p)) {}
	        		}
        		}
        		
        	}
        }

	}

	public void setSelectedPeak(Peak p) {
		table.setSelectedPeak(p);
	}
/*
	public Peak getSelectedPeak() {
		return table.getSelectedPeak();
	}
	*/
	public Peak[] getSelectedPeaks() {
		return table.getSelectedPeaks();
	}





}

