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

import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;

class ProjectTreeEditor extends DefaultCellEditor {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JTree projectTree;

    ProjectTreeEditor(JTree projectTree) {
	super(new JTextField());
	this.projectTree = projectTree;
	this.getComponent().setFont(ProjectTreeRenderer.smallerFont);
    }

    public boolean isCellEditable(EventObject e) {
	if (e instanceof MouseEvent) {
	    MouseEvent me = (MouseEvent) e;
	    TreePath clickedPath = projectTree.getPathForLocation(me.getX(),
		    me.getY());
	    if (clickedPath == null)
		return false;
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) clickedPath
		    .getLastPathComponent();
	    Object editedObject = node.getUserObject();
	    return ((editedObject instanceof RawDataFile) || (editedObject instanceof PeakList));
	}
	return true;
    }

}
