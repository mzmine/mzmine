/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.otherdectectors.MrmTransitionListType;
import io.github.mzmine.datamodel.otherdetectors.MrmTransition;
import io.github.mzmine.datamodel.otherdetectors.MrmTransitionList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BaselineCorrectionTask extends AbstractSimpleTask {

  private final FeatureList originalFlist;
  private final BaselineCorrector corrector;
  private final String suffix;
  private final MZmineProject project;
  private OriginalFeatureListOption handleOriginal;
  private ModularFeatureList newFlist;

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   */
  protected BaselineCorrectionTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, FeatureList flist,
      MZmineProject project) {
    super(storage, moduleCallDate, parameters, moduleClass);

    this.originalFlist = flist;
    handleOriginal = parameters.getValue(BaselineCorrectionParameters.handleOriginal);
    this.project = project;
    final BaselineCorrectors value = parameters.getValue(
        BaselineCorrectionParameters.correctionAlgorithm);
    corrector = value.getModuleInstance().newInstance(parameters, getMemoryMapStorage(), flist);
    suffix = parameters.getValue(BaselineCorrectionParameters.suffix);
    totalItems = flist.getNumberOfRows();
  }

  @Override
  protected void process() {
    if (originalFlist.getNumberOfRawDataFiles() > 1) {
      error("More than one raw file in feature list + " + originalFlist.getName());
    }

    newFlist = FeatureListUtils.createCopy(originalFlist, suffix, getMemoryMapStorage());

    final RawDataFile rawDataFile = originalFlist.getRawDataFile(0);
    final FeatureDataAccess access = EfficientDataAccess.of(originalFlist,
        EfficientDataAccess.FeatureDataType.INCLUDE_ZEROS, rawDataFile);

    while (access.hasNextFeature()) {
      final Feature feature = access.nextFeature();

      final IonTimeSeries<? extends Scan> its = corrector.correctBaseline(access);

      handleMrmFeature(feature);

      final ModularFeatureListRow newRow = new ModularFeatureListRow(newFlist,
          (ModularFeatureListRow) feature.getRow(), false);
      final ModularFeature newFeature = new ModularFeature(newFlist, feature);
      newFeature.set(FeatureDataType.class, its);
      FeatureDataUtils.recalculateIonSeriesDependingTypes(newFeature);
      newRow.addFeature(rawDataFile, newFeature);
      newFlist.addRow(newRow);
      finishedItems.getAndIncrement();
    }

    handleOriginal.reflectNewFeatureListToProject(suffix, project, newFlist, originalFlist);
  }

  private void handleMrmFeature(Feature feature) {
    final ModularFeature f = (ModularFeature) feature;
    if (f.get(MrmTransitionListType.class) instanceof MrmTransitionList transitions) {
      final List<? extends Scan> allScans = newFlist.getSeletedScans(f.getRawDataFile());
      final List<MrmTransition> correctedTransitions = new ArrayList<>();
      for (MrmTransition transition : transitions.transitions()) {
        // todo this may be optimised by an MrmDataAccess, similar to the feature data access,
        //  if this limits performance
        // remap so we get the same behaviour
        final IonTimeSeries<Scan> remapped = IonTimeSeriesUtils.remapRtAxis(
            transition.chromatogram(), allScans);
        final IonTimeSeries<? extends Scan> corrected = corrector.correctBaseline(remapped);
        final Range<Float> rtRange = f.getRawDataPointsRTRange();
        // make sure the corrected mrm only displays the specific rt range
        final IonTimeSeries<? extends Scan> remappedMrm = corrected.subSeries(getMemoryMapStorage(),
            rtRange.lowerEndpoint(), rtRange.upperEndpoint());
        correctedTransitions.add(transition.with(remappedMrm));
      }

      final MrmTransitionList corrected = new MrmTransitionList(correctedTransitions);
      f.set(MrmTransitionListType.class, corrected);
      corrected.setQuantifier(corrected.quantifier(), f);
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(newFlist);
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return List.of();
  }

  @Override
  public String getTaskDescription() {
    return "Correcting baseline for feature list " + originalFlist.getName();
  }
}
