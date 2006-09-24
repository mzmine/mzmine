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
package net.sf.mzmine.visualizers.alignmentresult.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.mainwindow.ItemSelector;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizer;
import sunutils.TableSorter;


/*
TODO
- this should implement AlignmentResultVisualizer
- everything

*/

public class AlignmentResultTableVisualizerWindow extends JInternalFrame implements MouseListener, ActionListener {



	private AlignmentResult alignmentResult;

	private JScrollPane scrollPane;

	private JPopupMenu popupMenu;
	private JMenuItem changeFormattingMenuItem;
	private JMenuItem zoomToPeakMenuItem;

	private boolean compactMode = true;



	/**
	 * Constructor: initializes an empty visualizer
	 */
	public AlignmentResultTableVisualizerWindow(AlignmentResult alignmentResult) {

		super(alignmentResult.toString(), true, true, true, true);

		setResizable( true );
		setIconifiable( true );

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

		// Build toolbar
        AlignmentResultTableVisualizerToolBar toolBar = new AlignmentResultTableVisualizerToolBar(this);

		// Build table
		AlignmentResultTable table = new AlignmentResultTable(this, alignmentResult);
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		scrollPane = new JScrollPane(table);

		add(toolBar, BorderLayout.EAST);
		add(scrollPane, BorderLayout.CENTER);

		table.addMouseListener(this);

		pack();

	}




	/**
	 * Methods for MouseListener interface implementation
	 */
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {

		// Normal click: select row
		if (e.getButton()==MouseEvent.BUTTON1) {
			//int rowInd = ((Integer)(table.getValueAt(table.getSelectedRow(), 0))).intValue();
			// TODO: What to do when row is clicked?
		}

	}



	/**
	 * Methods for ActionListener interface implementation
	 */
	public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("ZOOM_TO_PEAK")) {
			// TODO
		}

        if (command.equals("CHANGE_COLUMN_FORMAT")) {
			// TODO
			/*
				AbstractTableModel mtm = new MyTableModel(alignmentResult);
				TableSorter sorter = new TableSorter(mtm); //ADDED THIS
				table.getTableHeader().setReorderingAllowed(false);
				//sorter.setTableHeader(table.getTableHeader()); //ADDED THIS	(REMOVED FOR TEST)
				sorter.addMouseListenerToHeaderInTable(table); // ADDED THIS TODAY
				table.setModel(sorter);
			*/
		}

	}



}

