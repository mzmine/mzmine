/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.datamodel.features.types.MobilityUnitType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Task;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.maths.CenterFunction;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class FeatureResolverTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(FeatureResolverTask.class.getName());

  // Feature lists.
  private final MZmineProject project;
  private final FeatureList originalPeakList;
  // User parameters
  private final ParameterSet parameters;
  // function to find center mz of all feature data points
  private final CenterFunction mzCenterFunction;
  private FeatureList newPeakList;
  // Counters.
  private int processedRows;
  private int totalRows;
  private RSessionWrapper rSession;
  private String errorMsg;
  private boolean setMSMSRange, setMSMSRT;
  private double msmsRange;
  private float RTRangeMSMS;

  /**
   * Create the task.
   *
   * @param storage
   * @param list         feature list to operate on.
   * @param parameterSet task parameters.
   */
  public FeatureResolverTask(final MZmineProject project, MemoryMapStorage storage,
      final FeatureList list, final ParameterSet parameterSet, CenterFunction mzCenterFunction,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

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
      logger.info("Started feature resolving on " + originalPeakList);

      // Check raw data files.
      if (originalPeakList.getNumberOfRawDataFiles() > 1) {

        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "Feature resolving can only be performed on feature lists with a single raw data file");
      } else {
        try {
          if (((GeneralResolverParameters) parameters).getResolver(parameters,
              (ModularFeatureList) originalPeakList) != null) {
            dimensionIndependentResolve((ModularFeatureList) originalPeakList);
          } else {
            legacyResolve();
          }
          // resolving finished

          // sort and reset IDs here to ahve the same sorting for every feature list
          FeatureListUtils.sortByDefaultRT(newPeakList, true);

          // group MS2 with features
          if (parameters.getParameter(GeneralResolverParameters.groupMS2Parameters).getValue()) {
            GroupMS2SubParameters ms2params = parameters.getParameter(
                GeneralResolverParameters.groupMS2Parameters).getEmbeddedParameters();
            GroupMS2Task task = new GroupMS2Task(project, newPeakList, ms2params, moduleCallDate);
            // restart progress
            processedRows = 0;
            totalRows = newPeakList.getNumberOfRows();
            // group all features with MS/MS
            for (FeatureListRow row : newPeakList.getRows()) {
              task.processRow(row);
              processedRows++;
            }
          }

          if (!isCanceled()) {
            // add new list and remove old if requested
            final var handleOriginal = parameters.getValue(
                GeneralResolverParameters.handleOriginal);
            final String suffix = parameters.getValue(GeneralResolverParameters.SUFFIX);
            handleOriginal.reflectNewFeatureListToProject(suffix, project, newPeakList,
                originalPeakList);

            setStatus(TaskStatus.FINISHED);
            logger.info("Finished feature resolving on " + originalPeakList);
          }
          // Turn off R instance.
          if (this.rSession != null) {
            this.rSession.close(false);
          }

        } catch (RSessionWrapperException e) {
          e.printStackTrace();
          errorMsg = "'R computing error' during CentWave detection. \n" + e.getMessage();
        } catch (Exception e) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage(e.getMessage());
          logger.log(Level.SEVERE, "Feature resolving error: " + e.getMessage(), e);
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
   * @param originalFeatureList holds the chromatogram to deconvolve.
   * @param rSession
   * @return a new feature list holding the resolved peaks.
   * @throws RSessionWrapperException
   */
  /*private FeatureList resolvePeaks(final FeatureList originalFeatureList, RSessionWrapper rSession)
      throws RSessionWrapperException {

    // Get data file information.
    final RawDataFile dataFile = originalFeatureList.getRawDataFile(0);

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
    final ModularFeatureList resolvedFeatureList =
        new ModularFeatureList(
            originalFeatureList + " " + parameters.getParameter(SUFFIX).getValue(),
            dataFile);
    DataTypeUtils.addDefaultChromatographicTypeColumns(resolvedFeatureList);
    if (originalFeatureList.getRawDataFile(0) instanceof IMSRawDataFile) {
      DataTypeUtils.addDefaultIonMobilityTypeColumns(resolvedFeatureList);
    }

    // Load previous applied methods.
    for (final FeatureListAppliedMethod method : originalFeatureList.getAppliedMethods()) {
      resolvedFeatureList.addDescriptionOfAppliedTask(method);
    }

    // Add task description to feature list.
    resolvedFeatureList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        "Feature deconvolution by " + resolver, resolver.getParameterSet()));

    // Initialise counters.
    processedRows = 0;
    totalRows = originalFeatureList.getNumberOfRows();
    int peakId = 1;

    // Process each chromatogram.
    final FeatureListRow[] peakListRows = originalFeatureList.getRows()
        .toArray(FeatureListRow[]::new);
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
        final ModularFeatureListRow newRow = new ModularFeatureListRow(resolvedFeatureList,
            peakId++);
        final ModularFeature newFeature = FeatureConvertors
            .ResolvedPeakToMoularFeature(resolvedFeatureList, peak);
        if (newFeature.getRawDataFile() instanceof IMSRawDataFile) {
          newRow.addFeature(dataFile, FeatureConvertorIonMobility
              .mapResolvedCollapsedFeaturesToImsFeature(newFeature,
                  (ModularFeature) currentRow.getFeature(dataFile), mzCenterFunction, msmsRange,
                  RTRangeMSMS));
//          newRow.set(FeatureShapeIonMobilityRetentionTimeType.class, newRow.getFeaturesProperty());
//          newRow.set(FeatureShapeMobilogramType.class, true);
//          newFeature.set(FeatureShapeIonMobilityRetentionTimeHeatMapType.class,
//              true);
        } else {
          newRow.addFeature(dataFile, newFeature);
        }

        newRow.setFeatureInformation(peak.getPeakInformation());
        resolvedFeatureList.addRow(newRow);
      }

      processedRows++;
    }

    return resolvedFeatureList;
  }*/

  /**
   * Used for compatibility with old {@link FeatureResolver}s. New methods should implement {@link
   * Resolver}. See {@link io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver}
   * as an example implementation.
   *
   * @throws RSessionWrapperException
   */
  @Deprecated
  private void legacyResolve() throws RSessionWrapperException {
    final FeatureResolver resolver = ((GeneralResolverParameters) parameters).getResolver();

    if (resolver.getRequiresR()) {
      // Check R availability, by trying to open the
      // connection.
      String[] reqPackages = resolver.getRequiredRPackages();
      String[] reqPackagesVersions = resolver.getRequiredRPackagesVersions();
      String callerFeatureName = resolver.getName();

      REngineType rEngineType = resolver.getREngineType(parameters);
      this.rSession = new RSessionWrapper(rEngineType, callerFeatureName, reqPackages,
          reqPackagesVersions);
      this.rSession.open();
    } else {
      this.rSession = null;
    }

    // Resolve features.
    newPeakList = resolvePeaks((ModularFeatureList) originalPeakList, this.rSession);
  }

  private void dimensionIndependentResolve(ModularFeatureList originalFeatureList) {
    final Resolver resolver = ((GeneralResolverParameters) parameters).getResolver(parameters,
        originalFeatureList);
    if (resolver == null) {
      setErrorMessage("Resolver could not be initialised.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    final RawDataFile dataFile = originalFeatureList.getRawDataFile(0);
    final ModularFeatureList resolvedFeatureList = createNewFeatureList(originalFeatureList);

    final FeatureDataAccess access = EfficientDataAccess.of(originalFeatureList,
        EfficientDataAccess.FeatureDataType.INCLUDE_ZEROS, dataFile);

    processedRows = 0;
    totalRows = originalFeatureList.getNumberOfRows();
    int peakId = 1;

    int c = 0;

    while (access.hasNextFeature()) {
      final ModularFeature originalFeature = (ModularFeature) access.nextFeature();
      final List<IonTimeSeries<? extends Scan>> resolvedSeries = resolver.resolve(access,
          getMemoryMapStorage());

      for (IonTimeSeries<? extends Scan> resolved : resolvedSeries) {
        final ModularFeatureListRow newRow = new ModularFeatureListRow(resolvedFeatureList,
            peakId++);
        final ModularFeature f = new ModularFeature(resolvedFeatureList,
            originalFeature.getRawDataFile(), resolved, originalFeature.getFeatureStatus());

        if (originalFeature.getMobilityUnit() != null) {
          f.set(MobilityUnitType.class, originalFeature.getMobilityUnit());
        }
        if (originalFeature.get(ImageType.class) != null) {
          f.set(ImageType.class, true);
        }
        newRow.addFeature(originalFeature.getRawDataFile(), f);
        resolvedFeatureList.addRow(newRow);
        if (resolved.getSpectra().size() <= 3) {
          c++;
        }
      }
      processedRows++;
    }
    logger.info(c + "/" + resolvedFeatureList.getNumberOfRows()
        + " have less than 4 scans (frames for IMS data)");
    //    QualityParameters.calculateAndSetModularQualityParameters(resolvedFeatureList);

    resolvedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(resolver.getModuleClass(), parameters,
            getModuleCallDate()));

    newPeakList = resolvedFeatureList;
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

  /**
   * This method is kept around to keep compatibility with resolvers implementing the legacy
   * interface {@link FeatureResolver}. All new resolvers should implement {@link Resolver} or
   * {@link AbstractResolver} instead.
   */
  @Deprecated
  private FeatureList resolvePeaks(final ModularFeatureList originalFeatureList,
      RSessionWrapper rSession) throws RSessionWrapperException {

    final RawDataFile dataFile = originalFeatureList.getRawDataFile(0);
    final ModularFeatureList resolvedFeatureList = createNewFeatureList(originalFeatureList);

    final FeatureResolver resolver = ((GeneralResolverParameters) parameters).getResolver();

    processedRows = 0;
    totalRows = originalFeatureList.getNumberOfRows();
    int peakId = 1;

    for (int i = 0; i < totalRows; i++) {
      final ModularFeatureListRow originalRow = (ModularFeatureListRow) originalFeatureList.getRow(
          i);
      final ModularFeature originalFeature = originalRow.getFeature(dataFile);

      final ResolvedPeak[] peaks = resolver.resolvePeaks(originalFeature, parameters, rSession,
          mzCenterFunction, msmsRange, RTRangeMSMS);

      final Integer minNumDp = parameters.getValue(GeneralResolverParameters.MIN_NUMBER_OF_DATAPOINTS);
      for (final ResolvedPeak peak : peaks) {
        if(peak.getScanNumbers().length < minNumDp) {
          continue;
        }
        peak.setParentChromatogramRowID(originalRow.getID());
        final ModularFeatureListRow newRow = new ModularFeatureListRow(resolvedFeatureList,
            peakId++);
        final ModularFeature newFeature = FeatureConvertors.ResolvedPeakToMoularFeature(
            resolvedFeatureList, peak, originalFeature.getFeatureData());
        if (originalFeature.getMobilityUnit() != null) {
          newFeature.set(MobilityUnitType.class, originalFeature.getMobilityUnit());
        }

        newRow.addFeature(dataFile, newFeature);
        newRow.setFeatureInformation(peak.getPeakInformation());
        resolvedFeatureList.addRow(newRow);
      }
      processedRows++;
    }

    resolvedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(resolver.getModuleClass(), parameters,
            getModuleCallDate()));

    return resolvedFeatureList;
  }

  private ModularFeatureList createNewFeatureList(ModularFeatureList originalFeatureList) {
    if (originalFeatureList.getRawDataFiles().size() > 1) {
      throw new IllegalArgumentException("Resolving cannot be applied to aligned feature lists.");
    }
    final RawDataFile dataFile = originalFeatureList.getRawDataFile(0);

    // create a new feature list and don't copy. Previous annotations of features are invalidated
    // during resolution
    final ModularFeatureList resolvedFeatureList = new ModularFeatureList(
        originalFeatureList.getName() + " " + parameters.getParameter(
            GeneralResolverParameters.SUFFIX).getValue(), storage, dataFile);

    //    DataTypeUtils.addDefaultChromatographicTypeColumns(resolvedFeatureList);
    resolvedFeatureList.setSelectedScans(dataFile, originalFeatureList.getSeletedScans(dataFile));

    // since we dont create a copy, we have to copy manually
    originalFeatureList.getAppliedMethods()
        .forEach(m -> resolvedFeatureList.getAppliedMethods().add(m));
    // the new method is added later, since we don't know here which resolver module is used.

    // check the actual feature data. IMSRawDataFiles can also be built as classic lc-ms features
    final Feature exampleFeature =
        originalFeatureList.getNumberOfRows() > 0 ? originalFeatureList.getRow(0).getBestFeature()
            : null;

    boolean isImagingFile = (originalFeatureList.getRawDataFile(0) instanceof ImagingRawDataFile);
    if (exampleFeature != null
        && exampleFeature.getFeatureData() instanceof IonMobilogramTimeSeries) {
      DataTypeUtils.addDefaultIonMobilityTypeColumns(resolvedFeatureList);
    }
    if (originalFeatureList.hasRowType(RTType.class) && !isImagingFile) {
      DataTypeUtils.addDefaultChromatographicTypeColumns(resolvedFeatureList);
    }
    if (isImagingFile) {
      DataTypeUtils.addDefaultImagingTypeColumns(resolvedFeatureList);
    }

    return resolvedFeatureList;
  }
}
