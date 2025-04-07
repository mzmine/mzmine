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

package io.github.mzmine.modules.tools.batchwizard.builders;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerParameters;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaMs2CorrModule;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaMs2CorrParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDiaWizardParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.maths.Weighting;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.io.File;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

public class WizardBatchBuilderLcDIA extends BaseWizardBatchBuilder {

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
  private final Double minPearson;
  private final Integer minCorrelatedPoints;
  private final Boolean exportAnnotationGraphics;

  public WizardBatchBuilderLcDIA(WizardSequence steps) {
    super(steps);

    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.ION_INTERFACE);
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

    params = steps.get(WizardPart.WORKFLOW);
    OptionalValue<File> optional = getOptional(params, WorkflowDiaWizardParameters.exportPath);
    isExportActive = optional.active();
    exportPath = optional.value();
    exportGnps = getValue(params, WorkflowDiaWizardParameters.exportGnps);
    exportSirius = getValue(params, WorkflowDiaWizardParameters.exportSirius);
    exportAnnotationGraphics = getValue(params,
        WorkflowDiaWizardParameters.exportAnnotationGraphics);
    minPearson = getValue(params, WorkflowDiaWizardParameters.minPearson);
    minCorrelatedPoints = getValue(params, WorkflowDiaWizardParameters.minCorrelatedPoints);
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectorSteps(q);
    makeAndAddAdapChromatogramStep(q, minFeatureHeight, mzTolScans, massDetectorOption,
        minRtDataPoints, cropRtRange, polarity);
    makeAndAddSmoothingStep(q, rtSmoothing, minRtDataPoints, false);

    var groupMs2Params = createMs2GrouperParameters();
    makeAndAddRtLocalMinResolver(q, groupMs2Params, minRtDataPoints, cropRtRange, rtFwhm,
        maxIsomersInRt);

    if (isImsActive) {
      makeAndAddImsExpanderStep(q);
      makeAndAddSmoothingStep(q, false, minRtDataPoints, imsSmoothing);
      makeAndAddMobilityResolvingStep(q, groupMs2Params);
      makeAndAddSmoothingStep(q, rtSmoothing, minRtDataPoints, imsSmoothing);
    }

    makeAndAddDeisotopingStep(q, intraSampleRtTol);
    makeAndAddDiaMs2GroupingStep(q);

    makeAndAddIsotopeFinderStep(q);
    makeAndAddJoinAlignmentStep(q, interSampleRtTol);
    makeAndAddRowFilterStep(q);
    makeAndAddGapFillStep(q, interSampleRtTol, minRtDataPoints);
    makeAndAddDuplicateRowFilterStep(q, handleOriginalFeatureLists, mzTolFeaturesIntraSample,
        rtFwhm, imsInstrumentType);
    // ions annotation and feature grouping
    makeAndAddMetaCorrStep(q);
    makeAndAddIinStep(q);

    // annotation
    makeAndAddSpectralNetworkingSteps(q, isExportActive, exportPath, false);
    makeAndAddLibrarySearchStep(q, false);
    makeAndAddLocalCsvDatabaseSearchStep(q, interSampleRtTol);
    makeAndAddLipidAnnotationStep(q);
    // export
    makeAndAddDdaExportSteps(q, isExportActive, exportPath, exportGnps, exportSirius,
        exportAnnotationGraphics, mzTolScans);
    makeAndAddBatchExportStep(q, isExportActive, exportPath);
    return q;
  }

  private void makeAndAddDiaMs2GroupingStep(BatchQueue q) {
    final var param = MZmineCore.getConfiguration().getModuleParameters(DiaMs2CorrModule.class)
        .cloneParameterSet();

    param.setParameter(DiaMs2CorrParameters.minPearson, minPearson);
    param.setParameter(DiaMs2CorrParameters.flists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(DiaMs2CorrParameters.ms2ScanToScanAccuracy, mzTolScans);
    param.setParameter(DiaMs2CorrParameters.minMs1Intensity, minFeatureHeight);
    param.setParameter(DiaMs2CorrParameters.numCorrPoints, minCorrelatedPoints);
    param.setParameter(DiaMs2CorrParameters.minMs2Intensity,
        Math.max(minFeatureHeight * 0.1, massDetectorOption.getMs1NoiseLevel()));
    param.setParameter(DiaMs2CorrParameters.ms2ScanSelection,
        new ScanSelection(MsLevelFilter.of(2)));

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(DiaMs2CorrModule.class),
        param));
  }

  protected ParameterSet createMs2GrouperParameters() {
    return super.createMs2GrouperParameters(minRtDataPoints, minRtDataPoints >= 4, rtFwhm);
  }

  @Override
  protected void makeAndAddRtLocalMinResolver(final BatchQueue q, final ParameterSet groupMs2Params,
      final Integer minRtDataPoints, final Range<Double> cropRtRange, final RTTolerance rtFwhm,
      final Integer maxIsomersInRt) {
    super.makeAndAddRtLocalMinResolver(q, groupMs2Params, minRtDataPoints, cropRtRange, rtFwhm,
        maxIsomersInRt);
    disableMs2Pairing(q);
  }

  @Override
  protected void makeAndAddMobilityResolvingStep(final BatchQueue q,
      @Nullable ParameterSet groupMs2Params) {
    super.makeAndAddMobilityResolvingStep(q, groupMs2Params);

  }

  /**
   * Disables the MS2 scan pairing in the latest processing step or throws an exception if it's not
   * a resolver step.
   *
   * @param q The batch queue.
   */
  private void disableMs2Pairing(BatchQueue q) {
    final var step = q.get(q.size() - 1);
    final ParameterSet param = step.getParameterSet();
    if (!(param instanceof GeneralResolverParameters minParam)) {
      throw new IllegalStateException("Could not find resolver step to adapt for DIA data.");
    }
    minParam.setParameter(GeneralResolverParameters.groupMS2Parameters, false);
  }

  protected void makeAndAddMetaCorrStep(final BatchQueue q) {
    final boolean useCorrGrouping = minRtDataPoints > 3;
    RTTolerance rtTol = new RTTolerance(rtFwhm.getTolerance() * (useCorrGrouping ? 1.1f : 0.7f),
        rtFwhm.getUnit());
    makeAndAddMetaCorrStep(q, minRtDataPoints, rtTol, stableIonizationAcrossSamples);
  }

  /**
   * Specific for dia wizard: use ms1 and ms2
   *
   * @param q
   */
  protected void makeAndAddMobilityScanMergerStep(final BatchQueue q) {

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MobilityScanMergerModule.class).cloneParameterSet();

    param.setParameter(MobilityScanMergerParameters.mzTolerance, mzTolScans);
    param.setParameter(MobilityScanMergerParameters.scanSelection,
        new ScanSelection(MsLevelFilter.ALL_LEVELS));
    param.setParameter(MobilityScanMergerParameters.noiseLevel,
        0d); // the noise level of the mass detector already did all the filtering we want (at least in the wizard)
    param.setParameter(MobilityScanMergerParameters.mergingType, IntensityMergingType.SUMMED);
    param.setParameter(MobilityScanMergerParameters.weightingType, Weighting.LINEAR);

    final RawDataFilesSelection rawDataFilesSelection = new RawDataFilesSelection(
        RawDataFilesSelectionType.BATCH_LAST_FILES);
    param.setParameter(MobilityScanMergerParameters.rawDataFiles, rawDataFilesSelection);

    q.add(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(MobilityScanMergerModule.class),
            param));
  }
}
