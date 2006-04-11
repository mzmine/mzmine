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
 *
 */
public class ClientImpl extends java.rmi.server.UnicastRemoteObject implements Client {

	private ClientForCluster clip;

	/**
	 * Constructor
	 */
	public ClientImpl(ClientForCluster _clip) throws java.rmi.RemoteException {
		super();
		clip = _clip;
	}


	/**
	 * Controller calls this method to give basic information about how a task was distributed into jobs
	 * @param	taskID	TaskID of the process
	 * @param	jobDescription	Hashtable with jobID as key and human-readable description of the job as name
	 */
	public void defineJobs(int taskID, Hashtable<Integer, String> jobDescription) throws java.rmi.RemoteException {
		clip.defineJobs(taskID, jobDescription);
	}

	/**
	 * Controller calls this method to give name of node that is assigned to process a job
	 */
	public void updateJobNode(Integer taskID, Integer jobID, String nodeName) {
		clip.updateJobNode(taskID, jobID, nodeName);
	}

	/**
	 * Controller calls this method to update completion state & rate of a job
	 */
	public void updateJobStatus(Integer taskID, Integer jobID, Integer jobState, Double jobCompletionRate) throws java.rmi.RemoteException {
		clip.updateJobStatus(taskID, jobID, jobState, jobCompletionRate);
	}

	/**
	 * Controller calls this method to give results of task after processing has finished.
	 * @param	taskID	TaskID of the process
	 * @param	itemStates States for individual items (-1 = in queue, -2 = processing failed, 1 = being processed, 2 = done successfully)
	 * @param	itemResults	Result for each individual item in the task
	 */
	public void setTaskResults(int taskID, Hashtable<Integer, Integer> itemStates, Hashtable<Integer, Object> itemResults) throws java.rmi.RemoteException {
		clip.setTaskResults(taskID, itemStates, itemResults);
	}

}