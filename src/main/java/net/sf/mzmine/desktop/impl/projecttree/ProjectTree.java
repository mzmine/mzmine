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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.TreePath;

/**
 * This class implements a selector of raw data files and peak lists
 */
public class ProjectTree extends JTree {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public ProjectTree() {

	ProjectTreeRenderer renderer = new ProjectTreeRenderer();
	setCellRenderer(renderer);

	DefaultTreeCellEditor editor = new DefaultTreeCellEditor(this,
		renderer, new ProjectTreeEditor(this));
	setCellEditor(editor);
	setEditable(true);
	setInvokesStopCellEditing(true);

	setRootVisible(true);
	setShowsRootHandles(false);

	setToggleClickCount(-1);

	// Activate drag&drop
	ProjectTreeDnDHandler dndHandler = new ProjectTreeDnDHandler();
	setTransferHandler(dndHandler);
	setDropMode(DropMode.INSERT);
	setDragEnabled(true);

	// Attach a handler for handling popup menus and double clicks
	ProjectTreeMouseHandler popupHandler = new ProjectTreeMouseHandler(this);
	addMouseListener(popupHandler);

    }

    @SuppressWarnings("unchecked")
    public <T> T[] getSelectedObjects(Class<T> objectClass) {
	Vector<T> selectedObjects = new Vector<T>();
	int selectedRows[] = getSelectionRows();

	// getSelectionRows() may return null or empty array, depending on
	// TreeModel implementation
	if ((selectedRows == null) || (selectedRows.length == 0))
	    return (T[]) Array.newInstance(objectClass, 0);

	// Sorting is important to return the items in the same order as they
	// are presented in the tree. By default, JTree returns items in the
	// order in which they were selected by the user, which is not good for
	// us.
	Arrays.sort(selectedRows);

	for (int row : selectedRows) {
	    TreePath path = getPathForRow(row);
	    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path
		    .getLastPathComponent();
	    Object selectedObject = selectedNode.getUserObject();
	    if (objectClass.isInstance(selectedObject))
		selectedObjects.add((T) selectedObject);
	}
	return (T[]) selectedObjects.toArray((Object[]) Array.newInstance(
		objectClass, 0));
    }

}