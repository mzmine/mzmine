/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.sf.mzmine.visualizers.peaklist;
import java.awt.BorderLayout;
import java.awt.Graphics;
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
import net.sf.mzmine.methods.peakpicking.Peak;
import net.sf.mzmine.userinterface.mainwindow.ItemSelector;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.userinterface.mainwindow.Statusbar;
import net.sf.mzmine.visualizers.RawDataVisualizer;


/**
 *
 */
public class RawDataVisualizerPeakListView extends JInternalFrame implements Printable, InternalFrameListener, ActionListener, MouseListener {

	private static final int COLCOUNT = 10;

	private static final int COL_PEAKID = 0;
	private static final int COL_MZ = 1;
	private static final int COL_RT = 2;
	private static final int COL_HEIGHT = 3;
	private static final int COL_AREA = 4;
	private static final int COL_CHARGE = 5;
	private static final int COL_ISOTOPEPATTERNID = 6;
	private static final int COL_ISOTOPEPEAKNUMBER = 7;
	private static final int COL_DURATION = 8;
	private static final int COL_MZSTDEV = 9;

	private MainWindow mainWin;
	private Statusbar statBar;
	private ItemSelector itemSelector;

	private RawDataFile rawData;

	private JTable table;
	private JScrollPane scrollPane;

	private JPopupMenu popupMenu;
	private JMenuItem zoomToPeakMenuItem;
	private JMenuItem findInAlignmentsMenuItem;

	private boolean doNotAutoRefresh = false;
	private boolean firstTimer = true;
	private int selectedPeakID = -1;


	public RawDataVisualizerPeakListView(MainWindow _mainWin) {
		mainWin = _mainWin;
		statBar = mainWin.getStatusBar();
		itemSelector = mainWin.getItemSelector();

		setResizable( true );
		setIconifiable( true );

		// Build pop-up menu
		popupMenu = new JPopupMenu();
		zoomToPeakMenuItem = new JMenuItem("Zoom to peak");
		zoomToPeakMenuItem.addActionListener(this);
		zoomToPeakMenuItem.setEnabled(true);
		popupMenu.add(zoomToPeakMenuItem);
		findInAlignmentsMenuItem = new JMenuItem("Find this peak in alignments");
		findInAlignmentsMenuItem.addActionListener(this);
		findInAlignmentsMenuItem.setEnabled(true);
		popupMenu.add(findInAlignmentsMenuItem);


		// Build table
		table = new JTable();
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		scrollPane = new JScrollPane(table);

		SelectionListener listener = new SelectionListener(table);
    	table.getSelectionModel().addListSelectionListener(listener);
	    table.getColumnModel().getSelectionModel().addListSelectionListener(listener);

		//Set up tool tips for column headers.
		table.getTableHeader().setToolTipText("Click to specify sorting; Control-Click to specify secondary sorting");

		setTitle("-: Peak list");

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		table.addMouseListener(this);
		addInternalFrameListener(this);

		setAutoRefresh(true);

	}








	public void printMe() {}

	public void copyMe() {}

 	public void closeMe() {
		rawData = null;
		dispose();
	}


	/**
	 * This method paints the plot to this panel
	 */
	public void paint(Graphics g) {
		/*
		selectedPeakID = rawData.getPeakList().getSelectedPeakID();
		if (selectedPeakID != -1) {
			selectPeakFromList(selectedPeakID);
		}
		*/

		super.paint(g);
	}


	public int print(Graphics g, PageFormat pf, int pi) {
		return 1;
	}

	public void internalFrameActivated(InternalFrameEvent e) {
		//rawData.setActiveVisualizer(this);
		itemSelector.setActiveRawData(rawData);

	}
	public void internalFrameClosed(InternalFrameEvent e) {	}
	public void internalFrameClosing(InternalFrameEvent e) { }
	public void internalFrameDeactivated(InternalFrameEvent e) { }
	public void internalFrameDeiconified(InternalFrameEvent e) { }
	public void internalFrameIconified(InternalFrameEvent e) { }
	public void internalFrameOpened(InternalFrameEvent e) { }


	public void actionPerformed(java.awt.event.ActionEvent e) {
		Object src = e.getSource();

		if (src == zoomToPeakMenuItem) {
			setZoomAroundSelectedPeak();
		}

	
	}


	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {

		if (table.getSelectedRow()==-1) { return; }
	    if (e.getButton()!=MouseEvent.BUTTON1) {
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}


	public boolean getAutoRefresh() {
		return doNotAutoRefresh;
	}

	public void setAutoRefresh(boolean _ar) {
		doNotAutoRefresh= !_ar;
	}


	public void selectPeakFromList(int peakID) {

		int rowPeakID;
		int rowInd;

		// Find row where this peak is
		for (rowInd=0; rowInd<table.getRowCount(); rowInd++) {
			rowPeakID = ((Integer)table.getValueAt(rowInd, 0)).intValue();		// -1
			if (peakID==rowPeakID) { break; }
		}

		// If row was found
		if (rowInd<table.getRowCount()) {

			// Check if it is already selected
			if (table.getSelectedRow() != rowInd) {
				// No, then select it (preserve selected column, so horizontal scroll won't jump)
				table.clearSelection();
				table.changeSelection(rowInd, table.getSelectedColumn(), false, false);
			}
		}

	}

	private void setZoomAroundSelectedPeak() {

		int selectedRow = table.getSelectedRow();
//		int peakInd = ((Integer)table.getValueAt(selectedRow, 0)).intValue();	// -1

//		Peak p = rawData.getPeakList().getPeak(peakInd);

	//	rawData.setSelectionAroundPeak(p);

//		mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_BOTH, rawData.getRawDataID());

		/*
		BackgroundThread bt = new BackgroundThread(mainWin, rawData, Visualizer.CHANGETYPE_SELECTION_BOTH, BackgroundThread.TASK_REFRESHVISUALIZERS);
		bt.start();
		*/

	}



    private class SelectionListener implements ListSelectionListener {
        JTable table;

        // It is necessary to keep the table since it is not possible
        // to determine the table from the event's source
        SelectionListener(JTable table) {
            this.table = table;
        }
        public void valueChanged(ListSelectionEvent e) {

			if (doNotAutoRefresh) { return; }

            // If cell selection is enabled, both row and column change events are fired
            if (e.getSource() == table.getSelectionModel()
                  && table.getRowSelectionAllowed()) {
                // Column selection changed
                if (!e.getValueIsAdjusting()) {
					int row = table.getSelectedRow();
					if (row!=-1) {
						int peakID = ((Integer)table.getValueAt(row,0)).intValue();
						// peakID--;

						//rawData.getPeakList().setSelectedPeakID(peakID);
						//rawData.setCursorPositionByPeakID(peakID);

	//					mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_CURSORPOSITION_BOTH, rawData.getRawDataID());
						/*
						BackgroundThread bt = new BackgroundThread(mainWin, rawData, Visualizer.CHANGETYPE_CURSORPOSITION_SCAN, BackgroundThread.TASK_REFRESHVISUALIZERS);
						bt.start();
						*/
					}
				}
            } else if (e.getSource() == table.getColumnModel().getSelectionModel()
                   && table.getColumnSelectionAllowed() ){
                // Row selection changed
                int first = e.getFirstIndex();
                int last = e.getLastIndex();
            }

            if (e.getValueIsAdjusting()) {
                // The mouse button has not yet been released
            }
        }
    }





	private class MyTableModel extends AbstractTableModel {
		private String[] columnNames;
		private final String unassignedValue = new String("unassigned");

		private Object[][] data;

		public MyTableModel(Object[][] _data, String[] _columnNames) {
			data = _data;
			columnNames = _columnNames;
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return data.length;
		}

		public String getColumnName(int col) {
			return columnNames[col];
		}

		public Object getValueAt(int row, int col) {

			if (col == COL_CHARGE) { if (((Integer)(data[row][col]))<0) { return unassignedValue; } }
			if (col == COL_ISOTOPEPATTERNID) { if (((Integer)(data[row][col]))<0) { return unassignedValue; } }

			return data[row][col];
		}

		public Class getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

		public boolean isCellEditable(int row, int col) {
			return false;
		}
		public void setValueAt(Object value, int row, int col) {
			return;
		}

	}





    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#getRawDataFile()
     */
    public RawDataFile getRawDataFile() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRawDataFile(net.sf.mzmine.io.RawDataFile)
     */
    public void setRawDataFile(RawDataFile newFile) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZRange(double, double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTRange(double, double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZPosition(double)
     */
    public void setMZPosition(double mz) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTPosition(double)
     */
    public void setRTPosition(double rt) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#attachVisualizer(net.sf.mzmine.visualizers.RawDataVisualizer)
     */
    public void attachVisualizer(RawDataVisualizer visualizer) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#detachVisualizer(net.sf.mzmine.visualizers.RawDataVisualizer)
     */
    public void detachVisualizer(RawDataVisualizer visualizer) {
        // TODO Auto-generated method stub
        
    }

}

