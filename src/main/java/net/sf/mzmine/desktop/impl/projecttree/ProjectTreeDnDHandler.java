/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;

/**
 * Drag and drop transfer handler for project JTree
 */
class ProjectTreeDnDHandler extends TransferHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public boolean canImport(TransferSupport info) {

	ProjectTree projectTree = (ProjectTree) info.getComponent();
	TreeModel treeModel = projectTree.getModel();

	// Get location where we are dropping
	JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();

	// We want to insert between existing items, in such case the child
	// index is always >= 0
	if (dl.getChildIndex() < 0)
	    return false;

	// Get the path of the item where we are dropping
	TreePath dropPath = dl.getPath();
	DefaultMutableTreeNode droppedLocationNode = (DefaultMutableTreeNode) dropPath
		.getLastPathComponent();
	Object dropTargetObject = droppedLocationNode.getUserObject();

	// If the target is "Raw data files" item, accept the drop
	if (dropTargetObject == RawDataTreeModel.dataFilesNodeName)
	    return true;

	// If the target is last item AFTER "Raw data files" item, accept
	// the drop
	if ((droppedLocationNode == treeModel.getRoot())
		&& (dl.getChildIndex() == 1))
	    return true;

	// If the target is "Peak lists" item, accept the drop
	if (dropTargetObject == PeakListTreeModel.peakListsNodeName)
	    return true;

	// If the target is last item AFTER "Peak lists" item, accept the
	// drop
	if ((droppedLocationNode == treeModel.getRoot())
		&& (dl.getChildIndex() == 2))
	    return true;

	return false;
    }

    public boolean importData(TransferSupport info) {

	if (!info.isDrop()) {
	    return false;
	}

	ProjectTree projectTree = (ProjectTree) info.getComponent();
	DefaultTreeModel treeModel = (DefaultTreeModel) projectTree.getModel();

	MZmineProject project = MZmineCore.getProjectManager()
		.getCurrentProject();

	JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();
	TreePath dropPath = dl.getPath();
	DefaultMutableTreeNode droppedLocationNode = (DefaultMutableTreeNode) dropPath
		.getLastPathComponent();

	Object droppedLocationObject = droppedLocationNode.getUserObject();
	int childIndex = dl.getChildIndex();

	TreePath transferedPaths[] = projectTree.getSelectionPaths();

	// Check if the drop target is among the project data files
	if (droppedLocationObject == RawDataTreeModel.dataFilesNodeName) {

	    for (TreePath path : transferedPaths) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
			.getLastPathComponent();
		int currentIndex = node.getParent().getIndex(node);
		Object transferObject = node.getUserObject();
		if (transferObject instanceof RawDataFile) {
		    treeModel.removeNodeFromParent(node);

		    if (childIndex > currentIndex)
			childIndex--;
		    treeModel.insertNodeInto(node, droppedLocationNode,
			    childIndex);

		    childIndex++;

		}
	    }

	}

	// Check if the drop target is AFTER the data files (last position)
	if ((droppedLocationObject == project) && (childIndex == 1)) {
	    int numOfFiles = project.getDataFiles().length;
	    DefaultMutableTreeNode filesNode = (DefaultMutableTreeNode) droppedLocationNode
		    .getChildAt(0);

	    for (TreePath path : transferedPaths) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
			.getLastPathComponent();
		Object transferObject = node.getUserObject();
		if (transferObject instanceof RawDataFile) {
		    treeModel.removeNodeFromParent(node);
		    treeModel.insertNodeInto(node, filesNode, numOfFiles - 1);
		}
	    }

	}

	// Check if the drop target is among the project peak lists
	if (droppedLocationObject == PeakListTreeModel.peakListsNodeName) {
	    for (TreePath path : transferedPaths) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
			.getLastPathComponent();
		int currentIndex = node.getParent().getIndex(node);
		Object transferObject = node.getUserObject();
		if (childIndex > currentIndex)
		    childIndex--;
		if (transferObject instanceof PeakList) {
		    treeModel.removeNodeFromParent(node);
		    treeModel.insertNodeInto(node, droppedLocationNode,
			    childIndex);
		    childIndex++;
		}
	    }
	}

	// Check if the drop target is AFTER the peak lists (last position)
	if ((droppedLocationObject == project) && (childIndex == 2)) {
	    DefaultMutableTreeNode peakListsNode = (DefaultMutableTreeNode) droppedLocationNode
		    .getChildAt(1);

	    int numOfPeakLists = project.getPeakLists().length;
	    for (TreePath path : transferedPaths) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
			.getLastPathComponent();
		Object transferObject = node.getUserObject();
		if (transferObject instanceof PeakList) {
		    treeModel.removeNodeFromParent(node);
		    treeModel.insertNodeInto(node, peakListsNode,
			    numOfPeakLists - 1);
		}
	    }
	}

	return true;
    }

    public int getSourceActions(JComponent c) {
	// We only support moving, not copying
	return MOVE;
    }

    protected Transferable createTransferable(JComponent c) {
	return new ProjectTreeTransferable();
    }

}