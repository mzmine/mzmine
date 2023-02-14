/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.builders;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.RangeUtils;
import java.io.File;
import java.util.Optional;

public class WizardBatchBuilderLcDDA extends WizardBatchBuilder {

  private final Range<Double> cropRtRange;
  private final RTTolerance intraSampleRtTol;
  private final RTTolerance interSampleRtTol;
  private final Integer minRtDataPoints;
  private final Integer maxIsomersInRt;
  private final RTTolerance rtFwhm;
  private final Boolean stableIonizationAcrossSamples;
  private final Boolean isExportActive;
  private final Boolean exportGnps;
  private final Boolean exportSirius;
  private final File exportPath;
  private final Boolean rtSmoothing;

  public WizardBatchBuilderLcDDA(final WizardSequence steps) {
    // extract default parameters that are used for all workflows
    super(steps);

    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.ION_INTERFACE);
    // special workflow parameter are extracted here
    // chromatography
    rtSmoothing = getValue(params, IonInterfaceHplcWizardParameters.smoothing);
    cropRtRange = getValue(params, IonInterfaceHplcWizardParameters.cropRtRange);
    intraSampleRtTol = getValue(params, IonInterfaceHplcWizardParameters.intraSampleRTTolerance);
    interSampleRtTol = getValue(params, IonInterfaceHplcWizardParameters.interSampleRTTolerance);
    minRtDataPoints = getValue(params, IonInterfaceHplcWizardParameters.minNumberOfDataPoints);
    maxIsomersInRt = getValue(params,
        IonInterfaceHplcWizardParameters.maximumIsomersInChromatogram);
    rtFwhm = getValue(params, IonInterfaceHplcWizardParameters.approximateChromatographicFWHM);
    stableIonizationAcrossSamples = getValue(params,
        IonInterfaceHplcWizardParameters.stableIonizationAcrossSamples);

    // DDA workflow parameters
    params = steps.get(WizardPart.WORKFLOW);
    OptionalValue<File> optional = getOptional(params, WorkflowDdaWizardParameters.exportPath);
    isExportActive = optional.active();
    exportPath = optional.value();
    exportGnps = getValue(params, WorkflowDdaWizardParameters.exportGnps);
    exportSirius = getValue(params, WorkflowDdaWizardParameters.exportSirius);
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectorSteps(q);
    makeAndAddAdapChromatogramStep(q, minFeatureHeight, mzTolScans, noiseLevelMs1, minRtDataPoints,
        cropRtRange);
    makeAndAddSmoothingStep(q, rtSmoothing, minRtDataPoints, false);

    var groupMs2Params = createMs2GrouperParameters();
    makeAndAddRtLocalMinResolver(q, groupMs2Params);

    if (isImsActive) {
      makeAndAddImsExpanderStep(q);
      makeAndAddSmoothingStep(q, false, minRtDataPoints, imsSmoothing);
      makeAndAddMobilityResolvingStep(q, groupMs2Params);
      makeAndAddSmoothingStep(q, rtSmoothing, minRtDataPoints, imsSmoothing);
    }

    makeAndAddDeisotopingStep(q, intraSampleRtTol);
    makeAndAddIsotopeFinderStep(q);
    makeAndAddJoinAlignmentStep(q, interSampleRtTol);
    makeAndAddRowFilterStep(q);
    makeAndAddGapFillStep(q, interSampleRtTol);
    makeAndAddDuplicateRowFilterStep(q, handleOriginalFeatureLists, mzTolFeaturesIntraSample,
        rtFwhm, imsInstrumentType);
    // ions annotation and feature grouping
    makeAndAddMetaCorrStep(q);
    makeAndAddIinStep(q);

    // annotation
    makeAndAddLibrarySearchStep(q);
    makeAndAddLocalCsvDatabaseSearchStep(q, interSampleRtTol);
    // export
    makeAndAddDdaExportSteps(q, isExportActive, exportPath, exportGnps, exportSirius);
    return q;
  }

  protected ParameterSet createMs2GrouperParameters() {
    return super.createMs2GrouperParameters(minRtDataPoints, minRtDataPoints >= 4, rtFwhm);
  }

  protected void makeAndAddRtLocalMinResolver(final BatchQueue q,
      final ParameterSet groupMs2Params) {
    final double totalRtWidth = RangeUtils.rangeLength(cropRtRange);
    final float fwhm = rtFwhm.getToleranceInMinutes();

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, "r");
    param.setParameter(MinimumSearchFeatureResolverParameters.handleOriginal,
        handleOriginalFeatureLists);

    // set MS2 grouping
    param.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters,
        groupMs2Params != null);
    if (groupMs2Params != null) {
      // the grouper parameterset might not be SUB set but the original one from the Grouper Module
      var subParameterSet = param.getParameter(
              MinimumSearchFeatureResolverParameters.groupMS2Parameters).getEmbeddedParameters()
          .cloneParameterSet();
      ParameterUtils.copyParameters(groupMs2Params, subParameterSet);

      param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
          .setEmbeddedParameters((GroupMS2SubParameters) subParameterSet);
    }

    // important apply to retention time
    param.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.RETENTION_TIME);
    // should be relatively high - unless user suspects many same m/z peaks in chromatogram
    // e.g., isomers or fragments in GC-EI-MS
    // 10 isomers, 0.05 min FWHM, 10 min total time = 0.90 threshold
    // ranges from 0.3 - 0.9
    final double thresholdPercent = MathUtils.within(1d - fwhm * maxIsomersInRt / totalRtWidth * 2d,
        0.3, 0.9, 3);

    param.setParameter(MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL,
        thresholdPercent);
    param.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE, (double) fwhm);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT, 0d);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT,
        minFeatureHeight);
    final double ratioTopToEdge = minRtDataPoints == 3 ? 1.4 : (minRtDataPoints == 4 ? 1.8 : 2);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO, ratioTopToEdge);
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION,
        Range.closed(0d, fwhm * 30d));
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS,
        minRtDataPoints);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MinimumSearchFeatureResolverModule.class), param));
  }

  protected void makeAndAddMetaCorrStep(final BatchQueue q) {
    final boolean useCorrGrouping = minRtDataPoints > 3;
    RTTolerance rtTol = new RTTolerance(rtFwhm.getTolerance() * (useCorrGrouping ? 1.1f : 0.7f),
        rtFwhm.getUnit());
    makeAndAddMetaCorrStep(q, minRtDataPoints, rtTol, stableIonizationAcrossSamples);
  }

}
