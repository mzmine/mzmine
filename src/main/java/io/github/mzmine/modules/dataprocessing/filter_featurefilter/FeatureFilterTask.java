/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.filter_featurefilter;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.util.RangeUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import com.google.common.primitives.Booleans;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

/**
 * Filters out peaks from feature list.
 */
public class FeatureFilterTask extends AbstractTask {

  // Logger
  private static final Logger logger = Logger.getLogger(FeatureFilterTask.class.getName());

  // Feature lists
  private final MZmineProject project;
  private final FeatureList origPeakList;
  private ModularFeatureList filteredPeakList;

  // Processed rows counter
  private int processedRows, totalRows;

  // Parameters
  private final ParameterSet parameters;

  /**
   * Create the task.
   *
   * @param list feature list to process.
   * @param parameterSet task parameters.
   */
  public FeatureFilterTask(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet) {

    // Initialize
    this.project = project;
    parameters = parameterSet;
    origPeakList = list;
    filteredPeakList = null;
    processedRows = 0;
    totalRows = 0;
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Filtering feature list";
  }

  @Override
  public void run() {

    if (isCanceled()) {
      return;
    }

    try {
      setStatus(TaskStatus.PROCESSING);
      logger.info("Filtering feature list");

      // Filter the feature list
      filteredPeakList = filterPeakList(origPeakList);

      if (!isCanceled()) {

        // Add new peaklist to the project
        project.addFeatureList(filteredPeakList);

        // Remove the original peaklist if requested
        if (parameters.getParameter(FeatureFilterParameters.AUTO_REMOVE).getValue()) {
          project.removeFeatureList(origPeakList);
        }
        setStatus(TaskStatus.FINISHED);
        logger.info("Finished feature list filter");
      }
    } catch (Throwable t) {
      t.printStackTrace();
      setErrorMessage(t.getMessage());
      logger.log(Level.SEVERE, t.getMessage());
      setStatus(TaskStatus.ERROR);
    }

  }

  /**
   * Filter the feature list.
   *
   * @param peakList feature list to filter.
   * @return a new feature list with entries of the original feature list that pass the filtering.
   */
  private ModularFeatureList filterPeakList(final FeatureList peakList) {

    // Make a copy of the peakList
    final ModularFeatureList newPeakList = new ModularFeatureList(
        peakList.getName() + ' ' + parameters.getParameter(RowsFilterParameters.SUFFIX).getValue(),
        peakList.getRawDataFiles());

    // Get parameters - which filters are active
    final boolean filterByDuration =
        parameters.getParameter(FeatureFilterParameters.PEAK_DURATION).getValue();
    final boolean filterByArea =
        parameters.getParameter(FeatureFilterParameters.PEAK_AREA).getValue();
    final boolean filterByHeight =
        parameters.getParameter(FeatureFilterParameters.PEAK_HEIGHT).getValue();
    final boolean filterByDatapoints =
        parameters.getParameter(FeatureFilterParameters.PEAK_DATAPOINTS).getValue();
    final boolean filterByFWHM =
        parameters.getParameter(FeatureFilterParameters.PEAK_FWHM).getValue();
    final boolean filterByTailingFactor =
        parameters.getParameter(FeatureFilterParameters.PEAK_TAILINGFACTOR).getValue();
    final boolean filterByAsymmetryFactor =
        parameters.getParameter(FeatureFilterParameters.PEAK_ASYMMETRYFACTOR).getValue();
    final boolean filterByMS2 =
        parameters.getParameter(FeatureFilterParameters.MS2_Filter).getValue();

    // Loop through all rows in feature list
    final ModularFeatureListRow[] rows = peakList.getRows().toArray(ModularFeatureListRow[]::new);
    totalRows = rows.length;
    for (processedRows = 0; !isCanceled() && processedRows < totalRows; processedRows++) {
      final ModularFeatureListRow row = rows[processedRows];
      final RawDataFile[] rawdatafiles = row.getRawDataFiles().toArray(new RawDataFile[0]);
      int totalRawDataFiles = rawdatafiles.length;
      boolean[] keepPeak = new boolean[totalRawDataFiles];

      for (int i = 0; i < totalRawDataFiles; i++) {
        // Peak values
        keepPeak[i] = true;
        final Feature peak = row.getFeature(rawdatafiles[i]);
        final double peakDuration = peak.getRawDataPointsRTRange().upperEndpoint()
            - peak.getRawDataPointsRTRange().lowerEndpoint();
        final double peakArea = peak.getArea();
        final double peakHeight = peak.getHeight();
        final int peakDatapoints = peak.getScanNumbers().size();
        final Scan msmsScanNumber = peak.getMostIntenseFragmentScan();

        Float peakFWHM = peak.getFWHM();
        Float peakTailingFactor = peak.getTailingFactor();
        Float peakAsymmetryFactor = peak.getAsymmetryFactor();
        if (peakFWHM == null) {
          peakFWHM = -1.0f;
        }
        if (peakTailingFactor == null) {
          peakTailingFactor = -1.0f;
        }
        if (peakAsymmetryFactor == null) {
          peakAsymmetryFactor = -1.0f;
        }

        // Check Duration
        if (filterByDuration) {
          final Range<Double> durationRange =
              parameters.getParameter(FeatureFilterParameters.PEAK_DURATION).getEmbeddedParameter()
                  .getValue();
          if (!durationRange.contains(peakDuration)) {
            // Mark peak to be removed
            keepPeak[i] = false;
          }
        }

        // Check Area
        if (filterByArea) {
          final Range<Double> areaRange = parameters.getParameter(FeatureFilterParameters.PEAK_AREA)
              .getEmbeddedParameter().getValue();
          if (!areaRange.contains(peakArea)) {
            // Mark peak to be removed
            keepPeak[i] = false;
          }
        }

        // Check Height
        if (filterByHeight) {
          final Range<Double> heightRange = parameters
              .getParameter(FeatureFilterParameters.PEAK_HEIGHT).getEmbeddedParameter().getValue();
          if (!heightRange.contains(peakHeight)) {
            // Mark peak to be removed
            keepPeak[i] = false;
          }
        }

        // Check # Data Points
        if (filterByDatapoints) {
          final Range<Integer> datapointsRange =
              parameters.getParameter(FeatureFilterParameters.PEAK_DATAPOINTS)
                  .getEmbeddedParameter().getValue();
          if (!datapointsRange.contains(peakDatapoints)) {
            // Mark peak to be removed
            keepPeak[i] = false;
          }
        }

        // Check FWHM
        if (filterByFWHM) {
          final Range<Float> fwhmRange = RangeUtils.toFloatRange(parameters.getParameter(FeatureFilterParameters.PEAK_FWHM)
              .getEmbeddedParameter().getValue());
          if (!fwhmRange.contains(peakFWHM)) {
            // Mark peak to be removed
            keepPeak[i] = false;
          }
        }

        // Check Tailing Factor
        if (filterByTailingFactor) {
          final Range<Float> tailingRange =
              RangeUtils.toFloatRange(parameters.getParameter(FeatureFilterParameters.PEAK_TAILINGFACTOR)
                  .getEmbeddedParameter().getValue());
          if (!tailingRange.contains(peakTailingFactor)) {
            // Mark peak to be removed
            keepPeak[i] = false;
          }
        }

        // Check height
        if (filterByAsymmetryFactor) {
          final Range<Float> asymmetryRange =
              RangeUtils.toFloatRange(parameters.getParameter(FeatureFilterParameters.PEAK_ASYMMETRYFACTOR)
                  .getEmbeddedParameter().getValue());
          if (!asymmetryRange.contains(peakAsymmetryFactor)) {
            // Mark peak to be removed
            keepPeak[i] = false;
          }
        }

        // Check MS/MS filter
        if (filterByMS2) {
          if (msmsScanNumber != null)
            keepPeak[i] = false;
        }
      }
      // empty row?
      boolean isEmpty = Booleans.asList(keepPeak).stream().allMatch(keep -> !keep);
      if (!isEmpty)
        newPeakList.addRow(copyPeakRow(newPeakList, row, keepPeak));

    }

    newPeakList.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
        FeatureFilterModule.class, parameters));
    return newPeakList;
  }

  /**
   * Create a copy of a feature list row.
   */
  private FeatureListRow copyPeakRow(final ModularFeatureList filteredPeakList,
      final ModularFeatureListRow row, final boolean[] keepPeak) {
    // Copy the feature list row.
    final FeatureListRow newRow = new ModularFeatureListRow(filteredPeakList, row, false);

    // Copy the peaks.
    int i = 0;
    for (final Feature feature : row.getFeatures()) {
      // Only keep peak if it fulfills the filter criteria
      if (keepPeak[i]) {
        newRow.addFeature(feature.getRawDataFile(), new ModularFeature(filteredPeakList, feature));
      }
      i++;
    }
    return newRow;
  }
}
