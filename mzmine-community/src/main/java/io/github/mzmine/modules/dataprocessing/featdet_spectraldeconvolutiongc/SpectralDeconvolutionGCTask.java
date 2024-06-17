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
    spectralDeconvolutionAlgorithm = SpectralDeconvolutionTools.createSpectralDeconvolutionAlgorithm(
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
      List<FeatureListRow> deconvolutedFeatureListRows = SpectralDeconvolutionTools.generatePseudoSpectra(
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

  private List<FeatureListRow> generatePseudoSpectra(List<ModularFeature> features,
      List<List<ModularFeature>> groupedFeatures) {
    List<FeatureListRow> deconvolutedFeatureListRowsByRtOnly = new ArrayList<>();
    for (List<ModularFeature> group : groupedFeatures) {
      // find main feature as representative feature in new feature list
      ModularFeature mainFeature = getMainFeature(group);

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
          mainFeature.getRT(), null, // No MsMsInfo for pseudo spectrum
          mzs, intensities, mainFeature.getRepresentativeScan().getPolarity(),
          "Correlated Features Pseudo Spectrum", PseudoSpectrumType.GC_EI);

      mainFeature.setAllMS2FragmentScans(List.of(pseudoSpectrum));
      deconvolutedFeatureListRowsByRtOnly.add(mainFeature.getRow());
    }
    return deconvolutedFeatureListRowsByRtOnly;
  }

  private ModularFeature getMainFeature(List<ModularFeature> groups) {
    List<Range<Double>> adjustedRanges = new ArrayList<>();
    if (mzValuesToIgnore != null) {
      // Adjust ranges if min and max values are the same
      for (Range<Double> range : mzValuesToIgnore) {
        if (range.lowerEndpoint().equals(range.upperEndpoint())) {
          double minValue = range.lowerEndpoint();
          double maxValue = minValue + 1.0;
          adjustedRanges.add(Range.closed(minValue, maxValue));
        } else {
          adjustedRanges.add(range);
        }
      }
    }

    for (ModularFeature feature : groups) {
      double mz = feature.getMZ();
      boolean isIgnored = false;
      if (!adjustedRanges.isEmpty()) {
        for (Range<Double> range : adjustedRanges) {
          if (range.contains(mz)) {
            isIgnored = true;
            break;
          }
        }
      }
      if (!isIgnored) {
        return feature;
      }
    }
    return null; // Return null if all features are in the ignored ranges
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
