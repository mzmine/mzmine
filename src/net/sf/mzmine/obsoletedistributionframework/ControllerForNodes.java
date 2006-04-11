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
public interface ControllerForNodes extends java.rmi.Remote {


	/**
	 * Node calls this method to update task processing status of a item task
	 * @param	taskID	TaskID of the whole process
	 * @param	jobID	JobID of the job under processing
	 * @param	completionRate	Completion rate of the job (between 0..1)
	 */
	public void updateJobCompletionRate(int taskID, int jobID, int status, double completionRate) throws java.rmi.RemoteException;

/*
	// Various methods to set job results after processing

	// Method for Open raw data files task
	public void setJobResult(int taskID, int jobID, RawDataOnTransit result) throws java.rmi.RemoteException;

	// Method for Close raw data files task
	public void setJobResult(int taskID, int jobID, Integer rawDataID) throws java.rmi.RemoteException;

	// Method for Visualizer refresh task
	public void setJobResult(int taskID, int jobID, RawDataVisualizerRefreshResult refreshResult) throws java.rmi.RemoteException;
*/


	/**
	 * Node calls this method to give results of a task
	 * @param	taskID	TaskID of the process
	 * @param	itemID	ItemID of the item whose results are passed
	 * @param	itemResult Result for the item
	 */
	public void setJobResult(int taskID, int jobID, Object result) throws java.rmi.RemoteException;

	/**
	 * Node calls this method to inform controller that processing failed for an item
	 * @param	taskID	TaskID of the process
	 * @param	jobID	JobID of the job which couldn't be completed
	 * @param	errorMessage	Short, human readable explanation about what happend
	 */
	public void setJobErrorMessage(int taskID, int jobID, String errorMessage) throws java.rmi.RemoteException;

}
