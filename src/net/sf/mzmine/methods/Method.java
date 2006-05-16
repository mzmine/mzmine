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

import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.methods.alignment.AlignmentResult;

/**
 *
 */
public interface Method {

    /**
     * @return Textual description of method
     */
    public String getMethodDescription();

    /**
     * This function displays a modal dialog to define method parameters
     *
     * @param	parameters	Previous parameter values
     * @return	Parameters set by user
     */
    public MethodParameters askParameters(MethodParameters parameters);

    /**
     * Runs this method on a given items
     * @param	parameters	Parameter values for the method
     * @param	rawDataFiles	Raw data files to be processed (null if the method doesn't work on raw data files)
     * @param	alignmentResult	AlignmentResults to be processed (null if the method doesn't work on alignment results)
     */
    public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults);

}
