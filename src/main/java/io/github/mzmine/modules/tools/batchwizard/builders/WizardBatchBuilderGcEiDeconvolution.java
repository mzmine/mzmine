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
import dulab.adap.workflow.AlignmentParameters;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.adap_mcr.ADAP3DecompositionV2Parameters;
import io.github.mzmine.modules.dataprocessing.adap_mcr.ADAPMultivariateCurveResolutionModule;
import io.github.mzmine.modules.dataprocessing.align_adap3.ADAP3AlignerModule;
import io.github.mzmine.modules.dataprocessing.align_adap3.ADAP3AlignerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.ADAPResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.AdapResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.IntensityWindowsSNEstimator;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.SelectedScanTypes;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderParameters;
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
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.FeatureMeasurementType;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

public class WizardBatchBuilderGcEiDeconvolution extends WizardBatchBuilder {

  private final Range<Double> cropRtRange;
  private final RTTolerance intraSampleRtTol;
  private final RTTolerance interSampleRtTol;
  private final double rtFwhm;
  private final boolean isExportActive;
  private final File exportPath;
  private final Boolean exportGnps;
  private final Boolean exportMsp;
  private final Integer minRtDataPoints; //min number of data points
  private final Double snThreshold;
  private final Range<Double> rtforCWT;
  private final double sampleCountRatio;

  public WizardBatchBuilderGcEiDeconvolution(final WizardSequence steps) {
    // extract default parameters that are used for all workflows
    super(steps);

    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.ION_INTERFACE);
    Optional<? extends WizardStepParameters> filterParams = steps.get(WizardPart.FILTER);
    // special workflow parameter are extracted here
    // chromatography
    cropRtRange = getValue(params, IonInterfaceGcElectronImpactWizardParameters.cropRtRange);
    intraSampleRtTol = getValue(params,
        IonInterfaceGcElectronImpactWizardParameters.intraSampleRTTolerance);
    interSampleRtTol = getValue(params,
        IonInterfaceGcElectronImpactWizardParameters.interSampleRTTolerance);
    minRtDataPoints = getValue(params,
        IonInterfaceGcElectronImpactWizardParameters.minNumberOfDataPoints);
    rtFwhm = getValue(params,
        IonInterfaceGcElectronImpactWizardParameters.approximateChromatographicFWHM).getTolerance();
    snThreshold = getValue(params, IonInterfaceGcElectronImpactWizardParameters.SN_THRESHOLD);
    rtforCWT = getValue(params,
        IonInterfaceGcElectronImpactWizardParameters.RT_FOR_CWT_SCALES_DURATION);
    sampleCountRatio = getValue(filterParams, FilterWizardParameters.minNumberOfSamples).rel();

    // GC-EI specific workflow parameters can go into a workflow parameters class similar to WizardWorkflowDdaParameters
    params = steps.get(WizardPart.WORKFLOW);
    OptionalValue<File> optional = getOptional(params,
        WorkflowGcElectronImpactWizardParameters.exportPath);
    isExportActive = optional.active();
    exportPath = optional.value();
    exportGnps = getValue(params, WorkflowGcElectronImpactWizardParameters.exportGnps);
    exportMsp = getValue(params, WorkflowGcElectronImpactWizardParameters.exportMsp);
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectorSteps(q);
    makeAndAddAdapChromatogramStep(q, minFeatureHeight, mzTolScans, noiseLevelMs1, minRtDataPoints,
        cropRtRange);
    makeAndAddRtAdapResolver(q);
    makeMultiCurveResolutionStep(q);
    makeAndAddAlignmentStep(q);
    makeAndAddLibrarySearchStep(q);
    if (isExportActive) {
      if (exportGnps) {
        makeAndAddGnpsExportStep(q);
      }
      if(exportMsp){
        makeAndAddMSPExportStep(q);
      }
    }
    return q;
  }

  @Override
  protected void makeAndAddMassDetectorSteps(final BatchQueue q) {
    if (isImsActive) {
      final boolean isImsFromMzml = Arrays.stream(dataFiles)
          .anyMatch(file -> file.getName().toLowerCase().endsWith(".mzml"));
      if (!isImsFromMzml) { // == Bruker file
        makeAndAddMassDetectionStep(q, 1, SelectedScanTypes.FRAMES);
      }
      makeAndAddMassDetectionStep(q, 1, SelectedScanTypes.MOBLITY_SCANS);
      if (isImsFromMzml) {
        makeAndAddMobilityScanMergerStep(q);
      }
    } else {
      makeAndAddMassDetectionStep(q, 1, SelectedScanTypes.SCANS);
    }
  }

  protected void makeAndAddRtAdapResolver(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(AdapResolverModule.class).cloneParameterSet();
    param.setParameter(ADAPResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ADAPResolverParameters.handleOriginal, OriginalFeatureListOption.REMOVE);

    param.setParameter(ADAPResolverParameters.SN_THRESHOLD, snThreshold);
    param.setParameter(ADAPResolverParameters.MIN_FEAT_HEIGHT, minFeatureHeight);
    param.setParameter(ADAPResolverParameters.RT_FOR_CWT_SCALES_DURATION, rtforCWT);
    param.setParameter(ADAPResolverParameters.COEF_AREA_THRESHOLD, 110.0);
    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(AdapResolverModule.class),
        param));
  }

  private void makeMultiCurveResolutionStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ADAPMultivariateCurveResolutionModule.class).cloneParameterSet();
    param.setParameter(ADAP3DecompositionV2Parameters.PREF_WINDOW_WIDTH, rtFwhm*4);
    param.setParameter(ADAP3DecompositionV2Parameters.RET_TIME_TOLERANCE,
        (double) intraSampleRtTol.getTolerance());
    param.setParameter(ADAP3DecompositionV2Parameters.MIN_CLUSTER_SIZE, 1);
    param.setParameter(ADAP3DecompositionV2Parameters.CHROMATOGRAM_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ADAP3DecompositionV2Parameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ADAP3DecompositionV2Parameters.ADJUST_APEX_RET_TIME, false);
    param.setParameter(ADAP3DecompositionV2Parameters.SUFFIX, "Spectral Deconvolution");
    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ADAPMultivariateCurveResolutionModule.class), param));

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
    param.setParameter(GnpsGcExportAndSubmitParameters.FEATURE_INTENSITY,
        FeatureMeasurementType.AREA);
    param.setParameter(GnpsGcExportAndSubmitParameters.FILENAME, fileName);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(GnpsGcExportAndSubmitModule.class), param));
  }

  protected void makeAndAddMSPExportStep(final BatchQueue q) {
    final ParameterSet param = new AdapMspExportParameters().cloneParameterSet();

    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(), fileName.getName() + "_gc_ei_msp");

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
