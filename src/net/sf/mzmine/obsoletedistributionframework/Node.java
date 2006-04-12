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

import net.sf.mzmine.methods.alignment.GapFillerParameters;
import net.sf.mzmine.methods.alignment.PeakListAlignerParameters;
import net.sf.mzmine.methods.filtering.FilterParameters;
import net.sf.mzmine.methods.peakpicking.PeakList;
import net.sf.mzmine.methods.peakpicking.PeakListProcessorParameters;
import net.sf.mzmine.methods.peakpicking.PeakPickerParameters;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerRefreshRequest;


/**
 *
 */
public interface Node extends java.rmi.Remote {

	public void connectToNode(InetAddress controllerIP, String controllerPort) throws java.rmi.RemoteException;

	public void connectToNode(ControllerServer controllerServer) throws java.rmi.RemoteException;


	/**
	 * Open raw data file job
	 */
	public void addJob(int taskID, int jobID, File originalRawDataFile) throws java.rmi.RemoteException;

	/**
	 * Close raw data file job
	 */
	public void addJob(int taskID, int jobID, boolean saveChanges) throws java.rmi.RemoteException;

	/**
	 * Calc data for raw data file's visualizers
	 */
	public void addJob(int taskID, int jobID, RawDataVisualizerRefreshRequest refreshRequest) throws java.rmi.RemoteException;

	/**
	 * Filter raw data file job
	 */
	public void addJob(int taskID, int jobID, FilterParameters filterParameters) throws java.rmi.RemoteException;

	/**
	 * Peak picking job
	 */
	public void addJob(int taskID, int jobID, PeakPickerParameters parameters) throws java.rmi.RemoteException;

	/**
	 * Alignment job
	 */
	public void addJob(int taskID, int jobID, Hashtable<Integer, PeakList> peakLists, PeakListAlignerParameters parameters) throws java.rmi.RemoteException;

	/**
	 * Gap filling job
	 */
	public void addJob(int taskID, int jobID, Hashtable<Integer, double[]> gapsToFill, GapFillerParameters parameters) throws java.rmi.RemoteException;

	/**
	 * Calc total raw signal job
	 */
	public void addJob(int taskID, int jobID) throws java.rmi.RemoteException;

	/**
	 * Process peak lists jobs
	 */
	public void addJob(int taskID, int jobID, PeakList peakList, PeakListProcessorParameters parameters) throws java.rmi.RemoteException;



}