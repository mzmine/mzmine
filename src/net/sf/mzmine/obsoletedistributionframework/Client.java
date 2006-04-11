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
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;




// Java packages
import java.util.Hashtable;


/**
 *
 */
public interface Client extends java.rmi.Remote {



	/**
	 * Controller calls this method to give basic information about how a task was distributed into jobs
	 * @param	taskID	TaskID of the process
	 * @param	jobDescription	Hashtable with jobID as key and human-readable description of the job as name
	 */
	public void defineJobs(int taskID, Hashtable<Integer, String> jobDescription) throws java.rmi.RemoteException;

	/**
	 * Controller calls this method to update task processing status of a task
	 * @param	taskID	TaskID of the process
	 * @param	jobStates	Processing states for individual items in the task (-1 = in queue, -2 = done but failed, 1 = under processing, 2 = done successfully)
	 * @param	jobRates  Completion rates for individual items in the task (0..1 = completion rate)
	 */
	//public void updateJobStatus(int taskID, Hashtable<Integer, Integer> jobStates, Hashtable<Integer, Double> jobRates) throws java.rmi.RemoteException;
	public void updateJobStatus(Integer taskID, Integer jobID, Integer jobState, Double jobCompletionRate) throws java.rmi.RemoteException;

	/**
	 * Controller calls this method to update name of the node that is processing the job
	 */
	public void updateJobNode(Integer taskID, Integer jobID, String nodeName) throws java.rmi.RemoteException;


	/**
	 * Controller calls this method to give results of task after processing has finished.
	 * @param	taskID	TaskID of the process
	 * @param	taskResults	Result for each individual item in the task
	 */
	public void setTaskResults(int taskID, Hashtable<Integer, Integer> itemStates, Hashtable<Integer, Object> itemResults) throws java.rmi.RemoteException;


}