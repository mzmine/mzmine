/*
 * Copyright 2006-2011 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder;

import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massconnection.MassConnector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massdetection.MassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massfilters.MassFilter;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 *
 */
public class ChromatogramBuilderTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private RawDataFile dataFile;

	// scan counter
	private int processedScans = 0, totalScans;
	private int newPeakID = 1;
	private int[] scanNumbers;

	// User parameters
	private String suffix;

	// Mass detector
	private MassDetector massDetector;

	// Mass filter
	private MassFilter massFilter;

	// Mass connector
	private MassConnector massConnector;

	private SimplePeakList newPeakList;

	/**
	 * @param dataFile
	 * @param parameters
	 */
	public ChromatogramBuilderTask(RawDataFile dataFile,
			ChromatogramBuilderParameters parameters) {

		this.dataFile = dataFile;

		this.massDetector = parameters.getMassDetector();
		this.massFilter = parameters.getMassFilter();
		this.massConnector = parameters.getMassConnector();

		suffix = parameters.getSuffix();

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Detecting chromatograms in " + dataFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalScans == 0)
			return 0;
		else
			return (double) processedScans / totalScans;
	}

	public RawDataFile getDataFile() {
		return dataFile;
	}

	/**
	 * @see Runnable#run()
	 */
	public void run() {

		setStatus( TaskStatus.PROCESSING );

		logger.info("Started chromatogram builder on " + dataFile);

		scanNumbers = dataFile.getScanNumbers(1);
		totalScans = scanNumbers.length;

		// Create new peak list
		newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

		MzPeak[] mzValues;
		Chromatogram[] chromatograms;

		for (int i = 0; i < totalScans; i++) {

			if ( isCanceled( ))
				return;

			Scan scan = dataFile.getScan(scanNumbers[i]);

			mzValues = massDetector.getMassValues(scan);

			if (massFilter != null)
				mzValues = massFilter.filterMassValues(mzValues);

			massConnector.addScan(dataFile, scanNumbers[i], mzValues);
			processedScans++;
		}

		// peaks = peakBuilder.finishPeaks();
		chromatograms = massConnector.finishChromatograms();

		for (ChromatographicPeak finishedPeak : chromatograms) {
			SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
			newPeakID++;
			newRow.addPeak(dataFile, finishedPeak);
			newPeakList.addRow(newRow);
		}

		// Add new peaklist to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(newPeakList);

		setStatus( TaskStatus.FINISHED );

		logger.info("Finished chromatogram builder on " + dataFile);

	}

	public Object[] getCreatedObjects() {
		return new Object[] { newPeakList };
	}

}
