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

package net.sf.mzmine.modules.peaklistmethods.io.casmiimport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimpleMassList;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;

class CasmiImportTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private final String casmiProblemName, msSpectrum, msMsSpectrum;
    private final PolarityType polarity;
    private PeakList newPeakList;
    private RawDataFile newDataFile;

    CasmiImportTask(MZmineProject project, ParameterSet parameters) {

	this.project = project;
	casmiProblemName = parameters.getParameter(
		CasmiImportParameters.casmiProblemName).getValue();
	msSpectrum = parameters.getParameter(CasmiImportParameters.msSpectrum)
		.getValue();
	msMsSpectrum = parameters.getParameter(
		CasmiImportParameters.msMsSpectrum).getValue();
	polarity = parameters.getParameter(CasmiImportParameters.polarity)
		.getValue();

    }

    public double getFinishedPercentage() {
	if (newPeakList != null)
	    return 1d;
	if ((newPeakList == null) && (newDataFile != null))
	    return 0.5;
	return 0d;

    }

    public String getTaskDescription() {
	return "Generating CASMI task " + casmiProblemName;
    }

    public void run() {

	setStatus(TaskStatus.PROCESSING);

	logger.finest("Generating data for CASMI problem " + casmiProblemName);

	try {

	    final DataPoint[] msSpectrumDataPoints = convertTextSpectrumToDataPoints(msSpectrum);
	    final DataPoint[] msMsSpectrumDataPoints = convertTextSpectrumToDataPoints(msMsSpectrum);

	    assert msSpectrumDataPoints.length > 0;
	    assert msMsSpectrumDataPoints.length > 0;

	    final DataPoint firstDataPoint = msSpectrumDataPoints[0];
	    final int msScanNumber = 1;
	    final int msMsScanNumber = 2;

	    // Generate the raw data file
	    RawDataFileWriter dataFileWriter;

	    dataFileWriter = MZmineCore.createNewFile(casmiProblemName
		    + " raw data");
	    double precursorMz = firstDataPoint.getMZ();
	    Scan msScan = new SimpleScan(null, msScanNumber, 1, 1.0, 0, 0,
		    new int[] { 2 }, msSpectrumDataPoints,
		    MassSpectrumType.CENTROIDED, polarity, "", null);
	    Scan msMsScan = new SimpleScan(null, msMsScanNumber, 2, 1.1,
		    precursorMz, 1, null, msMsSpectrumDataPoints,
		    MassSpectrumType.CENTROIDED, polarity, "", null);
	    dataFileWriter.addScan(msScan);
	    dataFileWriter.addScan(msMsScan);
	    newDataFile = dataFileWriter.finishWriting();

	    // Add mass lists to scans
	    Scan finalMsScan = newDataFile.getScan(msScanNumber);
	    final MassList msScanMassList = new SimpleMassList("CASMI",
		    finalMsScan, msSpectrumDataPoints);
	    finalMsScan.addMassList(msScanMassList);
	    Scan finalMsMsScan = newDataFile.getScan(msMsScanNumber);
	    final MassList msMsScanMassList = new SimpleMassList("CASMI",
		    finalMsMsScan, msMsSpectrumDataPoints);
	    finalMsMsScan.addMassList(msMsScanMassList);

	    // Generate the peak
	    double mz = firstDataPoint.getMZ();
	    double rt = msScan.getRetentionTime();
	    double height = firstDataPoint.getIntensity();
	    double area = firstDataPoint.getIntensity();
	    int scanNumbers[] = new int[] { 1 };
	    DataPoint dataPointsPerScan[] = new DataPoint[] { firstDataPoint };
	    Range<Double> mzRange = ScanUtils.findMzRange(msSpectrumDataPoints);
	    Range<Double> rtRange = Range.singleton(msScan.getRetentionTime());
	    Range<Double> intensityRange = Range.closed(0.0,
		    firstDataPoint.getIntensity());
	    Feature newPeak = new SimpleFeature(newDataFile, mz, rt, height,
		    area, scanNumbers, dataPointsPerScan, FeatureStatus.MANUAL,
		    msScanNumber, msMsScanNumber, rtRange, mzRange,
		    intensityRange);

	    // Generate the isotope pattern
	    IsotopePattern isotopePat = new SimpleIsotopePattern(
		    msSpectrumDataPoints, IsotopePatternStatus.DETECTED,
		    casmiProblemName + " isotopes");
	    newPeak.setIsotopePattern(isotopePat);

	    // Generate the peak list row
	    PeakListRow newRow = new SimplePeakListRow(1);
	    newRow.addPeak(newDataFile, newPeak);

	    // Generate the final peak list
	    newPeakList = new SimplePeakList(casmiProblemName + " peak list",
		    newDataFile);
	    newPeakList.addRow(newRow);

	    project.addFile(newDataFile);
	    project.addPeakList(newPeakList);

	    logger.finest("Finished generating data for CASMI problem "
		    + casmiProblemName);

	} catch (IOException e) {
	    e.printStackTrace();
	    setStatus(TaskStatus.ERROR);
	    setErrorMessage(e.getMessage());
	}

	setStatus(TaskStatus.FINISHED);

    }

    private DataPoint[] convertTextSpectrumToDataPoints(String textSpectrum) {
	assert textSpectrum != null;
	ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
	final Scanner scanner = new Scanner(textSpectrum);
	while (scanner.hasNextLine()) {
	    final String line = scanner.nextLine();
	    final String items[] = line.split("\\s+");
	    if (items.length != 2)
		continue;
	    final double mz = Double.parseDouble(items[0]);
	    final double intensity = Double.parseDouble(items[1]);
	    DataPoint newDP = new SimpleDataPoint(mz, intensity);
	    dataPoints.add(newDP);
	}
	scanner.close();
	return dataPoints.toArray(new DataPoint[0]);
    }

}
