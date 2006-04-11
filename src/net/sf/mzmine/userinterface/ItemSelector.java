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
package net.sf.mzmine.userinterface;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.datastructures.RawDataAtClient;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerTICView;


/**
 * This class implements a selector of raw data files and alignment results
 */
public class ItemSelector extends JPanel implements ListSelectionListener, MouseListener, ActionListener {

	private MainWindow mainWin;
	private Statusbar statBar;

	private DefaultListModel rawDataObjects;
	private JList rawDataList;
	private JScrollPane rawDataScroll;

	private DefaultListModel resultObjects;
	private JList resultList;
	private JScrollPane resultScroll;

	private JPopupMenu popupMenu;
	private JMenuItem pmHideVisualizers;
	private JMenuItem pmShowVisualizers;
	private JMenuItem pmClose;

	private int alignmentResultIDCount = 0;


	/**
	 * Constructor
	 */
	public ItemSelector(MainWindow _mainWin) {

		mainWin = _mainWin;
		statBar = mainWin.getStatusBar();

		// Create panel for raw data objects
		JPanel rawDataPanel = new JPanel();
		JLabel rawDataTitle = new JLabel(new String("Raw data files"));

		rawDataObjects = new DefaultListModel();
		rawDataList = new JList(rawDataObjects);
		rawDataScroll = new JScrollPane(rawDataList);

		rawDataPanel.setLayout(new BorderLayout());
		rawDataPanel.add(rawDataTitle, BorderLayout.NORTH);
		rawDataPanel.add(rawDataScroll, BorderLayout.CENTER);
		rawDataPanel.setMinimumSize(new Dimension(150,10));


		// Create panel for alignment results
		JPanel resultsPanel = new JPanel();
		JLabel resultsTitle = new JLabel(new String("Alignment results"));

		resultObjects = new DefaultListModel();
		resultList = new JList(resultObjects);
		resultScroll = new JScrollPane(resultList);

		resultsPanel.setLayout(new BorderLayout());
		resultsPanel.add(resultsTitle, BorderLayout.NORTH);
		resultsPanel.add(resultScroll, BorderLayout.CENTER);
		resultsPanel.setMinimumSize(new Dimension(150,10));


		// Add panels to a split and put split on the main panel
		setPreferredSize(new Dimension(150,10));
		setLayout(new BorderLayout());

		JSplitPane rawAndResultsSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rawDataPanel, resultsPanel);
		add(rawAndResultsSplit, BorderLayout.CENTER);

		rawAndResultsSplit.setDividerLocation(230);

		rawDataList.addListSelectionListener(this);
		resultList.addListSelectionListener(this);


		// Create a pop-up menu
		popupMenu = new JPopupMenu();
		pmHideVisualizers = new JMenuItem("Hide visualizers");
		pmHideVisualizers.addActionListener(this);
		pmShowVisualizers = new JMenuItem("Show visualizers");
		pmShowVisualizers.addActionListener(this);
		pmClose = new JMenuItem("Close");
		pmClose.addActionListener(this);
		popupMenu.add(pmShowVisualizers);
		popupMenu.add(pmHideVisualizers);
		popupMenu.addSeparator();
		popupMenu.add(pmClose);

		// Add listeners to both lists
		rawDataList.addMouseListener(this);
		resultList.addMouseListener(this);

	}



	// Implementation of mouse listener interface

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {
		if (e.getButton()!=MouseEvent.BUTTON1) {
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}



	// Implementation of action listener interface

	public void actionPerformed(java.awt.event.ActionEvent e) {

		if (mainWin.isBusy()) { return; }

		Object src = e.getSource();

		// Hide visualizers for all selected objects
		if (src == pmHideVisualizers) {


			// Grab selected raw data objects
			Object[] tmpArray = rawDataList.getSelectedValues();
			if (tmpArray.length>0) {
				int[] rawDataIDs = new int[tmpArray.length];
				for (int i=0; i<tmpArray.length; i++) { rawDataIDs[i] = ((RawDataAtClient)tmpArray[i]).getRawDataID(); }

				// Show visualizers, tile all windows and refresh visualizers
				mainWin.toggleRawDataVisualizers(rawDataIDs, false);
				mainWin.tileWindows();
				mainWin.updateMenuAvailability();
				mainWin.repaint();
			}


			// Grab selected result objects
			tmpArray = resultList.getSelectedValues();
			int[] alignmentResultIDs = new int[tmpArray.length];
			for (int i=0; i<tmpArray.length; i++) { alignmentResultIDs[i] = ((AlignmentResult)tmpArray[i]).getAlignmentResultID(); }

			// Show visualizers, tile all windows and refresh visualizers
			if (alignmentResultIDs.length>0) {
				mainWin.toggleAlignmentResultVisualizers(alignmentResultIDs, false);
				mainWin.tileWindows();
				mainWin.updateMenuAvailability();
				mainWin.repaint();
			}

		}


		// Show visualizers for all selected objects
		if (src == pmShowVisualizers) {

			// Grab selected raw data objects
			Object[] tmpArray = rawDataList.getSelectedValues();
			if (tmpArray.length>0) {
				int[] rawDataIDs = new int[tmpArray.length];
				for (int i=0; i<tmpArray.length; i++) { rawDataIDs[i] = ((RawDataAtClient)tmpArray[i]).getRawDataID(); }

				// Show visualizers, tile all windows and refresh visualizers
				mainWin.toggleRawDataVisualizers(rawDataIDs, true);
				mainWin.tileWindows();
				mainWin.updateMenuAvailability();
				mainWin.startRefreshRawDataVisualizers(RawDataVisualizerTICView.CHANGETYPE_DATA, rawDataIDs);
			}

			// Grab selected result objects
			tmpArray = resultList.getSelectedValues();
			int[] alignmentResultIDs = new int[tmpArray.length];
			for (int i=0; i<tmpArray.length; i++) { alignmentResultIDs[i] = ((AlignmentResult)tmpArray[i]).getAlignmentResultID(); }


			// Show visualizers, tile all windows and refresh visualizers
			if (alignmentResultIDs.length>0) {
				mainWin.toggleAlignmentResultVisualizers(alignmentResultIDs, true);
				mainWin.tileWindows();
				mainWin.updateMenuAvailability();
				mainWin.repaint();
			}

			// (not implemented)
		}

		// Close selected items
		if (src == pmClose) {

			// Grab selected raw data files
			int[] rawDataIDs = getSelectedRawDataIDs();

			if (rawDataIDs.length>0) {

				// Check that any of these files is not participating any alignment
				String errorMessage = null;
				for (int rawDataID : rawDataIDs) {
					RawDataAtClient rawData = getRawDataByID(rawDataID);
					if (rawData.getAlignmentResultIDs().size()>0) {
						Vector<Integer> alignmentResultIDs = rawData.getAlignmentResultIDs();
						errorMessage = rawData.getNiceName() + " is participating in alignment(s). Before closing the raw data files, please close alignment result(s): ";
						for (Integer alignmentResultID : alignmentResultIDs) {
							errorMessage += getAlignmentResultByID(alignmentResultID.intValue()).getNiceName() + ", ";
						}
					}
					if (errorMessage!=null) {
						mainWin.displayErrorMessage(errorMessage);
						return;
					}
				}

				// Remove all visualizers for these raw data files
				for (int rawDataID : rawDataIDs) { mainWin.removeRawDataVisualizers(rawDataID); }

				// Initiate close the raw data files on the cluster
				mainWin.getClientForCluster().closeRawDataFiles(rawDataIDs);

				mainWin.getStatusBar().setStatusText("Closing " + rawDataIDs.length + " raw data file(s).");

				mainWin.updateMenuAvailability();
				mainWin.repaint();

			}

			int[] alignmentResultIDs = getSelectedAlignmentResultIDs();

			if (alignmentResultIDs.length>0) {
				// Remove dependency from each involved raw data file
				for (int alignmentResultID : alignmentResultIDs) {

					if (getAlignmentResultByID(alignmentResultID).isImported()) { continue; }

					rawDataIDs = getAlignmentResultByID(alignmentResultID).getRawDataIDs();
					for (int rawDataID : rawDataIDs) {
						RawDataAtClient rawData = getRawDataByID(rawDataID);
						rawData.removeAlignmentResultID(alignmentResultID);
					}
				}


				for (int alignmentResultID : alignmentResultIDs) {
					// Close all visualizers for these alignment results
					mainWin.removeAlignmentResultVisualizers(alignmentResultID);

					// Remove these alignment results from the item selector
					removeAlignmentResult(getAlignmentResultByID(alignmentResultID));
				}



				mainWin.getStatusBar().setStatusText("Closed " + alignmentResultIDs.length + " alignment result(s).");

				mainWin.updateMenuAvailability();
				mainWin.repaint();



			}

		}

	}


	/**
	 * Adds a raw data object to storage
	 */
	public void addRawData(RawDataAtClient r) {
		rawDataObjects.addElement(r);
	}

	/**
	 * Removes a raw data object from storage
	 */
	public boolean removeRawData(RawDataAtClient r) {
		boolean ans = rawDataObjects.removeElement(r);

		mainWin.updateMenuAvailability();

		return ans;
	}

	public boolean removeRawData(int rawDataID) {

		// Search for raw data file with the given rawDataID
		Enumeration rawDatas = rawDataObjects.elements();
		RawDataAtClient rdac = null;
		while (rawDatas.hasMoreElements()) {
			rdac = (RawDataAtClient)(rawDatas.nextElement());
			if (rdac.getRawDataID()==rawDataID) {
				break;
			}
			rdac = null;
		}
		if (rdac!=null) { removeRawData(rdac); }
		return false;
	}


	/**
	 * Returns an array containing raw data IDs for all selected raw data files.
	 */
	public int[] getSelectedRawDataIDs() {

		Object o[] = rawDataList.getSelectedValues();
		int[] rawDataIDs = new int[o.length];

		for (int i=0; i<o.length; i++) {
			rawDataIDs[i] = ((RawDataAtClient)(o[i])).getRawDataID();
		}

		return rawDataIDs;

	}

	/**
	 * Returns selected raw data objects in an array
	 */
	public RawDataAtClient[] getSelectedRawDatas() {

		Object o[] = rawDataList.getSelectedValues();

		RawDataAtClient res[] = new RawDataAtClient[o.length];

		for (int i=0; i<o.length; i++) { res[i] = (RawDataAtClient)(o[i]);	}

		return res;

	}

	/**
	 *
	 */
	public RawDataAtClient getRawDataByID(int rawDataID) {
		ListModel listModel = rawDataList.getModel();

		for (int i=0; i<listModel.getSize(); i++) {
			RawDataAtClient rawData = (RawDataAtClient)listModel.getElementAt(i);
			if (rawData.getRawDataID() == rawDataID) { return rawData; }
		}

		return null;
	}


	/**
	 * Sets the active raw data item in the list
	 */
	public void setActiveRawData(RawDataAtClient rawData) {
		rawDataList.setSelectedValue(rawData, true);

		mainWin.updateMenuAvailability();
		//repaint();
	}

	/**
	 * Returns the run that is selected in run list
	 */
	public RawDataAtClient getActiveRawData() {
		return (RawDataAtClient)rawDataList.getSelectedValue();
	}

	/**
	 * Returns all raw data objects
	 */
	public RawDataAtClient[] getRawDatas() {

		ListModel listModel = rawDataList.getModel();

		RawDataAtClient[] rawDatas = new RawDataAtClient[listModel.getSize()];

		for (int i=0; i<listModel.getSize(); i++) {
			rawDatas[i] = (RawDataAtClient)(listModel.getElementAt(i));
		}

		return rawDatas;

	}

	/**
	 * Returns all raw data ids
	 */
	public int[] getRawDataIDs() {
		ListModel listModel = rawDataList.getModel();

		int[] rawDataIDs = new int[listModel.getSize()];

		for (int i=0; i<listModel.getSize(); i++) {
			rawDataIDs[i] = ((RawDataAtClient)(listModel.getElementAt(i))).getRawDataID();
		}

		return rawDataIDs;

	}


	// METHODS FOR MAINTAINING LIST OF RESULTS
	// ---------------------------------------


	/**
	 * Returns a vector containing all currently selected result objects in the list
	 */
	public Vector<AlignmentResult> getSelectedAlignmentResults() {

		Vector<AlignmentResult> v = new Vector<AlignmentResult>();
		Object o[] = resultList.getSelectedValues();

		for (int i=0; i<o.length; i++) {
			v.add((AlignmentResult)o[i]);
		}

		return v;
	}

	public int[] getSelectedAlignmentResultIDs() {

		Object o[] = resultList.getSelectedValues();
		int[] alignmentResultIDs = new int[o.length];

		for (int i=0; i<o.length; i++) {
			alignmentResultIDs[i] = ((AlignmentResult)o[i]).getAlignmentResultID();
		}

		return alignmentResultIDs;

	}



	/**
	 * Adds alignment result to item storage
	 * @return ID for the alignment result
	 */
	public int addAlignmentResult(AlignmentResult ar) {
		int id = getNewAlignmentResultID();
		ar.setAlignmentResultID(id);

		resultObjects.addElement(ar);

		if (ar.isImported()) { return id; }

		// Add dependency to all involved raw data files
		for (int rawDataID : ar.getRawDataIDs()) {
			getRawDataByID(rawDataID).addAlignmentResultID(ar.getAlignmentResultID());
		}

		return id;

	}

	public boolean removeAlignmentResult(AlignmentResult ar) {
		boolean ans = resultObjects.removeElement(ar);

		mainWin.updateMenuAvailability();

		return ans;
	}


	public void setActiveAlignmentResult(AlignmentResult ar) {
		resultList.setSelectedValue(ar, true);

		mainWin.updateMenuAvailability();

	}

	/**
	 * Returns all alignment result ids
	 */
	public int[] getAlignmentResultIDs() {
		ListModel listModel = resultList.getModel();

		int[] alignmentResultIDs = new int[listModel.getSize()];

		for (int i=0; i<listModel.getSize(); i++) {
			alignmentResultIDs[i] = ((AlignmentResult)(listModel.getElementAt(i))).getAlignmentResultID();
		}

		return alignmentResultIDs;
	}


	public AlignmentResult getAlignmentResultByID(int alignmentResultID) {
		ListModel listModel = resultList.getModel();

		for (int i=0; i<listModel.getSize(); i++) {
			AlignmentResult alignmentResult = (AlignmentResult)listModel.getElementAt(i);
			if (alignmentResult.getAlignmentResultID() == alignmentResultID) { return alignmentResult; }
		}

		return null;
	}


	/**
	 * Returns the currently selected result object.
	 */
	public AlignmentResult getActiveResult() {
		return (AlignmentResult)resultList.getSelectedValue();
	}

	private int getNewAlignmentResultID() {
		alignmentResultIDCount++;
		return alignmentResultIDCount;
	}


	// MISC. STUFF
	// -----------


	/**
	 * Implementation of ListSelectionListener interface
	 */
	public void valueChanged(ListSelectionEvent e) {

		RawDataAtClient activeRawData;
		AlignmentResult activeResult;

		// Avoid reacting to unnecessary events
		if (e.getValueIsAdjusting()) { return; }



		// Run list selection changed?
		if (e.getSource()==rawDataList) {

			int i = rawDataList.getSelectedIndex();

			// Something selected in run list?
			if (i!=-1) {

				// Clear all selections in results list
				resultList.clearSelection();

				// Get run that was just selected in run list
				Object tmpObj = rawDataList.getSelectedValue();
				if (tmpObj!=null) {

					activeRawData = (RawDataAtClient)tmpObj;

					// Update cursor position in status bar
					statBar.setCursorPosition(activeRawData);

					// Bring visualizers for this run to top
					if ( !(mainWin.isBusy()) ) { mainWin.moveVisualizersToFront(activeRawData); }

				} else {}

			}


		}

		// Result list selection changed?
		if (e.getSource()==resultList) {

			int i = resultList.getSelectedIndex();

			if (i!=-1) {
				// Clear all selections in run list
				rawDataList.clearSelection();

				// Get result object that was just selected in the list
				activeResult = (AlignmentResult)resultList.getSelectedValue();

				// Bring visualizers for this run to top
				if ( !(mainWin.isBusy()) ) { mainWin.moveVisualizersToFront(activeResult); }

			}

		}

		if ( !(mainWin.isBusy()) ) { mainWin.updateMenuAvailability(); }

		//mainWin.repaint();

	}

}