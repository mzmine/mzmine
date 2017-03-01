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

package net.sf.mzmine.datamodel;

import java.io.IOException;

/**
 * This interface represents a raw data file which is being modified. Scans can
 * be added to the file by calling the addScan() method. Final RawDataFile
 * instance is obtained by calling finishWriting() method.
 */
public interface RawDataFileWriter {

    /**
     * Adds a new scan to the file.
     * 
     * @param newScan
     *            Scan to add
     */
    public void addScan(Scan newScan) throws IOException;

    /**
     * Finishes writing of the file
     * 
     * @return newly written file as RawDataFile
     */
    public RawDataFile finishWriting() throws IOException;

}
