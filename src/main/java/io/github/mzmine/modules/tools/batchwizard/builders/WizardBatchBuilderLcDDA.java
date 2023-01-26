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
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperParameters;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.InterSampleHeightCorrParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Polarity;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepPreset;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.MinimumFeaturesFilterParameters;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteNRelativeInt;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteNRelativeInt.Mode;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
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

    Optional<? extends WizardStepPreset> params = steps.get(WizardPart.ION_INTERFACE);
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
    makeAndAddRtLocalMinResolver(q);

    if (isImsActive) {
      makeAndAddImsExpanderStep(q);
      makeAndAddSmoothingStep(q, false, minRtDataPoints, imsSmoothing);
      makeAndAddMobilityResolvingStep(q);
      makeAndAddSmoothingStep(q, rtSmoothing, minRtDataPoints, imsSmoothing);
    }

    makeAndAddDeisotopingStep(q);
    makeAndAddIsotopeFinderStep(q);
    makeAndAddAlignmentStep(q);
    makeAndAddRowFilterStep(q);
    makeAndAddGapFillStep(q);
    if (!isImsActive) { // might filter IMS resolved isomers
      makeAndAddDuplicateRowFilterStep(q, handleOriginalFeatureLists, mzTolFeaturesIntraSample,
          rtFwhm);
    }
    // ions annotation and feature grouping
    makeAndAddMetaCorrStep(q);
    makeAndAddIinStep(q);

    // annotation
    makeAndAddLibrarySearchStep(q);
    // export
    makeAndAddDdaExportSteps(q, isExportActive, exportPath, exportGnps, exportSirius);
    return q;
  }

  protected void makeAndAddRtLocalMinResolver(final BatchQueue q) {
    // only TIMS currently supports DDA MS2 acquisition with PASEF
    // other instruments have the fragmentation before the IMS cell
    boolean hasIMS = isImsActive && imsInstrumentType.equals(MobilityType.TIMS);

    final double totalRtWidth = RangeUtils.rangeLength(cropRtRange);
    final float fwhm = rtFwhm.getToleranceInMinutes();

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, "r");
    param.setParameter(MinimumSearchFeatureResolverParameters.handleOriginal,
        handleOriginalFeatureLists);

    param.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, true);
    final GroupMS2SubParameters groupParam = param.getParameter(
        MinimumSearchFeatureResolverParameters.groupMS2Parameters).getEmbeddedParameters();
    // Using a fixed wide range here because precursor isolation is usually unit resolution
    groupParam.setParameter(GroupMS2SubParameters.mzTol, new MZTolerance(0.01, 10));
    // TODO check
    groupParam.setParameter(GroupMS2SubParameters.combineTimsMsMs, false);
    boolean limitByRTEdges = minRtDataPoints >= 4;
    groupParam.setParameter(GroupMS2SubParameters.limitRTByFeature, limitByRTEdges);
    groupParam.setParameter(GroupMS2SubParameters.lockMS2ToFeatureMobilityRange, true);
    // rt tolerance is +- while FWHM is the width. still the MS2 might be triggered very early
    // change rt tol depending on number of datapoints
    groupParam.setParameter(GroupMS2SubParameters.rtTol,
        new RTTolerance(limitByRTEdges ? fwhm * 3 : fwhm, Unit.MINUTES));
    groupParam.setParameter(GroupMS2SubParameters.outputNoiseLevel, hasIMS);
    groupParam.getParameter(GroupMS2SubParameters.outputNoiseLevel).getEmbeddedParameter()
        .setValue(noiseLevelMsn * 2);
    groupParam.setParameter(GroupMS2SubParameters.outputNoiseLevelRelative, hasIMS);
    groupParam.getParameter(GroupMS2SubParameters.outputNoiseLevelRelative).getEmbeddedParameter()
        .setValue(0.1);

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

  protected void makeAndAddDeisotopingStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IsotopeGrouperModule.class).cloneParameterSet();

    param.setParameter(IsotopeGrouperParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(IsotopeGrouperParameters.suffix, "deiso");
    param.setParameter(IsotopeGrouperParameters.mzTolerance, mzTolFeaturesIntraSample);
    param.setParameter(IsotopeGrouperParameters.rtTolerance, intraSampleRtTol);
    param.setParameter(IsotopeGrouperParameters.mobilityTolerace, isImsActive);
    param.getParameter(IsotopeGrouperParameters.mobilityTolerace).getEmbeddedParameter().setValue(
        imsInstrumentType == MobilityType.TIMS ? new MobilityTolerance(0.008f)
            : new MobilityTolerance(1f));
    param.setParameter(IsotopeGrouperParameters.monotonicShape, true);
    param.setParameter(IsotopeGrouperParameters.keepAllMS2, true);
    param.setParameter(IsotopeGrouperParameters.maximumCharge, 2);
    param.setParameter(IsotopeGrouperParameters.representativeIsotope,
        IsotopeGrouperParameters.ChooseTopIntensity);
    param.setParameter(IsotopeGrouperParameters.handleOriginal, handleOriginalFeatureLists);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IsotopeGrouperModule.class),
        param));
  }


  protected void makeAndAddAlignmentStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(JoinAlignerModule.class).cloneParameterSet();
    param.setParameter(JoinAlignerParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(JoinAlignerParameters.peakListName, "Aligned feature list");
    param.setParameter(JoinAlignerParameters.MZTolerance, mzTolInterSample);
    param.setParameter(JoinAlignerParameters.MZWeight, 3d);
    param.setParameter(JoinAlignerParameters.RTTolerance, interSampleRtTol);
    param.setParameter(JoinAlignerParameters.RTWeight, 1d);
    param.setParameter(JoinAlignerParameters.mobilityTolerance, isImsActive);
    param.getParameter(JoinAlignerParameters.mobilityTolerance).getEmbeddedParameter().setValue(
        imsInstrumentType == MobilityType.TIMS ? new MobilityTolerance(0.01f)
            : new MobilityTolerance(1f));
    param.setParameter(JoinAlignerParameters.SameChargeRequired, false);
    param.setParameter(JoinAlignerParameters.SameIDRequired, false);
    param.setParameter(JoinAlignerParameters.compareIsotopePattern, false);
    param.setParameter(JoinAlignerParameters.compareSpectraSimilarity, false);
    param.setParameter(JoinAlignerParameters.handleOriginal, handleOriginalFeatureLists);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(JoinAlignerModule.class),
        param));
  }

  protected void makeAndAddGapFillStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MultiThreadPeakFinderModule.class).cloneParameterSet();

    param.setParameter(MultiThreadPeakFinderParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // going back into scans so rather use scan mz tol
    param.setParameter(MultiThreadPeakFinderParameters.MZTolerance, mzTolScans);
    param.setParameter(MultiThreadPeakFinderParameters.RTTolerance, interSampleRtTol);
    param.setParameter(MultiThreadPeakFinderParameters.intTolerance, 0.2);
    param.setParameter(MultiThreadPeakFinderParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(MultiThreadPeakFinderParameters.suffix, "gaps");

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MultiThreadPeakFinderModule.class), param));
  }

  protected void makeAndAddMetaCorrStep(final BatchQueue q) {
    final boolean useCorrGrouping = minRtDataPoints > 3;
    RTTolerance rtTol = new RTTolerance(rtFwhm.getTolerance() * (useCorrGrouping ? 1.1f : 0.7f),
        rtFwhm.getUnit());

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(CorrelateGroupingModule.class);
    param.setParameter(CorrelateGroupingParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(CorrelateGroupingParameters.RT_TOLERANCE, rtTol);
    param.setParameter(CorrelateGroupingParameters.GROUPSPARAMETER, false);
    param.setParameter(CorrelateGroupingParameters.MIN_HEIGHT, 0d);
    param.setParameter(CorrelateGroupingParameters.NOISE_LEVEL, noiseLevelMs1);
    param.setParameter(CorrelateGroupingParameters.MIN_SAMPLES_FILTER, true);

    // min samples
    var minSampleP = param.getParameter(CorrelateGroupingParameters.MIN_SAMPLES_FILTER)
        .getEmbeddedParameters();
    minSampleP.setParameter(MinimumFeaturesFilterParameters.MIN_SAMPLES_GROUP,
        new AbsoluteNRelativeInt(0, 0, Mode.ROUND_DOWN));
    minSampleP.setParameter(MinimumFeaturesFilterParameters.MIN_SAMPLES_ALL,
        new AbsoluteNRelativeInt(minAlignedSamples, 0, Mode.ROUND_DOWN));
    minSampleP.setParameter(MinimumFeaturesFilterParameters.MIN_INTENSITY_OVERLAP, 0.6d);
    minSampleP.setParameter(MinimumFeaturesFilterParameters.EXCLUDE_ESTIMATED, true);

    //
    param.setParameter(CorrelateGroupingParameters.FSHAPE_CORRELATION, useCorrGrouping);
    var fshapeCorrP = param.getParameter(CorrelateGroupingParameters.FSHAPE_CORRELATION)
        .getEmbeddedParameters();
    // MIN_DP_CORR_PEAK_SHAPE, MIN_DP_FEATURE_EDGE, MEASURE, MIN_R_SHAPE_INTRA, MIN_TOTAL_CORR
    fshapeCorrP.setParameter(FeatureShapeCorrelationParameters.MIN_DP_FEATURE_EDGE, 2);
    fshapeCorrP.setParameter(FeatureShapeCorrelationParameters.MIN_DP_CORR_PEAK_SHAPE, 5);
    fshapeCorrP.setParameter(FeatureShapeCorrelationParameters.MEASURE, SimilarityMeasure.PEARSON);
    fshapeCorrP.setParameter(FeatureShapeCorrelationParameters.MIN_R_SHAPE_INTRA, 0.85);
    fshapeCorrP.setParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR, false);
    fshapeCorrP.getParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR)
        .getEmbeddedParameter().setValue(0.5d);

    // inter sample height correlation - only if same conditions
    param.setParameter(CorrelateGroupingParameters.IMAX_CORRELATION, stableIonizationAcrossSamples);
    var interSampleCorrParam = param.getParameter(CorrelateGroupingParameters.IMAX_CORRELATION)
        .getEmbeddedParameters();
    interSampleCorrParam.setParameter(InterSampleHeightCorrParameters.MIN_CORRELATION, 0.7);
    interSampleCorrParam.setParameter(InterSampleHeightCorrParameters.MIN_DP, 2);
    interSampleCorrParam.setParameter(InterSampleHeightCorrParameters.MEASURE,
        SimilarityMeasure.PEARSON);

    q.add(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(CorrelateGroupingModule.class),
            param));
  }

  protected void makeAndAddIinStep(final BatchQueue q) {
    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IonNetworkingModule.class);
    param.setParameter(IonNetworkingParameters.MIN_HEIGHT, 0d);
    param.setParameter(IonNetworkingParameters.MZ_TOLERANCE, mzTolFeaturesIntraSample);
    param.setParameter(IonNetworkingParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));

    // refinement
    param.setParameter(IonNetworkingParameters.ANNOTATION_REFINEMENTS, true);
    var refinementParam = param.getParameter(IonNetworkingParameters.ANNOTATION_REFINEMENTS)
        .getEmbeddedParameters();
    refinementParam.setParameter(IonNetworkRefinementParameters.MIN_NETWORK_SIZE, false);
    refinementParam.setParameter(IonNetworkRefinementParameters.TRUE_THRESHOLD, true);
    refinementParam.getParameter(IonNetworkRefinementParameters.TRUE_THRESHOLD)
        .getEmbeddedParameter().setValue(4);
    refinementParam.setParameter(IonNetworkRefinementParameters.DELETE_SMALL_NO_MAJOR, true);
    refinementParam.setParameter(IonNetworkRefinementParameters.DELETE_ROWS_WITHOUT_ID, false);
    refinementParam.setParameter(IonNetworkRefinementParameters.DELETE_WITHOUT_MONOMER, true);

    // ion library
    var ionLibraryParam = param.getParameter(IonNetworkingParameters.LIBRARY)
        .getEmbeddedParameters();
    ionLibraryParam.setParameter(IonLibraryParameterSet.POSITIVE_MODE,
        polarity == Polarity.Positive ? "POSITIVE" : "NEGATIVE");
    ionLibraryParam.setParameter(IonLibraryParameterSet.MAX_CHARGE, 2);
    ionLibraryParam.setParameter(IonLibraryParameterSet.MAX_MOLECULES, 2);
    IonModification[] adducts;
    if (polarity == Polarity.Positive) {
      adducts = new IonModification[]{IonModification.H, IonModification.NA,
          IonModification.Hneg_NA2, IonModification.K, IonModification.NH4, IonModification.H2plus};
    } else {
      adducts = new IonModification[]{IonModification.H_NEG, IonModification.FA,
          IonModification.NA_2H, IonModification.CL};
    }
    IonModification[] modifications = new IonModification[]{IonModification.H2O,
        IonModification.H2O_2};
    ionLibraryParam.setParameter(IonLibraryParameterSet.ADDUCTS,
        new IonModification[][]{adducts, modifications});

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IonNetworkingModule.class),
        param));
  }
}
