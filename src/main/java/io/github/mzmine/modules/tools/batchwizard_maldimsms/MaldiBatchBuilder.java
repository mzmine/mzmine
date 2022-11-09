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

package io.github.mzmine.modules.tools.batchwizard_maldimsms;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_maldispotfeaturedetection.MaldiSpotFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_maldispotfeaturedetection.MaldiSpotFeatureDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.SelectedScanTypes;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolayParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolaySmoothing;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderParameters;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderParameters.ScanRange;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperParameters;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterChoices;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import java.io.File;
import java.util.List;
import org.openscience.cdk.Element;

public class MaldiBatchBuilder {

  private final OriginalFeatureListOption option = OriginalFeatureListOption.REMOVE;

  private final MaldiWizardParameters parameters;
  private final MZTolerance defaultScanToScanTolerance = new MZTolerance(0.005, 15);
  private final MZTolerance defaultFeatureToFeatureTolerance = new MZTolerance(0.003, 5);

  public MaldiBatchBuilder(MaldiWizardParameters param) {
    this.parameters = param;
  }

  public BatchQueue buildBatch() {
    final BatchQueue batch = new BatchQueue();

    batch.add(makeImportTask(parameters.getParameter(MaldiWizardParameters.importFiles)));
    batch.add(makeMassDetectionStep(parameters.getValue(MaldiWizardParameters.frameNoiseLevel),
        SelectedScanTypes.FRAMES, 1));
    batch.add(
        makeMassDetectionStep(parameters.getValue(MaldiWizardParameters.mobilityScanNoiseLevel),
            SelectedScanTypes.MOBLITY_SCANS, 1));
    batch.add(makeMaldiSpotDetectionStep(parameters));
    batch.add(makeSmoothingStep(7));
    batch.add(
        makeMobilityResolvingStep(parameters.getValue(MaldiWizardParameters.frameNoiseLevel)));
    batch.add(makeDeisotopingStep());
    batch.add(makeIsotopeFinderStep());
    batch.add(makeFilterStep());

    return batch;
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeImportTask(FileNamesParameter files) {
    // todo make auto mass detector work, so we can use it here.
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(AllSpectralDataImportModule.class).cloneParameterSet();
    param.getParameter(AllSpectralDataImportParameters.advancedImport).setValue(false);
    param.getParameter(AllSpectralDataImportParameters.fileNames).setValue(files.getValue());
    param.getParameter(SpectralLibraryImportParameters.dataBaseFiles).setValue(new File[0]);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(AllSpectralDataImportModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMassDetectionStep(double noiseLevel,
      SelectedScanTypes scanType, int msLevel) {

    final ParameterSet detectorParam = MZmineCore.getConfiguration()
        .getModuleParameters(AutoMassDetector.class).cloneParameterSet();
    detectorParam.getParameter(AutoMassDetectorParameters.noiseLevel).setValue(noiseLevel);
    // per default do not detect isotope signals below noise. this might introduce too many signals
    // for the isotope finder later on and confuse users
    detectorParam.setParameter(AutoMassDetectorParameters.detectIsotopes, false);

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MassDetectionModule.class).cloneParameterSet();
    param.getParameter(MassDetectionParameters.dataFiles)
        .setValue(new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    param.getParameter(MassDetectionParameters.scanSelection).setValue(new ScanSelection(msLevel));
    param.getParameter(MassDetectionParameters.scanTypes).setValue(scanType);
    param.getParameter(MassDetectionParameters.massDetector).setValue(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(AutoMassDetector.class),
            detectorParam));

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(MassDetectionModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMaldiSpotDetectionStep(
      MaldiWizardParameters parameters) {

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MaldiSpotFeatureDetectionModule.class).cloneParameterSet();

    param.setParameter(MaldiSpotFeatureDetectionParameters.files,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    param.setParameter(MaldiSpotFeatureDetectionParameters.spotNameFile,
        parameters.getValue(MaldiWizardParameters.spotNameFile),
        param.getEmbeddedParameterValue(MaldiWizardParameters.spotNameFile));
    param.setParameter(MaldiSpotFeatureDetectionParameters.minIntensity,
        parameters.getValue(MaldiWizardParameters.frameNoiseLevel));
    param.setParameter(MaldiSpotFeatureDetectionParameters.mzTolerance, defaultScanToScanTolerance);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MaldiSpotFeatureDetectionModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeSmoothingStep(
      final int mobilitySmoothingWidth) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(SmoothingModule.class).cloneParameterSet();
    param.setParameter(SmoothingParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));

    ParameterSet sgParam = MZmineCore.getConfiguration()
        .getModuleParameters(SavitzkyGolaySmoothing.class).cloneParameterSet();
    sgParam.setParameter(SavitzkyGolayParameters.rtSmoothing, false);
    sgParam.getParameter(SavitzkyGolayParameters.rtSmoothing).getEmbeddedParameter().setValue(3);
    sgParam.setParameter(SavitzkyGolayParameters.mobilitySmoothing, true);
    sgParam.getParameter(SavitzkyGolayParameters.mobilitySmoothing).getEmbeddedParameter()
        .setValue(mobilitySmoothingWidth);

    param.getParameter(SmoothingParameters.smoothingAlgorithm).setValue(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(SavitzkyGolaySmoothing.class),
            sgParam));
    param.setParameter(SmoothingParameters.handleOriginal, option);
    param.setParameter(SmoothingParameters.suffix, "sm");

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(SmoothingModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMobilityResolvingStep(double minHeight) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, "r");
    param.setParameter(MinimumSearchFeatureResolverParameters.handleOriginal, option);
    param.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, false);

    param.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.MOBILITY);
    param.setParameter(MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL,
        0.75);
    param.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE, 0.003);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT, 0d);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT, minHeight);
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
    param.setParameter(IsotopeGrouperParameters.mzTolerance, defaultFeatureToFeatureTolerance);
    param.setParameter(IsotopeGrouperParameters.rtTolerance,
        new RTTolerance(100000000f, Unit.MINUTES));
    param.setParameter(IsotopeGrouperParameters.mobilityTolerace, true);
    param.getParameter(IsotopeGrouperParameters.mobilityTolerace).getEmbeddedParameter()
        .setValue(new MobilityTolerance(0.008f));
    param.setParameter(IsotopeGrouperParameters.monotonicShape, true);
    param.setParameter(IsotopeGrouperParameters.keepAllMS2, true);
    param.setParameter(IsotopeGrouperParameters.maximumCharge, 2);
    param.setParameter(IsotopeGrouperParameters.representativeIsotope,
        IsotopeGrouperParameters.ChooseTopIntensity);
    param.setParameter(IsotopeGrouperParameters.handleOriginal, option);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IsotopeGrouperModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeIsotopeFinderStep() {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IsotopeFinderModule.class).cloneParameterSet();

    param.setParameter(IsotopeFinderParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(IsotopeFinderParameters.isotopeMzTolerance,
        defaultFeatureToFeatureTolerance);
    param.setParameter(IsotopeFinderParameters.maxCharge, 1);
    param.setParameter(IsotopeFinderParameters.scanRange, ScanRange.SINGLE_MOST_INTENSE);
    param.setParameter(IsotopeFinderParameters.elements,
        List.of(new Element("H"), new Element("C"), new Element("N"), new Element("O"),
            new Element("S"), new Element("Cl")));

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IsotopeFinderModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeFilterStep() {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(RowsFilterModule.class).cloneParameterSet();

    param.setParameter(RowsFilterParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(RowsFilterParameters.SUFFIX, "filt");
    param.setParameter(RowsFilterParameters.MIN_FEATURE_COUNT, false);
    param.setParameter(RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT, false);
    param.setParameter(RowsFilterParameters.ISOTOPE_FILTER_13C, false);
    param.setParameter(RowsFilterParameters.removeRedundantRows, true); // remove redundant here
    param.setParameter(RowsFilterParameters.MZ_RANGE, false);
    param.setParameter(RowsFilterParameters.RT_RANGE, false);
    param.setParameter(RowsFilterParameters.FEATURE_DURATION, false);
    param.setParameter(RowsFilterParameters.CHARGE, false);
    param.setParameter(RowsFilterParameters.KENDRICK_MASS_DEFECT, false);
    param.setParameter(RowsFilterParameters.GROUPSPARAMETER, false);
    param.setParameter(RowsFilterParameters.HAS_IDENTITIES, false);
    param.setParameter(RowsFilterParameters.IDENTITY_TEXT, false);
    param.setParameter(RowsFilterParameters.COMMENT_TEXT, false);
    param.setParameter(RowsFilterParameters.REMOVE_ROW, RowsFilterChoices.KEEP_MATCHING);
    param.setParameter(RowsFilterParameters.KEEP_ALL_MS2, true);
    param.setParameter(RowsFilterParameters.Reset_ID, false);
    param.setParameter(RowsFilterParameters.massDefect, false);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(RowsFilterModule.class),
        param);
  }
}
