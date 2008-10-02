/* Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.io.rawdataimport.fileformats;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PreloadLevel;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
public class XcaliburRawFileReadTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private File originalFile;
	private RawDataFileWriter newMZmineFile;
	private PreloadLevel preloadLevel;
	private TaskStatus status;
	private String errorMessage;

	/*
	 * These fields are modified by the dynamic library in order to get the
	 * current scan information. These fields are referred as common fields
	 * in later comments.
	 */
	private int totalScans = 0;
	private int scanNumber = 0;
	private int msLevel = 0;
	private int parsedScans = 0;
	private double retentionTime = 0d;
	private double mz[] = new double[0];
	private double intensity[] = new double[0];
	private double precursorMz = 0d;
	private int precursorCharge = 0;
	private int peaksCount = 0;

	/*
	 * This array is used to set the number of fragments that one single
	 * scan can have. The initial size of array is set to 10, but it depends of
	 * fragmentation level.
	 */
	private int parentTreeValue[] = new int[10];

	/*
	 * This stack stores the current scan and all his fragments until all the
	 * information is recover. The logic is FIFO at the moment of write into the
	 * MZmineFile
	 */
	private LinkedList<SimpleScan> parentStack;

	/*
	 * This variable hold the present scan or fragment, it is send to the stack
	 * when another scan/fragment appears as a parser.startElement
	 */
	private SimpleScan buildingScan;

	/*
	 * This function resides in ThermoXcaliburRawFileReader.dll file
	 */
	private native int openFile(String fileName);

	public XcaliburRawFileReadTask(File fileToOpen, PreloadLevel preloadLevel) {
		originalFile = fileToOpen;
		status = TaskStatus.WAITING;
		this.preloadLevel = preloadLevel;
		parentStack = new LinkedList<SimpleScan>();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Opening file " + originalFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;
		logger.info("Started parsing file " + originalFile);

		try {
			String libraryFullPath = System.getProperty("user.dir") + "\\lib\\ThermoRawFileReader.dll";
			System.load(libraryFullPath);
		} catch (Throwable e) {
			status = TaskStatus.ERROR;
			errorMessage = "Unable to load dynamic library to bind to Thermo Xcalibur";
			return;
		}

		try {
			newMZmineFile = MZmineCore.createNewFile(originalFile.getName(), 
					preloadLevel);
			logger.finest("Calling native function openFile "
					+ originalFile.getPath());
			int result = openFile(originalFile.getPath());
			if ((result != 0) && (status != TaskStatus.CANCELED)) {
				status = TaskStatus.ERROR;
				errorMessage = "There is an error in the RAW file, please check the log file";
				return;
			}

			if (status == TaskStatus.CANCELED) {
				return;
			}
			
			// Close file
			RawDataFile finalRawDataFile = newMZmineFile.finishWriting();
			MZmineCore.getCurrentProject().addFile(finalRawDataFile);

		} catch (Throwable e) {
			/* we may already have set the status to CANCELED */
			if (status == TaskStatus.PROCESSING)
				status = TaskStatus.ERROR;
			errorMessage = e.toString();
			return;
		}

		if (parsedScans == 0) {
			status = TaskStatus.ERROR;
			errorMessage = "No scans found";
			return;
		}

		logger.info("Finished parsing " + originalFile + ", parsed "
				+ parsedScans + " scans");
		status = TaskStatus.FINISHED;

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		logger.info("Cancelling opening of RAW file " + originalFile);
		status = TaskStatus.CANCELED;
	}

	/*
	 * This method is called by the native code each time that completes the 
	 * parsing process of the current scan. Always this is called after update
	 * common fields 
	 */
	public void startScan() throws IOException {

		if (status == TaskStatus.CANCELED)
			return;

		/*
		 * This section verify if the current scan is a full scan. If this condition
		 * is true means that the previous scans (scan/fragments), stored in the stack, 
		 * are complete and ready to be written in MZmineFile. 
		 */
		if ((msLevel == 1) && (!parentStack.isEmpty())) {
			while (!parentStack.isEmpty()) {
				SimpleScan currentScan = parentStack.removeLast();
				newMZmineFile.addScan(currentScan);
				parsedScans++;
			}
			parentStack.clear();
		}

		if (retentionTime == 0) {
			status = TaskStatus.ERROR;
			errorMessage = "This file does not contain retentionTime for scans";
			throw new IOException("Could not read retention time");
		}

		/*
		 * Setting the current parentScan
		 */
		int parentScan = -1;
		if (msLevel > 1)
			parentScan = parentTreeValue[msLevel - 1];

		// Setting the parent scan number for this level of fragments 
		parentTreeValue[msLevel] = scanNumber;

		buildingScan = new SimpleScan(scanNumber, msLevel, retentionTime,
				parentScan, 0f, null, new DataPoint[0], false);

		/*
		 * The common fields mz and intensity have the list of peaks for
		 * the current scan.
		 */
		if ((mz == null) || (intensity == null)) {
			status = TaskStatus.ERROR;
			errorMessage = "This file does not contain data for the current scan";
			throw new IOException(
					"Could not read mass over charge and intensity time");
		}

		// Copy m/z and intensity data
		DataPoint completeDataPoints[] = new DataPoint[peaksCount];
		DataPoint tempDataPoints[] = new DataPoint[peaksCount];

		for (int i = 0; i < completeDataPoints.length; i++) {
			completeDataPoints[i] = new SimpleDataPoint(mz[i], intensity[i]);
		}
		/*
		 * This section verifies DataPoints with intensity="0" and exclude them
		 * from tempDataPoints array. Only accept some of these points because
		 * they are part the left/right part of the peak.
		 */

		int i, j;
		for (i = 0, j = 0; i < completeDataPoints.length; i++) {
			double intensity = completeDataPoints[i].getIntensity();
			double mz = completeDataPoints[i].getMZ();
			
			//logger.finest("Mz " + mz + " " + intensity);
			
			if (completeDataPoints[i].getIntensity() > 0) {
				tempDataPoints[j] = new SimpleDataPoint(mz, intensity);
				j++;
				continue;
			}
			if ((i > 0) && (completeDataPoints[i - 1].getIntensity() > 0)) {
				tempDataPoints[j] = new SimpleDataPoint(mz, intensity);
				j++;
				continue;
			}
			if ((i < completeDataPoints.length - 1)
					&& (completeDataPoints[i + 1].getIntensity() > 0)) {
				tempDataPoints[j] = new SimpleDataPoint(mz, intensity);
				j++;
				continue;
			}
		}

		// If we have no peaks with intensity of 0, we assume the scan is
		// centroided
		if (i == j) {
			buildingScan.setCentroided(true);
			buildingScan.setDataPoints(tempDataPoints);
		} else {
			int sizeArray = j;
			DataPoint[] dataPoints = new DataPoint[j];

			System.arraycopy(tempDataPoints, 0, dataPoints, 0, sizeArray);
			buildingScan.setDataPoints(dataPoints);
		}

		if (precursorCharge != 0)
			buildingScan.setPrecursorCharge(precursorCharge);

		if (precursorMz != 0)
			buildingScan.setPrecursorMZ(precursorMz);

		if (!parentStack.isEmpty()) {
			for (SimpleScan currentScan : parentStack) {
				if (currentScan.getScanNumber() == buildingScan.getParentScanNumber()) {
					int[] currentFragmentScanNumbers = currentScan
							.getFragmentScanNumbers();
					if (currentFragmentScanNumbers != null) {
						int[] tempFragmentScanNumbers = currentFragmentScanNumbers;
						currentFragmentScanNumbers = new int[tempFragmentScanNumbers.length + 1];
						System.arraycopy(tempFragmentScanNumbers, 0,
								currentFragmentScanNumbers, 0,
								tempFragmentScanNumbers.length);
						currentFragmentScanNumbers[tempFragmentScanNumbers.length] = buildingScan
								.getScanNumber();
						currentScan.setFragmentScanNumbers(currentFragmentScanNumbers);
					} else {
						currentFragmentScanNumbers = new int[1];
						currentFragmentScanNumbers[0] = buildingScan
								.getScanNumber();
						currentScan.setFragmentScanNumbers(currentFragmentScanNumbers);
					}
				}
			}
		}

		parentStack.addFirst(buildingScan);
		if (scanNumber == totalScans) {
			while (!parentStack.isEmpty()) {
				SimpleScan currentScan = parentStack.removeLast();
				newMZmineFile.addScan(currentScan);
				parsedScans++;
			}
			parentStack.clear();
			
		}

		buildingScan = null;
	}

	/*
	 * This method clean all common fields for the next scan. The native code calls this after
	 * each single call of startScan method.
	 */
	public void clean() {
		scanNumber = 0;
		msLevel = 0;
		retentionTime = 0;
		mz = new double[0];
		intensity = new double[0];
		precursorMz = 0;
		precursorCharge = 0;
		peaksCount = 0;
	}
}
