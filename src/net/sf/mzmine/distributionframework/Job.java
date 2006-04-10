/*
    Copyright 2005 VTT Biotechnology

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


package net.sf.mzmine.distributionframework;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;


// Java packages
import java.util.Hashtable;


/**
 * This class is used for representing one task.
 */
public class Job {

	public final static int TYPE_OPENRAWDATA = 1;
	public final static int TYPE_CLOSERAWDATA = 2;
	public final static int TYPE_REFRESHVISUALIZERS = 3;
	public final static int TYPE_FILTERRAWDATA = 4;
	public final static int TYPE_FINDPEAKS = 5;
	public final static int TYPE_ALIGNMENT = 6;
	public final static int TYPE_GAPFILLING = 7;
	public final static int TYPE_CALCTOTALRAWSIGNAL = 8;
	public final static int TYPE_PROCESSPEAKLIST = 9;

	public final static Integer PARAM_ORIGINALFILE = 1;
	public final static Integer PARAM_SAVECHANGES = 2;
	public final static Integer PARAM_REFRESHREQUEST = 3;
	public final static Integer PARAM_FILTERPARAMETERS = 4;
	public final static Integer PARAM_PEAKPICKERPARAMETERS = 5;
	public final static Integer PARAM_ALIGNERPARAMETERS = 6;
	public final static Integer PARAM_PEAKLISTS = 7;
	public final static Integer PARAM_GAPFILLERPARAMETERS = 8;
	//public final static Integer PARAM_ALIGNMENTRESULT = 9;
	public final static Integer PARAM_GAPLIST = 9;
	public final static Integer PARAM_PEAKLIST = 10;
	public final static Integer PARAM_PEAKLISTPROCESSORPARAMETERS = 11;

	private int taskID;
	private int jobID;
	private int jobType;
	private Hashtable<Integer, Object> jobParameters;

	/**
	 * Constructor
	 * @param	_taskID TaskID of the task of which this job is a part
	 * @param	_jobID	JobID of the target of this job
	 * @param	_jobType	Type of the job
	 */
	public Job(int _taskID, int _jobID, int _jobType) {
		taskID = _taskID;
		jobID = _jobID;
		jobType = _jobType;
		jobParameters = new Hashtable<Integer, Object>();
	}

	/**
	 * This method returns the taskID of task where this job is part of.
	 */
	public int getTaskID() {
		return taskID;
	}

	/**
	 * This method returns the jobID of this job
	 */
	public int getJobID() {
		return jobID;
	}

	/**
	 * This method returns the task type
	 */
	public int getJobType() {
		return jobType;
	}

	/**
	 * This method assigns value to a "task parameter"
	 * @param	paramID		ID of job parameter
	 * @param	paramValue	Value of job parameter
	 */
	public void addJobParameter(Integer paramID, Object paramValue) {
		jobParameters.put(paramID, paramValue);
	}

	/**
	 * This method returns value of a "task parameter"
	 * @param	paramID		ID of the task parameter
	 * @return	Value of the task parameter
	 */
	 public Object getJobParameter(Integer paramID) {
		 return jobParameters.get(paramID);
	 }

}
