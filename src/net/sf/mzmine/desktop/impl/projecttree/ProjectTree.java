/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl.projecttree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.DropMode;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.TreePath;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.infovisualizer.InfoVisualizer;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableVisualizer;
import net.sf.mzmine.modules.visualization.peaksummary.PeakSummaryVisualizer;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizer;
import net.sf.mzmine.util.GUIUtils;

/**
 * This class implements a selector of raw data files and alignment results
 */
public class ProjectTree extends JTree implements MouseListener, ActionListener {

	private ProjectTreeModel treeModel;

	private JPopupMenu dataFilePopupMenu, peakListPopupMenu, scanPopupMenu,
			peakListRowPopupMenu;

	/**
	 * Constructor
	 */
	public ProjectTree() {

		treeModel = new ProjectTreeModel(this);
		setModel(treeModel);

		ProjectTreeRenderer renderer = new ProjectTreeRenderer();
		setCellRenderer(renderer);

		DefaultTreeCellEditor editor = new DefaultTreeCellEditor(this,
				renderer, new ProjectTreeEditor(this));
		setCellEditor(editor);
		setEditable(true);

		setRootVisible(true);
		setShowsRootHandles(false);

		setToggleClickCount(-1);

		// Activate drag&drop
		ProjectTreeDnDHandler dndHandler = new ProjectTreeDnDHandler(treeModel);
		setTransferHandler(dndHandler);
		setDropMode(DropMode.INSERT);
		setDragEnabled(true);

		addMouseListener(this);

		expandPath(new TreePath(new Object[] { treeModel.getRoot(),
				ProjectTreeModel.dataFilesItem }));
		expandPath(new TreePath(new Object[] { treeModel.getRoot(),
				ProjectTreeModel.peakListsItem }));

		dataFilePopupMenu = new JPopupMenu();
		GUIUtils.addMenuItem(dataFilePopupMenu, "Show TIC", this, "SHOW_TIC");
		GUIUtils.addMenuItem(dataFilePopupMenu, "Remove", this, "REMOVE_FILE");

		scanPopupMenu = new JPopupMenu();
		GUIUtils.addMenuItem(scanPopupMenu, "Show spectrum", this,
				"SHOW_SPECTRA");

		peakListPopupMenu = new JPopupMenu();
		GUIUtils.addMenuItem(peakListPopupMenu, "Show peak list table", this,
				"SHOW_PEAKLIST_TABLES");
		GUIUtils.addMenuItem(peakListPopupMenu, "Show peak list info", this,
				"SHOW_PEAKLIST_INFO");
		GUIUtils.addMenuItem(peakListPopupMenu, "Remove", this,
				"REMOVE_PEAKLIST");

		peakListRowPopupMenu = new JPopupMenu();
		GUIUtils.addMenuItem(peakListRowPopupMenu, "Show peak summary", this,
				"SHOW_PEAK_SUMMARY");

	}

	public ProjectTreeModel getModel() {
		return treeModel;
	}

	@SuppressWarnings("unchecked")
	public <T> T[] getSelectedObjects(Class<T> objectClass) {
		Vector<T> selectedObjects = new Vector<T>();
		int selectedRows[] = getSelectionRows();

		// Sorting is important to return the items in the same order as they
		// are presented in the tree. By default, JTree returns items in the
		// order in which they were selected by the user, which is not good for
		// us.
		Arrays.sort(selectedRows);

		for (int row : selectedRows) {
			TreePath path = getPathForRow(row);
			Object selectedObject = path.getLastPathComponent();
			if (objectClass.isInstance(selectedObject))
				selectedObjects.add((T) selectedObject);
		}
		return (T[]) selectedObjects.toArray((Object[]) Array.newInstance(
				objectClass, 0));
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		if (command.equals("REMOVE_FILE")) {
			RawDataFile[] selectedFiles = getSelectedObjects(RawDataFile.class);
			for (RawDataFile file : selectedFiles)
				MZmineCore.getCurrentProject().removeFile(file);
		}

		if (command.equals("SHOW_TIC")) {
			RawDataFile[] selectedFiles = getSelectedObjects(RawDataFile.class);
			TICVisualizer.showNewTICVisualizerWindow(selectedFiles, null, null);
		}

		if (command.equals("SHOW_SPECTRA")) {
			Scan selectedScans[] = getSelectedObjects(Scan.class);
			for (Scan scan : selectedScans) {
				SpectraVisualizer.showNewSpectrumWindow(scan);
			}
		}

		if (command.equals("REMOVE_PEAKLIST")) {
			PeakList[] selectedPeakLists = getSelectedObjects(PeakList.class);
			for (PeakList peakList : selectedPeakLists)
				MZmineCore.getCurrentProject().removePeakList(peakList);
		}

		if (command.equals("SHOW_PEAKLIST_TABLES")) {
			PeakList[] selectedPeakLists = getSelectedObjects(PeakList.class);
			for (PeakList peakList : selectedPeakLists) {
				PeakListTableVisualizer
						.showNewPeakListVisualizerWindow(peakList);
			}
		}

		if (command.equals("SHOW_PEAKLIST_INFO")) {
			PeakList[] selectedPeakLists = getSelectedObjects(PeakList.class);
			for (PeakList peakList : selectedPeakLists) {
				InfoVisualizer.showNewPeakListInfo(peakList);
			}
		}

		if (command.equals("SHOW_PEAK_SUMMARY")) {
			PeakListRow[] selectedRows = getSelectedObjects(PeakListRow.class);
			for (PeakListRow row : selectedRows) {
				PeakSummaryVisualizer.showNewPeakSummaryWindow(row);
			}
		}

	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {

		if (e.isPopupTrigger())
			handlePopupTriggerEvent(e);

		if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1))
			handleDoubleClickEvent(e);

	}

	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
			handlePopupTriggerEvent(e);
	}

	private void handlePopupTriggerEvent(MouseEvent e) {
		TreePath clickedPath = getPathForLocation(e.getX(), e.getY());
		Object clickedObject = null;
		if (clickedPath != null)
			clickedObject = clickedPath.getLastPathComponent();

		if (clickedObject instanceof RawDataFile)
			dataFilePopupMenu.show(e.getComponent(), e.getX(), e.getY());
		if (clickedObject instanceof Scan)
			scanPopupMenu.show(e.getComponent(), e.getX(), e.getY());
		if (clickedObject instanceof PeakList)
			peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
		if (clickedObject instanceof PeakListRow)
			peakListRowPopupMenu.show(e.getComponent(), e.getX(), e.getY());
	}

	private void handleDoubleClickEvent(MouseEvent e) {
		TreePath clickedPath = getPathForLocation(e.getX(), e.getY());
		Object clickedObject = null;
		if (clickedPath != null)
			clickedObject = clickedPath.getLastPathComponent();

		if (clickedObject instanceof RawDataFile) {
			RawDataFile clickedFile = (RawDataFile) clickedObject;
			TICVisualizer.showNewTICVisualizerWindow(
					new RawDataFile[] { clickedFile }, null, null);
		}

		if (clickedObject instanceof PeakList) {
			PeakList clickedPeakList = (PeakList) clickedObject;
			PeakListTableVisualizer
					.showNewPeakListVisualizerWindow(clickedPeakList);
		}

		if (clickedObject instanceof Scan) {
			Scan clickedScan = (Scan) clickedObject;
			SpectraVisualizer.showNewSpectrumWindow(clickedScan);
		}

		if (clickedObject instanceof PeakListRow) {
			PeakListRow clickedPeak = (PeakListRow) clickedObject;
			PeakSummaryVisualizer.showNewPeakSummaryWindow(clickedPeak);
		}

	}

}