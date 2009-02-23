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

package net.sf.mzmine.desktop.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.visualization.infovisualizer.InfoWindow;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableWindow;
import net.sf.mzmine.modules.visualization.tic.TICVisualizer;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.NameChangeDialog;
import net.sf.mzmine.util.dialogs.NameChangeable;

/**
 * This class implements a selector of raw data files and alignment results
 */
class ProjectTree extends JTree implements MouseListener, ActionListener {

	private ProjectTreeModel treeModel;

	private JPopupMenu dataFilePopupMenu, peakListPopupMenu;

	/**
	 * Constructor
	 */
	ProjectTree() {

		super();

		treeModel = new ProjectTreeModel();
		setModel(treeModel);

		ProjectTreeRenderer renderer = new ProjectTreeRenderer();
		setCellRenderer(renderer);

		ProjectTreeEditor editor = new ProjectTreeEditor(this, renderer);
		setCellEditor(editor);

		setRootVisible(true);
		setShowsRootHandles(false);
		setToggleClickCount(-1);

		addMouseListener(this);

		expandPath(new TreePath(new Object[] { ProjectTreeModel.rootItem,
				ProjectTreeModel.dataFilesItem }));
		expandPath(new TreePath(new Object[] { ProjectTreeModel.rootItem,
				ProjectTreeModel.peakListsItem }));

		dataFilePopupMenu = new JPopupMenu();
		GUIUtils.addMenuItem(dataFilePopupMenu, "Show TIC", this, "SHOW_TIC");
		GUIUtils.addMenuItem(dataFilePopupMenu, "Rename", this, "RENAME_FILE");
		GUIUtils.addMenuItem(dataFilePopupMenu, "Remove", this, "REMOVE_FILE");

		peakListPopupMenu = new JPopupMenu();
		GUIUtils.addMenuItem(peakListPopupMenu, "Show peak list", this,
				"SHOW_ALIGNED_PEAKLIST");
		GUIUtils.addMenuItem(peakListPopupMenu, "Show list info", this,
				"SHOW_PEAKLIST_INFO");
		GUIUtils.addMenuItem(peakListPopupMenu, "Rename", this,
				"RENAME_PEAKLIST");
		GUIUtils.addMenuItem(peakListPopupMenu, "Remove", this,
				"REMOVE_PEAKLIST");

	}

	public ProjectTreeModel getModel() {
		return treeModel;
	}

	public RawDataFile[] getSelectedDataFiles() {
		Vector<RawDataFile> selectedDataFiles = new Vector<RawDataFile>();
		TreePath selectedItems[] = getSelectionPaths();
		if (selectedItems == null)
			return new RawDataFile[0];
		for (TreePath path : selectedItems) {
			if (path.getLastPathComponent() instanceof RawDataFile)
				selectedDataFiles
						.add((RawDataFile) path.getLastPathComponent());
		}
		return selectedDataFiles.toArray(new RawDataFile[0]);
	}

	public PeakList[] getSelectedPeakLists() {
		Vector<PeakList> selectedDataFiles = new Vector<PeakList>();
		TreePath selectedItems[] = getSelectionPaths();
		if (selectedItems == null)
			return new PeakList[0];
		for (TreePath path : selectedItems) {
			if (path.getLastPathComponent() instanceof PeakList)
				selectedDataFiles.add((PeakList) path.getLastPathComponent());
		}
		return selectedDataFiles.toArray(new PeakList[0]);
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		if (command.equals("RENAME_FILE")) {
			RawDataFile[] selectedFiles = getSelectedDataFiles();
			for (RawDataFile file : selectedFiles) {
				if (file instanceof NameChangeable) {
					NameChangeDialog dialog = new NameChangeDialog(
							(NameChangeable) file);
					dialog.setVisible(true);
				}
			}
		}

		if (command.equals("REMOVE_FILE")) {
			RawDataFile[] selectedFiles = getSelectedDataFiles();
			for (RawDataFile file : selectedFiles)
				MZmineCore.getCurrentProject().removeFile(file);
		}

		if (command.equals("SHOW_TIC")) {
			RawDataFile[] selectedFiles = getSelectedDataFiles();
			TICVisualizer.showNewTICVisualizerWindow(selectedFiles, null, null);
		}

		if (command.equals("RENAME_PEAKLIST")) {
			PeakList[] selectedPeakLists = getSelectedPeakLists();
			for (PeakList peakList : selectedPeakLists) {
				if (peakList instanceof NameChangeable) {
					NameChangeDialog dialog = new NameChangeDialog(
							(NameChangeable) peakList);
					dialog.setVisible(true);
				}
			}
		}

		if (command.equals("REMOVE_PEAKLIST")) {
			PeakList[] selectedPeakLists = getSelectedPeakLists();
			for (PeakList peakList : selectedPeakLists)
				MZmineCore.getCurrentProject().removePeakList(peakList);
		}

		if (command.equals("SHOW_ALIGNED_PEAKLIST")) {
			PeakList[] selectedPeakLists = getSelectedPeakLists();
			Desktop desktop = MZmineCore.getDesktop();
			for (PeakList peakList : selectedPeakLists) {
				PeakListTableWindow window = new PeakListTableWindow(peakList);
				desktop.addInternalFrame(window);
			}
		}

		if (command.equals("SHOW_PEAKLIST_INFO")) {
			PeakList[] selectedPeakLists = getSelectedPeakLists();
			Desktop desktop = MZmineCore.getDesktop();
			for (PeakList peakList : selectedPeakLists) {
				InfoWindow window = new InfoWindow(peakList);
				desktop.addInternalFrame(window);
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
		TreePath clickedPath = getPathForLocation(e.getX(), e.getY());
		Object clickedObject = null;
		if (clickedPath != null)
			clickedObject = clickedPath.getLastPathComponent();

		if (e.isPopupTrigger()) {
			if (clickedObject instanceof RawDataFile)
				dataFilePopupMenu.show(e.getComponent(), e.getX(), e.getY());
			if (clickedObject instanceof PeakList)
				peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
		}

		if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1)) {

			if (clickedObject instanceof RawDataFile) {
				RawDataFile clickedFile = (RawDataFile) clickedObject;
				TICVisualizer.showNewTICVisualizerWindow(
						new RawDataFile[] { clickedFile }, null, null);
			}

			if (clickedObject instanceof PeakList) {
				PeakList clickedPeakList = (PeakList) clickedObject;
				PeakListTableWindow window = new PeakListTableWindow(
						clickedPeakList);
				Desktop desktop = MZmineCore.getDesktop();
				desktop.addInternalFrame(window);
			}

		}
	}

	public void mouseReleased(MouseEvent e) {
		TreePath clickedPath = getPathForLocation(e.getX(), e.getY());
		Object clickedObject = null;
		if (clickedPath != null)
			clickedObject = clickedPath.getLastPathComponent();

		if (e.isPopupTrigger()) {
			if (clickedObject instanceof RawDataFile)
				dataFilePopupMenu.show(e.getComponent(), e.getX(), e.getY());
			if (clickedObject instanceof PeakList)
				peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
}