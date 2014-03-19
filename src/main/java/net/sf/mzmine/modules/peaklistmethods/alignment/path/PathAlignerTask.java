/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.peaklistmethods.alignment.path;

import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.path.functions.Aligner;
import net.sf.mzmine.modules.peaklistmethods.alignment.path.functions.ScoreAligner;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 *
 */
class PathAlignerTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private PeakList peakLists[], alignedPeakList;
    private String peakListName;
    private ParameterSet parameters;
    private Aligner aligner;

    PathAlignerTask(ParameterSet parameters) {

	this.parameters = parameters;
	peakLists = parameters.getParameter(PathAlignerParameters.peakLists)
		.getValue();
	;
	peakListName = parameters.getParameter(
		PathAlignerParameters.peakListName).getValue();
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Path aligner, " + peakListName + " (" + peakLists.length
		+ " peak lists)";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
	if (aligner == null) {
	    return 0f;
	} else {
	    return aligner.getProgress();
	}
    }

    /**
     * @see Runnable#run()
     */
    public void run() {
	setStatus(TaskStatus.PROCESSING);
	logger.info("Running Path aligner");

	aligner = (Aligner) new ScoreAligner(this.peakLists, parameters);
	alignedPeakList = aligner.align();
	// Add new aligned peak list to the project
	MZmineProject currentProject = MZmineCore.getCurrentProject();
	currentProject.addPeakList(alignedPeakList);

	// Add task description to peakList
	alignedPeakList
		.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
			"Path aligner", parameters));

	logger.info("Finished Path aligner");
	setStatus(TaskStatus.FINISHED);

    }

    public Object[] getCreatedObjects() {
	return new Object[] { alignedPeakList };
    }
}
