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



/**
 *
 */
public class ControllerForNodesImpl extends java.rmi.server.UnicastRemoteObject implements ControllerForNodes {

	private ControllerServer controllerMain;

	/**
	 * Constructor
	 */
	public ControllerForNodesImpl(ControllerServer cm) throws java.rmi.RemoteException {
		super();
		controllerMain = cm;
	}



	/**
	 * Node calls this method to update processing status and rate of a job
	 * @param	taskID	TaskID of the process
	 * @param	itemID	ItemID of the item under processing
	 * @param	completionRate	Completion rate of the item processing (between 0..1)
	 */
	public void updateJobCompletionRate(int taskID, int jobID, int status, double completionRate) throws java.rmi.RemoteException {
		controllerMain.updateJobCompletionRate(taskID, jobID, status, completionRate);
	}


	/**
	 * Node calls this method to give results of a task
	 * @param	taskID	TaskID of the process
	 * @param	itemID	ItemID of the item whose results are passed
	 * @param	itemResult Result for the item
	 */
	public void setJobResult(int taskID, int jobID, Object result) throws java.rmi.RemoteException {

		controllerMain.updateJobCompletionRate(taskID, jobID, Task.JOBSTATUS_PROCESSINGDONE, 1.0F);
		controllerMain.setJobResult(taskID, jobID, result);
	}


	/**
	 * Node calls this method to give results of CLOSE RAW DATA FILE task
	 * @param	taskID	TaskID of the process
	 * @param	jobID	JobID (rawDataID) of the raw data file that was succesfully closed
	 * @param	rawDataID	Same as above, passed only to maintain uniform look of this method
	 */
	 /*
	public void setJobResult(int taskID, int jobID, Integer rawDataID) throws java.rmi.RemoteException {

		controllerMain.updateJobCompletionRate(taskID, jobID, Task.JOBSTATUS_PROCESSINGDONE, 1.0F);
		controllerMain.setJobResult(taskID, jobID, rawDataID);
	}
*/

	/**
	 * Node calls this method to give results of REFRESH VISUALIZERS task
	 * @param	taskID	TaskID of the process
	 * @param	jobID	JobID (rawDataID) of the raw data file that was succesfully closed
	 * @param	refreshResults Refresh results for a single raw data file
	 */
	/*
	public void setJobResult(int taskID, int jobID, RawDataVisualizerRefreshResult refreshResult) throws java.rmi.RemoteException {
		controllerMain.updateJobCompletionRate(taskID, jobID, Task.JOBSTATUS_PROCESSINGDONE, 1.0F);
		controllerMain.setJobResult(taskID, jobID, refreshResult);
	}
	*/


	/**
	 * Node calls this method to inform controller that processing failed for an item
	 * @param	taskID	TaskID of the process
	 * @param	itemID	ItemID of the item whose processing failed
	 * @param	errorMessage	Short human readable explanation about what happend
	 */

	public void setJobErrorMessage(int taskID, int jobID, String errorMessage) throws java.rmi.RemoteException {
		controllerMain.updateJobCompletionRate(taskID, jobID, Task.JOBSTATUS_PROCESSINGFAILED, 1.0F);
		controllerMain.setJobResult(taskID, jobID, errorMessage);
	}



}