/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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
import dulab.adap.workflow.AlignmentParameters;
import dulab.adap.workflow.TwoStepDecompositionParameters;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.adap_hierarchicalclustering.ADAP3DecompositionV1_5Parameters;
import io.github.mzmine.modules.dataprocessing.adap_hierarchicalclustering.ADAPHierarchicalClusteringModule;
import io.github.mzmine.modules.dataprocessing.adap_mcr.ADAP3DecompositionV2Parameters;
import io.github.mzmine.modules.dataprocessing.adap_mcr.ADAPMultivariateCurveResolutionModule;
import io.github.mzmine.modules.dataprocessing.align_adap3.ADAP3AlignerModule;
import io.github.mzmine.modules.dataprocessing.align_adap3.ADAP3AlignerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.ADAPResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.AdapResolverModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.AdvancedSpectralLibrarySearchParameters;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchParameters;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchParameters.ScanMatchingSelection;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.export_features_gnps.gc.GnpsGcExportAndSubmitModule;
import io.github.mzmine.modules.io.export_features_gnps.gc.GnpsGcExportAndSubmitParameters;
import io.github.mzmine.modules.io.export_features_mgf.AdapMgfExportParameters.MzMode;
import io.github.mzmine.modules.io.export_features_msp.AdapMspExportModule;
import io.github.mzmine.modules.io.export_features_msp.AdapMspExportParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.FilterWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceGcElectronImpactWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowGcElectronImpactWizardParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelection;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.scans.similarity.impl.composite.CompositeCosineSpectralSimilarityParameters;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarity;
import java.io.File;
import java.util.Optional;

public class WizardBatchBuilderGcEiDeconvolution extends BaseWizardBatchBuilder {

  private final Range<Double> cropRtRange;
  private final RTTolerance intraSampleRtTol;
  private final RTTolerance interSampleRtTol;
  private final RTTolerance rtFwhm;
  private final boolean isExportActive;
  private final File exportPath;
  private final Boolean exportGnps;
  private final Boolean exportMsp;
  private final Integer minRtDataPoints; //min number of data points
  private final Double snThreshold;
  private final Range<Double> rtForCWT;
  private final double sampleCountRatio;
  private final boolean rtSmoothing;
  private final Boolean exportAnnotationGraphics;

  public WizardBatchBuilderGcEiDeconvolution(final WizardSequence steps) {
    // extract default parameters that are used for all workflows
    super(steps);

    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.ION_INTERFACE);
    var filterParams = steps.get(WizardPart.FILTER);

    // special workflow parameter are extracted here
    // chromatography
    rtSmoothing = getValue(params, IonInterfaceGcElectronImpactWizardParameters.smoothing);
    cropRtRange = getValue(params, IonInterfaceGcElectronImpactWizardParameters.cropRtRange);
    intraSampleRtTol = getValue(params,
        IonInterfaceGcElectronImpactWizardParameters.intraSampleRTTolerance);
    interSampleRtTol = getValue(params,
        IonInterfaceGcElectronImpactWizardParameters.interSampleRTTolerance);
    minRtDataPoints = getValue(params,
        IonInterfaceGcElectronImpactWizardParameters.minNumberOfDataPoints);
    rtFwhm = getValue(params,
        IonInterfaceGcElectronImpactWizardParameters.approximateChromatographicFWHM);
    snThreshold = getValue(params, IonInterfaceGcElectronImpactWizardParameters.SN_THRESHOLD);
    rtForCWT = getValue(params,
        IonInterfaceGcElectronImpactWizardParameters.RT_FOR_CWT_SCALES_DURATION);

    // currently the alignment uses only relative values as input.
    // in the future the ADAP alignment should use either absolute or relative as defined by the
    // MinimumSamplesParameter
    AbsoluteAndRelativeInt minSamples = getValue(filterParams,
        FilterWizardParameters.minNumberOfSamples);
    double numOfSamples = dataFiles.length;
    sampleCountRatio = Math.max(minSamples.abs() / numOfSamples, minSamples.rel());

    // GC-EI specific workflow parameters can go into a workflow parameters class similar to WizardWorkflowDdaParameters
    params = steps.get(WizardPart.WORKFLOW);
    OptionalValue<File> exportPath = getOptional(params,
        WorkflowGcElectronImpactWizardParameters.exportPath);
    isExportActive = exportPath.active();
    this.exportPath = exportPath.value();
    exportGnps = getValue(params, WorkflowGcElectronImpactWizardParameters.exportGnps);
    exportMsp = getValue(params, WorkflowGcElectronImpactWizardParameters.exportMsp);
    exportAnnotationGraphics = getValue(params,
        WorkflowGcElectronImpactWizardParameters.exportAnnotationGraphics);
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectionStepForAllScans(q);
    makeAndAddAdapChromatogramStep(q, minFeatureHeight, mzTolScans, massDetectorOption,
        minRtDataPoints, cropRtRange);
    makeAndAddSmoothingStep(q, rtSmoothing, minRtDataPoints, false);
    makeAndAddRtLocalMinResolver(q, null, minRtDataPoints, cropRtRange, rtFwhm,
        100);
    // TODO, use hierarchical clustering, because we cannot facilitate chromatograms
    // and features are present to use MultiCurveResolution
    // makeMultiCurveResolutionStep(q);
    makeHierarchicalClustering(q);
    makeAndAddAlignmentStep(q);
    makeAndAddGapFillStep(q, interSampleRtTol);
    makeAndAddLibrarySearchMS1Step(q, false);

    if (isExportActive) {
      if (exportGnps) {
        makeAndAddGnpsExportStep(q);
      }
      if (exportMsp) {
        makeAndAddMSPExportStep(q);
      }
      // last as it might crash
      if (exportAnnotationGraphics) {
        makeAndAddAnnotationGraphicsExportStep(q, exportPath);
      }
    }
    return q;
  }



  protected void makeAndAddRtAdapResolver(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(AdapResolverModule.class).cloneParameterSet();
    param.setParameter(ADAPResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ADAPResolverParameters.handleOriginal, OriginalFeatureListOption.REMOVE);

    param.setParameter(ADAPResolverParameters.SN_THRESHOLD, snThreshold);
    param.setParameter(ADAPResolverParameters.MIN_FEAT_HEIGHT, minFeatureHeight);
    param.setParameter(ADAPResolverParameters.RT_FOR_CWT_SCALES_DURATION, rtForCWT);
    param.setParameter(ADAPResolverParameters.COEF_AREA_THRESHOLD, 110.0);
    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(AdapResolverModule.class),
        param));
  }

  private void makeMultiCurveResolutionStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ADAPMultivariateCurveResolutionModule.class).cloneParameterSet();

    double retentionTimeFWHM = rtFwhm.getToleranceInMinutes() * 4;
    param.setParameter(ADAP3DecompositionV2Parameters.PREF_WINDOW_WIDTH,
        retentionTimeFWHM);
    param.setParameter(ADAP3DecompositionV2Parameters.RET_TIME_TOLERANCE,
        (double) intraSampleRtTol.getToleranceInMinutes());
    param.setParameter(ADAP3DecompositionV2Parameters.MIN_CLUSTER_SIZE, 1);

    param.setParameter(ADAP3DecompositionV2Parameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // TODO chromatograms need to be picked by name pattern
    // chromatograms are optional parameter
    param.setParameter(ADAP3DecompositionV2Parameters.CHROMATOGRAM_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));

    param.setParameter(ADAP3DecompositionV2Parameters.ADJUST_APEX_RET_TIME, false);
    param.setParameter(ADAP3DecompositionV2Parameters.HANDLE_ORIGINAL, handleOriginalFeatureLists);
    param.setParameter(ADAP3DecompositionV2Parameters.SUFFIX, "spec_decon");
    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ADAPMultivariateCurveResolutionModule.class), param));

  }

  private void makeHierarchicalClustering(BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ADAPHierarchicalClusteringModule.class).cloneParameterSet();

    param.setParameter(ADAP3DecompositionV1_5Parameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_DISTANCE,
        0.01);
    param.setParameter(ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_SIZE,
        3);
    param.setParameter(ADAP3DecompositionV1_5Parameters.MIN_CLUSTER_INTENSITY,
        minFeatureHeight);
    param.setParameter(ADAP3DecompositionV1_5Parameters.EDGE_TO_HEIGHT_RATIO,
        0.3);
    param.setParameter(ADAP3DecompositionV1_5Parameters.DELTA_TO_HEIGHT_RATIO, 0.2);
    param.setParameter(ADAP3DecompositionV1_5Parameters.USE_ISSHARED,
        false);
    param.setParameter(ADAP3DecompositionV1_5Parameters.MIN_MODEL_SHARPNESS,
        10.0);
    param.setParameter(ADAP3DecompositionV1_5Parameters.SHAPE_SIM_THRESHOLD,
        18.0);
    param.setParameter(ADAP3DecompositionV1_5Parameters.MODEL_PEAK_CHOICE,
        TwoStepDecompositionParameters.MODEL_PEAK_CHOICE_INTENSITY);
    param.setParameter(ADAP3DecompositionV1_5Parameters.SUFFIX,
        "spec-decon");
    param.setParameter(ADAP3DecompositionV1_5Parameters.MZ_VALUES,
        null);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ADAPHierarchicalClusteringModule.class), param));
  }


  protected void makeAndAddAlignmentStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ADAP3AlignerModule.class).cloneParameterSet();
    param.setParameter(ADAP3AlignerParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ADAP3AlignerParameters.NEW_PEAK_LIST_NAME, "Aligned feature list");
    param.setParameter(ADAP3AlignerParameters.SAMPLE_COUNT_RATIO, sampleCountRatio);
    param.setParameter(ADAP3AlignerParameters.RET_TIME_RANGE, interSampleRtTol);
    param.setParameter(ADAP3AlignerParameters.MZ_RANGE, mzTolInterSample);
    param.setParameter(ADAP3AlignerParameters.SCORE_TOLERANCE, 0.5);
    param.setParameter(ADAP3AlignerParameters.SCORE_WEIGHT, 0.1);
    param.setParameter(ADAP3AlignerParameters.EIC_SCORE, AlignmentParameters.RT_DIFFERENCE);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ADAP3AlignerModule.class),
        param));
  }

  private void makeAndAddLibrarySearchMS1Step(final BatchQueue q,
      boolean libraryGenerationWorkflow) {
    if (!libraryGenerationWorkflow && !checkLibraryFiles()) {
      return;
    }

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(SpectralLibrarySearchModule.class).cloneParameterSet();

    param.setParameter(SpectralLibrarySearchParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(SpectralLibrarySearchParameters.libraries, new SpectralLibrarySelection());
    param.setParameter(SpectralLibrarySearchParameters.scanMatchingSelection,
        ScanMatchingSelection.MS1);
    param.setParameter(SpectralLibrarySearchParameters.mzTolerancePrecursor,
        new MZTolerance(0.01, 20));
    param.setParameter(SpectralLibrarySearchParameters.mzTolerance, new MZTolerance(0.01, 20));
    param.setParameter(SpectralLibrarySearchParameters.removePrecursor, false);
    param.setParameter(SpectralLibrarySearchParameters.minMatch, 8);
    // similarity
    ModuleComboParameter<SpectralSimilarityFunction> simFunction = param.getParameter(
        SpectralLibrarySearchParameters.similarityFunction);

    ParameterSet weightedCosineParam = MZmineCore.getConfiguration()
        .getModuleParameters(WeightedCosineSpectralSimilarity.class).cloneParameterSet();
    weightedCosineParam.setParameter(CompositeCosineSpectralSimilarityParameters.weight,
        Weights.NIST_GC);
    weightedCosineParam.setParameter(CompositeCosineSpectralSimilarityParameters.minCosine, 0.8);
    weightedCosineParam.setParameter(CompositeCosineSpectralSimilarityParameters.handleUnmatched,
        HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO);

    SpectralSimilarityFunction weightedCosineModule = SpectralSimilarityFunction.weightedCosine;
    var libMatchStep = new MZmineProcessingStepImpl<>(weightedCosineModule, weightedCosineParam);

    // finally set the libmatch module plus parameters as step
    simFunction.setValue(libMatchStep);
    // advanced off
    param.setParameter(SpectralLibrarySearchParameters.advanced, false);
    var advanced = param.getEmbeddedParameterValue(SpectralLibrarySearchParameters.advanced);
    advanced.setParameter(AdvancedSpectralLibrarySearchParameters.cropSpectraToOverlap, false);
    advanced.setParameter(AdvancedSpectralLibrarySearchParameters.deisotoping, false);
    advanced.setParameter(AdvancedSpectralLibrarySearchParameters.needsIsotopePattern, false);
    advanced.setParameter(AdvancedSpectralLibrarySearchParameters.rtTolerance, false);
    advanced.setParameter(AdvancedSpectralLibrarySearchParameters.ccsTolerance, false, 0.05);

    MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(SpectralLibrarySearchModule.class), param);
    q.add(step);
  }


  protected void makeAndAddGnpsExportStep(final BatchQueue q) {
    final ParameterSet param = new GnpsGcExportAndSubmitParameters().cloneParameterSet();

    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(), fileName.getName() + "_gc_ei_gnps");
    param.setParameter(GnpsGcExportAndSubmitParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // going back into scans so rather use scan mz tol
    param.setParameter(GnpsGcExportAndSubmitParameters.REPRESENTATIVE_MZ,
        MzMode.AS_IN_FEATURE_TABLE);
    param.setParameter(GnpsGcExportAndSubmitParameters.OPEN_FOLDER, false);
    param.setParameter(GnpsGcExportAndSubmitParameters.FEATURE_INTENSITY, AbundanceMeasure.Area);
    param.setParameter(GnpsGcExportAndSubmitParameters.FILENAME, fileName);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(GnpsGcExportAndSubmitModule.class), param));
  }

  protected void makeAndAddMSPExportStep(final BatchQueue q) {
    final ParameterSet param = new AdapMspExportParameters().cloneParameterSet();

    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(), fileName.getName() + "_gc_ei.msp");

    param.setParameter(AdapMspExportParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(AdapMspExportParameters.FILENAME, fileName);
    param.setParameter(AdapMspExportParameters.ADD_RET_TIME, true);
    param.setParameter(AdapMspExportParameters.ADD_ANOVA_P_VALUE, true);
    param.setParameter(AdapMspExportParameters.INTEGER_MZ, false);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(AdapMspExportModule.class),
        param));
  }

}
