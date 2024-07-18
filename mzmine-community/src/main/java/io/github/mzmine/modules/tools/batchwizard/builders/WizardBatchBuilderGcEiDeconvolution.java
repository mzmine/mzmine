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

package io.github.mzmine.modules.tools.batchwizard.builders;


import com.google.common.collect.Range;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.align_gc.GCAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_gc.GCAlignerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionAlgorithm;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionAlgorithms;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionGCModule;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionGCParameters;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.rtgroupingandsharecorrelation.RtGroupingAndShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
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
import java.util.Objects;
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
  private final double sampleCountRatio;
  private final boolean rtSmoothing;
  private final boolean recalibrateRetentionTime;
  private final int minNumberOfSignalsInDeconSpectra;
  private final Boolean exportAnnotationGraphics;

  public WizardBatchBuilderGcEiDeconvolution(final WizardSequence steps) {
    // extract default parameters that are used for all workflows
    super(steps);

    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.ION_INTERFACE);
    var filterParams = steps.get(WizardPart.FILTER);

    // special workflow parameter are extracted here
    // chromatography
    if (dataFiles.length > 1) {
      recalibrateRetentionTime = getValue(params,
          IonInterfaceGcElectronImpactWizardParameters.RECALIBRATE_RETENTION_TIMES);
    } else {
      recalibrateRetentionTime = false;
    }
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

    // currently the alignment uses only relative values as input.
    // in the future the ADAP alignment should use either absolute or relative as defined by the
    // MinimumSamplesParameter
    AbsoluteAndRelativeInt minSamples = getValue(filterParams,
        FilterWizardParameters.minNumberOfSamples);
    if (Objects.requireNonNull(minSamples).getAbsolute() == 1) {
      sampleCountRatio = 0;
    } else {
      double numOfSamples = dataFiles.length;
      sampleCountRatio = Math.max(minSamples.abs() / numOfSamples, minSamples.rel());
    }
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
    minNumberOfSignalsInDeconSpectra = getValue(params,
        WorkflowGcElectronImpactWizardParameters.MIN_NUMBER_OF_SIGNALS_IN_DECON_SPECTRA);
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectionStepForAllScans(q);
    makeAndAddAdapChromatogramStep(q, minFeatureHeight, mzTolScans, massDetectorOption,
        minRtDataPoints, cropRtRange, polarity);
    if (rtSmoothing) {
      makeAndAddSmoothingStep(q, true, minRtDataPoints, false);
    }
    makeAndAddRtLocalMinResolver(q, null, minRtDataPoints, cropRtRange, rtFwhm, 10);
    if (recalibrateRetentionTime) {
      makeAndAddRetentionTimeCalibration(q, mzTolInterSample, interSampleRtTol,
          handleOriginalFeatureLists);
    }
    makeSpectralDeconvolutionStep(q);
    makeAndAddAlignmentStep(q);
    makeAndAddRowFilterStep(q);
    makeAndAddDuplicateRowFilterStep(q, handleOriginalFeatureLists, mzTolFeaturesIntraSample,
        rtFwhm, imsInstrumentType);
    makeAndAddSpectralNetworkingSteps(q, isExportActive, exportPath);
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

  @Override
  protected void makeAndAddRtLocalMinResolver(final BatchQueue q, final ParameterSet groupMs2Params,
      final Integer minRtDataPoints, final Range<Double> cropRtRange, final RTTolerance rtFwhm,
      final Integer maxIsomersInRt) {
    super.makeAndAddRtLocalMinResolver(q, groupMs2Params, minRtDataPoints, cropRtRange, rtFwhm,
        maxIsomersInRt);
  }


  private void makeSpectralDeconvolutionStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(SpectralDeconvolutionGCModule.class).cloneParameterSet();
    MZmineProcessingStep<SpectralDeconvolutionAlgorithm> spectralDeconvolutionAlgorithmMZmineProcessingStep = createSpectralDeconvolutionStep();
    param.setParameter(SpectralDeconvolutionGCParameters.SPECTRAL_DECONVOLUTION_ALGORITHM,
        spectralDeconvolutionAlgorithmMZmineProcessingStep);
    param.setParameter(SpectralDeconvolutionGCParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(SpectralDeconvolutionGCParameters.HANDLE_ORIGINAL,
        handleOriginalFeatureLists);
    param.setParameter(SpectralDeconvolutionGCParameters.SUFFIX, "decon");
    param.setParameter(SpectralDeconvolutionGCParameters.MZ_VALUES_TO_IGNORE, false);
    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(SpectralDeconvolutionGCModule.class), param));
  }

  private void makeAndAddAlignmentStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(GCAlignerModule.class).cloneParameterSet();
    param.setParameter(GCAlignerParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(GCAlignerParameters.MZ_TOLERANCE, mzTolScans);
    param.setParameter(GCAlignerParameters.RT_TOLERANCE, interSampleRtTol);
    ModuleComboParameter<SpectralSimilarityFunction> simFunction = param.getParameter(
        GCAlignerParameters.SIMILARITY_FUNCTION);
    ParameterSet weightedCosineParam = MZmineCore.getConfiguration()
        .getModuleParameters(WeightedCosineSpectralSimilarity.class).cloneParameterSet();
    weightedCosineParam.setParameter(CompositeCosineSpectralSimilarityParameters.weight,
        Weights.NIST_GC);
    weightedCosineParam.setParameter(CompositeCosineSpectralSimilarityParameters.minCosine, 0.7);
    weightedCosineParam.setParameter(CompositeCosineSpectralSimilarityParameters.handleUnmatched,
        HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO);
    SpectralSimilarityFunction weightedCosineModule = SpectralSimilarityFunction.compositeCosine;
    var similarityStep = new MZmineProcessingStepImpl<>(weightedCosineModule, weightedCosineParam);
    simFunction.setValue(similarityStep);
    param.setParameter(GCAlignerParameters.SIMILARITY_FUNCTION, similarityStep);
    param.setParameter(GCAlignerParameters.FEATURE_LIST_NAME, "Aligned feature list");
    param.setParameter(GCAlignerParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(GCAlignerParameters.RT_WEIGHT, 0.5d);
    q.add(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(GCAlignerModule.class), param));
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
        new MZTolerance(mzTolScans.getMzTolerance(), mzTolScans.getPpmTolerance()));
    param.setParameter(SpectralLibrarySearchParameters.mzTolerance,
        new MZTolerance(mzTolScans.getMzTolerance(), mzTolScans.getPpmTolerance()));
    param.setParameter(SpectralLibrarySearchParameters.removePrecursor, false);
    param.setParameter(SpectralLibrarySearchParameters.minMatch, 8);
    // similarity
    ModuleComboParameter<SpectralSimilarityFunction> simFunction = param.getParameter(
        SpectralLibrarySearchParameters.similarityFunction);

    ParameterSet weightedCosineParam = MZmineCore.getConfiguration()
        .getModuleParameters(WeightedCosineSpectralSimilarity.class).cloneParameterSet();
    weightedCosineParam.setParameter(CompositeCosineSpectralSimilarityParameters.weight,
        Weights.NIST_GC);
    weightedCosineParam.setParameter(CompositeCosineSpectralSimilarityParameters.minCosine, 0.85);
    weightedCosineParam.setParameter(CompositeCosineSpectralSimilarityParameters.handleUnmatched,
        HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO);

    SpectralSimilarityFunction weightedCosineModule = SpectralSimilarityFunction.compositeCosine;
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

  private MZmineProcessingStep<SpectralDeconvolutionAlgorithm> createSpectralDeconvolutionStep() {
    SpectralDeconvolutionAlgorithm detect = SpectralDeconvolutionAlgorithms.RT_GROUPING_AND_SHAPE_CORRELATION.getDefaultModule();
    ParameterSet param = SpectralDeconvolutionAlgorithms.RT_GROUPING_AND_SHAPE_CORRELATION.getParametersCopy();
    param.setParameter(RtGroupingAndShapeCorrelationParameters.RT_TOLERANCE, intraSampleRtTol);
    param.setParameter(RtGroupingAndShapeCorrelationParameters.MIN_NUMBER_OF_SIGNALS,
        minNumberOfSignalsInDeconSpectra);
    return new MZmineProcessingStepImpl<>(detect, param);
  }

  @Override
  protected void makeAndAddRowFilterStep(BatchQueue q) {
    super.makeAndAddRowFilterStep(q);
    // dont change if the step was not created
    final MZmineProcessingStep<MZmineProcessingModule> filterStep = q.getLast();
    if (!filter13C && !minAlignedSamples.isGreaterZero() || !filterStep.getModule()
        .equals(MZmineCore.getModuleInstance(RowsFilterModule.class))) {
      return;
    }

    // deactivate for gc-ei, everything has a spectrum
    final ParameterSet param = filterStep.getParameterSet();
    param.setParameter(RowsFilterParameters.KEEP_ALL_MS2, false);
  }
}
