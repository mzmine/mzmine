/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;

/**
 * Drag and drop transfer handler for project JTree
 */
class ProjectTreeDnDHandler extends TransferHandler {

	private ProjectTreeModel projectTreeModel;

	public ProjectTreeDnDHandler(ProjectTreeModel projectTreeModel) {
		this.projectTreeModel = projectTreeModel;
	}

	public boolean canImport(TransferSupport info) {

		// Get location where we are dropping
		JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();

		// We want to insert between existing items, in such case the child
		// index is always >= 0
		if (dl.getChildIndex() < 0)
			return false;

		// Get the path of the item where we are dropping
		TreePath dropPath = dl.getPath();
		Object dropTargetObject = dropPath.getLastPathComponent();

		DataFlavor flavors[] = info.getDataFlavors();
		if (flavors.length < 1)
			return false;
		Class<?> transferClass = flavors[0].getRepresentationClass();

		// Check if we are transferring raw data files
		if (RawDataFile[].class.equals(transferClass)) {
			// If the target is "Raw data files" item, accept the drop
			if (dropTargetObject == ProjectTreeModel.dataFilesItem)
				return true;
			// If the target is last item AFTER "Raw data files" item, accept
			// the drop
			if ((dropTargetObject == projectTreeModel.getRoot())
					&& (dl.getChildIndex() == 1))
				return true;
		}

		// Check if we are transferring peak lists
		if (PeakList[].class.equals(transferClass)) {
			// If the target is "Peak lists" item, accept the drop
			if (dropTargetObject == ProjectTreeModel.peakListsItem)
				return true;
			// If the target is last item AFTER "Peak lists" item, accept the
			// drop
			if ((dropTargetObject == projectTreeModel.getRoot())
					&& (dl.getChildIndex() == 2))
				return true;
		}

		return false;
	}

	public boolean importData(TransferSupport info) {

		if (!info.isDrop()) {
			return false;
		}

		Desktop desktop = MZmineCore.getDesktop();
		MZmineProject project = MZmineCore.getCurrentProject();
		
		JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();

		Object droppedLocationObject = dl.getPath().getLastPathComponent();
		int childIndex = dl.getChildIndex();
		
		// Check if the drop target is among the project data files
		if (droppedLocationObject == ProjectTreeModel.dataFilesItem) {
			RawDataFile selectedFiles[] = desktop.getSelectedDataFiles();
			project.moveDataFiles(selectedFiles, childIndex);
		}

		// Check if the drop target is AFTER the data files (last position)
		if ((droppedLocationObject == project) && (childIndex == 1)) {
			int numOfFiles = project.getDataFiles().length;
			RawDataFile selectedFiles[] = desktop.getSelectedDataFiles();
			project.moveDataFiles(selectedFiles, numOfFiles);
		}
		
		// Check if the drop target is among the project peak lists
		if (droppedLocationObject == ProjectTreeModel.peakListsItem) {
			PeakList selectedPeakLists[] = desktop.getSelectedPeakLists();
			project.movePeakLists(selectedPeakLists, childIndex);
		}

		// Check if the drop target is AFTER the peak lists (last position)
		if ((droppedLocationObject == project) && (childIndex == 2)) {
			int numOfPeakLists = project.getPeakLists().length;
			PeakList selectedPeakLists[] = desktop.getSelectedPeakLists();
			project.movePeakLists(selectedPeakLists, numOfPeakLists);
		}

		return true;
	}

	public int getSourceActions(JComponent c) {
		// We only support moving, not copying
		return MOVE;
	}

	protected Transferable createTransferable(JComponent c) {

		ProjectTree tree = (ProjectTree) c;

		// Get selected items in the list
		RawDataFile selectedDataFiles[] = tree
				.getSelectedObjects(RawDataFile.class);
		PeakList selectedPeakLists[] = tree.getSelectedObjects(PeakList.class);

		// If nothing is selected, we have nothing to transfer
		if ((selectedDataFiles.length == 0) && (selectedPeakLists.length == 0))
			return null;

		// If both raw data and peaklists are selected, we cannot transfer both
		if ((selectedDataFiles.length > 0) && (selectedPeakLists.length > 0))
			return null;

		ProjectTreeTransferable transferable;

		if (selectedDataFiles.length > 0) {
			transferable = new ProjectTreeTransferable(selectedDataFiles);
		} else {
			transferable = new ProjectTreeTransferable(selectedPeakLists);
		}

		return transferable;
	}

}