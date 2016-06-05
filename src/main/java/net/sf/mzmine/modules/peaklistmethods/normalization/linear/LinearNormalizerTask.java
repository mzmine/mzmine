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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.normalization.linear;

import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakMeasurementType;
import net.sf.mzmine.util.PeakUtils;

class LinearNormalizerTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    static final double maximumOverallPeakHeightAfterNormalization = 100000.0;

    private final MZmineProject project;
    private PeakList originalPeakList, normalizedPeakList;

    private int processedDataFiles, totalDataFiles;

    private String suffix;
    private NormalizationType normalizationType;
    private PeakMeasurementType peakMeasurementType;
    private boolean removeOriginal;
    private ParameterSet parameters;

    public LinearNormalizerTask(MZmineProject project, PeakList peakList,
	    ParameterSet parameters) {

	this.project = project;
	this.originalPeakList = peakList;
	this.parameters = parameters;

	totalDataFiles = originalPeakList.getNumberOfRawDataFiles();

	suffix = parameters.getParameter(LinearNormalizerParameters.suffix)
		.getValue();
	normalizationType = parameters.getParameter(
		LinearNormalizerParameters.normalizationType).getValue();
	peakMeasurementType = parameters.getParameter(
		LinearNormalizerParameters.peakMeasurementType).getValue();
	removeOriginal = parameters.getParameter(
		LinearNormalizerParameters.autoRemove).getValue();

    }

    public double getFinishedPercentage() {
	return (double) processedDataFiles / (double) totalDataFiles;
    }

    public String getTaskDescription() {
	return "Linear normalization of " + originalPeakList + " by "
		+ normalizationType;
    }

    public void run() {

	setStatus(TaskStatus.PROCESSING);
	logger.info("Running linear normalizer");

	// This hashtable maps rows from original alignment result to rows of
	// the normalized alignment
	Hashtable<PeakListRow, SimplePeakListRow> rowMap = new Hashtable<PeakListRow, SimplePeakListRow>();

	// Create new peak list
	normalizedPeakList = new SimplePeakList(
		originalPeakList + " " + suffix,
		originalPeakList.getRawDataFiles());

	// Loop through all raw data files, and find the peak with biggest
	// height
	double maxOriginalHeight = 0.0;
	for (RawDataFile file : originalPeakList.getRawDataFiles()) {
	    for (PeakListRow originalpeakListRow : originalPeakList.getRows()) {
		Feature p = originalpeakListRow.getPeak(file);
		if (p != null) {
		    if (maxOriginalHeight <= p.getHeight())
			maxOriginalHeight = p.getHeight();
		}
	    }
	}

	// Loop through all raw data files, and normalize peak values
	for (RawDataFile file : originalPeakList.getRawDataFiles()) {

	    // Cancel?
	    if (isCanceled()) {
		return;
	    }

	    // Determine normalization type and calculate normalization factor
	    double normalizationFactor = 1.0;

	    // - normalization by average peak intensity
	    if (normalizationType == NormalizationType.AverageIntensity) {
		double intensitySum = 0;
		int intensityCount = 0;
		for (PeakListRow peakListRow : originalPeakList.getRows()) {
		    Feature p = peakListRow.getPeak(file);
		    if (p != null) {
			if (peakMeasurementType == PeakMeasurementType.HEIGHT) {
			    intensitySum += p.getHeight();
			} else {
			    intensitySum += p.getArea();
			}
			intensityCount++;
		    }
		}
		normalizationFactor = intensitySum / (double) intensityCount;
	    }

	    // - normalization by average squared peak intensity
	    if (normalizationType == NormalizationType.AverageSquaredIntensity) {
		double intensitySum = 0.0;
		int intensityCount = 0;
		for (PeakListRow peakListRow : originalPeakList.getRows()) {
		    Feature p = peakListRow.getPeak(file);
		    if (p != null) {
			if (peakMeasurementType == PeakMeasurementType.HEIGHT) {
			    intensitySum += (p.getHeight() * p.getHeight());
			} else {
			    intensitySum += (p.getArea() * p.getArea());
			}
			intensityCount++;
		    }
		}
		normalizationFactor = intensitySum / (double) intensityCount;
	    }

	    // - normalization by maximum peak intensity
	    if (normalizationType == NormalizationType.MaximumPeakHeight) {
		double maximumIntensity = 0.0;
		for (PeakListRow peakListRow : originalPeakList.getRows()) {
		    Feature p = peakListRow.getPeak(file);
		    if (p != null) {
			if (peakMeasurementType == PeakMeasurementType.HEIGHT) {
			    if (maximumIntensity < p.getHeight())
				maximumIntensity = p.getHeight();
			} else {
			    if (maximumIntensity < p.getArea())
				maximumIntensity = p.getArea();
			}

		    }
		}
		normalizationFactor = maximumIntensity;
	    }

	    // - normalization by total raw signal
	    if (normalizationType == NormalizationType.TotalRawSignal) {
		normalizationFactor = 0;
		for (int scanNumber : file.getScanNumbers(1)) {
		    Scan scan = file.getScan(scanNumber);
		    normalizationFactor += scan.getTIC();
		}
	    }

	    // Readjust normalization factor so that maximum height will be
	    // equal to maximumOverallPeakHeightAfterNormalization after
	    // normalization
	    double maxNormalizedHeight = maxOriginalHeight
		    / normalizationFactor;
	    normalizationFactor = normalizationFactor * maxNormalizedHeight
		    / maximumOverallPeakHeightAfterNormalization;

	    // Normalize all peak intenisities using the normalization factor
	    for (PeakListRow originalpeakListRow : originalPeakList.getRows()) {

		// Cancel?
		if (isCanceled()) {
		    return;
		}

		Feature originalPeak = originalpeakListRow.getPeak(file);
		if (originalPeak != null) {

		    SimpleFeature normalizedPeak = new SimpleFeature(
			    originalPeak);
		    PeakUtils.copyPeakProperties(originalPeak, normalizedPeak);

		    double normalizedHeight = originalPeak.getHeight()
			    / normalizationFactor;
		    double normalizedArea = originalPeak.getArea()
			    / normalizationFactor;
		    normalizedPeak.setHeight(normalizedHeight);
		    normalizedPeak.setArea(normalizedArea);

		    SimplePeakListRow normalizedRow = rowMap
			    .get(originalpeakListRow);

		    if (normalizedRow == null) {

			normalizedRow = new SimplePeakListRow(
				originalpeakListRow.getID());

			PeakUtils.copyPeakListRowProperties(
				originalpeakListRow, normalizedRow);

			rowMap.put(originalpeakListRow, normalizedRow);
		    }

		    normalizedRow.addPeak(file, normalizedPeak);

		}

	    }

	    // Progress
	    processedDataFiles++;

	}

	// Finally add all normalized rows to normalized alignment result
	for (PeakListRow originalpeakListRow : originalPeakList.getRows()) {
	    SimplePeakListRow normalizedRow = rowMap.get(originalpeakListRow);
	    if (normalizedRow == null) continue;
	    normalizedPeakList.addRow(normalizedRow);
	}

	// Add new peaklist to the project
	project.addPeakList(normalizedPeakList);

	// Load previous applied methods
	for (PeakListAppliedMethod proc : originalPeakList.getAppliedMethods()) {
	    normalizedPeakList.addDescriptionOfAppliedTask(proc);
	}

	// Add task description to peakList
	normalizedPeakList
		.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
			"Linear normalization of by " + normalizationType,
			parameters));

	// Remove the original peaklist if requested
	if (removeOriginal)
	    project.removePeakList(originalPeakList);

	logger.info("Finished linear normalizer");
	setStatus(TaskStatus.FINISHED);

    }

}
