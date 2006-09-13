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

package net.sf.mzmine.visualizers.peaklist.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.visualizers.peaklist.PeakListVisualizer;


/**
 *
 */
public class PeakListTableViewWindow extends JInternalFrame implements PeakListVisualizer, ActionListener {

	private OpenedRawDataFile rawData;
	private PeakListTable table;


	public PeakListTableViewWindow(OpenedRawDataFile rawData) {

		super(rawData.toString() + " Peak list", true, true, true, true);

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


        if (command.equals("ZOOM_TO_PEAK")) {

			Peak p = getSelectedPeak();
			// TODO: Update cursor position on all(?) raw data visualizers showing this rawData
			// Requires solution for raw data visualizer interaction.
		}

        if (command.equals("FIND_IN_ALIGNMENTS")) {

			Peak p = getSelectedPeak();
			// TODO: Update selected row on visualizers for all(?) alignment result where this peakList is participating
			// Requires implementing  alignment results and their visualizers.
		}

	}

	public void setSelectedPeak(Peak p) {
		table.setSelectedPeak(p);
	}

	public Peak getSelectedPeak() {
		return table.getSelectedPeak();
	}





}

