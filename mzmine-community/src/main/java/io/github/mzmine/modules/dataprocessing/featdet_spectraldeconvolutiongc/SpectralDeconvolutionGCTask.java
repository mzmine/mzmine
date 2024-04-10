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

import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectralDeconvolutionGCTask extends AbstractFeatureListTask {

  private static final Logger LOGGER = Logger.getLogger(
      SpectralDeconvolutionGCTask.class.getName());
  private final FeatureList featureList;
  private final MZmineProject project;
  private final ParameterSet parameters;
  private final OriginalFeatureListOption handleOriginal;
  private final String suffix;
  private final RTTolerance rtTolerance;
  private final int minNumberOfSignals;
  private FeatureList deconvolutedFeatureList;

  protected SpectralDeconvolutionGCTask(MZmineProject project, FeatureList featureList,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.parameters = parameters;
    this.project = project;
    this.featureList = featureList;
    rtTolerance = parameters.getValue(SpectralDeconvolutionGCParameters.RT_TOLERANCE);
    minNumberOfSignals = parameters.getValue(
        SpectralDeconvolutionGCParameters.MIN_NUMBER_OF_SIGNALS);
    handleOriginal = parameters.getValue(SpectralDeconvolutionGCParameters.HANDLE_ORIGINAL);
    suffix = parameters.getValue(SpectralDeconvolutionGCParameters.SUFFIX);

  }

  @Override
  protected void process() {
    LOGGER.info("Starting spectral deconvolution on " + featureList.getName());
    try {
      List<ModularFeature> features = featureList.getFeatures(featureList.getRawDataFile(0));
      List<FeatureListRow> deconvolutedFeatureListRows = generatePseudoSpectraByRtOnly(features);
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

  private List<List<ModularFeature>> groupFeaturesByRT(List<ModularFeature> features) {
    features.sort(Comparator.comparingDouble(ModularFeature::getHeight).reversed());

    RangeMap<Float, List<ModularFeature>> rangeRtMap = TreeRangeMap.create();

    for (ModularFeature feature : features) {
      Float rt = feature.getRT();
      List<ModularFeature> group = rangeRtMap.get(rt);
      if (group == null) {
        group = new ArrayList<>();
        rangeRtMap.put(rtTolerance.getToleranceRange(rt), group);
      }
      group.add(feature);
    }

    return new ArrayList<>(rangeRtMap.asMapOfRanges().values());
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

  private List<FeatureListRow> generatePseudoSpectraByRtOnly(List<ModularFeature> features) {
    List<FeatureListRow> deconvolutedFeatureListRowsByRtOnly = new ArrayList<>();
    List<List<ModularFeature>> rtGroupedFeatures = groupFeaturesByRT(features);
    for (List<ModularFeature> group : rtGroupedFeatures) {
      if (group.size() < minNumberOfSignals) {
        continue;
      }

      // is already sorted by intensity best first
      ModularFeature mainFeature = group.getFirst();

      group.sort(Comparator.comparingDouble(ModularFeature::getMZ));
      double[] mzs = new double[group.size()];
      double[] intensities = new double[group.size()];
      for (int i = 0; i < group.size(); i++) {
        mzs[i] = group.get(i).getMZ();
        intensities[i] = group.get(i).getHeight();
      }

      // Create PseudoSpectrum, take first feature to ensure most intense is representative feature
      PseudoSpectrum pseudoSpectrum = new SimplePseudoSpectrum(featureList.getRawDataFile(0), 1,
          // MS Level
          group.getFirst().getRT(), null, // No MsMsInfo for pseudo spectrum
          mzs, intensities, group.getFirst().getRepresentativeScan().getPolarity(),
          "Correlated Features Pseudo Spectrum", PseudoSpectrumType.GC_EI);

      mainFeature.setAllMS2FragmentScans(List.of(pseudoSpectrum));
      deconvolutedFeatureListRowsByRtOnly.add(mainFeature.getRow());
    }
    return deconvolutedFeatureListRowsByRtOnly;
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
