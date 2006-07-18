/*
 * Copyright 2006 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class TransferableImage implements Transferable {

    private Image img;

    public TransferableImage(Image _img) {
        img = _img;
    }

    public Object getTransferData(DataFlavor flavor) throws IOException,
            UnsupportedFlavorException {
        if (DataFlavor.imageFlavor.equals(flavor)) {
            return img;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }

    }

    public DataFlavor[] getTransferDataFlavors() {
        DataFlavor[] flav = new DataFlavor[1];
        flav[0] = DataFlavor.imageFlavor;
        return flav;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        if (DataFlavor.imageFlavor.equals(flavor)) {
            return true;
        } else {
            return false;
        }
    }

}