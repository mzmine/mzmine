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

package net.sf.mzmine.modules.batchmode;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

/**
 * Interface representing a data processing method which can be executed in a batch
 */
public interface BatchStep extends MZmineModule {

    /**
     * Show a setup dialog for the module parameter set
     * 
     * @return ExitCode.OK or ExitCode.CANCEL depending how user closed the dialog 
     */
    public ExitCode setupParameters(ParameterSet parameters);

    /**
     * Runs this method on a given items, and calls another task listener after
     * task is complete and results have been processed.
     * 
     * @param dataFiles Data files to be processed
     * @param alignmentResult AlignmentResults to be processed
     * @param methodListener A method listener whose methodFinished method is
     *            called after whole method has been completed on all given
     *            files/peak lists.
     * 
     */
    public TaskGroup runModule(RawDataFile[] dataFiles,
            PeakList[] alignmentResults, ParameterSet parameters,
            TaskGroupListener methodListener);

}
