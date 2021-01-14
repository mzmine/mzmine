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

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs chromatographic smoothing of a peak-list.
 */
public class SmoothingTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(SmoothingTask.class.getName());

  // Feature lists: original and processed.
  private final MZmineProject project;
  private final ModularFeatureList origPeakList;
  // Parameters.
  private final ParameterSet parameters;
  private final String suffix;
  private final boolean removeOriginal;
  private final int filterWidth;
  private final int progressMax;
  private ModularFeatureList newPeakList;
  private int progress;

  /**
   * Create the task.
   *
   * @param peakList            the peak-list.
   * @param smoothingParameters smoothing parameters.
   */
  public SmoothingTask(final MZmineProject project, final FeatureList peakList,
      final ParameterSet smoothingParameters) {

    // Initialize.
    this.project = project;
    origPeakList = (ModularFeatureList) peakList;
    progress = 0;
    progressMax = peakList.getNumberOfRows();

    // Parameters.
    parameters = smoothingParameters;
    suffix = parameters.getParameter(SmoothingParameters.SUFFIX).getValue();
    removeOriginal = parameters.getParameter(SmoothingParameters.REMOVE_ORIGINAL).getValue();
    filterWidth = parameters.getParameter(SmoothingParameters.FILTER_WIDTH).getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Smoothing " + origPeakList;
  }

  @Override
  public double getFinishedPercentage() {
    return progressMax == 0 ? 0.0 : (double) progress / (double) progressMax;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    try {
      // Get filter weights.
      final double[] filterWeights = SavitzkyGolayFilter.getNormalizedWeights(filterWidth);

      // Create new feature list
      newPeakList = origPeakList.createEmptyCopyWithSameTypes(origPeakList + " " + suffix);

      // Process each row.
      for (final FeatureListRow row : origPeakList.getRows()) {

        if (!isCanceled()) {

          // Create a new peak-list row.
          final int originalID = row.getID();
          final ModularFeatureListRow newRow = new ModularFeatureListRow(newPeakList, originalID);

          // Process each peak.
          for (final Feature peak : row.getFeatures()) {
            if (isCanceled()) {
              return;
            }

            ModularFeature feature = (ModularFeature) peak;
            // TODO feature data
            IonTimeSeries<? extends Scan> featureData = feature.getFeatureData();

            IonTimeSeries<? extends Scan> smoothedSeries = null;
            if (featureData instanceof SimpleIonTimeSeries) {

              IonSpectrumSeriesSmoothing<SimpleIonTimeSeries> smoother = new IonSpectrumSeriesSmoothing<>(
                  ((SimpleIonTimeSeries) featureData), newPeakList.getMemoryMapStorage(),
                  (List<? extends MassSpectrum>) origPeakList
                      .getSeletedScans(feature.getRawDataFile()));
              smoothedSeries = smoother.smooth(filterWeights);

            } else if (featureData instanceof IonMobilogramTimeSeries) {
              IonSpectrumSeriesSmoothing<IonMobilogramTimeSeries> smoother = new IonSpectrumSeriesSmoothing<>(
                  ((IonMobilogramTimeSeries) featureData), newPeakList.getMemoryMapStorage(),
                  (List<? extends MassSpectrum>) origPeakList
                      .getSeletedScans(feature.getRawDataFile()));
              smoothedSeries = smoother.smooth(filterWeights);
            } else {
              logger.info("Could not smooth feature, unknown ion series type.");
              continue;
            }

            ModularFeature newFeature = new ModularFeature(newPeakList, feature);
            newFeature.set(FeatureDataType.class, smoothedSeries);
            FeatureDataUtils.recalculateIonSeriesDependingTypes(newFeature);

            newRow.addFeature(newFeature.getRawDataFile(), newFeature);
          }
          newPeakList.addRow(newRow);
          progress++;
        }
      }

      // Finish up.
      if (!isCanceled()) {

        // Add new peak-list to the project.
        project.addFeatureList(newPeakList);

        // Add quality parameters to peaks
        //QualityParameters.calculateQualityParameters(newPeakList);

        // Remove the original peak-list if requested.
        if (removeOriginal) {
          project.removeFeatureList(origPeakList);
        }

        // Copy previously applied methods
        for (final FeatureListAppliedMethod method : origPeakList.getAppliedMethods()) {

          newPeakList.addDescriptionOfAppliedTask(method);
        }

        // Add task description to peak-list.
        newPeakList.addDescriptionOfAppliedTask(
            new SimpleFeatureListAppliedMethod("Peaks smoothed by Savitzky-Golay filter",
                parameters));

        logger.finest("Finished peak smoothing: " + progress + " rows processed");

        setStatus(TaskStatus.FINISHED);
      }
    } catch (Throwable t) {
      t.printStackTrace();
      logger.log(Level.SEVERE, "Smoothing error", t);
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
    }
  }
}
