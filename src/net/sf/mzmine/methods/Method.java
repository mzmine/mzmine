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

package net.sf.mzmine.methods;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineModule;

/**
 * Interface representing a data processing method
 */
public interface Method extends MZmineModule {

    /**
     * This function displays a modal dialog to define method parameters
     * 
     * @return Newly created parameters or null if user clicked "Cancel"
     */
    public MethodParameters askParameters();

    /**
     * Runs this method on a given items
     * 
     * @param parameters Parameter values for the method
     * @param dataFiles Data files to be processed (null ok if the method
     *            doesn't work on raw data files)
     * @param alignmentResult AlignmentResults to be processed (null ok if the
     *            method doesn't work on alignment results)
     */
    public void runMethod(MethodParameters parameters,
            OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults);

}
