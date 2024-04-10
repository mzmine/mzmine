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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
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
    List<List<ModularFeature>> groupedFeatures = new ArrayList<>();
    for (ModularFeature feature : features) {
      boolean addedToGroup = false;
      for (List<ModularFeature> group : groupedFeatures) {
        // Compare current feature with the first feature in each group (representative feature)
        ModularFeature representativeFeature = group.get(0);
        if (rtTolerance.checkWithinTolerance(feature.getRT(), representativeFeature.getRT())) {
          // If within RT tolerance, add to the group
          group.add(feature);
          addedToGroup = true;
          break;
        }
      }
      // If feature doesn't fit into any existing group, create a new group
      if (!addedToGroup) {
        List<ModularFeature> newGroup = new ArrayList<>();
        newGroup.add(feature);
        groupedFeatures.add(newGroup);
      }
    }

    return groupedFeatures;
  }

  private void createNewDeconvolutedFeatureList(List<FeatureListRow> deconvolutedFeatureListRows) {

    // Create new feature list.
    deconvolutedFeatureList = new ModularFeatureList(featureList.getName() + " " + suffix,
        getMemoryMapStorage(), featureList.getRawDataFiles());
    // Copy previous applied methods.
    for (final FeatureListAppliedMethod method : featureList.getAppliedMethods()) {
      deconvolutedFeatureList.addDescriptionOfAppliedTask(method);
    }

    featureList.getRawDataFiles().forEach(
        file -> deconvolutedFeatureList.setSelectedScans(file, featureList.getSeletedScans(file)));

    DataTypeUtils.addDefaultChromatographicTypeColumns(
        (ModularFeatureList) deconvolutedFeatureList);
    // Add task description to featureList.
    deconvolutedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(getTaskDescription(),
            SpectralDeconvolutionGCModule.class, parameters, getModuleCallDate()));

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
      List<SpectralDeconvolutionGCTask.PseudoSpectrumDataPoint> pseudoSpectrumDataPoints = new ArrayList<>();
      group.sort(Comparator.comparingDouble(ModularFeature::getHeight).reversed());
      for (int i = 0; i < group.size(); i++) {
        pseudoSpectrumDataPoints.add(
            new PseudoSpectrumDataPoint(group.get(i).getMZ(), group.get(i).getHeight()));
      }
      if (pseudoSpectrumDataPoints.size() >= minNumberOfSignals) {
        pseudoSpectrumDataPoints.sort(Comparator.comparingDouble(PseudoSpectrumDataPoint::mz));
        double[] mzs = pseudoSpectrumDataPoints.stream().mapToDouble(PseudoSpectrumDataPoint::mz)
            .toArray();
        double[] intensities = pseudoSpectrumDataPoints.stream()
            .mapToDouble(PseudoSpectrumDataPoint::intensity).toArray();

        // Create PseudoSpectrum, take first feature to ensure most intense is representative feature
        PseudoSpectrum pseudoSpectrum = new SimplePseudoSpectrum(featureList.getRawDataFile(0), 1,
            // MS Level
            group.get(0).getRT(), null, // No MsMsInfo for pseudo spectrum
            mzs, intensities, group.get(0).getRepresentativeScan().getPolarity(),
            "Correlated Features Pseudo Spectrum", PseudoSpectrumType.GC_EI);

        group.get(0).setAllMS2FragmentScans(List.of(pseudoSpectrum));
        deconvolutedFeatureListRowsByRtOnly.add(group.get(0).getRow());
      }
    }
    return deconvolutedFeatureListRowsByRtOnly;
  }

  private record PseudoSpectrumDataPoint(double mz, double intensity) {

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
