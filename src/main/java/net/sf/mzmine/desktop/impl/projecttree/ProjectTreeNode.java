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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl.projecttree;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class represents a simple JTree node which always return false from
 * isLeaf() method. The purpose is that this node can always be
 * expanded/collapsed, even when it is empty. Normally, leafs cannot be
 * expanded/collapsed unless they have children. But we want our main project
 * tree nodes to be always expanded by default.
 */
public class ProjectTreeNode extends DefaultMutableTreeNode {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ProjectTreeNode(String name) {
	super(name);
    }

    public boolean isLeaf() {
	return false;
    }

}
