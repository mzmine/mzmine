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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionParameters.AUTO_REMOVE;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionParameters.PEAK_RESOLVER;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionParameters.RetentionTimeMSMS;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionParameters.SUFFIX;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionParameters.mzRangeMSMS;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureShapeIonMobilityRetentionTimeHeatMapType;
import io.github.mzmine.datamodel.features.types.FeatureShapeIonMobilityRetentionTimeType;
import io.github.mzmine.datamodel.features.types.FeatureShapeMobilogramType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureConvertorIonMobility;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.maths.CenterFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeconvolutionTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(DeconvolutionTask.class.getName());

  // Feature lists.
  private final MZmineProject project;
  private final FeatureList originalPeakList;
  private FeatureList newPeakList;

  // Counters.
  private int processedRows;
  private int totalRows;

  // User parameters
  private final ParameterSet parameters;

  private RSessionWrapper rSession;
  private String errorMsg;
  private boolean setMSMSRange, setMSMSRT;
  private double msmsRange;
  private float RTRangeMSMS;

  // function to find center mz of all feature data points
  private final CenterFunction mzCenterFunction;

  /**
   * Create the task.
   *
   * @param list         feature list to operate on.
   * @param parameterSet task parameters.
   */
  public DeconvolutionTask(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet, CenterFunction mzCenterFunction) {

    // Initialize.
    this.project = project;
    parameters = parameterSet;
    originalPeakList = list;
    newPeakList = null;
    processedRows = 0;
    totalRows = 0;
    this.mzCenterFunction = mzCenterFunction;
  }

  @Override
  public String getTaskDescription() {

    return "Feature recognition on " + originalPeakList;
  }

  @Override
  public double getFinishedPercentage() {

    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public void run() {

    errorMsg = null;

    if (!isCanceled()) {

      setStatus(TaskStatus.PROCESSING);
      logger.info("Started feature deconvolution on " + originalPeakList);

      // Check raw data files.
      if (originalPeakList.getNumberOfRawDataFiles() > 1) {

        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "Peak deconvolution can only be performed on feature lists with a single raw data file");

      } else {

        try {

          // Peak resolver.
          final MZmineProcessingStep<PeakResolver> resolver =
              parameters.getParameter(PEAK_RESOLVER).getValue();

          if (resolver.getModule().getRequiresR()) {
            // Check R availability, by trying to open the
            // connection.
            String[] reqPackages = resolver.getModule().getRequiredRPackages();
            String[] reqPackagesVersions = resolver.getModule().getRequiredRPackagesVersions();
            String callerFeatureName = resolver.getModule().getName();

            REngineType rEngineType =
                resolver.getModule().getREngineType(resolver.getParameterSet());
            this.rSession = new RSessionWrapper(rEngineType, callerFeatureName, reqPackages,
                reqPackagesVersions);
            this.rSession.open();
          } else {
            this.rSession = null;
          }

          // Deconvolve features.
          newPeakList = resolvePeaks(originalPeakList, this.rSession);

          if (!isCanceled()) {

            // Add new featurelist to the project.
            project.addFeatureList(newPeakList);

            // Add quality parameters to features
            //QualityParameters.calculateQualityParameters(newPeakList);

            // Remove the original feature list if requested.
            if (parameters.getParameter(AUTO_REMOVE).getValue()) {
              project.removeFeatureList(originalPeakList);
            }

            setStatus(TaskStatus.FINISHED);
            logger.info("Finished feature deconvolution on " + originalPeakList);
          }
          // Turn off R instance.
          if (this.rSession != null) {
            this.rSession.close(false);
          }

        } catch (RSessionWrapperException e) {
          errorMsg = "'R computing error' during CentWave detection. \n" + e.getMessage();
        } catch (Exception e) {
          e.printStackTrace();
          errorMsg = "'Unknown error' during CentWave detection. \n" + e.getMessage();
        } catch (Throwable t) {

          setStatus(TaskStatus.ERROR);
          setErrorMessage(t.getMessage());
          logger.log(Level.SEVERE, "Feature deconvolution error", t);
        }

        // Turn off R instance, once task ended UNgracefully.
        try {
          if (this.rSession != null && !isCanceled()) {
            rSession.close(isCanceled());
          }
        } catch (RSessionWrapperException e) {
          if (!isCanceled()) {
            // Do not override potential previous error message.
            if (errorMsg == null) {
              errorMsg = e.getMessage();
            }
          } else {
            // User canceled: Silent.
          }
        }

        // Report error.
        if (errorMsg != null) {
          setErrorMessage(errorMsg);
          setStatus(TaskStatus.ERROR);
        }
      }
    }
  }

  /**
   * Deconvolve a chromatogram into separate peaks.
   *
   * @param peakList holds the chromatogram to deconvolve.
   * @param rSession
   * @return a new feature list holding the resolved peaks.
   * @throws RSessionWrapperException
   */
  private FeatureList resolvePeaks(final FeatureList peakList, RSessionWrapper rSession)
      throws RSessionWrapperException {

    // Get data file information.
    final RawDataFile dataFile = peakList.getRawDataFile(0);

    // Feature resolver.
    final MZmineProcessingStep<PeakResolver> resolver =
        parameters.getParameter(PEAK_RESOLVER).getValue();
    // set msms pairing range
    this.setMSMSRange = parameters.getParameter(mzRangeMSMS).getValue();
    if (setMSMSRange) {
      this.msmsRange = parameters.getParameter(mzRangeMSMS).getEmbeddedParameter().getValue();
    } else {
      this.msmsRange = 0;
    }

    this.setMSMSRT = parameters.getParameter(RetentionTimeMSMS).getValue();
    if (setMSMSRT) {
      this.RTRangeMSMS =
          parameters.getParameter(RetentionTimeMSMS).getEmbeddedParameter().getValue().floatValue();
    } else {
      this.RTRangeMSMS = 0;
    }

    // Create new feature list.
    final ModularFeatureList resolvedPeaks =
        new ModularFeatureList(peakList + " " + parameters.getParameter(SUFFIX).getValue(),
            dataFile);
    DataTypeUtils.addDefaultChromatographicTypeColumns(resolvedPeaks);
    if(peakList.getRawDataFile(0) instanceof IMSRawDataFile) {
      resolvedPeaks.addRowType(new FeatureShapeIonMobilityRetentionTimeType());
      resolvedPeaks.addRowType(new FeatureShapeIonMobilityRetentionTimeHeatMapType());
      resolvedPeaks.addRowType(new FeatureShapeMobilogramType());
      resolvedPeaks.addRowType(new MobilityType());
    }

    // Load previous applied methods.
    for (final FeatureListAppliedMethod method : peakList.getAppliedMethods()) {
      resolvedPeaks.addDescriptionOfAppliedTask(method);
    }

    // Add task description to feature list.
    resolvedPeaks.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        "Feature deconvolution by " + resolver, resolver.getParameterSet()));

    // Initialise counters.
    processedRows = 0;
    totalRows = peakList.getNumberOfRows();
    int peakId = 1;

    // Process each chromatogram.
    final FeatureListRow[] peakListRows = peakList.getRows().toArray(FeatureListRow[]::new);
    final int chromatogramCount = peakListRows.length;
    for (int index = 0; !isCanceled() && index < chromatogramCount; index++) {

      final FeatureListRow currentRow = peakListRows[index];
      final Feature chromatogram =
          (dataFile instanceof IMSRawDataFile) ? FeatureConvertorIonMobility
              .collapseMobilityDimensionOfModularFeature(
                  (ModularFeature) currentRow.getFeature(dataFile))
              : currentRow.getFeature(dataFile);

      // Resolve peaks.
      final PeakResolver resolverModule = resolver.getModule();
      final ParameterSet resolverParams = resolver.getParameterSet();
      final ResolvedPeak[] peaks = resolverModule.resolvePeaks(chromatogram, resolverParams,
          rSession, mzCenterFunction, msmsRange, RTRangeMSMS);

      // Add peaks to the new feature list.
      for (final ResolvedPeak peak : peaks) {
        peak.setParentChromatogramRowID(currentRow.getID());
        final ModularFeatureListRow newRow = new ModularFeatureListRow(resolvedPeaks,
            peakId++);
        final ModularFeature newFeature = FeatureConvertors
            .ResolvedPeakToMoularFeature(resolvedPeaks, peak);
        if (newFeature.getRawDataFile() instanceof IMSRawDataFile) {
          newRow.addFeature(dataFile, FeatureConvertorIonMobility
              .mapResolvedCollapsedFeaturesToImsFeature(newFeature,
                  (ModularFeature) currentRow.getFeature(dataFile), mzCenterFunction));
          newRow.set(FeatureShapeIonMobilityRetentionTimeType.class, newRow.getFeaturesProperty());
          newRow.set(FeatureShapeMobilogramType.class, newRow.getFeaturesProperty());
          newRow.set(FeatureShapeIonMobilityRetentionTimeHeatMapType.class,
              newRow.getFeaturesProperty());
        } else {
          newRow.addFeature(dataFile, newFeature);
        }

        newRow.setFeatureInformation(peak.getPeakInformation());
        resolvedPeaks.addRow(newRow);
      }

      processedRows++;
    }

    return resolvedPeaks;
  }

  @Override
  public void cancel() {

    super.cancel();
    // Turn off R instance, if already existing.
    try {
      if (this.rSession != null) {
        this.rSession.close(true);
      }
    } catch (RSessionWrapperException e) {
      // Silent, always...
    }
  }


}
