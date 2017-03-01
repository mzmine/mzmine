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

package net.sf.mzmine.modules.rawdatamethods.filtering.alignscans;

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

public class AlignScansTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private final RawDataFile dataFile;

    // scan counter
    private int processedScans = 0, totalScans;
    private int[] scanNumbers;

    // User parameters
    private String suffix;
    private double minimumHeight;
    private int scanSpan, mzSpan;
    private boolean logScale = false;
    private boolean removeOriginal;
    RawDataFile newRDF = null;

    /**
     * @param dataFile
     * @param parameters
     */
    public AlignScansTask(MZmineProject project, RawDataFile dataFile,
	    ParameterSet parameters) {

	this.project = project;
	this.dataFile = dataFile;

	this.scanSpan = parameters.getParameter(AlignScansParameters.scanSpan)
		.getValue();
	this.mzSpan = parameters.getParameter(AlignScansParameters.mzSpan)
		.getValue();
	this.minimumHeight = parameters.getParameter(
		AlignScansParameters.minimumHeight).getValue();
	this.suffix = parameters.getParameter(AlignScansParameters.suffix)
		.getValue();
	this.removeOriginal = parameters.getParameter(
		AlignScansParameters.removeOld).getValue();
	this.logScale = parameters.getParameter(
		AlignScansParameters.logTransform).getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "Aligning scans in " + dataFile;
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

	logger.info("Started Scan Alignment on " + dataFile);

	scanNumbers = dataFile.getScanNumbers(1);
	totalScans = scanNumbers.length;

	RawDataFileWriter newRDFW = null;
	try {
	    newRDFW = MZmineCore.createNewFile(dataFile.getName() + ' '
		    + suffix);

	    DataPoint mzValues[][] = null; // [relative scan][j value]
	    int i, j, si, sj, ii, k, shift, ks;
	    int shiftedScans[] = new int[mzSpan * 2 + 1];
	    for (i = 0; i < totalScans; i++) {

		if (isCanceled())
		    return;

		Scan scan = dataFile.getScan(scanNumbers[i]);
		si = (int) Math.max(0, i - scanSpan);
		sj = (int) (si + 2 * scanSpan);
		if (sj >= totalScans) {
		    si = (int) Math.max(0, si - (sj - totalScans + 1));
		    sj = (int) (si + 2 * scanSpan);
		}
		if (scan != null) {
		    // Allocate
		    if (mzValues == null || mzValues.length < sj - si + 1)
			mzValues = new DataPoint[sj - si + 1][];
		    // Load Data Points
		    for (j = si; j <= sj; j++) {
			Scan xscan = dataFile.getScan(scanNumbers[j]);
			mzValues[j - si] = xscan.getDataPoints();
		    }
		    // Estimate Correlations
		    ii = i - si;
		    final SimpleScan newScan = new SimpleScan(scan);
		    DataPoint[] newDP = new DataPoint[mzValues[ii].length];
		    int maxShift = 0;
		    double maxCorrelation = 0;
		    int ndp = mzValues[ii].length;
		    // System.out.print("Scan="+i);
		    for (shift = -mzSpan; shift <= mzSpan; shift++) {
			PearsonCorrelation thisShift = new PearsonCorrelation();
			for (k = 0; k < ndp; k++) {
			    ks = k + shift;
			    if (ks >= 0
				    && ks < ndp
				    && mzValues[ii][ks].getIntensity() >= minimumHeight) {
				DataPoint dp = mzValues[ii][k];
				double mz = dp.getMZ();
				int f = 0;
				for (j = 0; j < mzValues.length; j++) {
				    // System.out.println(j);
				    if (j != ii) {
					if (mzValues[j].length > k
						&& Math.abs(mzValues[j][k]
							.getMZ() - mz) < 1e-10) {
					    f = k;
					} else {
					    f = findFirstMass(mz, mzValues[j]);
					    if (Math.abs(mzValues[j][f].getMZ()
						    - mz) > 1e-10) {
						f = -f;
					    }
					}
					if (f >= 0) {
					    if (logScale) {
						thisShift
							.enter(Math
								.log(mzValues[j][f]
									.getIntensity()),
								Math.log(mzValues[ii][ks]
									.getIntensity()));
					    } else {
						thisShift
							.enter(mzValues[j][f]
								.getIntensity(),
								mzValues[ii][ks]
									.getIntensity());
					    }
					}
				    }
				}
			    }
			}
			// System.out.print(", shift="+shift+", correlation="+Math.round(thisShift.correlation()*1000)/1000.0);
			if (thisShift.correlation() > maxCorrelation) {
			    maxShift = shift;
			    maxCorrelation = thisShift.correlation();
			}
			// newDP[k] = new SimpleDataPoint(mz, c > 0 ? a/c : 0);
		    }
		    // Copy DataPoints with maxShift as the shift
		    shift = maxShift;
		    // System.out.println("\nScan="+i+", Shift="+maxShift+", Correlation="+maxCorrelation);
		    shiftedScans[maxShift + mzSpan]++;
		    for (k = 0; k < ndp; k++) {
			ks = k + shift;
			if (ks >= 0 && ks < ndp) {
			    newDP[k] = new SimpleDataPoint(
				    mzValues[ii][k].getMZ(),
				    mzValues[ii][ks].getIntensity());
			} else {
			    newDP[k] = new SimpleDataPoint(
				    mzValues[ii][k].getMZ(), 0);
			}
		    }
		    newScan.setDataPoints(newDP);
		    newRDFW.addScan(newScan);
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

		String shifts = "";
		for (i = -mzSpan; i <= mzSpan; i++) {
		    shifts = shifts + i + ":" + shiftedScans[i + mzSpan]
			    + " | ";
		}
		logger.info("Finished Scan Alignment on " + dataFile
			+ ". Scans per shift = " + shifts);

	    }

	} catch (IOException e) {
	    e.printStackTrace();
	}

    }

    int findFirstMass(double mass, DataPoint mzValues[]) {
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

class PearsonCorrelation {

    private int count; // Number of numbers that have been entered.
    private double sumX = 0;
    private double sumY = 0;
    private double sumXX = 0;
    private double sumYY = 0;
    private double sumXY = 0;

    void enter(double x, double y) {
	count++;
	sumX += x;
	sumY += y;
	sumXX += x * x;
	sumYY += y * y;
	sumXY += x * y;
    }

    int getCount() {
	return count;
    }

    double meanX() {
	return sumX / count;
    }

    double meanY() {
	return sumY / count;
    }

    double correlation() {

	double numerator = count * sumXY - sumX * sumY;
	int n = count; // here always use the same ... (count > 50 ? count - 1 :
		       // count);
	double denominator = Math.sqrt(n * sumXX - sumX * sumX)
		* Math.sqrt(n * sumYY - sumY * sumY);
	double c = (count < 3 ? 0 : numerator / denominator);
	return c;
    }
}
