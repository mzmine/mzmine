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
package net.sf.mzmine.modules.visualization.alignmentresult.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;


import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelectionDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

public class AlignmentResultTableVisualizerWindow extends JInternalFrame implements ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private JScrollPane scrollPane;

    /*
	private JPopupMenu popupMenu;
	private JMenuItem changeFormattingMenuItem;
	private JMenuItem zoomToPeakMenuItem;
    */
    
	private AlignmentResultTable table;
	
	


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
        add(toolBar, BorderLayout.EAST);

		// Build table
		table = new AlignmentResultTable(this, alignmentResult);
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		scrollPane = new JScrollPane(table);
		
		add(scrollPane, BorderLayout.CENTER);

		pack();

	}




	/**
	 * Methods for ActionListener interface implementation
	 */
	public void actionPerformed(ActionEvent event) {
		
        String command = event.getActionCommand();

        if (command.equals("ZOOM_TO_PEAK")) {
			// TODO
		}

        if (command.equals("CHANGE_FORMAT")) {
        	
        	AlignmentResultColumnSelectionDialog dialog = new AlignmentResultColumnSelectionDialog(table.getColumnSelection(), table);
        	MainWindow.getInstance().addInternalFrame(dialog);
		}

	}



}

