/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.peakpicking.threestep;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.threestep.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ChromatogramBuilder;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

/**
 * @see
 */
class ThreeStepPickerTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private RawDataFile dataFile;

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	// scan counter
	private int processedScans, totalScans;
	private int newPeakID = 1;
	private int[] scanNumbers;

	// User parameters
	private String suffix;

	private int massDetectorTypeNumber, chromatogramBuilderTypeNumber,
			peakBuilderTypeNumber;

	// Mass Detector
	private MassDetector massDetector;

	// Chromatogram Builders
	private ChromatogramBuilder chromatogramBuilder;

	// Peak Builders
	private PeakBuilder peakBuilder;

	private ParameterSet mdParameters, cbParameters, pbParameters;

	private String description;

	/**
	 * @param dataFile
	 * @param parameters
	 */
	ThreeStepPickerTask(RawDataFile dataFile,
			ThreeStepPickerParameters parameters) {

		this.dataFile = dataFile;

		massDetectorTypeNumber = parameters.getMassDetectorTypeNumber();
		mdParameters = parameters
				.getMassDetectorParameters(massDetectorTypeNumber);

		chromatogramBuilderTypeNumber = parameters.getChromatogramBuilderTypeNumber();
		cbParameters = parameters
				.getChromatogramBuilderParameters(chromatogramBuilderTypeNumber);

		peakBuilderTypeNumber = parameters.getPeakBuilderTypeNumber();
		pbParameters = parameters
				.getPeakBuilderParameters(peakBuilderTypeNumber);
		suffix = parameters.getSuffix();
		scanNumbers = dataFile.getScanNumbers(1);
		Arrays.sort(scanNumbers);
		totalScans = scanNumbers.length;
		description = "Three step peak detection on "+ dataFile + " (Chromatogram building)";
		
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return description;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public float getFinishedPercentage() {
		if (totalScans == 0)
			return 0.0f;
		else
			return (float) processedScans / totalScans;
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

	public RawDataFile getDataFile() {
		return dataFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	/**
	 * @see Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;

		// Create new mass detector according with the user's selection
		String massDetectorClassName = ThreeStepPickerParameters.massDetectorClasses[massDetectorTypeNumber];
		try {
			Class massDetectorClass = Class.forName(massDetectorClassName);
			Constructor massDetectorConstruct = massDetectorClass
					.getConstructors()[0];
			massDetector = (MassDetector) massDetectorConstruct
					.newInstance(mdParameters);
		} catch (Exception e) {
			logger.finest("Error trying to make an instance of mass detector "
					+ massDetectorClassName);
			status = TaskStatus.ERROR;
			return;
		}

		// Create new chromatogram builder according with the user's selection
		String chromatogramBuilderClassName = ThreeStepPickerParameters.chromatogramBuilderClasses[chromatogramBuilderTypeNumber];
		try {
			Class chromatogramBuilderClass = Class
					.forName(chromatogramBuilderClassName);
			Constructor chromtogramBuilderConstruct = chromatogramBuilderClass
					.getConstructors()[0];
			chromatogramBuilder = (ChromatogramBuilder) chromtogramBuilderConstruct
					.newInstance(cbParameters);
		} catch (Exception e) {
			logger
					.finest("Error trying to make an instance of chromatogram builder "
							+ chromatogramBuilderClassName);
			status = TaskStatus.ERROR;
			return;
		}

		// Create new peak constructor according with the user's selection
		String peakBuilderClassName = ThreeStepPickerParameters.peakBuilderClasses[peakBuilderTypeNumber];
		try {
			Class peakBuilderClass = Class.forName(peakBuilderClassName);
			Constructor peakBuilderConstruct = peakBuilderClass
					.getConstructors()[0];
			peakBuilder = (PeakBuilder) peakBuilderConstruct
					.newInstance(pbParameters);
		} catch (Exception e) {
			logger.finest("Error trying to make an instance of peak builder "
					+ peakBuilderClassName);
			status = TaskStatus.ERROR;
			return;
		}

		// Create new peak list
		SimplePeakList newPeakList = new SimplePeakList(
				dataFile + " " + suffix, dataFile);

		MzPeak[] mzValues;
		Chromatogram[] chromatograms;
		ChromatographicPeak[] peaks;
		
		// TODO Verify the process in three steps
		for (int i = 0; i < totalScans; i++) {

			if (status == TaskStatus.CANCELED)
				return;

			Scan scan = dataFile.getScan(scanNumbers[i]);

			mzValues = massDetector.getMassValues(scan);

			chromatogramBuilder.addScan(dataFile, scan, mzValues);
			processedScans++;
		}
		
		// peaks = peakBuilder.finishPeaks();
		chromatograms = chromatogramBuilder.finishChromatograms();

		description = "Three step peak detection on "+ dataFile + " (Peak recognition)";
		totalScans = chromatograms.length;
		processedScans = 0;

		for (Chromatogram chromatogram : chromatograms) {
			if (status == TaskStatus.CANCELED)
				return;
			
			peaks = peakBuilder.addChromatogram(chromatogram, dataFile);

			if (peaks != null)
				for (ChromatographicPeak finishedPeak : peaks) {
					SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
					newPeakID++;
					newRow.addPeak(dataFile, finishedPeak, finishedPeak);
					newPeakList.addRow(newRow);
				}

			processedScans++;
		}
		
		// Add new peaklist to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(newPeakList);

		status = TaskStatus.FINISHED;
	}
	
}
