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
import java.util.logging.Logger;


import javax.swing.JDesktopPane;
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

public class AlignmentResultTableVisualizerWindow extends JInternalFrame implements ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private AlignmentResult alignmentResult;

	private JScrollPane scrollPane;

	private JPopupMenu popupMenu;
	private JMenuItem changeFormattingMenuItem;
	private JMenuItem zoomToPeakMenuItem;

	private AlignmentResultTable table;
	
	private boolean compactMode = true;



	/**
	 * Constructor: initializes an empty visualizer
	 */
	public AlignmentResultTableVisualizerWindow(AlignmentResult alignmentResult) {

		super(alignmentResult.toString(), true, true, true, true);
		
		logger.info("Initializing alignment result table visualizer window");

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
		
		logger.info("actionPerformed");
		
        String command = event.getActionCommand();
        
        logger.info("command=" + command);
        

        if (command.equals("ZOOM_TO_PEAK")) {
			// TODO
		}

        if (command.equals("CHANGE_FORMAT")) {
        	
        	AlignmentResultTableColumnSelectionDialog dialog = new AlignmentResultTableColumnSelectionDialog (table);
        	JDesktopPane desktop = MainWindow.getInstance().getDesktopPane();
        	desktop.add(dialog);
        	
        	logger.info("setting choose columns dialog visible");
        	dialog.setVisible(true);
        	dialog.setLocation( (int)(0.5*(desktop.getWidth()-dialog.getWidth())),
        						(int)(0.5*(desktop.getHeight()-dialog.getHeight())) );

		}

	}



}

