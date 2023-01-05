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

package io.github.mzmine.modules.tools.batchwizard;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.DetectIsotopesParameter;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.SelectedScanTypes;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolayParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolaySmoothing;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterParameters;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterParameters.FilterMode;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderParameters;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderParameters.ScanRange;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperParameters;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.Isotope13CFilterParameters;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterChoices;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.InterSampleHeightCorrParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementParameters;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnExportAndSubmitModule;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnExportAndSubmitParameters;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportModule;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Polarity;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardChromatographyParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardChromatographyParameters.ChromatographyWorkflow;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardExportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardFilterParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardIonMobilityParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardMassSpectrometerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.MinimumFeaturesFilterParameters;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteNRelativeInt;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteNRelativeInt.Mode;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.FeatureMeasurementType;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.maths.Weighting;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarity;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarityParameters;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

/**
 * Creates a batch queue from {@link BatchWizardParameters}
 */
public class WizardBatchBuilder {

  private final File[] dataFiles;
  private final File[] libraries;
  private final Range<Double> cropRtRange;
  private final RTTolerance intraSampleRtTol;
  private final RTTolerance interSampleRtTol;
  private final Integer minRtDataPoints;
  private final Integer maxIsomersInRt;
  private final RTTolerance rtFwhm;
  private final Boolean stableIonizationAcrossSamples;
  private final Double imsFwhm;
  private final Integer minImsDataPoints;
  private final Double noiseLevelMsn;
  private final Double noiseLevelMs1;
  private final Double minFeatureHeight;
  private final MZTolerance mzTolScans;
  private final MZTolerance mzTolFeaturesIntraSample;
  private final MZTolerance mzTolInterSample;
  private final Boolean filter13C;
  private final Integer minAlignedSamples;
  private final OriginalFeatureListOption handleOriginalFeatureLists;
  private final Boolean isExportActive;
  private final Boolean exportGnps;
  private final Boolean exportSirius;
  private final File exportPath;
  private final Boolean isImsActive;
  private final MobilityType imsInstrumentType;
  private final Polarity polarity;
  private final ChromatographyWorkflow chromatographyWorkflow;
  private final Boolean imsSmoothing;
  private final Boolean rtSmoothing;

  public WizardBatchBuilder(ParameterSet wizardParams) {
    // input
    ParameterSet inParam = wizardParams.getValue(BatchWizardParameters.dataInputParams);
    dataFiles = inParam.getValue(AllSpectralDataImportParameters.fileNames);
    libraries = inParam.getValue(SpectralLibraryImportParameters.dataBaseFiles);

    // chromatography
    ParameterSet chromParam = wizardParams.getValue(BatchWizardParameters.hplcParams);
    chromatographyWorkflow = chromParam.getValue(WizardChromatographyParameters.workflow);
    rtSmoothing = chromParam.getValue(WizardChromatographyParameters.smoothing);
    cropRtRange = chromParam.getValue(WizardChromatographyParameters.cropRtRange);
    intraSampleRtTol = chromParam.getValue(WizardChromatographyParameters.intraSampleRTTolerance);
    interSampleRtTol = chromParam.getValue(WizardChromatographyParameters.interSampleRTTolerance);
    minRtDataPoints = chromParam.getValue(WizardChromatographyParameters.minNumberOfDataPoints);
    maxIsomersInRt = chromParam.getValue(
        WizardChromatographyParameters.maximumIsomersInChromatogram);
    rtFwhm = chromParam.getValue(WizardChromatographyParameters.approximateChromatographicFWHM);
    stableIonizationAcrossSamples = chromParam.getValue(
        WizardChromatographyParameters.stableIonizationAcrossSamples);

    // ion mobility IMS
    ParameterSet imsParam = wizardParams.getValue(BatchWizardParameters.imsParameters);
    isImsActive = imsParam.getValue(WizardIonMobilityParameters.imsActive);
    imsInstrumentType = imsParam.getValue(WizardIonMobilityParameters.instrumentType);
    imsFwhm = imsParam.getValue(WizardIonMobilityParameters.approximateImsFWHM);
    minImsDataPoints = imsParam.getValue(WizardIonMobilityParameters.minNumberOfDataPoints);
    imsSmoothing = imsParam.getValue(WizardIonMobilityParameters.smoothing);

    // mass spectrometer
    ParameterSet msParam = wizardParams.getValue(BatchWizardParameters.msParams);
    polarity = msParam.getValue(WizardMassSpectrometerParameters.polarity);
    noiseLevelMsn = msParam.getValue(WizardMassSpectrometerParameters.ms2NoiseLevel);
    noiseLevelMs1 = msParam.getValue(WizardMassSpectrometerParameters.ms1NoiseLevel);
    minFeatureHeight = msParam.getValue(WizardMassSpectrometerParameters.minimumFeatureHeight);
    mzTolScans = msParam.getValue(WizardMassSpectrometerParameters.scanToScanMzTolerance);
    mzTolFeaturesIntraSample = msParam.getValue(
        WizardMassSpectrometerParameters.featureToFeatureMzTolerance);
    mzTolInterSample = msParam.getValue(WizardMassSpectrometerParameters.sampleToSampleMzTolerance);

    //
    ParameterSet filterParam = wizardParams.getValue(BatchWizardParameters.filterParameters);
    filter13C = filterParam.getValue(WizardFilterParameters.filter13C);
    minAlignedSamples = filterParam.getValue(WizardFilterParameters.minNumberOfSamples);
    handleOriginalFeatureLists = filterParam.getValue(
        WizardFilterParameters.handleOriginalFeatureLists);
    //
    ParameterSet exportP = wizardParams.getValue(BatchWizardParameters.exportParameters);
    isExportActive = exportP.getValue(WizardExportParameters.exportPath);
    exportPath = exportP.getEmbeddedParameterValueIfSelectedOrElse(
        WizardExportParameters.exportPath, null);
    exportGnps = exportP.getValue(WizardExportParameters.exportGnps);
    exportSirius = exportP.getValue(WizardExportParameters.exportSirius);
  }

  /**
   * Create different workflows in {@link BatchQueue}. Workflows are defined in
   * {@link ChromatographyWorkflow}
   *
   * @return a batch queue
   */
  public BatchQueue createQueue() {
    return switch (chromatographyWorkflow) {
      case GC -> makeGcWorkflow();
      case LC -> makeLcDataDependentWorkflow();
    };
  }

  private BatchQueue makeLcDataDependentWorkflow() {
    final BatchQueue q = new BatchQueue();
    // mass detect, Chromatogram builder, smoother, local minimum resolver
    makeMassDetectChromatogramSmoothResolver(q);

    if (isImsActive) {
      q.add(makeImsExpanderStep());
      makeSmoothingStep(q, false, imsSmoothing);
      q.add(makeMobilityResolvingStep());
      makeSmoothingStep(q, rtSmoothing, imsSmoothing);
    }

    q.add(makeDeisotopingStep());
    q.add(makeIsotopeFinderStep());
    q.add(makeAlignmentStep());
    final var rowsFilter = makeRowFilterStep();
    if (rowsFilter != null) {
      q.add(rowsFilter);
    }
    q.add(makeGapFillStep());
    if (!isImsActive) { // might filter IMS resolved isomers
      q.add(makeDuplicateRowFilterStep());
    }
    q.add(makeMetaCorrStep());
    q.add(makeIinStep());

    makeLibrarySearchStep(q);
    makeExportSteps(q);
    return q;
  }

  private BatchQueue makeGcWorkflow() {
    final BatchQueue q = new BatchQueue();
    // mass detect, Chromatogram builder, smoother, local minimum resolver
    makeMassDetectChromatogramSmoothResolver(q);

    // TODO add GC specific steps

    makeLibrarySearchStep(q);
    makeExportSteps(q);
    return q;
  }

  /**
   * Mass detect - until resolver. Common elements in LC and GC workflows
   */
  private void makeMassDetectChromatogramSmoothResolver(final BatchQueue q) {
    q.add(makeImportTask());
    makeMassDetectorSteps(q);
    q.add(makeAdapStep());
    makeSmoothingStep(q, rtSmoothing, false);
    q.add(makeRtResolvingStep());
  }

  private void makeMassDetectorSteps(final BatchQueue q) {
    if (isImsActive) {
      final boolean isImsFromMzml = Arrays.stream(dataFiles)
          .anyMatch(file -> file.getName().toLowerCase().endsWith(".mzml"));
      if (!isImsFromMzml) { // == Bruker file
        q.add(makeMassDetectionStep(1, SelectedScanTypes.FRAMES));
      }
      q.add(makeMassDetectionStep(1, SelectedScanTypes.MOBLITY_SCANS));
      q.add(makeMassDetectionStep(2, SelectedScanTypes.MOBLITY_SCANS));
      if (isImsFromMzml) {
        q.add(makeMobilityScanMergerStep());
      }
    } else {
      q.add(makeMassDetectionStep(1, SelectedScanTypes.SCANS));
      q.add(makeMassDetectionStep(2, SelectedScanTypes.SCANS));
    }
  }

  private void makeExportSteps(final BatchQueue q) {
    if (isExportActive && exportPath != null) {
      if (exportGnps) {
        q.add(makeIimnGnpsExportStep());
      }
      if (exportSirius) {
        q.add(makeSiriusExportStep());
      }
    }
  }

  /**
   * Checks if at least one library file was selected
   */
  private boolean checkLibraryFiles() {
    return Arrays.stream(libraries).anyMatch(Objects::nonNull);
  }

  @Nullable
  private MZmineProcessingStep<MZmineProcessingModule> makeRowFilterStep() {
    if (!filter13C && minAlignedSamples < 2) {
      return null;
    }

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(RowsFilterModule.class).cloneParameterSet();
    param.setParameter(RowsFilterParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    String suffix = (filter13C ? "13C" : "");
    if (minAlignedSamples > 1) {
      suffix = suffix + (suffix.isEmpty() ? "" : " ") + "peak";
    }
    param.setParameter(RowsFilterParameters.SUFFIX, suffix);
    param.setParameter(RowsFilterParameters.MIN_FEATURE_COUNT, minAlignedSamples > 0);
    // TODO maybe change to a relative cutoff? 5% of samples, i.e. 0.05
    param.getParameter(RowsFilterParameters.MIN_FEATURE_COUNT).getEmbeddedParameter()
        .setValue((double) minAlignedSamples);

    param.setParameter(RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT, false);
    param.setParameter(RowsFilterParameters.ISOTOPE_FILTER_13C, filter13C);

    final Isotope13CFilterParameters filterIsoParam = param.getParameter(
        RowsFilterParameters.ISOTOPE_FILTER_13C).getEmbeddedParameters();
    filterIsoParam.setParameter(Isotope13CFilterParameters.mzTolerance, mzTolFeaturesIntraSample);
    filterIsoParam.setParameter(Isotope13CFilterParameters.maxCharge, 2);
    filterIsoParam.setParameter(Isotope13CFilterParameters.applyMinCEstimation, true);
    filterIsoParam.setParameter(Isotope13CFilterParameters.removeIfMainIs13CIsotope, true);
    filterIsoParam.setParameter(Isotope13CFilterParameters.elements, List.of(new Element("O")));

    //
    param.setParameter(RowsFilterParameters.MZ_RANGE, false);
    param.setParameter(RowsFilterParameters.RT_RANGE, false);
    param.setParameter(RowsFilterParameters.FEATURE_DURATION, false);
    param.setParameter(RowsFilterParameters.FWHM, false);
    param.setParameter(RowsFilterParameters.CHARGE, false);
    param.setParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT, false);
    param.setParameter(RowsFilterParameters.GROUPSPARAMETER, RowsFilterParameters.defaultGrouping);
    param.setParameter(RowsFilterParameters.HAS_IDENTITIES, false);
    param.setParameter(RowsFilterParameters.IDENTITY_TEXT, false);
    param.setParameter(RowsFilterParameters.COMMENT_TEXT, false);
    param.setParameter(RowsFilterParameters.REMOVE_ROW, RowsFilterChoices.KEEP_MATCHING);
    param.setParameter(RowsFilterParameters.MS2_Filter, false);
    param.setParameter(RowsFilterParameters.KEEP_ALL_MS2, true);
    param.setParameter(RowsFilterParameters.Reset_ID, false);
    param.setParameter(RowsFilterParameters.massDefect, false);
    param.setParameter(RowsFilterParameters.handleOriginal, handleOriginalFeatureLists);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(RowsFilterModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeGapFillStep() {
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

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MultiThreadPeakFinderModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeDuplicateRowFilterStep() {
    // reduced rt tolerance - after gap filling the rt difference should be very small
    RTTolerance rtTol = new RTTolerance(rtFwhm.getTolerance() * 0.7f, rtFwhm.getUnit());

    MZTolerance mzTol = mzTolFeaturesIntraSample;
    mzTol = new MZTolerance(mzTol.getMzTolerance() / 2f, mzTol.getPpmTolerance() / 2f);

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(DuplicateFilterModule.class).cloneParameterSet();

    param.setParameter(DuplicateFilterParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // going back into scans so rather use scan mz tol
    param.setParameter(DuplicateFilterParameters.mzDifferenceMax, mzTol);
    param.setParameter(DuplicateFilterParameters.rtDifferenceMax, rtTol);
    param.setParameter(DuplicateFilterParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(DuplicateFilterParameters.suffix, "dup");
    param.setParameter(DuplicateFilterParameters.requireSameIdentification, false);
    param.setParameter(DuplicateFilterParameters.filterMode, FilterMode.NEW_AVERAGE);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(DuplicateFilterModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeImportTask() {
    // todo make auto mass detector work, so we can use it here.
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(AllSpectralDataImportModule.class).cloneParameterSet();
    param.getParameter(AllSpectralDataImportParameters.advancedImport).setValue(false);
    param.getParameter(AllSpectralDataImportParameters.fileNames).setValue(dataFiles);
    param.getParameter(SpectralLibraryImportParameters.dataBaseFiles).setValue(libraries);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(AllSpectralDataImportModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMassDetectionStep(int msLevel,
      SelectedScanTypes scanTypes) {

    final ParameterSet detectorParam = MZmineCore.getConfiguration()
        .getModuleParameters(AutoMassDetector.class).cloneParameterSet();

    final double noiseLevel;
    if (msLevel == 1 && scanTypes == SelectedScanTypes.MOBLITY_SCANS) {
      noiseLevel = noiseLevelMs1 / 10; // lower threshold for mobility scans
    } else if (msLevel >= 2) {
      noiseLevel = noiseLevelMsn;
    } else {
      noiseLevel = noiseLevelMs1;
    }
    detectorParam.setParameter(AutoMassDetectorParameters.noiseLevel, noiseLevel);

    // per default do not detect isotope signals below noise. this might introduce too many signals
    // for the isotope finder later on and confuse users
    detectorParam.setParameter(AutoMassDetectorParameters.detectIsotopes, false);
    final DetectIsotopesParameter detectIsotopesParameter = detectorParam.getParameter(
        AutoMassDetectorParameters.detectIsotopes).getEmbeddedParameters();
    detectIsotopesParameter.setParameter(DetectIsotopesParameter.elements,
        List.of(new Element("H"), new Element("C"), new Element("N"), new Element("O"),
            new Element("S")));
    detectIsotopesParameter.setParameter(DetectIsotopesParameter.isotopeMzTolerance,
        mzTolFeaturesIntraSample);
    detectIsotopesParameter.setParameter(DetectIsotopesParameter.maxCharge, 2);

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MassDetectionModule.class).cloneParameterSet();
    param.setParameter(MassDetectionParameters.dataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    param.setParameter(MassDetectionParameters.scanSelection, new ScanSelection(msLevel));
    param.setParameter(MassDetectionParameters.scanTypes, scanTypes);
    param.setParameter(MassDetectionParameters.massDetector,
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(AutoMassDetector.class),
            detectorParam));

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(MassDetectionModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMobilityScanMergerStep() {

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MobilityScanMergerModule.class).cloneParameterSet();

    param.setParameter(MobilityScanMergerParameters.mzTolerance, mzTolScans);
    param.setParameter(MobilityScanMergerParameters.scanSelection, new ScanSelection());
    param.setParameter(MobilityScanMergerParameters.noiseLevel,
        0d); // the noise level of the mass detector already did all the filtering we want (at least in the wizard)
    param.setParameter(MobilityScanMergerParameters.mergingType, IntensityMergingType.SUMMED);
    param.setParameter(MobilityScanMergerParameters.weightingType, Weighting.LINEAR);

    final RawDataFilesSelection rawDataFilesSelection = new RawDataFilesSelection(
        RawDataFilesSelectionType.BATCH_LAST_FILES);
    param.setParameter(MobilityScanMergerParameters.rawDataFiles, rawDataFilesSelection);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MobilityScanMergerModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeAdapStep() {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ModularADAPChromatogramBuilderModule.class).cloneParameterSet();
    param.setParameter(ADAPChromatogramBuilderParameters.dataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    // crop rt range
    param.setParameter(ADAPChromatogramBuilderParameters.scanSelection,
        new ScanSelection(RangeUtils.toFloatRange(cropRtRange), 1));
    param.setParameter(ADAPChromatogramBuilderParameters.minimumScanSpan, minRtDataPoints);
    param.setParameter(ADAPChromatogramBuilderParameters.mzTolerance, mzTolScans);
    param.setParameter(ADAPChromatogramBuilderParameters.suffix, "chroms");
    param.setParameter(ADAPChromatogramBuilderParameters.minGroupIntensity, noiseLevelMs1);
    param.setParameter(ADAPChromatogramBuilderParameters.minHighestPoint, minFeatureHeight);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ModularADAPChromatogramBuilderModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeImsExpanderStep() {
    ParameterSet param = MZmineCore.getConfiguration().getModuleParameters(ImsExpanderModule.class)
        .cloneParameterSet();

    param.setParameter(ImsExpanderParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(ImsExpanderParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ImsExpanderParameters.useRawData, true);
    param.getParameter(ImsExpanderParameters.useRawData).getEmbeddedParameter().setValue(1E1);
    param.setParameter(ImsExpanderParameters.mzTolerance, true);
    param.getParameter(ImsExpanderParameters.mzTolerance).getEmbeddedParameter()
        .setValue(mzTolScans);
    param.setParameter(ImsExpanderParameters.mobilogramBinWidth, false);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ImsExpanderModule.class),
        param);
  }

  private void makeSmoothingStep(final BatchQueue q, final boolean rt, final boolean mobility) {
    if (!rt && !mobility) {
      return;
    }

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(SmoothingModule.class).cloneParameterSet();
    param.setParameter(SmoothingParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));

    ParameterSet sgParam = MZmineCore.getConfiguration()
        .getModuleParameters(SavitzkyGolaySmoothing.class).cloneParameterSet();
    sgParam.setParameter(SavitzkyGolayParameters.rtSmoothing, rt);
    sgParam.getParameter(SavitzkyGolayParameters.rtSmoothing).getEmbeddedParameter()
        .setValue(minRtDataPoints < 6 ? 5 : Math.min(21, minRtDataPoints / 2 * 2 + 1));
    sgParam.setParameter(SavitzkyGolayParameters.mobilitySmoothing, mobility);
    // next odd number
    sgParam.getParameter(SavitzkyGolayParameters.mobilitySmoothing).getEmbeddedParameter()
        .setValue(minImsDataPoints < 6 ? 5 : Math.min(21, minImsDataPoints / 2 * 2 + 1));

    param.getParameter(SmoothingParameters.smoothingAlgorithm).setValue(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(SavitzkyGolaySmoothing.class),
            sgParam));
    param.setParameter(SmoothingParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(SmoothingParameters.suffix, "sm");

    MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(SmoothingModule.class), param);
    q.add(step);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeRtResolvingStep() {
    // TODO check if still needed
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

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MinimumSearchFeatureResolverModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMobilityResolvingStep() {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, "r");
    param.setParameter(MinimumSearchFeatureResolverParameters.handleOriginal,
        handleOriginalFeatureLists);

    param.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.mzTol, mzTolScans);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.combineTimsMsMs, false);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.limitRTByFeature, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters()
        .setParameter(GroupMS2SubParameters.lockMS2ToFeatureMobilityRange, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters()
        .setParameter(GroupMS2SubParameters.rtTol, new RTTolerance(5, Unit.SECONDS));
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.outputNoiseLevel, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().getParameter(GroupMS2SubParameters.outputNoiseLevel)
        .getEmbeddedParameter().setValue(noiseLevelMsn * 2);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.outputNoiseLevelRelative, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().getParameter(GroupMS2SubParameters.outputNoiseLevelRelative)
        .getEmbeddedParameter().setValue(0.01);

    param.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.MOBILITY);
    param.setParameter(MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL,
        0.80);
    param.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE, imsFwhm);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT, 0d);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT,
        minFeatureHeight);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO, 1.8d);
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION, Range.closed(0d, 10d));
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS, 5);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MinimumSearchFeatureResolverModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeDeisotopingStep() {
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

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IsotopeGrouperModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeIsotopeFinderStep() {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IsotopeFinderModule.class).cloneParameterSet();

    param.setParameter(IsotopeFinderParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(IsotopeFinderParameters.isotopeMzTolerance, mzTolFeaturesIntraSample);
    param.setParameter(IsotopeFinderParameters.maxCharge, 1);
    param.setParameter(IsotopeFinderParameters.scanRange, ScanRange.SINGLE_MOST_INTENSE);
    param.setParameter(IsotopeFinderParameters.elements,
        List.of(new Element("H"), new Element("C"), new Element("N"), new Element("O"),
            new Element("S")));

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IsotopeFinderModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeAlignmentStep() {
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

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(JoinAlignerModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMetaCorrStep() {
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

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(CorrelateGroupingModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeIinStep() {
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

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IonNetworkingModule.class),
        param);
  }


  private void makeLibrarySearchStep(final BatchQueue q) {
    if (!checkLibraryFiles()) {
      return;
    }

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(SpectralLibrarySearchModule.class);

    param.setParameter(SpectralLibrarySearchParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(SpectralLibrarySearchParameters.libraries, new SpectralLibrarySelection());
    param.setParameter(SpectralLibrarySearchParameters.msLevel, 2);
    param.setParameter(SpectralLibrarySearchParameters.allMS2Spectra, false);
    param.setParameter(SpectralLibrarySearchParameters.cropSpectraToOverlap, false);
    param.setParameter(SpectralLibrarySearchParameters.mzTolerancePrecursor,
        new MZTolerance(0.01, 20));
    param.setParameter(SpectralLibrarySearchParameters.mzTolerance, new MZTolerance(0.01, 20));
    param.setParameter(SpectralLibrarySearchParameters.removePrecursor, true);
    param.setParameter(SpectralLibrarySearchParameters.deisotoping, false);
    param.setParameter(SpectralLibrarySearchParameters.noiseLevel, 0d);
    param.setParameter(SpectralLibrarySearchParameters.needsIsotopePattern, false);
    param.setParameter(SpectralLibrarySearchParameters.rtTolerance, false);
    param.setParameter(SpectralLibrarySearchParameters.minMatch, 4);
    // similarity
    ModuleComboParameter<SpectralSimilarityFunction> simFunction = param.getParameter(
        SpectralLibrarySearchParameters.similarityFunction);

    ParameterSet weightedCosineParam = MZmineCore.getConfiguration()
        .getModuleParameters(WeightedCosineSpectralSimilarity.class).cloneParameterSet();
    weightedCosineParam.setParameter(WeightedCosineSpectralSimilarityParameters.weight,
        Weights.MASSBANK);
    weightedCosineParam.setParameter(WeightedCosineSpectralSimilarityParameters.weight,
        Weights.MASSBANK);
    weightedCosineParam.setParameter(WeightedCosineSpectralSimilarityParameters.minCosine, 0.65);
    weightedCosineParam.setParameter(WeightedCosineSpectralSimilarityParameters.handleUnmatched,
        HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO);

    SpectralSimilarityFunction weightedCosineModule = SpectralSimilarityFunction.weightedCosine;
    var libMatchStep = new MZmineProcessingStepImpl<>(weightedCosineModule, weightedCosineParam);

    // finally set the libmatch module plus parameters as step
    simFunction.setValue(libMatchStep);
    // IMS
    param.setParameter(SpectralLibrarySearchParameters.ccsTolerance, false);
    param.getParameter(SpectralLibrarySearchParameters.ccsTolerance).getEmbeddedParameter()
        .setValue(0.05);

    MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(SpectralLibrarySearchModule.class), param);
    q.add(step);
  }

  // export
  private MZmineProcessingStep<MZmineProcessingModule> makeIimnGnpsExportStep() {
    final ParameterSet param = new GnpsFbmnExportAndSubmitParameters().cloneParameterSet();

    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(), fileName.getName() + "_iimn_gnps");

    param.setParameter(GnpsFbmnExportAndSubmitParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // going back into scans so rather use scan mz tol
    param.setParameter(GnpsFbmnExportAndSubmitParameters.MERGE_PARAMETER, false);
    param.setParameter(GnpsFbmnExportAndSubmitParameters.SUBMIT, false);
    param.setParameter(GnpsFbmnExportAndSubmitParameters.OPEN_FOLDER, false);
    param.setParameter(GnpsFbmnExportAndSubmitParameters.FEATURE_INTENSITY,
        FeatureMeasurementType.AREA);
    param.setParameter(GnpsFbmnExportAndSubmitParameters.FILENAME, fileName);
    param.setParameter(GnpsFbmnExportAndSubmitParameters.FILTER,
        FeatureListRowsFilter.MS2_OR_ION_IDENTITY);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(GnpsFbmnExportAndSubmitModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeSiriusExportStep() {
    final ParameterSet param = new SiriusExportParameters().cloneParameterSet();

    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(), fileName.getName() + "_sirius.mgf");

    param.setParameter(SiriusExportParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // going back into scans so rather use scan mz tol
    param.setParameter(SiriusExportParameters.MERGE_PARAMETER, false);
    param.setParameter(SiriusExportParameters.EXCLUDE_EMPTY_MSMS, false);
    param.setParameter(SiriusExportParameters.EXCLUDE_MULTICHARGE, false);
    param.setParameter(SiriusExportParameters.EXCLUDE_MULTIMERS, false);
    param.setParameter(SiriusExportParameters.RENUMBER_ID, false);
    param.setParameter(SiriusExportParameters.NEED_ANNOTATION, false);
    param.setParameter(SiriusExportParameters.MZ_TOL, new MZTolerance(0.002, 5));
    param.setParameter(SiriusExportParameters.FILENAME, fileName);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(SiriusExportModule.class),
        param);
  }

}
