/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.filtering.scansmoothing;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class ScanSmoothingTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private final RawDataFile dataFile;

    // scan counter
    private int processedScans = 0, totalScans;
    private int[] scanNumbers;

    // User parameters
    private String suffix;
    private double timeSpan, minimumHeight;
    private int scanSpan;
    private int mzPoints;
    private double mzTol;
    private boolean removeOriginal;
    RawDataFile newRDF = null;

    /**
     * @param dataFile
     * @param parameters
     */
    public ScanSmoothingTask(MZmineProject project, RawDataFile dataFile,
	    ParameterSet parameters) {

	this.project = project;
	this.dataFile = dataFile;

	this.timeSpan = parameters.getParameter(
		ScanSmoothingParameters.timeSpan).getValue();
	this.scanSpan = parameters.getParameter(
		ScanSmoothingParameters.scanSpan).getValue();
	this.mzTol = parameters.getParameter(
		ScanSmoothingParameters.mzTolerance).getValue();
	this.mzPoints = parameters.getParameter(
		ScanSmoothingParameters.mzPoints).getValue();
	this.minimumHeight = parameters.getParameter(
		ScanSmoothingParameters.minimumHeight).getValue();
	this.suffix = parameters.getParameter(ScanSmoothingParameters.suffix)
		.getValue();
	this.removeOriginal = parameters.getParameter(
		ScanSmoothingParameters.removeOld).getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Smoothing scans in " + dataFile;
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

	setStatus(TaskStatus.PROCESSING);

	logger.info("Started Scan Smoothing on " + dataFile);

	scanNumbers = dataFile.getScanNumbers(1);
	totalScans = scanNumbers.length;

	RawDataFileWriter newRDFW = null;
	int timepassed = 0;
	int mzpassed = 0;
	try {
	    newRDFW = MZmineCore.createNewFile(dataFile.getName() + ' '
		    + suffix);

	    DataPoint mzValues[][] = null; // [relative scan][j value]
	    int i, j, si, sj, ii, k, ssi, ssj;
	    for (i = 0; i < totalScans; i++) {

		if (isCanceled())
		    return;

		// Smoothing in TIME space
		Scan scan = dataFile.getScan(scanNumbers[i]);
		if (scan != null) {
		    double rt = scan.getRetentionTime();
		    final SimpleScan newScan = new SimpleScan(scan);
		    DataPoint[] newDP = null;
		    sj = si = i;
		    ssi = ssj = i;
		    if (timeSpan > 0 || scanSpan > 0) {
			double timeMZtol = Math.max(mzTol, 1e-5);
			for (si = i; si > 1; si--) {
			    Scan scanS = dataFile.getScan(scanNumbers[si - 1]);
			    if (scanS == null
				    || scanS.getRetentionTime() < rt - timeSpan
					    / 2) {
				break;
			    }
			}
			for (sj = i; sj < totalScans - 1; sj++) {
			    Scan scanS = dataFile.getScan(scanNumbers[sj + 1]);
			    if (scanS == null
				    || scanS.getRetentionTime() >= rt
					    + timeSpan / 2) {
				break;
			    }
			}
			ssi = i - (scanSpan - 1) / 2;
			ssj = i + (scanSpan - 1) / 2;
			if (ssi < 0) {
			    ssj += -ssi;
			    ssi = 0;
			}
			if (ssj >= totalScans) {
			    ssi -= (ssj - totalScans + 1);
			    ssj = totalScans - 1;
			}
			if (sj - si + 1 < scanSpan) {
			    si = ssi;
			    sj = ssj;
			    // si = Math.min(si, ssi);
			    // sj = Math.max(sj, ssj);
			}
			if (sj > si) {
			    timepassed++;
			    // Allocate
			    if (mzValues == null
				    || mzValues.length < sj - si + 1)
				mzValues = new DataPoint[sj - si + 1][];
			    // Load Data Points
			    for (j = si; j <= sj; j++) {
				Scan xscan = dataFile.getScan(scanNumbers[j]);
				mzValues[j - si] = xscan.getDataPoints();
			    }
			    // Estimate Averages
			    ii = i - si;
			    newDP = new DataPoint[mzValues[ii].length];
			    for (k = 0; k < mzValues[ii].length; k++) {
				DataPoint dp = mzValues[ii][k];
				double mz = dp.getMZ();
				double intensidad = 0;
				if (dp.getIntensity() > 0) { // only process
							     // those > 0
				    double a = 0;
				    short c = 0;
				    int f = 0;
				    for (j = 0; j < mzValues.length; j++) {
					// System.out.println(j);
					if (mzValues[j].length > k
						&& Math.abs(mzValues[j][k]
							.getMZ() - mz) < timeMZtol) {
					    f = k;
					} else {
					    f = findFirstMass(mz, mzValues[j]);
					    if (Math.abs(mzValues[j][f].getMZ()
						    - mz) > timeMZtol) {
						f = -f;
					    }
					}
					if (f >= 0
						&& mzValues[j][f]
							.getIntensity() >= minimumHeight) {
					    a += mzValues[j][f].getIntensity();
					    c++;
					} else {
					    c = (short) (c + 0);
					}
				    }
				    intensidad = c > 0 ? a / c : 0;
				}
				newDP[k] = new SimpleDataPoint(mz, intensidad);
			    }
			}
		    } else if (scan != null) {
			newDP = scan.getDataPoints();
		    }

		    // Smoothing in MZ space

		    if ((mzTol > 0 || mzPoints > 0)) {
			mzpassed++;
			DataPoint[] updatedDP = new DataPoint[newDP.length];
			for (k = 0; k < newDP.length; k++) {
			    double mz = newDP[k].getMZ();
			    double intensidad = 0;
			    if (newDP[k].getIntensity() > 0) {
				for (si = k; si > 0
					&& (newDP[si].getMZ() + mzTol >= mz || k
						- si <= mzPoints); si--)
				    ;
				for (sj = k; sj < newDP.length - 1
					&& (newDP[sj].getMZ() - mzTol <= mz || sj
						- k <= mzPoints); sj++)
				    ;
				double sum = 0;
				for (j = si; j <= sj; j++) {
				    sum += newDP[j].getIntensity();
				}
				intensidad = sum / (sj - si + 1);
			    }
			    updatedDP[k] = new SimpleDataPoint(mz, intensidad);
			}
			newDP = updatedDP;
		    }

		    // Register new smoothing data
		    if (scan != null && newDP != null) {
			newScan.setDataPoints(newDP);
			newRDFW.addScan(newScan);
		    }
		}
		processedScans++;
	    }

	    if (!isCanceled()) {

		// Finalize writing
		newRDF = newRDFW.finishWriting();

		// Add the newly created file to the project
		project.addFile(newRDF);

		// Remove the original data file if requested
		if (removeOriginal) {
		    project.removeFile(dataFile);
		}

		setStatus(TaskStatus.FINISHED);

		if (mzpassed + timepassed < totalScans / 2) {
		    logger.warning("It seems that parameters were not properly set. Scans processed : time="
			    + timepassed + ", mz=" + mzpassed);
		}

		logger.info("Finished Scan Smoothing on " + dataFile);

	    }

	} catch (IOException e) {
	    e.printStackTrace();
	}

    }

    static int findFirstMass(double mass, DataPoint mzValues[]) {
	int l = 0;
	int r = mzValues.length - 1;
	int mid = 0;
	while (l < r) {
	    mid = (r + l) / 2;
	    if (mzValues[mid].getMZ() > mass) {
		r = mid - 1;
	    } else if (mzValues[mid].getMZ() < mass) {
		l = mid + 1;
	    } else {
		r = mid;
	    }
	}
	return l;
    }

}
