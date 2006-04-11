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
import java.io.File;
import java.net.InetAddress;
import java.util.Hashtable;

import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.methods.alignment.GapFillerParameters;
import net.sf.mzmine.methods.alignment.PeakListAlignerParameters;
import net.sf.mzmine.methods.peakpicking.PeakList;
import net.sf.mzmine.methods.peakpicking.PeakListProcessorParameters;
import net.sf.mzmine.methods.peakpicking.PeakPickerParameters;
import net.sf.mzmine.methods.rawdata.FilterParameters;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerRefreshRequest;

/**
 *
 */
public interface ControllerForClients extends java.rmi.Remote {

	/**
	 * Client calls this method when it starts connection with the controller.
	 * Controller gives a ClientID that must be included in all further communication
	 * @param	Client's IP address
	 * @return	ClientID given by the controller
	 */
	public int connectToController(InetAddress clientIP, String clientPort) throws java.rmi.RemoteException;

	public int connectToController(ClientForCluster client) throws java.rmi.RemoteException;

	/**
	 * Client calls this method to disconnect from controller.
	 * @param	Client's IP address
	 * @return	ClientID given by the controller
	 */
	public void disconnectFromController(int clientID) throws java.rmi.RemoteException;


	/* Add task methods: different versions for different tasks */

	/**
	 * Add task method used for opening a set of raw data files
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems 	Files pointing to raw data files to be opened
	 * @return taskID of this task
	 */
	public int addTask(int clientID, int taskType, File[] taskItems) throws java.rmi.RemoteException;


	/**
	 * Add task method for closing a set of raw data files
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Array of raw data IDs
	 * @param	saveChanges	True/false flag telling whether modified working copies should overwrite to original raw data files
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, int[] taskItems, boolean saveChanges) throws java.rmi.RemoteException;


	/**
	 * Add task method for refreshing visualizers for a set of raw data files
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Array of raw data IDs
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, RawDataVisualizerRefreshRequest[] taskItems) throws java.rmi.RemoteException;

	/**
	 * Add task method for filtering a set of raw data files
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Array of raw data IDs
	 * @param	parameters	Parameters for raw data filtering method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, int[] taskItems, FilterParameters parameters) throws java.rmi.RemoteException;

	/**
	 * Add task method for finding peaks in a set of raw data files
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Array of raw data IDs
	 * @param	parameters	Parameters for peak picking method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, int[] taskItems, PeakPickerParameters parameters) throws java.rmi.RemoteException;

	/**
	 * Add task method for aligning peak lists
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Hashtable where key is raw data ID and value is a vector of peaks for the raw data
	 * @param	parameters	Parameters for aligner method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, Hashtable<Integer,PeakList> taskItems, PeakListAlignerParameters parameters) throws java.rmi.RemoteException;

	/**
	 * Add task method for filling gaps in alignment result
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Hashtable where key is raw data ID and value is a vector of peaks for the raw data
	 * @param	parameters	Parameters for aligner method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, AlignmentResult alignmentResult, GapFillerParameters parameters) throws java.rmi.RemoteException;

	/**
	 * Add task method for normalization by total raw signal
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	alignmentResult 	Total raw signal must be measured for all raw data files present in this alignment
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, int rawDataIDs[]) throws java.rmi.RemoteException;

	/**
	 * Add task method for processing peak lists
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Hashtable where key is raw data ID and value is a vector of peaks for the raw data
	 * @param	parameters	Parameters for aligner method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, Hashtable<Integer,PeakList> taskItems, PeakListProcessorParameters parameters) throws java.rmi.RemoteException;




	/**
	 * This methods return a list of itemIDs assigned for items in given task
	 * @param	taskID	TaskID whose items are to be listed
	 * @return	array of itemIDs for items in given task (listed in the same order as items were given when client called addTask-method)
	 */
	public int[] getItemIDs(int taskID) throws java.rmi.RemoteException;

}
