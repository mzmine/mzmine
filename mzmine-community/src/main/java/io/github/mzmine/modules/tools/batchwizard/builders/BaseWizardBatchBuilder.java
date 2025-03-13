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

import static io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportParameters;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.autosave.AutoSaveBatchModule;
import io.github.mzmine.modules.batchmode.autosave.AutoSaveBatchParameters;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters;
import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters.ChimericMsOption;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.SelectedScanTypes;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.FeatureSmoothingOptions;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolayParameters;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterParameters;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterParameters.FilterMode;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Module;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Parameters;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMs2AdvancedParameters;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderParameters;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderParameters.ScanRange;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperParameters;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.Isotope13CFilterParameters;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterChoices;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.InputSpectraSelectParameters.SelectInputScans;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderModule;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.MultiThreadPeakFinderParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.InterSampleHeightCorrParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralNetworkingOptions;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.cosine_no_precursor.NoPrecursorCosineSpectralNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.AdvancedLipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationMSMSParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClassesProvider;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchModule;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.AdvancedSpectralLibrarySearchParameters;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchParameters;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.library_to_featurelist.SpectralLibraryToFeatureListModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.library_to_featurelist.SpectralLibraryToFeatureListParameters;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration.RTCorrectionModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration.RTCorrectionParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.export_compoundAnnotations_csv.CompoundAnnotationsCSVExportModule;
import io.github.mzmine.modules.io.export_compoundAnnotations_csv.CompoundAnnotationsCSVExportParameters;
import io.github.mzmine.modules.io.export_features_all_speclib_matches.ExportAllIdsGraphicalModule;
import io.github.mzmine.modules.io.export_features_all_speclib_matches.ExportAllIdsGraphicalParameters;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularModule;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularParameters;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnExportAndSubmitModule;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnExportAndSubmitParameters;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportModule;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportParameters;
import io.github.mzmine.modules.io.export_network_graphml.NetworkGraphMlExportModule;
import io.github.mzmine.modules.io.export_network_graphml.NetworkGraphMlExportParameters;
import io.github.mzmine.modules.io.export_scans_modular.AdvancedExportScansFeatureParameters;
import io.github.mzmine.modules.io.export_scans_modular.ExportScansFeatureMainParameters;
import io.github.mzmine.modules.io.export_scans_modular.ExportScansFeatureModule;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportModule;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationModule;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchMetadataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryExportQualityParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.SpectralLibraryExportFormats;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationLocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationLocalCSVDatabaseSearchParameters.MassOptions;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.FilterWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonMobilityWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDiaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMassDetectorNoiseLevels;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMsPolarity;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportModule;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportParameters;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportParameters.MetadataFileFormat;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizer;
import io.github.mzmine.parameters.parametertypes.MinimumFeaturesFilterParameters;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt.Mode;
import io.github.mzmine.parameters.parametertypes.combowithinput.FeatureLimitOptions;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter.Options;
import io.github.mzmine.parameters.parametertypes.combowithinput.RtLimitsFilter;
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
import io.github.mzmine.util.DimensionUnitUtil.DimUnit;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.maths.Weighting;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunctions;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarityParameters;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

/**
 * This class defines all parameters that are equal in all workflows. Data import annotation etc.
 */
public abstract class BaseWizardBatchBuilder extends WizardBatchBuilder {

  // only parameters that are used in all workflows
  // input
  protected final File[] dataFiles;
  protected final OptionalValue<File> metadataFile;
  // annotation
  protected final File[] libraries;
  //filter
  protected final boolean filter13C;
  protected final AbsoluteAndRelativeInt minAlignedSamples;
  protected final OriginalFeatureListOption handleOriginalFeatureLists;
  // IMS parameter currently all the same
  protected final boolean isImsActive;
  protected final boolean isNativeIms;
  protected final boolean imsSmoothing;
  protected final MobilityType imsInstrumentType;
  protected final Integer minImsDataPoints;
  protected final Double imsFwhm;
  protected final MobilityTolerance imsFwhmMobTolerance;

  //Imaging
  protected final boolean isImaging;

  // MS parameters currently all the same
  protected final WizardMassDetectorNoiseLevels massDetectorOption;
  protected final Double minFeatureHeight;
  protected final MZTolerance mzTolScans;
  protected final MZTolerance mzTolFeaturesIntraSample;
  protected final MZTolerance mzTolInterSample;
  protected final WizardMsPolarity polarity;
  // csv database
  private final boolean checkLocalCsvDatabase;
  // lipid annotation
  private final boolean annotateLipids;
  private @NotNull String csvFilterSamplesColumn = "";
  private MassOptions csvMassOptions;
  private List<ImportType> csvColumns;
  protected File csvLibraryFile;

  protected BaseWizardBatchBuilder(final WizardSequence steps) {
    super(steps);
    // input
    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.DATA_IMPORT);
    dataFiles = getValue(params, DataImportWizardParameters.fileNames);
    metadataFile = getOptional(params, DataImportWizardParameters.metadataFile);

    // annotation
    params = steps.get(WizardPart.ANNOTATION);
    libraries = getValue(params, SpectralLibraryImportParameters.dataBaseFiles);

    var csvOptional = getOptionalParameters(params, AnnotationWizardParameters.localCsvSearch);
    checkLocalCsvDatabase = csvOptional.active();
    var csvParams = csvOptional.value();
    if (checkLocalCsvDatabase) {
      csvColumns = csvParams.getValue(LocalCSVDatabaseSearchParameters.columns);
      csvLibraryFile = csvParams.getValue(LocalCSVDatabaseSearchParameters.dataBaseFile);
      csvMassOptions = csvParams.getValue(
          AnnotationLocalCSVDatabaseSearchParameters.massOptionsComboParameter);
      csvFilterSamplesColumn = csvParams.getEmbeddedParameterValueIfSelectedOrElse(
          AnnotationLocalCSVDatabaseSearchParameters.filterSamplesColumn, "");
    }

    annotateLipids = getValue(params, AnnotationWizardParameters.lipidAnnotation);

    // filter
    params = steps.get(WizardPart.FILTER);
    filter13C = getValue(params, FilterWizardParameters.filter13C);
    minAlignedSamples = getValue(params, FilterWizardParameters.minNumberOfSamples);
    handleOriginalFeatureLists = getValue(params,
        FilterWizardParameters.handleOriginalFeatureLists);

    // ion mobility IMS
    params = steps.get(WizardPart.IMS);
    isImsActive = getValue(params, IonMobilityWizardParameters.imsActive);
    imsInstrumentType = getValue(params, IonMobilityWizardParameters.instrumentType);
    minImsDataPoints = getValue(params, IonMobilityWizardParameters.minNumberOfDataPoints);
    imsSmoothing = getValue(params, IonMobilityWizardParameters.smoothing);
    imsFwhm = getValue(params, IonMobilityWizardParameters.approximateImsFWHM);
    imsFwhmMobTolerance = new MobilityTolerance(imsFwhm.floatValue());
    isNativeIms = Arrays.stream(dataFiles).map(RawDataFileTypeDetector::detectDataFileType)
        .allMatch(type -> type == RawDataFileType.BRUKER_TDF);

    // Imaging
    isImaging = steps.isImaging();

    // mass spectrometer
    params = steps.get(WizardPart.MS);
    polarity = getValue(params, MassSpectrometerWizardParameters.polarity);
    massDetectorOption = getValue(params, MassSpectrometerWizardParameters.massDetectorOption);
    minFeatureHeight = getValue(params, MassSpectrometerWizardParameters.minimumFeatureHeight);
    mzTolScans = getValue(params, MassSpectrometerWizardParameters.scanToScanMzTolerance);
    mzTolFeaturesIntraSample = getValue(params,
        MassSpectrometerWizardParameters.featureToFeatureMzTolerance);
    mzTolInterSample = getValue(params, MassSpectrometerWizardParameters.sampleToSampleMzTolerance);
  }

  // #################################################################################
  // create various steps

  protected static void makeAndAddDuplicateRowFilterStep(final BatchQueue q,
      final OriginalFeatureListOption handleOriginalFeatureLists,
      final MZTolerance mzTolFeaturesIntraSample, final RTTolerance rtFwhm,
      final MobilityType mobilityType) {
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
    param.setParameter(DuplicateFilterParameters.mobilityDifferenceMax,
        mobilityType != MobilityType.NONE,
        new MobilityTolerance(mobilityType == MobilityType.TIMS ? 0.008f : 1f));
    param.setParameter(DuplicateFilterParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(DuplicateFilterParameters.suffix, "dup");
    param.setParameter(DuplicateFilterParameters.requireSameIdentification, false);
    param.setParameter(DuplicateFilterParameters.filterMode, FilterMode.NEW_AVERAGE);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(DuplicateFilterModule.class),
        param));
  }

  protected static void makeAndAddAdapChromatogramStep(final BatchQueue q,
      final Double minFeatureHeight, final MZTolerance mzTolScans,
      final WizardMassDetectorNoiseLevels massDetectorOption, final Integer minRtDataPoints,
      @Nullable final Range<Double> cropRtRange, WizardMsPolarity polarity) {
    MassDetectorWizardOptions detectorType = massDetectorOption.getValueType();

    double noiseLevelMs1;
    if (detectorType == MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL) {
      noiseLevelMs1 = massDetectorOption.getMs1NoiseLevel() * 2d;
    } else {
      noiseLevelMs1 = minFeatureHeight / 5d;
    }

    noiseLevelMs1 = Math.min(minFeatureHeight, noiseLevelMs1);

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ModularADAPChromatogramBuilderModule.class).cloneParameterSet();
    param.setParameter(ADAPChromatogramBuilderParameters.dataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    // crop rt range
    var scanSelection = new ScanSelection(cropRtRange, 1, polarity.toScanPolaritySelection());
    param.setParameter(ADAPChromatogramBuilderParameters.scanSelection, scanSelection);

    param.setParameter(ADAPChromatogramBuilderParameters.minimumConsecutiveScans, minRtDataPoints);
    param.setParameter(ADAPChromatogramBuilderParameters.mzTolerance, mzTolScans);
    param.setParameter(ADAPChromatogramBuilderParameters.suffix, "eics");
    param.setParameter(ADAPChromatogramBuilderParameters.minGroupIntensity, noiseLevelMs1);
    param.setParameter(ADAPChromatogramBuilderParameters.minHighestPoint, minFeatureHeight);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ModularADAPChromatogramBuilderModule.class), param));
  }

  protected static void makeAndAddDdaExportSteps(final BatchQueue q, final WizardSequence steps,
      final MZTolerance mzTolScans) {
    // DDA workflow parameters
    var params = steps.get(WizardPart.WORKFLOW);
    OptionalValue<File> optional = getOptional(params, WorkflowDdaWizardParameters.exportPath);
    boolean isExportActive = optional.active();
    File exportPath = optional.value();
    boolean exportGnps = getValue(params, WorkflowDdaWizardParameters.exportGnps);
    boolean exportSirius = getValue(params, WorkflowDdaWizardParameters.exportSirius);
    boolean exportAnnotationGraphics = getValue(params,
        WorkflowDdaWizardParameters.exportAnnotationGraphics);
    makeAndAddDdaExportSteps(q, isExportActive, exportPath, exportGnps, exportSirius,
        exportAnnotationGraphics, mzTolScans);
  }

  // export for DDA
  protected static void makeAndAddDdaExportSteps(final BatchQueue q, final boolean isExportActive,
      final File exportPath, final boolean exportGnps, final boolean exportSirius,
      final boolean exportAnnotationGraphics, final MZTolerance mzTolScans) {
    if (isExportActive && exportPath != null) {
      makeAndAddProjectMetadataExport(q, exportPath);
      makeAndAddCsvModularExportStep(q, exportPath);
      if (exportGnps) {
        makeAndAddIimnGnpsExportStep(q, exportPath, mzTolScans, "_iimn_gnps");
      }
      if (exportSirius) {
        makeAndAddSiriusExportStep(q, exportPath);
      }
      makeAndAddAllAnnotationExportStep(q, exportPath);
      // have this last as it might fail
      if (exportAnnotationGraphics) {
        makeAndAddAnnotationGraphicsExportStep(q, exportPath);
      }
    }
  }

  public static void makeAndAddCsvModularExportStep(final BatchQueue q, final File exportPath) {
    final ParameterSet param = new CSVExportModularParameters().cloneParameterSet();

    param.setParameter(CSVExportModularParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(CSVExportModularParameters.fieldSeparator, ",");
    param.setParameter(CSVExportModularParameters.idSeparator, ";");
    param.setParameter(CSVExportModularParameters.omitEmptyColumns, true);
    param.setParameter(CSVExportModularParameters.filter, FeatureListRowsFilter.ALL);

    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(), fileName.getName() + "_full_feature_table.csv");
    param.setParameter(CSVExportModularParameters.filename, fileName);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(CSVExportModularModule.class),
        param));
  }

  public static void makeAndAddAnnotationGraphicsExportStep(final BatchQueue q,
      final File exportPath) {
    final ParameterSet param = new ExportAllIdsGraphicalParameters().cloneParameterSet();

    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(), "graphics");

    param.setParameter(ExportAllIdsGraphicalParameters.flists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // going back into scans so rather use scan mz tol
    param.setParameter(ExportAllIdsGraphicalParameters.exportSpectralLibMatches, true);

    param.setParameter(ExportAllIdsGraphicalParameters.exportLipidMatches, false);
    param.setParameter(ExportAllIdsGraphicalParameters.exportMobilogram, false);
    param.setParameter(ExportAllIdsGraphicalParameters.exportImages, false);
    param.setParameter(ExportAllIdsGraphicalParameters.exportShape, false);
    // formats
    param.setParameter(ExportAllIdsGraphicalParameters.exportPdf, true);
    param.setParameter(ExportAllIdsGraphicalParameters.exportPng, false);
    param.setParameter(ExportAllIdsGraphicalParameters.dir, fileName);
    param.setParameter(ExportAllIdsGraphicalParameters.dpiScalingFactor, 3);
    param.setParameter(ExportAllIdsGraphicalParameters.numMatches, 1);
    //
    GraphicsExportParameters exp = param.getValue(ExportAllIdsGraphicalParameters.export);
    exp.setParameter(GraphicsExportParameters.unit, DimUnit.MM);
    exp.setParameter(GraphicsExportParameters.width, 180d);
    exp.setParameter(GraphicsExportParameters.height, true, 60d);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ExportAllIdsGraphicalModule.class), param));
  }

  protected static void makeAndAddProjectMetadataExport(final BatchQueue q, final File exportPath) {
    final ParameterSet param = ProjectMetadataExportParameters.create(exportPath, true,
        MetadataFileFormat.DEFAULT);
    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ProjectMetadataExportModule.class), param));
  }

  protected static void makeAndAddIimnGnpsExportStep(final BatchQueue q, final File exportPath,
      final MZTolerance mzTolScans, final String fileNameSuffix) {
    final ParameterSet param = new GnpsFbmnExportAndSubmitParameters().cloneParameterSet();

    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(), fileName.getName() + fileNameSuffix);

    param.setParameter(GnpsFbmnExportAndSubmitParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.getParameter(GnpsFbmnExportAndSubmitParameters.spectraMergeSelect)
        .setSimplePreset(SpectraMergeSelectPresets.SINGLE_MERGED_SCAN, mzTolScans);

    param.setParameter(GnpsFbmnExportAndSubmitParameters.NORMALIZER,
        IntensityNormalizer.createDefault());
    param.setParameter(GnpsFbmnExportAndSubmitParameters.SUBMIT, false);
    param.setParameter(GnpsFbmnExportAndSubmitParameters.OPEN_FOLDER, false);
    param.setParameter(GnpsFbmnExportAndSubmitParameters.FEATURE_INTENSITY, AbundanceMeasure.Area);
    param.setParameter(GnpsFbmnExportAndSubmitParameters.FILENAME, fileName);
    param.setParameter(GnpsFbmnExportAndSubmitParameters.FILTER,
        FeatureListRowsFilter.MS2_OR_ION_IDENTITY);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(GnpsFbmnExportAndSubmitModule.class), param));
  }

  protected static void makeAndAddSiriusExportStep(final BatchQueue q, final File exportPath) {
    final ParameterSet param = new SiriusExportParameters().cloneParameterSet();

    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(), fileName.getName() + "_sirius.mgf");

    param.setParameter(SiriusExportParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // going back into scans so rather use scan mz tol
    param.getParameter(SiriusExportParameters.spectraMergeSelect)
        .setUseInputScans(SelectInputScans.ALL_SCANS);
    param.setParameter(SiriusExportParameters.NORMALIZE, IntensityNormalizer.createScientific());
    param.setParameter(SiriusExportParameters.EXCLUDE_MULTICHARGE, false);
    param.setParameter(SiriusExportParameters.EXCLUDE_MULTIMERS, false);
    param.setParameter(SiriusExportParameters.NEED_ANNOTATION, false);
    param.setParameter(SiriusExportParameters.MZ_TOL, new MZTolerance(0.002, 5));
    param.setParameter(SiriusExportParameters.FILENAME, fileName);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(SiriusExportModule.class),
        param));
  }

  protected static void makeAndAddAllAnnotationExportStep(final BatchQueue q,
      final File exportPath) {
    final ParameterSet param = new CompoundAnnotationsCSVExportParameters().cloneParameterSet();

    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(), fileName.getName() + "_annotations.csv");

    param.setParameter(CompoundAnnotationsCSVExportParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(CompoundAnnotationsCSVExportParameters.topNMatches, 10);
    param.setParameter(CompoundAnnotationsCSVExportParameters.filename, fileName);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(CompoundAnnotationsCSVExportModule.class), param));
  }

  protected void makeAndAddRtLocalMinResolver(final BatchQueue q, final ParameterSet groupMs2Params,
      final Integer minRtDataPoints, final Range<Double> cropRtRange, final RTTolerance rtFwhm,
      final Integer maxIsomersInRt) {
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

  protected void makeAndAddDeisotopingStep(final BatchQueue q, final @Nullable RTTolerance rtTol) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IsotopeGrouperModule.class).cloneParameterSet();

    param.setParameter(IsotopeGrouperParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(IsotopeGrouperParameters.suffix, "deiso");
    param.setParameter(IsotopeGrouperParameters.mzTolerance, mzTolFeaturesIntraSample);
    param.setParameter(IsotopeGrouperParameters.rtTolerance,
        Objects.requireNonNullElse(rtTol, new RTTolerance(9999999, Unit.MINUTES)));
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

  protected void makeAndAddJoinAlignmentStep(final BatchQueue q,
      final @Nullable RTTolerance rtTol) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(JoinAlignerModule.class).cloneParameterSet();
    param.setParameter(JoinAlignerParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(JoinAlignerParameters.peakListName, "Aligned feature list");
    param.setParameter(JoinAlignerParameters.MZTolerance, mzTolInterSample);
    param.setParameter(JoinAlignerParameters.MZWeight, 3d);
    // RT tolerance is not needed for some workflows
    param.setParameter(JoinAlignerParameters.RTTolerance,
        Objects.requireNonNullElse(rtTol, new RTTolerance(9999999, Unit.MINUTES)));
    param.setParameter(JoinAlignerParameters.RTWeight, rtTol != null ? 1d : 0d);
    // IMS
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

  /**
   * @param minRtDataPoints The minimum rt data points used during initial feature finding. This
   *                        value is slightly reduced in this method  for the gap filling (3/5).
   */
  protected void makeAndAddGapFillStep(final BatchQueue q, final @Nullable RTTolerance rtTol,
      final int minRtDataPoints) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MultiThreadPeakFinderModule.class).cloneParameterSet();

    param.setParameter(MultiThreadPeakFinderParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // going back into scans so rather use scan mz tol
    param.setParameter(MultiThreadPeakFinderParameters.MZTolerance, mzTolScans);
    param.setParameter(MultiThreadPeakFinderParameters.RTTolerance,
        Objects.requireNonNullElse(rtTol, new RTTolerance(9999999, Unit.MINUTES)));
    param.setParameter(MultiThreadPeakFinderParameters.intTolerance, 0.2);
    param.setParameter(MultiThreadPeakFinderParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(MultiThreadPeakFinderParameters.suffix, "gaps");
    param.setParameter(MultiThreadPeakFinderParameters.minDataPoints, minRtDataPoints * 3 / 5);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MultiThreadPeakFinderModule.class), param));
  }

  protected void makeAndAddMetaCorrStep(final BatchQueue q, final int minRtDataPoints,
      final @Nullable RTTolerance rtTol, final boolean isStableIonization) {
    final boolean useCorrGrouping = minRtDataPoints > 3;

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(CorrelateGroupingModule.class).cloneParameterSet();
    param.setParameter(CorrelateGroupingParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));

    // for now we set this to keep as this was the initial state before handle original parameter was added
    // some workflows depend on the inital feature list to be present
    param.setParameter(CorrelateGroupingParameters.handleOriginal, OriginalFeatureListOption.KEEP);

    param.setParameter(CorrelateGroupingParameters.RT_TOLERANCE,
        Objects.requireNonNullElse(rtTol, new RTTolerance(9999999, Unit.MINUTES)));
    param.setParameter(CorrelateGroupingParameters.MIN_HEIGHT, 0d);
    param.setParameter(CorrelateGroupingParameters.NOISE_LEVEL,
        massDetectorOption.getMs1NoiseLevel());

    // min samples
    var minSampleP = param.getParameter(CorrelateGroupingParameters.MIN_SAMPLES_FILTER)
        .getEmbeddedParameters();
    minSampleP.setParameter(MinimumFeaturesFilterParameters.MIN_SAMPLES_GROUP,
        new AbsoluteAndRelativeInt(0, 0, Mode.ROUND_DOWN));
    minSampleP.setParameter(MinimumFeaturesFilterParameters.MIN_SAMPLES_ALL, minAlignedSamples);
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
    param.setParameter(CorrelateGroupingParameters.IMAX_CORRELATION, isStableIonization);
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

  /**
   * Ion identity networking
   */
  protected void makeAndAddIinStep(final BatchQueue q) {
    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IonNetworkingModule.class).cloneParameterSet();
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
    createAndSetIonLibrary(ionLibraryParam);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IonNetworkingModule.class),
        param));
  }

  private void createAndSetIonLibrary(final IonLibraryParameterSet ionLibraryParam) {
    Set<IonModification> adducts = new HashSet<>();
    Set<IonModification> adductChoices = new HashSet<>();
    // No filter or Positive option --> add positive
    if (polarity.toScanPolaritySelection().includesPositive()) {
      // default positive
      Collections.addAll(adducts, IonModification.H, IonModification.H_H2O_1, IonModification.NA,
          IonModification.Hneg_NA2, IonModification.K, IonModification.NH4, IonModification.H2plus);
      Collections.addAll(adductChoices, IonModification.getDefaultValuesPos());
    }
    if (polarity.toScanPolaritySelection().includesNegative()) {
      Collections.addAll(adducts, IonModification.H_NEG, IonModification.FA, IonModification.NA_2H,
          IonModification.CL);
      Collections.addAll(adductChoices, IonModification.getDefaultValuesNeg());
    }
    IonModification[] modifications = new IonModification[]{};
    // set choices first then values
    var modificationChoices = IonModification.getDefaultModifications();
    var selected = new IonModification[][]{adducts.toArray(IonModification[]::new), modifications};

    ionLibraryParam.setAll(2, 2, adductChoices.toArray(IonModification[]::new), modificationChoices,
        selected);
  }

  /**
   * Batch generation of library spectra
   */
  protected void makeAndAddExportScansStep(final BatchQueue q, final File exportPath,
      final LibraryBatchMetadataParameters libGenMetadata, final boolean skipAnnotatedFeatures,
      final String fileSuffix) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ExportScansFeatureModule.class).cloneParameterSet();

    MZTolerance isolationToleranceForInstrument = getIsolationToleranceForInstrument(steps);

    ExportScansFeatureMainParameters.setAll(param, exportPath, fileSuffix, libGenMetadata,
        mzTolScans, isolationToleranceForInstrument, skipAnnotatedFeatures, false);

    q.add(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ExportScansFeatureModule.class),
            param));
  }

  /**
   * Batch generation of library spectra
   */
  protected void makeAndAddBatchLibraryGeneration(final BatchQueue q, final File exportPath,
      final LibraryBatchMetadataParameters libGenMetadata) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(LibraryBatchGenerationModule.class).cloneParameterSet();

    var exportFormat = SpectralLibraryExportFormats.json_mzmine;
    File fileName = FileAndPathUtil.eraseFormat(exportPath);
    fileName = new File(fileName.getParentFile(),
        fileName.getName() + "_lib." + exportFormat.getExtension());

    param.setParameter(LibraryBatchGenerationParameters.flists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));

    // set merging
    param.getParameter(LibraryBatchGenerationParameters.merging)
        .setSimplePreset(SpectraMergeSelectPresets.REPRESENTATIVE_MSn_TREE, mzTolScans);

    param.setParameter(LibraryBatchGenerationParameters.exportFormat, exportFormat);
    param.setParameter(LibraryBatchGenerationParameters.normalizer,
        IntensityNormalizer.createDefault());
    param.setParameter(LibraryBatchGenerationParameters.file, fileName);
    param.setParameter(LibraryBatchGenerationParameters.postMergingMsLevelFilter,
        new MsLevelFilter(Options.MSn));
    // chimerics
    param.setParameter(LibraryBatchGenerationParameters.handleChimerics, true);
    var chimerics = param.getParameter(LibraryBatchGenerationParameters.handleChimerics)
        .getEmbeddedParameters();
    chimerics.setParameter(HandleChimericMsMsParameters.option, ChimericMsOption.FLAG);
    chimerics.setParameter(HandleChimericMsMsParameters.mainMassWindow, mzTolScans);
    chimerics.setParameter(HandleChimericMsMsParameters.isolationWindow,
        getIsolationToleranceForInstrument(steps));
    chimerics.setParameter(HandleChimericMsMsParameters.minimumPrecursorPurity, 0.75);

    // quality
    var quality = param.getParameter(LibraryBatchGenerationParameters.quality)
        .getEmbeddedParameters();
    quality.setParameter(LibraryExportQualityParameters.minExplainedIntensity, true, 0.35);
    quality.setParameter(LibraryExportQualityParameters.minExplainedSignals, false, 0.2);
    quality.setParameter(LibraryExportQualityParameters.exportFlistNameMatchOnly, false);
    quality.setParameter(LibraryExportQualityParameters.exportExplainedSignalsOnly, false);
    quality.setParameter(LibraryExportQualityParameters.formulaTolerance, mzTolScans);

    // metadata is user defined
    param.getParameter(LibraryBatchGenerationParameters.metadata)
        .setEmbeddedParameters(libGenMetadata);

    // advanced
    param.setParameter(LibraryBatchGenerationParameters.advanced, true);
    var advanced = param.getParameter(LibraryBatchGenerationParameters.advanced)
        .getEmbeddedParameters();
    advanced.setParameter(AdvancedExportScansFeatureParameters.compactUSI, true);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(LibraryBatchGenerationModule.class), param));

    // add reimport of library as sanity check
    var importParams = MZmineCore.getConfiguration()
        .getModuleParameters(SpectralLibraryImportModule.class).cloneParameterSet();
    importParams.setParameter(SpectralLibraryImportParameters.dataBaseFiles, new File[]{fileName});

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(SpectralLibraryImportModule.class), importParams));
  }

  /**
   * convert loaded library to feature list
   */
  protected void makeAndAddLibraryToFeatureListStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(SpectralLibraryToFeatureListModule.class).cloneParameterSet();

    param.setParameter(SpectralLibraryToFeatureListParameters.libraries,
        new SpectralLibrarySelection());

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(SpectralLibraryToFeatureListModule.class), param));
  }

  protected MZTolerance getIsolationToleranceForInstrument(final WizardSequence steps) {
    var ms = steps.get(WizardPart.MS).get().getFactory();
    return switch ((MassSpectrometerWizardParameterFactory) ms) {
      case Orbitrap, Orbitrap_Astral, FTICR, LOW_RES -> new MZTolerance(0.4, 5);
      case QTOF -> new MZTolerance(1.6, 5);
    };
  }

  protected void makeAndAddMassDetectorSteps(final BatchQueue q) {
    if (isImsActive) {
      if (isNativeIms) { // == Bruker file
        makeAndAddMassDetectionStep(q, 1, SelectedScanTypes.FRAMES);
        makeAndAddMassDetectionStep(q, 2, SelectedScanTypes.FRAMES);
      }
      makeAndAddMassDetectionStep(q, 1, SelectedScanTypes.MOBLITY_SCANS);
      makeAndAddMassDetectionStep(q, 2, SelectedScanTypes.MOBLITY_SCANS);
      if (!isNativeIms) {
        makeAndAddMobilityScanMergerStep(q);
      }
    } else {
      makeAndAddMassDetectionStep(q, 1, SelectedScanTypes.SCANS);
      makeAndAddMassDetectionStep(q, 2, SelectedScanTypes.SCANS);
    }
  }

  /**
   * Checks if at least one library file was selected
   */
  protected boolean checkLibraryFiles() {
    return Arrays.stream(libraries).anyMatch(Objects::nonNull);
  }

  protected void makeAndAddRowFilterStep(final BatchQueue q) {
    if (!filter13C && !minAlignedSamples.isGreaterZero()) {
      return;
    }

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(RowsFilterModule.class).cloneParameterSet();
    param.setParameter(RowsFilterParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    String suffix = (filter13C ? "13C" : "");
    if (minAlignedSamples.isGreaterZero()) {
      suffix = suffix + (suffix.isEmpty() ? "" : " ") + "peak";
    }
    param.setParameter(RowsFilterParameters.SUFFIX, suffix);
    param.setParameter(RowsFilterParameters.MIN_FEATURE_COUNT, minAlignedSamples.isGreaterZero());
    param.getParameter(RowsFilterParameters.MIN_FEATURE_COUNT).getEmbeddedParameter()
        .setValue(minAlignedSamples);

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
    param.setParameter(RowsFilterParameters.KEEP_ALL_ANNOTATED, false);
    param.setParameter(RowsFilterParameters.Reset_ID, false);
    param.setParameter(RowsFilterParameters.massDefect, false);
    param.setParameter(RowsFilterParameters.handleOriginal, handleOriginalFeatureLists);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(RowsFilterModule.class),
        param));
  }

  protected void makeAndAddImportTask(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(AllSpectralDataImportModule.class).cloneParameterSet();
    param.setParameter(AllSpectralDataImportParameters.sortAndRecolor, true);
    param.getParameter(AllSpectralDataImportParameters.fileNames).setValue(dataFiles);
    param.setParameter(AllSpectralDataImportParameters.metadataFile, metadataFile.active(),
        metadataFile.value());
    param.getParameter(SpectralLibraryImportParameters.dataBaseFiles).setValue(libraries);
    // turn advanced off but still set the noise levels etc
    param.getParameter(AllSpectralDataImportParameters.advancedImport).setValue(false);

    boolean denormalize = massDetectorOption.getValueType() == FACTOR_OF_LOWEST_SIGNAL;
    var advancedParameters = AdvancedSpectraImportParameters.create(
        massDetectorOption.getValueType(), getNoiseLevelForMsLevel(1), getNoiseLevelForMsLevel(2),
        null, ScanSelection.ALL_SCANS, denormalize);
    param.getParameter(AllSpectralDataImportParameters.advancedImport)
        .setEmbeddedParameters(advancedParameters);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(AllSpectralDataImportModule.class), param));
  }

  protected void makeAndAddMassDetectionStepForAllScans(final BatchQueue q) {
    makeAndAddMassDetectionStep(q, 0, SelectedScanTypes.SCANS);
  }

  public double getNoiseLevelForMsLevel(int msLevel) {
    return getNoiseLevelForMsLevel(msLevel, SelectedScanTypes.SCANS);
  }

  public double getNoiseLevelForMsLevel(int msLevel, SelectedScanTypes scanTypes) {
    return switch (massDetectorOption.getValueType()) {
      case ABSOLUTE_NOISE_LEVEL -> {
        if (msLevel == 1 && scanTypes == SelectedScanTypes.MOBLITY_SCANS) {
          yield massDetectorOption.getMs1NoiseLevel() / 5; // lower threshold for mobility scans
        } else if (msLevel >= 2 && scanTypes == SelectedScanTypes.MOBLITY_SCANS) {
          yield massDetectorOption.getMsnNoiseLevel() / 5; // lower threshold for mobility scans
        } else if (msLevel >= 2) {
          yield massDetectorOption.getMsnNoiseLevel();
        } else {
          yield massDetectorOption.getMs1NoiseLevel();
        }
      }
      case FACTOR_OF_LOWEST_SIGNAL -> msLevel >= 2 ? massDetectorOption.getMsnNoiseLevel()
          : massDetectorOption.getMs1NoiseLevel();
    };
  }

  /**
   * @param msLevel use 0 to apply without MS level filter
   */
  protected void makeAndAddMassDetectionStep(final BatchQueue q, int msLevel,
      SelectedScanTypes scanTypes) {

    // use factor of lowest or auto mass detector
    // factor of lowest only works on centroid for now
    var mdParams = massDetectorOption.getValueType()
        .createMassDetectorParameters(getNoiseLevelForMsLevel(msLevel, scanTypes));

    // set the main parameters
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MassDetectionModule.class).cloneParameterSet();

    final Boolean isDia = steps.get(WizardPart.WORKFLOW)
        .map(w -> w instanceof WorkflowDiaWizardParameters).orElse(false);
    boolean denormalize = massDetectorOption.getValueType() == FACTOR_OF_LOWEST_SIGNAL && !isDia;
    param.setParameter(MassDetectionParameters.denormalizeMSnScans, denormalize);

    param.setParameter(MassDetectionParameters.dataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    // if MS level 0 then apply to all scans
    if (msLevel != 0) {
      param.getParameter(MassDetectionParameters.scanSelection)
          .setValue(true, new ScanSelection(MsLevelFilter.of(msLevel, true)));
    } else {
      param.getParameter(MassDetectionParameters.scanSelection)
          .setValue(true, new ScanSelection(MsLevelFilter.ALL_LEVELS));
    }
    param.setParameter(MassDetectionParameters.scanTypes, scanTypes);
    param.getParameter(MassDetectionParameters.massDetector)
        .setValue(mdParams.value(), mdParams.parameters());

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(MassDetectionModule.class),
        param));
  }

  protected void makeAndAddMobilityScanMergerStep(final BatchQueue q) {

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MobilityScanMergerModule.class).cloneParameterSet();

    param.setParameter(MobilityScanMergerParameters.mzTolerance, mzTolScans);
    param.setParameter(MobilityScanMergerParameters.scanSelection, new ScanSelection());
    param.setParameter(MobilityScanMergerParameters.noiseLevel,
        0d); // the noise level of the mass detector already did all the filtering we want (at least in the wizard)
    param.setParameter(MobilityScanMergerParameters.mergingType, IntensityMergingType.SUMMED);
    param.setParameter(MobilityScanMergerParameters.weightingType, Weighting.LINEAR);
    param.setParameter(MobilityScanMergerParameters.minNumberOfDetections, 2);

    final RawDataFilesSelection rawDataFilesSelection = new RawDataFilesSelection(
        RawDataFilesSelectionType.BATCH_LAST_FILES);
    param.setParameter(MobilityScanMergerParameters.rawDataFiles, rawDataFilesSelection);

    q.add(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(MobilityScanMergerModule.class),
            param));
  }

  protected void makeAndAddImsExpanderStep(final BatchQueue q) {
    ParameterSet param = MZmineCore.getConfiguration().getModuleParameters(ImsExpanderModule.class)
        .cloneParameterSet();

    param.setParameter(ImsExpanderParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(ImsExpanderParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ImsExpanderParameters.useRawData, isNativeIms);
    param.getParameter(ImsExpanderParameters.useRawData).getEmbeddedParameter().setValue(1E1);
    param.setParameter(ImsExpanderParameters.mzTolerance, true);
    param.getParameter(ImsExpanderParameters.mzTolerance).getEmbeddedParameter()
        .setValue(mzTolScans);
    param.setParameter(ImsExpanderParameters.mobilogramBinWidth, false);
    param.setParameter(ImsExpanderParameters.maxNumTraces, false);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ImsExpanderModule.class),
        param));
  }

  protected void makeAndAddSmoothingStep(final BatchQueue q, final boolean rt,
      final Integer minRtDataPoints, final boolean mobility) {
    if (!rt && !mobility) {
      return;
    }

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(SmoothingModule.class).cloneParameterSet();
    param.setParameter(SmoothingParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(SmoothingParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(SmoothingParameters.suffix, "sm");

    param.getParameter(SmoothingParameters.smoothingAlgorithm)
        .setValue(FeatureSmoothingOptions.SAVITZKY_GOLAY);

    var sgParam = param.getEmbeddedParameterValue(SmoothingParameters.smoothingAlgorithm);
    sgParam.setParameter(SavitzkyGolayParameters.rtSmoothing, rt);
    sgParam.getParameter(SavitzkyGolayParameters.rtSmoothing).getEmbeddedParameter()
        .setValue(minRtDataPoints < 6 ? 5 : Math.min(21, minRtDataPoints / 2 * 2 + 1));
    sgParam.setParameter(SavitzkyGolayParameters.mobilitySmoothing, mobility);
    // next odd number
    sgParam.getParameter(SavitzkyGolayParameters.mobilitySmoothing).getEmbeddedParameter()
        .setValue(minImsDataPoints < 6 ? 5 : Math.min(21, minImsDataPoints / 2 * 2 + 1));

    MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(SmoothingModule.class), param);
    q.add(step);
  }

  protected void makeAndAddRetentionTimeCalibration(BatchQueue q, MZTolerance mzTolInterSample,
      RTTolerance interSampleRtTol, OriginalFeatureListOption handleOriginalFeatureLists) {

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(RTCorrectionModule.class).cloneParameterSet();
    param.setParameter(RTCorrectionParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(RTCorrectionParameters.MZTolerance, mzTolInterSample);
    param.setParameter(RTCorrectionParameters.RTTolerance, interSampleRtTol);
    param.setParameter(RTCorrectionParameters.minHeight, minFeatureHeight);
    param.setParameter(RTCorrectionParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(RTCorrectionParameters.suffix, "rt_cal");

    MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(RTCorrectionModule.class), param);
    q.add(step);
  }

  /**
   * MS2 grouping can be done in the resolver steps or as a standalone module
   *
   * @param rtTol if null, a wide range is used that includes all
   * @return creates a parameter set for the main module. Can also be used in the resolver steps
   */
  protected ParameterSet createMs2GrouperParameters(final int minRtDataPoints,
      final boolean limitRtEdges, @Nullable RTTolerance rtTol) {
    // only TIMS currently supports DDA MS2 acquisition with PASEF
    // other instruments have the fragmentation before the IMS cell
    boolean hasTims = isImsActive && imsInstrumentType.equals(MobilityType.TIMS);

    var groupMs2Params = MZmineCore.getConfiguration().getModuleParameters(GroupMS2Module.class)
        .cloneParameterSet();
    // Using a fixed wide range here because precursor isolation is usually unit resolution
    groupMs2Params.setParameter(GroupMS2Parameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    groupMs2Params.setParameter(GroupMS2Parameters.mzTol, MZTolerance.max(mzTolScans, 0.01, 10.0));
    groupMs2Params.setParameter(GroupMS2Parameters.combineTimsMsMs, false);
    groupMs2Params.setParameter(GroupMS2Parameters.limitMobilityByFeature, true);
    groupMs2Params.setParameter(GroupMS2Parameters.minRequiredSignals, true, 1);
    groupMs2Params.setParameter(GroupMS2Parameters.minimumRelativeFeatureHeight, true, 0.25);
    groupMs2Params.setParameter(GroupMS2Parameters.minImsRawSignals, 2);

    final GroupMs2AdvancedParameters advanced = GroupMs2AdvancedParameters.create(
        hasTims ? massDetectorOption.getMsnNoiseLevel() * 2 : null, hasTims ? 0.01 : null);

    groupMs2Params.setParameter(GroupMS2Parameters.advancedParameters, false);
    groupMs2Params.getParameter(GroupMS2Parameters.advancedParameters)
        .setEmbeddedParameters(advanced);

    // retention time
    // rt tolerance is +- while FWHM is the width. still the MS2 might be triggered very early
    // change rt tol depending on number of data points
    var rtLimitOption = minRtDataPoints >= 4 && limitRtEdges ? FeatureLimitOptions.USE_FEATURE_EDGES
        : FeatureLimitOptions.USE_TOLERANCE;
    var realRTTol = Objects.requireNonNullElse(rtTol, new RTTolerance(9999999, Unit.MINUTES));
    groupMs2Params.setParameter(GroupMS2Parameters.rtFilter,
        new RtLimitsFilter(rtLimitOption, realRTTol));
    return groupMs2Params;
  }

  protected void makeAndAddMs2GrouperStep(final BatchQueue q, ParameterSet groupMs2Params) {
    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(GroupMS2Module.class),
        groupMs2Params));
  }

  /**
   * @param groupMs2Params this might be the same parameterset used for retention time resolving.
   *                       Will be cloned
   */
  protected void makeAndAddMobilityResolvingStep(final BatchQueue q,
      @Nullable ParameterSet groupMs2Params) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, "r");
    param.setParameter(MinimumSearchFeatureResolverParameters.handleOriginal,
        handleOriginalFeatureLists);

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

    param.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.MOBILITY);
    param.setParameter(MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL,
        0.80);
    param.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE, imsFwhm);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT, 0d);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT,
        minFeatureHeight);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO, 1.8d);
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION, Range.closed(0d, 20d));
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS,
        minImsDataPoints);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MinimumSearchFeatureResolverModule.class), param));
  }

  protected void makeAndAddIsotopeFinderStep(final BatchQueue q) {
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

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IsotopeFinderModule.class),
        param));
  }

  /**
   * @param noPrecursor no precursor matching like in GC-EI-MS
   */
  protected void makeAndAddSpectralNetworkingSteps(final BatchQueue q, final boolean isExportActive,
      final File exportPath, final boolean noPrecursor) {
    // NETWORKING
    ParameterSet mainParams = MZmineCore.getConfiguration()
        .getModuleParameters(MainSpectralNetworkingModule.class).cloneParameterSet();
    mainParams.setParameter(MainSpectralNetworkingParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));

    var algorithm = noPrecursor ? SpectralNetworkingOptions.COSINE_NO_PRECURSOR
        : SpectralNetworkingOptions.MODIFIED_COSINE;
    mainParams.setParameter(MainSpectralNetworkingParameters.algorithms, algorithm);
    var param = mainParams.getEmbeddedParameterValue(MainSpectralNetworkingParameters.algorithms);

    switch (algorithm) {
      case MODIFIED_COSINE -> {
        ModifiedCosineSpectralNetworkingParameters.setAll(param, mzTolScans, true, 500d, 4, 0.7,
            mzTolScans, SpectralSignalFilter.DEFAULT);
      }
      case COSINE_NO_PRECURSOR -> {
        NoPrecursorCosineSpectralNetworkingParameters.setAll(param, 8, 0.7, mzTolScans,
            SpectralSignalFilter.DEFAULT_NO_PRECURSOR);
      }
      // MS2Deepscore hard to do without the file path or maybe just dont set the file path
    }

    MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MainSpectralNetworkingModule.class), mainParams);
    q.add(step);

    // GRAPHML EXPORT
    if (isExportActive) {
      ParameterSet graphml = MZmineCore.getConfiguration()
          .getModuleParameters(NetworkGraphMlExportModule.class).cloneParameterSet();

      graphml.setParameter(NetworkGraphMlExportParameters.featureLists,
          new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
      File file = FileAndPathUtil.getRealFilePathWithSuffix(exportPath, "_mzmine_networking",
          "graphml");
      graphml.setParameter(NetworkGraphMlExportParameters.filename, file);

      step = new MZmineProcessingStepImpl<>(
          MZmineCore.getModuleInstance(NetworkGraphMlExportModule.class), graphml);
      q.add(step);
    }
  }

  protected void makeAndAddLibrarySearchStep(final BatchQueue q,
      boolean libraryGenerationWorkflow) {
    if (!libraryGenerationWorkflow && !checkLibraryFiles()) {
      return;
    }

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(SpectralLibrarySearchModule.class).cloneParameterSet();

    param.setParameter(SpectralLibrarySearchParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(SpectralLibrarySearchParameters.libraries, new SpectralLibrarySelection());

    // merging and selection to representative scan
    param.getParameter(SpectralLibrarySearchParameters.spectraMergeSelect)
        .setSimplePreset(SpectraMergeSelectPresets.REPRESENTATIVE_SCANS, mzTolScans);

    param.setParameter(SpectralLibrarySearchParameters.msLevelFilter, MsLevelFilter.of(2));

    param.setParameter(SpectralLibrarySearchParameters.mzTolerancePrecursor,
        new MZTolerance(0.01, 20));
    param.setParameter(SpectralLibrarySearchParameters.mzTolerance, new MZTolerance(0.01, 20));
    param.setParameter(SpectralLibrarySearchParameters.removePrecursor, true);
    param.setParameter(SpectralLibrarySearchParameters.minMatch, 4);
    // similarity
    var simFunction = param.getParameter(SpectralLibrarySearchParameters.similarityFunction);
    simFunction.setValue(SpectralSimilarityFunctions.WEIGHTED_COSINE);
    var simParam = simFunction.getEmbeddedParameters();
    simParam.setParameter(WeightedCosineSpectralSimilarityParameters.weight, Weights.SQRT);
    simParam.setParameter(WeightedCosineSpectralSimilarityParameters.minCosine, 0.7);
    simParam.setParameter(WeightedCosineSpectralSimilarityParameters.handleUnmatched,
        HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO);

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

  protected void makeAndAddLocalCsvDatabaseSearchStep(final BatchQueue q,
      final @Nullable RTTolerance rtTol) {
    if (!checkLocalCsvDatabase) {
      return;
    }

    var param = MZmineCore.getConfiguration()
        .getModuleParameters(LocalCSVDatabaseSearchModule.class).cloneParameterSet();

    param.setParameter(LocalCSVDatabaseSearchParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(LocalCSVDatabaseSearchParameters.dataBaseFile, csvLibraryFile);
    param.setParameter(LocalCSVDatabaseSearchParameters.fieldSeparator,
        csvLibraryFile.getName().toLowerCase().endsWith(".csv") ? "," : "\\t");
    param.setParameter(LocalCSVDatabaseSearchParameters.columns, csvColumns);
    param.setParameter(LocalCSVDatabaseSearchParameters.mzTolerance, mzTolInterSample);
    param.setParameter(LocalCSVDatabaseSearchParameters.rtTolerance,
        Objects.requireNonNullElse(rtTol, new RTTolerance(9999999, Unit.MINUTES)));
    param.setParameter(LocalCSVDatabaseSearchParameters.mobTolerance, imsFwhmMobTolerance);
    param.setParameter(LocalCSVDatabaseSearchParameters.ccsTolerance, 0.05);
    param.setParameter(LocalCSVDatabaseSearchParameters.filterSamples,
        !csvFilterSamplesColumn.isBlank(), csvFilterSamplesColumn.trim());
    param.setParameter(LocalCSVDatabaseSearchParameters.commentFields, "");
    // define ions
    var ionLibParams = param.getParameter(LocalCSVDatabaseSearchParameters.ionLibrary)
        .getEmbeddedParameters();
    createAndSetIonLibrary(ionLibParams);
    param.setParameter(LocalCSVDatabaseSearchParameters.ionLibrary,
        csvMassOptions == MassOptions.MASS_AND_IONS);

    MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(LocalCSVDatabaseSearchModule.class), param);
    q.add(step);
  }

  protected void makeAndAddLipidAnnotationStep(final BatchQueue q) {
    if (!annotateLipids) {
      return;
    }

    var param = MZmineCore.getConfiguration().getModuleParameters(LipidAnnotationModule.class)
        .cloneParameterSet();

    param.setParameter(LipidAnnotationParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(LipidAnnotationParameters.lipidClasses,
        LipidClassesProvider.getListOfAllLipidClasses().toArray());
    param.setParameter(LipidAnnotationParameters.lipidChainParameters,
        new LipidAnnotationChainParameters());
    param.setParameter(LipidAnnotationParameters.mzTolerance, mzTolInterSample);
    param.setParameter(LipidAnnotationParameters.searchForMSMSFragments, true);
    var ms2Param = param.getParameter(LipidAnnotationParameters.searchForMSMSFragments)
        .getEmbeddedParameters();
    // all input scans as default to avoid to many chimeric merged spectra in lipids
    ms2Param.getParameter(LipidAnnotationMSMSParameters.spectraMergeSelect)
        .setUseInputScans(SelectInputScans.ALL_SCANS);
    ms2Param.setParameter(LipidAnnotationMSMSParameters.keepUnconfirmedAnnotations, isImaging);
    ms2Param.setParameter(LipidAnnotationMSMSParameters.minimumMsMsScore, 0.6);
    ms2Param.setParameter(LipidAnnotationMSMSParameters.mzToleranceMS2, mzTolScans);
    param.setParameter(LipidAnnotationParameters.advanced, false);
    var advanced = param.getEmbeddedParameterValue(LipidAnnotationParameters.advanced);
    advanced.setParameter(AdvancedLipidAnnotationParameters.IONS_TO_IGNORE,
        IonizationType.values());
    MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(LipidAnnotationModule.class), param);
    q.add(step);
  }

  protected void makeAndAddBatchExportStep(final BatchQueue q, boolean exportEnabled,
      @Nullable final File exportPath) {
    if (!exportEnabled) {
      return;
    }

    final ParameterSet param = new AutoSaveBatchParameters().cloneParameterSet();

    if (exportPath != null) {
      File fileName = FileAndPathUtil.eraseFormat(exportPath);
      final String extension = ExtensionFilters.getExtensionName(ExtensionFilters.MZ_BATCH);
      fileName = new File(fileName.getParentFile(),
          fileName.getName() + "_batch.%s".formatted(extension));
      param.setParameter(AutoSaveBatchParameters.savePath, true, fileName);
    } else {
      param.setParameter(AutoSaveBatchParameters.savePath, false);
    }

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(AutoSaveBatchModule.class),
        param));
  }
}
