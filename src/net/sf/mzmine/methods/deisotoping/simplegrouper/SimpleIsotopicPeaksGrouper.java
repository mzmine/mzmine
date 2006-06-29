/*
    Copyright 2005-2006 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.methods.deisotoping.simplegrouper;


import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.interfaces.Peak;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.Logger;


/**
 * This class implements a simple isotopic peaks grouper method based on searhing for neighbouring peaks from expected locations.
 *
 * @version 31 March 2006
 */

public class SimpleIsotopicPeaksGrouper implements Method, TaskListener {

	private static final double neutronMW = 1.008665;

	public String getMethodDescription() {
		return new String("Simple isotopic peaks grouper");
	}

	/**
	 * Method asks parameter values from user
	 */
	public boolean askParameters(MethodParameters parameters) {

		SimpleIsotopicPeaksGrouperParameters currentParameters = (SimpleIsotopicPeaksGrouperParameters)parameters;
		if (currentParameters==null) return false;

		SimpleIsotopicPeaksGrouperParameterSetupDialog sdpsd = new SimpleIsotopicPeaksGrouperParameterSetupDialog(currentParameters);
		sdpsd.show();

		if (sdpsd.getExitCode()==-1) { return false; }

		return true;

	}


	public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {

		Task peaklistProcessorTask;
		SimpleIsotopicPeaksGrouperParameters param = (SimpleIsotopicPeaksGrouperParameters)parameters;

		for (RawDataFile rawDataFile: rawDataFiles) {
			peaklistProcessorTask = new SimpleIsotopicPeaksGrouperTask(rawDataFile, param);
			TaskController.getInstance().addTask(peaklistProcessorTask, this);
		}

	}


	public void taskStarted(Task task) {}

    public void taskFinished(Task task) {}



}

