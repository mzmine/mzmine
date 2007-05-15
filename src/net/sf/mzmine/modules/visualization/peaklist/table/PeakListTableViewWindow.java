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

package net.sf.mzmine.modules.visualization.peaklist.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.modules.visualization.peaklist.PeakListVisualizer;
import net.sf.mzmine.modules.visualization.rawdata.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.modules.visualization.rawdata.tic.TICSetupDialog;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;


/**
 *
 */
public class PeakListTableViewWindow extends JInternalFrame implements PeakListVisualizer, ActionListener {

	
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

			Peak p = getSelectedPeak();
			
			if (p != null)
				// TODO: M/Z bin size is hard coded here
                new SpectraVisualizerWindow(taskController, desktop, rawData, p.getScanNumbers(), 0.01);			
			
		}

        if (command.equals("SHOW_XIC_FOR_PEAK")) {
        	
        	logger.fine("Show XIC for a peak");
      	
        	
			Peak p = getSelectedPeak();
			
			// Open a new chromatographic visualizer showing XIC around peak's M/Z and filled area for peaks duration
			if (p != null) {
				
				logger.fine("Show setup dialog");
				
				// TODO: Use mass accuracy to define XIC width
				TICSetupDialog setupDialog = new TICSetupDialog(taskController, desktop, rawData, p.getMinMZ(), p.getMaxMZ(), p);
				setupDialog.setVisible(true);
				
			}
		}

	}

	public void setSelectedPeak(Peak p) {
		table.setSelectedPeak(p);
	}

	public Peak getSelectedPeak() {
		return table.getSelectedPeak();
	}





}

