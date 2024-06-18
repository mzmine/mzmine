/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectralDeconvolutionGCTask extends AbstractFeatureListTask {

  private static final Logger LOGGER = Logger.getLogger(
      SpectralDeconvolutionGCTask.class.getName());
  private final FeatureList featureList;
  private final MZmineProject project;
  private final OriginalFeatureListOption handleOriginal;
  private final String suffix;
  private final SpectralDeconvolutionAlgorithm spectralDeconvolutionAlgorithm;
  private final List<Range<Double>> mzValuesToIgnore;
  private FeatureList deconvolutedFeatureList;

  protected SpectralDeconvolutionGCTask(MZmineProject project, FeatureList featureList,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.project = project;
    this.featureList = featureList;
    var spectralDeconvolutionAlgorithmMZmineProcessingStep = parameters.getValue(
        SpectralDeconvolutionGCParameters.SPECTRAL_DECONVOLUTION_ALGORITHM);
    spectralDeconvolutionAlgorithm = SpectralDeconvolutionUtils.createSpectralDeconvolutionAlgorithm(
        spectralDeconvolutionAlgorithmMZmineProcessingStep);
    if (parameters.getParameter(SpectralDeconvolutionGCParameters.MZ_VALUES_TO_IGNORE).getValue()) {
      mzValuesToIgnore = parameters.getParameter(
          SpectralDeconvolutionGCParameters.MZ_VALUES_TO_IGNORE).getEmbeddedParameter().getValue();
    } else {
      mzValuesToIgnore = null;
    }
    handleOriginal = parameters.getValue(SpectralDeconvolutionGCParameters.HANDLE_ORIGINAL);
    suffix = parameters.getValue(SpectralDeconvolutionGCParameters.SUFFIX);

  }

  @Override
  protected void process() {
    LOGGER.info("Starting spectral deconvolution on " + featureList.getName());
    try {
      List<ModularFeature> features = featureList.getFeatures(featureList.getRawDataFile(0));
      List<List<ModularFeature>> groupedFeatures = spectralDeconvolutionAlgorithm.groupFeatures(
          features);
      List<FeatureListRow> deconvolutedFeatureListRows = SpectralDeconvolutionUtils.generatePseudoSpectra(
          groupedFeatures, featureList, mzValuesToIgnore);
      createNewDeconvolutedFeatureList(deconvolutedFeatureListRows);
      if (!isCanceled()) {
        handleOriginal.reflectNewFeatureListToProject(suffix, project, deconvolutedFeatureList,
            featureList);
        setStatus(TaskStatus.FINISHED);
        LOGGER.info("Spectral deconvolution completed on " + featureList.getName());
      }
    } catch (Exception e) {
      LOGGER.severe("Error during spectral deconvolution: " + e.getMessage());
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.getMessage());
    }
  }

  private void createNewDeconvolutedFeatureList(List<FeatureListRow> deconvolutedFeatureListRows) {
    deconvolutedFeatureList = FeatureListUtils.createCopy(featureList, suffix,
        getMemoryMapStorage());
    deconvolutedFeatureListRows.sort(FeatureListRowSorter.DEFAULT_RT);
    int newID = 1;
    for (FeatureListRow featureListRow : deconvolutedFeatureListRows) {
      deconvolutedFeatureList.addRow(
          new ModularFeatureListRow((ModularFeatureList) deconvolutedFeatureList, newID,
              (ModularFeatureListRow) featureListRow, true));
      newID++;
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(deconvolutedFeatureList);
  }

  @Override
  public String getTaskDescription() {
    return "GC-EI spectral deconvolution";
  }
}
