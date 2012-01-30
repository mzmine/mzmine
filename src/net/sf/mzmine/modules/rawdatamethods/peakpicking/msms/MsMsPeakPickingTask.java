/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.peakpicking.msms;

import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleChromatographicPeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

public class MsMsPeakPickingTask extends AbstractTask {
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private int processedScans, totalScans;

	private RawDataFile dataFile;
	private double binSize;
	private double binTime;
	private int msLevel;

	private SimplePeakList newPeakList;

	public MsMsPeakPickingTask(RawDataFile dataFile,
			ParameterSet parameters) {
		this.dataFile = dataFile;
		binSize = parameters.getParameter(MsMsPeakPickerParameters.mzWindow)
				.getValue();
		binTime = parameters.getParameter(MsMsPeakPickerParameters.rtWindow)
				.getValue();

		msLevel = parameters.getParameter(MsMsPeakPickerParameters.msLevel)
				.getValue();
		newPeakList = new SimplePeakList(dataFile.getName() + " MS/MS peaks",
				dataFile);
	}

	public RawDataFile getDataFile() {
		return dataFile;
	}

	public double getFinishedPercentage() {
		if (totalScans == 0)
			return 0f;
		return (double) processedScans / totalScans;
	}

	public String getTaskDescription() {
		return "Building MS/MS Peaklist based on MS/MS from " + dataFile;
	}

	public void run() {

		setStatus(TaskStatus.PROCESSING);

		int[] scanNumbers = dataFile.getScanNumbers(msLevel);
		totalScans = scanNumbers.length;
		for (int scanNumber : scanNumbers) {
			if (isCanceled())
				return;

			// Get next MS/MS scan
			Scan scan = dataFile.getScan(scanNumber);

			// no parents scan for this msms scan
			if (scan.getParentScanNumber() <= 0) {
				continue;
			}

			// Get the MS Scan
			Scan bestScan = null;
			Range rtWindow = new Range(scan.getRetentionTime()
					- (binTime / 2.0f), scan.getRetentionTime()
					+ (binTime / 2.0f));
			Range mzWindow = new Range(
					scan.getPrecursorMZ() - (binSize / 2.0f),
					scan.getPrecursorMZ() + (binSize / 2.0f));
			DataPoint point;
			DataPoint maxPoint = null;
			int[] regionScanNumbers = dataFile.getScanNumbers(1, rtWindow);
			for (int regionScanNumber : regionScanNumbers) {
				Scan regionScan = dataFile.getScan(regionScanNumber);
				point = ScanUtils.findBasePeak(regionScan, mzWindow);
				// no datapoint found
				if (point == null) {
					continue;
				}
				if (maxPoint == null) {
					maxPoint = point;
				}
				int result = Double.compare(maxPoint.getIntensity(),
						point.getIntensity());
				if (result <= 0) {
					maxPoint = point;
					bestScan = regionScan;
				}

			}

			// if no representative dataPoint
			if (bestScan == null) {
				continue;
			}
			
			assert maxPoint != null;

			SimpleChromatographicPeak c = new SimpleChromatographicPeak(
					dataFile, scan.getPrecursorMZ(),
					bestScan.getRetentionTime(), maxPoint.getIntensity(),
					maxPoint.getIntensity(),
					new int[] { bestScan.getScanNumber() },
					new DataPoint[] { maxPoint }, PeakStatus.DETECTED,
					bestScan.getScanNumber(), scan.getScanNumber(), new Range(
							bestScan.getRetentionTime()), new Range(
							scan.getPrecursorMZ()), new Range(
							maxPoint.getIntensity()));

			PeakListRow entry = new SimplePeakListRow(scan.getScanNumber());
			entry.addPeak(dataFile, c);

			newPeakList.addRow(entry);
			processedScans++;
		}

		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(newPeakList);
		logger.info("Finished MS/MS peak builder on " + dataFile + ", "
				+ processedScans + " scans processed");

		setStatus(TaskStatus.FINISHED);
	}

	public Object[] getCreatedObjects() {
		return new Object[] { newPeakList };
	}

}
