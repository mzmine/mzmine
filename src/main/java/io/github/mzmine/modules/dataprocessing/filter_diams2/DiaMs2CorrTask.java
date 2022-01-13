/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.filter_diams2;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderTask;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverTask;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingAlgorithm;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Comparator;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiaMs2CorrTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiaMs2CorrTask.class.getName());

  private final ModularFeatureList flist;
  private final ScanSelection scanSelection;
  private final double minIntensity;
  private final MZTolerance mzTolerance;

  private final ParameterSet adapParameters;
  private final ParameterSet smoothingParameters;
  private final ParameterSet resolverParameters;

  private AbstractTask currentTask = null;
  private int currentTaksIndex = 1;
  private int numSubTasks = 4;

  private String description;

  protected DiaMs2CorrTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    description = "DIA MS2 scan builder";
  }

  @Override
  public String getTaskDescription() {
    return "DIA MS2 for feature list: " + flist.getName() + " " + (currentTask != null
        ? currentTask.getTaskDescription() : "");
  }

  @Override
  public double getFinishedPercentage() {
    return currentTask != null ? currentTask.getFinishedPercentage() * ((double) 1 / numSubTasks)
        : 0d + ((double) currentTaksIndex / numSubTasks);
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (flist.getNumberOfRawDataFiles() != 1) {
      setErrorMessage("Cannot build DIA MS2 for feature lists with more than one raw data file.");
      setStatus(TaskStatus.ERROR);
    }

    final RawDataFile file = flist.getRawDataFile(0);

    // build chromatograms
    final MZmineProject dummyProject = new MZmineProjectImpl();
    var ms2Flist = runADAP(dummyProject, file);
    ms2Flist = runSmoothing(dummyProject, ms2Flist);
    ms2Flist = runResolving(dummyProject, ms2Flist);

    ms2Flist.streamFeatures().sorted(Comparator.comparingDouble(ModularFeature::getMZ));
    // compare chromatogram shapes with features
  }

  private ModularFeatureList runADAP(MZmineProject dummyProject, RawDataFile file) {
    currentTask = new ModularADAPChromatogramBuilderTask(dummyProject, file, adapParameters,
        flist.getMemoryMapStorage(), getModuleCallDate());
    currentTask.run();
    currentTask = null;
    currentTaksIndex++;

    // todo remove method from raw file
    var ms2Flist = dummyProject.getCurrentFeatureLists().get(0);
    if (dummyProject.getCurrentFeatureLists().isEmpty()) {
      logger.warning("Cannot find ms2 feature list.");
      return null;
    }

    return (ModularFeatureList) ms2Flist;
  }

  private ModularFeatureList runSmoothing(MZmineProject dummyProject, ModularFeatureList ms2Flist) {
    if (smoothingParameters != null) {
      final MZmineProcessingStep<SmoothingAlgorithm> smoother = smoothingParameters.getParameter(
          SmoothingParameters.smoothingAlgorithm).getValue();
      ParameterSet fullSmoothingParams = MZmineCore.getConfiguration()
          .getModuleParameters(SmoothingModule.class).cloneParameterSet();
      fullSmoothingParams.setParameter(SmoothingParameters.smoothingAlgorithm, smoother);
      fullSmoothingParams.setParameter(SmoothingParameters.suffix, "_sm");
      fullSmoothingParams.setParameter(SmoothingParameters.handleOriginal,
          OriginalFeatureListOption.REMOVE);

      currentTask = new SmoothingTask(dummyProject, (ModularFeatureList) ms2Flist,
          flist.getMemoryMapStorage(), fullSmoothingParams, getModuleCallDate());
      currentTask.run();
    }
    currentTask = null;
    currentTaksIndex++;
    return (ModularFeatureList) dummyProject.getCurrentFeatureLists().get(0);
  }

  private ModularFeatureList runResolving(MZmineProject dummyProject, ModularFeatureList ms2Flist) {
    ParameterSet fullResolverParameters = MZmineCore.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class);
    fullResolverParameters.setParameter(
        MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL,
        resolverParameters.getValue(
            MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO,
        resolverParameters.getValue(MinimumSearchFeatureResolverParameters.MIN_RATIO));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE,
        resolverParameters.getValue(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT,
        resolverParameters.getValue(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT));
    fullResolverParameters.setParameter(
        MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS,
        resolverParameters.getValue(
            MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT,
        resolverParameters.getValue(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION,
        resolverParameters.getValue(MinimumSearchFeatureResolverParameters.PEAK_DURATION));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, "_res");
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.handleOriginal,
        OriginalFeatureListOption.REMOVE);
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.RETENTION_TIME);
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters,
        false);

    currentTask = new FeatureResolverTask(dummyProject, getMemoryMapStorage(), ms2Flist, fullResolverParameters,
        FeatureDataUtils.DEFAULT_CENTER_FUNCTION, getModuleCallDate());
    currentTask.run();
    currentTask = null;
    currentTaksIndex++;

    return (ModularFeatureList) dummyProject.getCurrentFeatureLists().get(0);
  }
}
