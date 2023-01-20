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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
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
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.Isotope13CFilterParameters;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterChoices;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
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
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset;
import io.github.mzmine.modules.tools.batchwizard.WizardWorkflow;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AbstractWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.FilterWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters.ChromatographyWorkflow;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonMobilityWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowWizardParameters.WorkflowDefaults;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.util.FeatureMeasurementType;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.maths.Weighting;
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
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

/**
 * Creates a batch queue from a list of {@link WizardPreset} making up a workflow defined in
 * {@link WizardPart}
 */
public abstract class WizardBatchBuilder {

  private static final Logger logger = Logger.getLogger(WizardBatchBuilder.class.getName());
  // only parameters that are used in all workflows
  // input
  protected final File[] dataFiles;
  // annotation
  protected final File[] libraries;
  //filter
  protected final Boolean filter13C;
  protected final Integer minAlignedSamples;
  protected final OriginalFeatureListOption handleOriginalFeatureLists;
  // MS parameters currently all the same
  protected final Double noiseLevelMsn;
  protected final Double noiseLevelMs1;
  protected final Double minFeatureHeight;
  protected final MZTolerance mzTolScans;
  protected final MZTolerance mzTolFeaturesIntraSample;
  protected final MZTolerance mzTolInterSample;
  protected final Polarity polarity;
  // IMS parameter currently all the same
  protected final Boolean isImsActive;
  protected final MobilityType imsInstrumentType;
  protected final Boolean imsSmoothing;
  protected final Double imsFwhm;
  protected final Integer minImsDataPoints;
  private final WizardWorkflow steps;


  /**
   * Create workflow builder for workflow steps
   *
   * @param steps workflow
   * @return the builder
   */
  public static WizardBatchBuilder createBatchBuilderForWorkflow(final WizardWorkflow steps) {
    // workflow is always set
    Optional<WizardPreset> preset = steps.get(WizardPart.WORKFLOW);
    WorkflowDefaults workflowPreset = WorkflowDefaults.valueOf(preset.get().uniquePresetId());
    return switch (workflowPreset) {
      case DDA -> steps.isImaging() ? new WizardBatchBuilderImagingDda(steps)
          : new WizardBatchBuilderLcDDA(steps);
      case GC_EI_DECONVOLUTION -> new WizardBatchBuilderGcEiDeconvolution(steps);
      case LIBRARY_GENERATION, MS1_ONLY -> throw new UnsupportedOperationException(
          "Currently not implemented workflow " + workflowPreset);
    };
  }

  protected WizardBatchBuilder(WizardWorkflow steps) {
    this.steps = steps;
    // input
    Optional<? extends AbstractWizardParameters<?>> params = steps.get(WizardPart.DATA_IMPORT)
        .map(WizardPreset::parameters);
    dataFiles = orElseGet(params, AllSpectralDataImportParameters.fileNames, () -> new File[0]);

    // annotation
    params = steps.get(WizardPart.ANNOTATION).map(WizardPreset::parameters);
    libraries = orElseGet(params, SpectralLibraryImportParameters.dataBaseFiles, () -> new File[0]);

    // filter
    params = steps.get(WizardPart.FILTER).map(WizardPreset::parameters);
    filter13C = get(params, FilterWizardParameters.filter13C, false);
    minAlignedSamples = orElseGet(params, FilterWizardParameters.minNumberOfSamples, () -> 1);
    handleOriginalFeatureLists = orElseGet(params,
        FilterWizardParameters.handleOriginalFeatureLists, () -> OriginalFeatureListOption.REMOVE);

    // ion mobility IMS
    params = steps.get(WizardPart.IMS).map(WizardPreset::parameters);
    isImsActive = get(params, IonMobilityWizardParameters.imsActive, false);
    imsInstrumentType = get(params, IonMobilityWizardParameters.instrumentType, MobilityType.NONE);
    imsFwhm = get(params, IonMobilityWizardParameters.approximateImsFWHM, 0d);
    minImsDataPoints = get(params, IonMobilityWizardParameters.minNumberOfDataPoints, 4);
    imsSmoothing = get(params, IonMobilityWizardParameters.smoothing, false);

    // mass spectrometer
    params = steps.get(WizardPart.MS).map(WizardPreset::parameters);
    polarity = get(params, MassSpectrometerWizardParameters.polarity, Polarity.Positive);
    noiseLevelMsn = get(params, MassSpectrometerWizardParameters.ms2NoiseLevel, 0d);
    noiseLevelMs1 = get(params, MassSpectrometerWizardParameters.ms1NoiseLevel, 0d);
    minFeatureHeight = get(params, MassSpectrometerWizardParameters.minimumFeatureHeight, 0d);
    mzTolScans = orElseGet(params, MassSpectrometerWizardParameters.scanToScanMzTolerance,
        () -> new MZTolerance(0.002, 15));
    mzTolFeaturesIntraSample = orElseGet(params,
        MassSpectrometerWizardParameters.featureToFeatureMzTolerance,
        () -> new MZTolerance(0.002, 15));
    mzTolInterSample = orElseGet(params, MassSpectrometerWizardParameters.sampleToSampleMzTolerance,
        () -> new MZTolerance(0.002, 15));
  }

  protected static MZmineProcessingStep<MZmineProcessingModule> makeDuplicateRowFilterStep(
      final OriginalFeatureListOption handleOriginalFeatureLists,
      final MZTolerance mzTolFeaturesIntraSample, final RTTolerance rtFwhm) {
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

  protected static MZmineProcessingStep<MZmineProcessingModule> makeAdapChromatogramStep(
      final Double minFeatureHeight, final MZTolerance mzTolScans, final Double noiseLevelMs1,
      final Integer minRtDataPoints, @Nullable final Range<Double> cropRtRange) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ModularADAPChromatogramBuilderModule.class).cloneParameterSet();
    param.setParameter(ADAPChromatogramBuilderParameters.dataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    // crop rt range
    param.setParameter(ADAPChromatogramBuilderParameters.scanSelection,
        new ScanSelection(cropRtRange == null ? null : RangeUtils.toFloatRange(cropRtRange), 1));
    param.setParameter(ADAPChromatogramBuilderParameters.minimumScanSpan, minRtDataPoints);
    param.setParameter(ADAPChromatogramBuilderParameters.mzTolerance, mzTolScans);
    param.setParameter(ADAPChromatogramBuilderParameters.suffix, "chroms");
    param.setParameter(ADAPChromatogramBuilderParameters.minGroupIntensity, noiseLevelMs1);
    param.setParameter(ADAPChromatogramBuilderParameters.minHighestPoint, minFeatureHeight);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ModularADAPChromatogramBuilderModule.class), param);
  }

  // export for DDA
  protected static void makeDdaExportSteps(final BatchQueue q, final Boolean isExportActive,
      final File exportPath, final Boolean exportGnps, final Boolean exportSirius) {
    if (isExportActive && exportPath != null) {
      if (exportGnps) {
        q.add(makeIimnGnpsExportStep(exportPath));
      }
      if (exportSirius) {
        q.add(makeSiriusExportStep(exportPath));
      }
    }
  }

  protected static MZmineProcessingStep<MZmineProcessingModule> makeIimnGnpsExportStep(
      final File exportPath) {
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

  protected static MZmineProcessingStep<MZmineProcessingModule> makeSiriusExportStep(
      final File exportPath) {
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

  /**
   * Create different workflows in {@link BatchQueue}. Workflows are defined in
   * {@link ChromatographyWorkflow}
   *
   * @return a batch queue
   */
  public abstract BatchQueue createQueue();

  /**
   * Get parameter if available or else
   *
   * @param params
   * @param parameter
   * @param orElse
   * @param <T>
   * @return
   */
  protected <T> T orElseGet(@NotNull final Optional<? extends AbstractWizardParameters<?>> params,
      @NotNull final Parameter<T> parameter, @NotNull final Supplier<T> orElse) {
    if (params.isPresent()) {
      try {
        return params.get().getValue(parameter);
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Error during extraction of value from parameter " + parameter.getName(), ex);
      }
    }
    return orElse.get();
  }

  /**
   * Get parameter if available or else
   *
   * @param params
   * @param parameter
   * @param orElse
   * @param <T>
   * @return
   */
  protected <T> T get(@NotNull final Optional<? extends AbstractWizardParameters<?>> params,
      @NotNull final Parameter<T> parameter, T orElse) {
    if (params.isPresent()) {
      try {
        return params.get().getValue(parameter);
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Error during extraction of value from parameter " + parameter.getName(), ex);
      }
    }
    return orElse;
  }

  /**
   * Get parameter if available or else
   *
   * @param params
   * @param parameter
   * @param <T>
   * @return
   */
  protected <T> T get(@NotNull final Optional<? extends AbstractWizardParameters<?>> params,
      @NotNull final Parameter<T> parameter) {
    if (params.isPresent()) {
      try {
        return params.get().getValue(parameter);
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Error during extraction of value from parameter " + parameter.getName(), ex);
      }
    }
    return null;
  }

  /**
   * Get parameter if available or else
   *
   * @param params
   * @param parameter
   * @return value and selection state of an OptionalParameter
   */
  protected <V, T extends UserParameter<V, ?>> OptionalValue<V> getOptional(
      @NotNull final Optional<? extends AbstractWizardParameters<?>> params,
      @NotNull final OptionalParameter<T> parameter) {
    if (params.isPresent()) {
      try {
        OptionalParameter<T> param = params.get().getParameter(parameter);
        return new OptionalValue<>(param.getValue(), param.getEmbeddedParameter().getValue());
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Error during extraction of value from parameter " + parameter.getName(), ex);
      }
    }
    return null;
  }

  protected void makeMassDetectorSteps(final BatchQueue q) {
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

  /**
   * Checks if at least one library file was selected
   */
  protected boolean checkLibraryFiles() {
    return Arrays.stream(libraries).anyMatch(Objects::nonNull);
  }

  @Nullable
  protected void makeRowFilterStep(final BatchQueue q) {
    if (!filter13C && minAlignedSamples < 2) {
      return;
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

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(RowsFilterModule.class),
        param));
  }

  protected MZmineProcessingStep<MZmineProcessingModule> makeImportTask() {
    // todo make auto mass detector work, so we can use it here.
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(AllSpectralDataImportModule.class).cloneParameterSet();
    param.getParameter(AllSpectralDataImportParameters.advancedImport).setValue(false);
    param.getParameter(AllSpectralDataImportParameters.fileNames).setValue(dataFiles);
    param.getParameter(SpectralLibraryImportParameters.dataBaseFiles).setValue(libraries);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(AllSpectralDataImportModule.class), param);
  }

  protected MZmineProcessingStep<MZmineProcessingModule> makeMassDetectionStep(int msLevel,
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

  protected MZmineProcessingStep<MZmineProcessingModule> makeMobilityScanMergerStep() {

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

  protected MZmineProcessingStep<MZmineProcessingModule> makeImsExpanderStep() {
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

  protected void makeSmoothingStep(final BatchQueue q, final boolean rt,
      final Integer minRtDataPoints, final boolean mobility) {
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

  protected MZmineProcessingStep<MZmineProcessingModule> makeMobilityResolvingStep() {
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

  protected MZmineProcessingStep<MZmineProcessingModule> makeIsotopeFinderStep() {
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

  protected void makeLibrarySearchStep(final BatchQueue q) {
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

}
