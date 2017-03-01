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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.tree.TreePath;

/**
 * Drag and drop transfer handler for project JTree
 */
class ProjectTreeTransferable implements Transferable {

    public static final DataFlavor transerFlavor = new DataFlavor(
	    TreePath[].class, "TreePath[]");

    public Object getTransferData(DataFlavor flavor)
	    throws UnsupportedFlavorException, IOException {
	return null;
    }

    public DataFlavor[] getTransferDataFlavors() {
	return new DataFlavor[] { transerFlavor };
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
	return (flavor.equals(transerFlavor));
    }

}