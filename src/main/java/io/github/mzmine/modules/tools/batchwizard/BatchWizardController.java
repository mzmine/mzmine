/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.tools.batchwizard;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchModeParameters;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.AdvancedImsTraceBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IonMobilityTraceBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IonMobilityTraceBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.DetectIsotopesParameter;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingParameters;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.InterSampleHeightCorrParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Polarity;
import io.github.mzmine.modules.tools.batchwizard.defaults.DefaultLcParameters;
import io.github.mzmine.modules.tools.batchwizard.defaults.DefaultMsParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.MinimumFeaturesFilterParameters;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteNRelativeInt;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteNRelativeInt.Mode;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesComponent;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

public class BatchWizardController {

  public GridPane pnParameters;
  public RadioButton rbOrbitrap;
  public RadioButton rbTOF;
  public ToggleGroup massSpec;
  public RadioButton rbHPLC;
  public RadioButton rbUHPLC;
  public ToggleGroup hplc;
  public CheckBox cbIonMobility;
  public Button btnSetMsDefaults;
  public Button btnSetLcDefaults;
  public ComboBox<Polarity> cbPolarity;

  final SimpleParameterSet hplcParameters = new BatchWizardHPLCParameters();
  final ParameterSetupDialog hplcDialog = new ParameterSetupDialog(false, hplcParameters);

  final SimpleParameterSet msParameters = new BatchWizardMassSpectrometerParameters();
  final ParameterSetupDialog msDialog = new ParameterSetupDialog(false, msParameters);
  public ComboBox<MobilityType> cbMobilityType;

  private FileNamesParameter files;
  private FileNamesComponent filesComponent;

  public void initialize() {
    pnParameters.add(hplcDialog.getParamsPane(), 1, 2, 1, 1);
    pnParameters.add(msDialog.getParamsPane(), 0, 2, 1, 1);

    files = new FileNamesParameter("MS data files",
        "Please select the data files you want to process.",
        AllSpectralDataImportParameters.extensions);
    filesComponent = files.createEditingComponent();

    pnParameters.add(filesComponent, 0, 4, 2, 1);
    pnParameters.layout();

    cbIonMobility.disableProperty().bind(rbTOF.selectedProperty().not());
    cbMobilityType.disableProperty().bind(rbTOF.selectedProperty().not());
    rbOrbitrap.selectedProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue) {
        cbIonMobility.setSelected(false);
      }
    }));

    cbPolarity.setItems(FXCollections.observableArrayList(Polarity.values()));
    cbPolarity.setValue(Polarity.Positive);

    cbMobilityType.setItems(FXCollections.observableArrayList(MobilityType.values()));
    cbMobilityType.setValue(MobilityType.TIMS);

  }

  public void onSetMsDefaults(ActionEvent actionEvent) {
    if (rbTOF.isSelected() && cbIonMobility.isSelected()) {
      DefaultMsParameters.defaultImsTofParameters.setToParameterSet(msParameters);
      msDialog.setParameterValuesToComponents();
      return;
    }
    if (rbTOF.isSelected() && !cbIonMobility.isSelected()) {
      DefaultMsParameters.defaultTofParameters.setToParameterSet(msParameters);
      msDialog.setParameterValuesToComponents();
      return;
    }
    if (rbOrbitrap.isSelected() && cbPolarity.getValue() == Polarity.Positive) {
      DefaultMsParameters.defaultOrbitrapPositiveParameters.setToParameterSet(msParameters);
      msDialog.setParameterValuesToComponents();
      return;
    }
    if (rbOrbitrap.isSelected() && cbPolarity.getValue() == Polarity.Negative) {
      DefaultMsParameters.defaultOrbitrapNegativeParameters.setToParameterSet(msParameters);
      msDialog.setParameterValuesToComponents();
      return;
    }
  }

  public void onSetLcDefaults(ActionEvent actionEvent) {
    if (rbUHPLC.isSelected()) {
      DefaultLcParameters.uhplc.setToParameterSet(hplcParameters);
      hplcDialog.setParameterValuesToComponents();
      return;
    }
    if (rbHPLC.isSelected()) {
      DefaultLcParameters.hplc.setToParameterSet(hplcParameters);
      hplcDialog.setParameterValuesToComponents();
      return;
    }
  }


  public void onRunPressed(ActionEvent actionEvent) {
    List<String> errorMessages = new ArrayList<>();
    msDialog.updateParameterSetFromComponents();
    msParameters.checkParameterValues(errorMessages);

    hplcDialog.updateParameterSetFromComponents();
    hplcParameters.checkParameterValues(errorMessages);

    files.setValueFromComponent(filesComponent);
    files.checkValue(errorMessages);

    if (!errorMessages.isEmpty()) {
      MZmineCore.getDesktop()
          .displayErrorMessage("Please check the parameters.\n" + errorMessages.toString());
      return;
    }

    final BatchQueue q = rbTOF.isSelected() ? createTofQueue() : createOrbitrapQueue();
    BatchModeParameters batchModeParameters = (BatchModeParameters) MZmineCore.getConfiguration()
        .getModuleParameters(BatchModeModule.class);
    batchModeParameters.getParameter(BatchModeParameters.batchQueue).setValue(q);
    if (batchModeParameters.showSetupDialog(false) == ExitCode.OK) {
      MZmineCore.runMZmineModule(BatchModeModule.class, batchModeParameters.cloneParameterSet());
    }
  }

  private BatchQueue createTofQueue() {
    final BatchQueue q = new BatchQueue();
    q.add(makeImportTask(msParameters, files));
    q.add(makeMassDetectionStep(msParameters, 1));
    q.add(makeMassDetectionStep(msParameters, 2));

    if (cbIonMobility.isSelected()) {
      q.add(makeImsTraceStep(msParameters));
    } else {
      q.add(makeAdapStep(msParameters));
    }

    q.add(makeSmoothingStep(true, false));
    q.add(makeRtResolvingStep(msParameters, hplcParameters));
    if (cbIonMobility.isSelected()) {
      q.add(makeSmoothingStep(false, true));
      q.add(makeMobilityResolvingStep(msParameters, hplcParameters));
      q.add(makeSmoothingStep(true, true));
    }

    q.add(makeDeisotopingStep(msParameters, hplcParameters, cbIonMobility.isSelected(),
        cbMobilityType.getValue()));
    q.add(makeAlignmentStep(msParameters, hplcParameters, cbIonMobility.isSelected(),
        cbMobilityType.getValue()));
    q.add(makeMetaCorrStep(msParameters, hplcParameters));
    q.add(makeIinStep(msParameters, hplcParameters, cbPolarity.getValue()));
    return q;
  }

  private BatchQueue createOrbitrapQueue() {
    final BatchQueue q = new BatchQueue();
    q.add(makeImportTask(msParameters, files));
    q.add(makeMassDetectionStep(msParameters, 1));
    q.add(makeMassDetectionStep(msParameters, 2));
    q.add(makeAdapStep(msParameters));
    q.add(makeSmoothingStep(true, false));
    q.add(makeRtResolvingStep(msParameters, hplcParameters));
    q.add(makeDeisotopingStep(msParameters, hplcParameters, cbIonMobility.isSelected(),
        cbMobilityType.getValue()));
    q.add(makeAlignmentStep(msParameters, hplcParameters, cbIonMobility.isSelected(),
        cbMobilityType.getValue()));
    q.add(makeMetaCorrStep(msParameters, hplcParameters));
    q.add(makeIinStep(msParameters, hplcParameters, cbPolarity.getValue()));
    return q;
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeImportTask(
      @NotNull final ParameterSet msParameters, FileNamesParameter files) {
    // todo make auto mass detector work, so we can use it here.
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(AllSpectralDataImportModule.class).cloneParameterSet();
    param.getParameter(AllSpectralDataImportParameters.advancedImport).setValue(false);
    param.getParameter(AllSpectralDataImportParameters.fileNames).setValue(files.getValue());

    return new MZmineProcessingStepImpl<MZmineProcessingModule>(
        MZmineCore.getModuleInstance(AllSpectralDataImportModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMassDetectionStep(
      @NotNull final ParameterSet msParameters, int msLevel) {

    final ParameterSet detectorParam = MZmineCore.getConfiguration()
        .getModuleParameters(AutoMassDetector.class).cloneParameterSet();
    detectorParam.getParameter(AutoMassDetectorParameters.noiseLevel).setValue(
        msLevel == 1 ? msParameters
            .getParameter(BatchWizardMassSpectrometerParameters.ms1NoiseLevel).getValue()
            : msParameters.getParameter(BatchWizardMassSpectrometerParameters.ms2NoiseLevel)
                .getValue());
    detectorParam.setParameter(AutoMassDetectorParameters.detectIsotopes, true);
    final DetectIsotopesParameter detectIsotopesParameter = detectorParam
        .getParameter(AutoMassDetectorParameters.detectIsotopes).getEmbeddedParameters();
    detectIsotopesParameter.setParameter(DetectIsotopesParameter.elements,
        List.of(new Element("H"), new Element("C"), new Element("N"), new Element("O"),
            new Element("S")));
    detectIsotopesParameter.setParameter(DetectIsotopesParameter.isotopeMzTolerance,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.featureToFeatureMzTolerance)
            .getValue());
    detectIsotopesParameter.setParameter(DetectIsotopesParameter.maxCharge, 2);

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MassDetectionModule.class).cloneParameterSet();
    param.getParameter(MassDetectionParameters.dataFiles)
        .setValue(new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    param.getParameter(MassDetectionParameters.scanSelection).setValue(new ScanSelection(msLevel));
    param.getParameter(MassDetectionParameters.massDetector).setValue(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(AutoMassDetector.class),
            detectorParam));

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(MassDetectionModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeAdapStep(
      @NotNull final ParameterSet msParameters) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ModularADAPChromatogramBuilderModule.class).cloneParameterSet();
    param.setParameter(ADAPChromatogramBuilderParameters.dataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    param.setParameter(ADAPChromatogramBuilderParameters.scanSelection, new ScanSelection(1));
    param.setParameter(ADAPChromatogramBuilderParameters.minimumScanSpan, 5);
    param.setParameter(ADAPChromatogramBuilderParameters.mzTolerance,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance)
            .getValue());
    param.setParameter(ADAPChromatogramBuilderParameters.suffix, "chroms");
    param.setParameter(ADAPChromatogramBuilderParameters.IntensityThresh2,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.ms1NoiseLevel).getValue());
    param.setParameter(ADAPChromatogramBuilderParameters.startIntensity,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.minimumFeatureHeight)
            .getValue());

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ModularADAPChromatogramBuilderModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeImsTraceStep(
      @NotNull final ParameterSet msParameters) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IonMobilityTraceBuilderModule.class).cloneParameterSet();
    param.setParameter(IonMobilityTraceBuilderParameters.rawDataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    param.setParameter(IonMobilityTraceBuilderParameters.scanSelection, new ScanSelection(1));
    param.setParameter(IonMobilityTraceBuilderParameters.minDataPointsRt, 5);
    param.setParameter(IonMobilityTraceBuilderParameters.minTotalSignals, 60);
    param.setParameter(IonMobilityTraceBuilderParameters.mzTolerance,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance)
            .getValue());
    param.setParameter(IonMobilityTraceBuilderParameters.suffix, "traces");

    ParameterSet advanced = new AdvancedImsTraceBuilderParameters().cloneParameterSet();
    advanced.setParameter(AdvancedImsTraceBuilderParameters.dtimsBinningWidth, false);
    advanced.setParameter(AdvancedImsTraceBuilderParameters.timsBinningWidth, false);
    advanced.setParameter(AdvancedImsTraceBuilderParameters.twimsBinningWidth, false);

    param.setParameter(IonMobilityTraceBuilderParameters.advancedParameters, advanced);
    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(IonMobilityTraceBuilderModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeSmoothingStep(final boolean rt,
      final boolean mobility) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(SmoothingModule.class).cloneParameterSet();
    param.setParameter(SmoothingParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(SmoothingParameters.rtSmoothing, rt);
    param.getParameter(SmoothingParameters.rtSmoothing).getEmbeddedParameter().setValue(5);
    param.setParameter(SmoothingParameters.mobilitySmoothing, mobility);
    param.getParameter(SmoothingParameters.mobilitySmoothing).getEmbeddedParameter().setValue(13);
    param.setParameter(SmoothingParameters.removeOriginal, true);
    param.setParameter(SmoothingParameters.suffix, "smthd");

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(SmoothingModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeRtResolvingStep(
      @NotNull final ParameterSet msParameters, @NotNull final ParameterSet hplcParam) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, "r");
    param.setParameter(MinimumSearchFeatureResolverParameters.AUTO_REMOVE, true);

    param.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.mzTol,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance)
            .getValue());
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.combineTimsMsMs, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.limitRTByFeature, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters()
        .setParameter(GroupMS2SubParameters.lockMS2ToFeatureMobilityRange, false);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters()
        .setParameter(GroupMS2SubParameters.rtTol, new RTTolerance(5, Unit.SECONDS));
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.outputNoiseLevel, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().getParameter(GroupMS2SubParameters.outputNoiseLevel)
        .getEmbeddedParameter().setValue(
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.ms2NoiseLevel).getValue()
            * 2);

    param.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.RETENTION_TIME);
    param
        .setParameter(MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL, 0.95);
    param.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE,
        (double) hplcParam.getParameter(BatchWizardHPLCParameters.approximateChromatographicFWHM)
            .getValue().getToleranceInMinutes());
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT, 0d);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.minimumFeatureHeight)
            .getValue());
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO, 1.8d);
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION, Range.closed(0d, 10d));
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS, 5);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MinimumSearchFeatureResolverModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMobilityResolvingStep(
      @NotNull final ParameterSet msParameters, @NotNull final ParameterSet hplcParam) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, "r");
    param.setParameter(MinimumSearchFeatureResolverParameters.AUTO_REMOVE, true);

    param.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.mzTol,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance)
            .getValue());
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.combineTimsMsMs, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.limitRTByFeature, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters()
        .setParameter(GroupMS2SubParameters.lockMS2ToFeatureMobilityRange, false);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters()
        .setParameter(GroupMS2SubParameters.rtTol, new RTTolerance(5, Unit.SECONDS));
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.outputNoiseLevel, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().getParameter(GroupMS2SubParameters.outputNoiseLevel)
        .getEmbeddedParameter().setValue(
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.ms2NoiseLevel).getValue()
            * 2);

    param.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.MOBILITY);
    param
        .setParameter(MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL, 0.80);
    param.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE, 0.01);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT, 0d);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.minimumFeatureHeight)
            .getValue());
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO, 1.8d);
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION, Range.closed(0d, 10d));
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS, 5);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MinimumSearchFeatureResolverModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeDeisotopingStep(
      ParameterSet msParameters, ParameterSet hplcParameters, boolean mobility,
      MobilityType mobilityType) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IsotopeGrouperModule.class).cloneParameterSet();

    param.setParameter(IsotopeGrouperParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(IsotopeGrouperParameters.suffix, "deiso");
    param.setParameter(IsotopeGrouperParameters.mzTolerance,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.featureToFeatureMzTolerance)
            .getValue());
    param.setParameter(IsotopeGrouperParameters.rtTolerance,
        hplcParameters.getParameter(BatchWizardHPLCParameters.intraSampleRTTolerance).getValue());
    param.setParameter(IsotopeGrouperParameters.mobilityTolerace, mobility);
    param.getParameter(IsotopeGrouperParameters.mobilityTolerace).getEmbeddedParameter().setValue(
        mobilityType == MobilityType.TIMS ? new MobilityTolerance(0.008f)
            : new MobilityTolerance(1f));
    param.setParameter(IsotopeGrouperParameters.monotonicShape, true);
    param.setParameter(IsotopeGrouperParameters.maximumCharge, 2);
    param.setParameter(IsotopeGrouperParameters.representativeIsotope,
        IsotopeGrouperParameters.ChooseTopIntensity);
    param.setParameter(IsotopeGrouperParameters.autoRemove, true);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IsotopeGrouperModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeAlignmentStep(
      @NotNull final ParameterSet msParameters, @NotNull final ParameterSet hplcParameters,
      boolean mobility, MobilityType mobilityType) {

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(JoinAlignerModule.class).cloneParameterSet();
    param.setParameter(JoinAlignerParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(JoinAlignerParameters.peakListName, "Aligned feature list");
    param.setParameter(JoinAlignerParameters.MZTolerance,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.sampleToSampleMzTolerance)
            .getValue());
    param.setParameter(JoinAlignerParameters.MZWeight, 1d);
    param.setParameter(JoinAlignerParameters.RTTolerance,
        hplcParameters.getParameter(BatchWizardHPLCParameters.interSampleRTTolerance).getValue());
    param.setParameter(JoinAlignerParameters.RTWeight, 1d);
    param.setParameter(JoinAlignerParameters.mobilityTolerance, mobility);
    param.getParameter(JoinAlignerParameters.mobilityTolerance).getEmbeddedParameter().setValue(
        mobilityType == MobilityType.TIMS ? new MobilityTolerance(0.01f)
            : new MobilityTolerance(1f));
    param.setParameter(JoinAlignerParameters.SameChargeRequired, false);
    param.setParameter(JoinAlignerParameters.SameIDRequired, false);
    param.setParameter(JoinAlignerParameters.compareIsotopePattern, false);
    param.setParameter(JoinAlignerParameters.compareSpectraSimilarity, false);
    param.setParameter(JoinAlignerParameters.removeOriginalFeatureLists, false);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(JoinAlignerModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMetaCorrStep(final ParameterSet msParam,
      final ParameterSet hplcParam) {

    final Double ms1NoiseLevel = msParameters
        .getParameter(BatchWizardMassSpectrometerParameters.ms1NoiseLevel).getValue();
    final Double minHeight = msParameters
        .getParameter(BatchWizardMassSpectrometerParameters.minimumFeatureHeight).getValue();

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(CorrelateGroupingModule.class);
    param.setParameter(CorrelateGroupingParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(CorrelateGroupingParameters.RT_TOLERANCE,
        hplcParam.getParameter(BatchWizardHPLCParameters.interSampleRTTolerance).getValue());
    param.setParameter(CorrelateGroupingParameters.GROUPSPARAMETER, false);
    param.setParameter(CorrelateGroupingParameters.MIN_HEIGHT, minHeight * 2);
    param.setParameter(CorrelateGroupingParameters.NOISE_LEVEL, ms1NoiseLevel * 2);

    var minFeaturesFilterParam = new MinimumFeaturesFilterParameters().cloneParameterSet();
    minFeaturesFilterParam.setParameter(MinimumFeaturesFilterParameters.GROUPSPARAMETER, false);
    minFeaturesFilterParam.setParameter(MinimumFeaturesFilterParameters.MIN_HEIGHT, minHeight * 2);
    minFeaturesFilterParam.setParameter(MinimumFeaturesFilterParameters.RT_TOLERANCE,
        hplcParam.getParameter(BatchWizardHPLCParameters.interSampleRTTolerance).getValue());
    minFeaturesFilterParam.setParameter(MinimumFeaturesFilterParameters.MIN_SAMPLES_GROUP,
        new AbsoluteNRelativeInt(0, 0, Mode.ROUND_DOWN));
    minFeaturesFilterParam.setParameter(MinimumFeaturesFilterParameters.MIN_SAMPLES_ALL,
        new AbsoluteNRelativeInt(1, 0, Mode.ROUND_DOWN));
    minFeaturesFilterParam
        .setParameter(MinimumFeaturesFilterParameters.MIN_INTENSITY_OVERLAP, 0.6d);
    minFeaturesFilterParam.setParameter(MinimumFeaturesFilterParameters.EXCLUDE_ESTIMATED, true);
    param.setParameter(CorrelateGroupingParameters.MIN_SAMPLES_FILTER, true);
    param.getParameter(CorrelateGroupingParameters.MIN_SAMPLES_FILTER)
        .setEmbeddedParameters((MinimumFeaturesFilterParameters) minFeaturesFilterParam);

    var fshapeCorrParam = new FeatureShapeCorrelationParameters().cloneParameterSet();
    fshapeCorrParam.setParameter(FeatureShapeCorrelationParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    fshapeCorrParam.setParameter(FeatureShapeCorrelationParameters.RT_TOLERANCE,
        hplcParam.getParameter(BatchWizardHPLCParameters.interSampleRTTolerance).getValue());
    fshapeCorrParam
        .setParameter(FeatureShapeCorrelationParameters.NOISE_LEVEL_PEAK_SHAPE, ms1NoiseLevel * 2);
//    fshapeCorrParam.setParameter(FeatureShapeCorrelationParameters.USE_MASS_LIST_DATA, true);
    fshapeCorrParam.setParameter(FeatureShapeCorrelationParameters.MIN_DP_FEATURE_EDGE, 2);
    fshapeCorrParam.setParameter(FeatureShapeCorrelationParameters.MIN_DP_CORR_PEAK_SHAPE, 5);
    fshapeCorrParam
        .setParameter(FeatureShapeCorrelationParameters.MEASURE, SimilarityMeasure.PEARSON);
    fshapeCorrParam.setParameter(FeatureShapeCorrelationParameters.MIN_R_SHAPE_INTRA, 0.85);
    fshapeCorrParam.setParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR, true);
    fshapeCorrParam.getParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR)
        .getEmbeddedParameter().setValue(0.5d);
    param.setParameter(CorrelateGroupingParameters.FSHAPE_CORRELATION, true);
    param.getParameter(CorrelateGroupingParameters.FSHAPE_CORRELATION)
        .setEmbeddedParameters((FeatureShapeCorrelationParameters) fshapeCorrParam);

    var interSampleCorrParam = new InterSampleHeightCorrParameters().cloneParameterSet();
    interSampleCorrParam.setParameter(InterSampleHeightCorrParameters.MIN_HEIGHT, minHeight * 2);
    interSampleCorrParam
        .setParameter(InterSampleHeightCorrParameters.NOISE_LEVEL, ms1NoiseLevel * 2);
    interSampleCorrParam.setParameter(InterSampleHeightCorrParameters.MIN_CORRELATION, 0.7);
    interSampleCorrParam.setParameter(InterSampleHeightCorrParameters.MIN_DP, 2);
    interSampleCorrParam
        .setParameter(InterSampleHeightCorrParameters.MEASURE, SimilarityMeasure.PEARSON);
    param.setParameter(CorrelateGroupingParameters.IMAX_CORRELATION, true);
    param.getParameter(CorrelateGroupingParameters.IMAX_CORRELATION)
        .setEmbeddedParameters((InterSampleHeightCorrParameters) interSampleCorrParam);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(CorrelateGroupingModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeIinStep(ParameterSet msParam,
      ParameterSet hplcParam, Polarity polarity) {
    final Double ms1NoiseLevel = msParameters
        .getParameter(BatchWizardMassSpectrometerParameters.ms1NoiseLevel).getValue();
    final Double minHeight = msParameters
        .getParameter(BatchWizardMassSpectrometerParameters.minimumFeatureHeight).getValue();

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IonNetworkingModule.class);
    param.setParameter(IonNetworkingParameters.MIN_HEIGHT, minHeight * 2);
    param.setParameter(IonNetworkingParameters.MZ_TOLERANCE,
        msParam.getParameter(BatchWizardMassSpectrometerParameters.featureToFeatureMzTolerance)
            .getValue());
    param.setParameter(IonNetworkingParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));

    ParameterSet refinementParam = new IonNetworkRefinementParameters().cloneParameterSet();
    refinementParam.setParameter(IonNetworkRefinementParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    refinementParam.setParameter(IonNetworkRefinementParameters.MIN_NETWORK_SIZE, false);
    refinementParam.setParameter(IonNetworkRefinementParameters.TRUE_THRESHOLD, true);
    refinementParam.getParameter(IonNetworkRefinementParameters.TRUE_THRESHOLD)
        .getEmbeddedParameter().setValue(4);
    refinementParam.setParameter(IonNetworkRefinementParameters.DELETE_SMALL_NO_MAJOR, true);
    refinementParam.setParameter(IonNetworkRefinementParameters.DELETE_ROWS_WITHOUT_ID, false);
    refinementParam.setParameter(IonNetworkRefinementParameters.DELETE_WITHOUT_MONOMER, true);
    param.setParameter(IonNetworkingParameters.ANNOTATION_REFINEMENTS, true);
    param.getParameter(IonNetworkingParameters.ANNOTATION_REFINEMENTS)
        .setEmbeddedParameters((IonNetworkRefinementParameters) refinementParam);

    ParameterSet ionLibraryParam = new IonLibraryParameterSet().cloneParameterSet();
    ionLibraryParam.setParameter(IonLibraryParameterSet.POSITIVE_MODE,
        polarity == Polarity.Positive ? "POSITIVE" : "NEGATIVE");
    ionLibraryParam.setParameter(IonLibraryParameterSet.MAX_CHARGE, 2);
    ionLibraryParam.setParameter(IonLibraryParameterSet.MAX_MOLECULES, 3);
    IonModification[] adducts =
        polarity == Polarity.Positive ? new IonModification[]{IonModification.H,
            IonModification.NA, IonModification.K, IonModification.NH4, IonModification.H2plus}
            : new IonModification[]{IonModification.H_NEG, IonModification.FA,
                IonModification.NA_2H};
    IonModification[] modifications = new IonModification[]{IonModification.H2O,
        IonModification.H2O_2, IonModification.HFA, IonModification.ACN, IonModification.MEOH};
    ionLibraryParam.setParameter(IonLibraryParameterSet.ADDUCTS,
        new IonModification[][]{adducts, modifications});
    param.getParameter(IonNetworkingParameters.LIBRARY)
        .setEmbeddedParameters((IonLibraryParameterSet) ionLibraryParam);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IonNetworkingModule.class),
        param);
  }
}
