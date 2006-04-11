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

package net.sf.mzmine.obsoletedistributionframework;
import java.util.Hashtable;



/**
 * This class represent one task
 */
public class Task {
	public static final int TASKSTATUS_JUSTCREATED = 1;
	public static final int TASKSTATUS_READYFORSENDINGTONODES = 2;
	public static final int TASKSTATUS_UNDERPROCESSING = 3;
	public static final int TASKSTATUS_PROCESSINGDONE = 4;
	public static final int TASKSTATUS_RESULTSSENTTOCLIENT = 5;

	public static final int ITEMSTATUS_READYFORSENDINGTONODE = 1;
	public static final int ITEMSTATUS_INQUEUEATNODE = 2;
	public static final int ITEMSTATUS_UNDERPROCESSING = 3;
	public static final int ITEMSTATUS_PROCESSINGDONE = 4;
	public static final int ITEMSTATUS_PROCESSINGFAILED = 5;

	public static final int JOBSTATUS_UNKNOWN = 0;
	public static final int JOBSTATUS_READYFORSENDINGTONODE = 1;
	public static final int JOBSTATUS_INQUEUEATNODE = 2;
	public static final int JOBSTATUS_UNDERPROCESSING = 3;
	public static final int JOBSTATUS_PROCESSINGDONE = 4;
	public static final int JOBSTATUS_PROCESSINGFAILED = 5;

	public static final String JOBSTATUS_UNKNOWN_STR = "Unknown";
	public static final String JOBSTATUS_READYFORSENDINGTONODE_STR = "Dispatching";
	public static final String JOBSTATUS_INQUEUEATNODE_STR = "Queued";
	public static final String JOBSTATUS_UNDERPROCESSING_STR = "Processing";
	public static final String JOBSTATUS_PROCESSINGDONE_STR = "Done";
	public static final String JOBSTATUS_PROCESSINGFAILED_STR = "Failed";

	public static final int TASKTYPE_OPENRAWDATAFILES = 1;
	public static final int TASKTYPE_CLOSERAWDATAFILES = 2;
	public static final int TASKTYPE_REFRESHVISUALIZERS = 3;
	public static final int TASKTYPE_FILTERRAWDATAFILES = 4;
	public static final int TASKTYPE_FINDPEAKS = 5;
	public static final int TASKTYPE_ALIGNMENT = 6;
	public static final int TASKTYPE_GAPFILL = 7;
	public static final int TASKTYPE_CALCTOTALRAWSIGNAL = 8;
	public static final int TASKTYPE_PROCESSPEAKLISTS = 9;

	public static final Integer TASKPARAMETER_SAVECHANGES = 1;
	public static final Integer TASKPARAMETER_FILTERPARAMETERS = 2;
	public static final Integer TASKPARAMETER_PEAKPICKERPARAMETERS = 3;
	public static final Integer TASKPARAMETER_ALIGNMENTPARAMETERS = 4;
	public static final Integer TASKPARAMETER_GAPFILLERPARAMETERS = 5;
	public static final Integer TASKPARAMETER_PEAKLISTPROCESSORPARAMETERS = 6;

	public Client client;	// Client whose order this task is.

	public int taskID;		// TaskID assigned to this task
	public int taskType;	// Type of the task
	public int taskStatus;	// Current status of the task

	public Hashtable<Integer, Object> taskParameters;	// This table stores task specific parameters

	public int[] itemIDs;	// ItemIDs assigned to individual items in this task
	public Object[] items;	// Data for each item
	//public Hashtable<Integer, Integer> itemStatus;

	public int numOfJobs;	// JobIDs assigned to individual jobs in this task
	public Hashtable<Integer, Integer> jobStatus;
	public Hashtable<Integer, Double> jobCompletionRate;
	public Hashtable<Integer, Object> jobResult;

	public Task() {
		// itemStatus = new Hashtable<Integer, Integer>();

		taskParameters = new Hashtable<Integer, Object>();

		jobStatus = new Hashtable<Integer, Integer>();
		jobCompletionRate = new Hashtable<Integer, Double>();
		jobResult = new Hashtable<Integer, Object>();
	}

}
