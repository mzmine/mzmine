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
import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.Vector;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.interfaces.Peak;
import net.sf.mzmine.userinterface.mainwindow.ItemSelector;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.userinterface.mainwindow.Statusbar;
import net.sf.mzmine.visualizers.peaklist.PeakListVisualizer;


/**
 *
 */
public class TableView extends JInternalFrame implements PeakListVisualizer, ActionListener {

	private RawDataFile rawData;

	private JTable table;
	private JScrollPane scrollPane;

	private JMenuItem zoomToPeakMenuItem;
	private JMenuItem findInAlignmentsMenuItem;

	private boolean doNotAutoRefresh = false;
	private boolean firstTimer = true;
	private int selectedPeakID = -1;


	public TableView(RawDataFile rawData) {

		super(rawData.toString() + " Peak list", true, true, true, true);

		this.rawData = rawData;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);


		// Build toolbar
        TableViewToolBar toolBar = new TableViewToolBar(this);


		// Build pop-up menu
		/*
		JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.addSeparator();
		GUIUtils.addMenuItem(popupMenu, "Zoom visualizers to peak", this, "ZOOM_TO_PEAK");
		GUIUtils.addMenuItem(popupMenu, "Find peak in alignments", this, "FIND_IN_ALIGNMENTS");
		popupMenu.addSeparator();
		pack();
		*/

		// Build table
		TableViewTable table = new TableViewTable(rawData);
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		JScrollPane tableScroll = new JScrollPane(table);

		/*
		SelectionListener listener = new SelectionListener(table);
    	table.getSelectionModel().addListSelectionListener(listener);
	    table.getColumnModel().getSelectionModel().addListSelectionListener(listener);

	    table.getTableHeader().setToolTipText("Click to specify sorting; Control-Click to specify secondary sorting");
	    */

		add(toolBar, BorderLayout.EAST);
		add(tableScroll, BorderLayout.CENTER);

		pack();
	}



	public void actionPerformed(java.awt.event.ActionEvent e) {
		Object src = e.getSource();

		if (src == zoomToPeakMenuItem) {
			//setZoomAroundSelectedPeak();
		}


	}

	public void setSelectedPeak(Peak p) {
	}

	public Peak getSelectedPeak() {
		return null;
	}





}

