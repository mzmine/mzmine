/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;

/**
 * Drag and drop transfer handler for project JTree
 */
class ProjectTreeTransferable implements Transferable {

	private DataFlavor transferFlavor;
	private Object transferObject;

	ProjectTreeTransferable(RawDataFile rawDataFiles[]) {
		this.transferObject = rawDataFiles;
		try {
			this.transferFlavor = new DataFlavor(
					DataFlavor.javaJVMLocalObjectMimeType + ";class=\""
							+ RawDataFile[].class.getName() + "\"");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	ProjectTreeTransferable(PeakList peakLists[]) {
		this.transferObject = peakLists;
		try {
			this.transferFlavor = new DataFlavor(
					DataFlavor.javaJVMLocalObjectMimeType + ";class=\""
							+ PeakList[].class.getName() + "\"");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (!flavor.equals(transferFlavor)) {
			throw (new UnsupportedFlavorException(flavor));
		}
		return transferObject;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { transferFlavor };
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return (flavor.equals(transferFlavor));
	}

}