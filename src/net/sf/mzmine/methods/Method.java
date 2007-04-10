/*
 * Copyright 2006-2007 The MZmine Development Team
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
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.TaskSequence;
import net.sf.mzmine.taskcontrol.TaskSequenceListener;

/**
 * Interface representing a data processing method
 */
public interface Method extends MZmineModule {

    /**
     * This function displays a modal dialog to define method parameters
     * 
     * @return parameter set or null if used clicked "cancel"
     */
    public ParameterSet setupParameters(ParameterSet current);

    /**
     * Runs this method on a given items, and calls another task listener after
     * task is complete and results have been processed.
     * 
     * @param dataFiles Data files to be processed
     * @param alignmentResult AlignmentResults to be processed
     * @param methodListener A method listener whose methodFinished method is
     *            called after whole method has been completed on all given
     *            files/results.
     * 
     */
    public TaskSequence runMethod(OpenedRawDataFile[] dataFiles,
            AlignmentResult[] alignmentResults, ParameterSet parameters,
            TaskSequenceListener methodListener);

}
