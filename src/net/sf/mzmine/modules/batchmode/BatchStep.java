/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.batchmode;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.mzmineclient.MZmineModule;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Interface representing a data processing method which can be executed in a
 * batch
 */
public interface BatchStep extends MZmineModule {

    /**
     * Show a setup dialog for the module parameter set
     * 
     * @return ExitCode.OK or ExitCode.CANCEL depending how user closed the
     *         dialog
     */
    public ExitCode setupParameters(ParameterSet parameters);

    /**
     * Runs this method on a given items, and calls another task listener after
     * task is complete and results have been processed.
     * 
     * @param dataFiles Data files to be processed
     * @param alignmentResult AlignmentResults to be processed
     * 
     */
    public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters);

    /**
     * Returns the category of the batch step (e.g. raw data processing, peak
     * picking etc.)
     * 
     * @return Category of this batch step
     */
    public BatchStepCategory getBatchStepCategory();

}
