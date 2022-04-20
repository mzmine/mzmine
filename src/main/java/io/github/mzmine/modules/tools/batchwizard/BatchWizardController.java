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
import io.github.mzmine.modules.tools.batchwizard.defaults.DefaultLcParameters;
import io.github.mzmine.modules.tools.batchwizard.defaults.DefaultMsParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.MinimumFeaturesFilterParameters;
import io.github.mzmine.parameters.parametertypes.OptionalParameterComponent;
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
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FeatureMeasurementType;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.maths.Weighting;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import io.github.mzmine.util.scans.SpectraMerging.MergingType;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

public class BatchWizardController {

  final ParameterSet wizardParam = MZmineCore.getConfiguration()
      .getModuleParameters(BatchWizardModule.class).cloneParameterSet();
  final ParameterSet hplcParameters = wizardParam.getParameter(BatchWizardParameters.hplcParams)
      .getEmbeddedParameters();
  final ParameterSetupDialog hplcDialog = new ParameterSetupDialog(false, hplcParameters);
  final ParameterSet msParameters = wizardParam.getParameter(BatchWizardParameters.msParams)
      .getEmbeddedParameters();
  final ParameterSetupDialog msDialog = new ParameterSetupDialog(false, msParameters);

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
  public ComboBox<MobilityType> cbMobilityType;
  public VBox rightMenu;
  public GridPane pnParametersMS;
  public GridPane pnParametersLC;

  private FileNamesParameter files;
  private FileNamesComponent filesComponent;
  private OptionalParameterComponent<?> exportPathComponent;

  public void initialize() {
    pnParametersMS.add(msDialog.getParamsPane(), 0, 1, 1, 1);
    pnParametersLC.add(hplcDialog.getParamsPane(), 0, 1, 1, 1);

    // add export file param
    final var exportParam = wizardParam.getParameter(BatchWizardParameters.exportPath);
    exportPathComponent = exportParam.createEditingComponent();
    final Label label = new Label(exportParam.getName());
    label.setTooltip(new Tooltip(exportParam.getDescription()));
    label.setStyle("-fx-font-weight: bold");
    HBox box = new HBox(4, label, exportPathComponent);
    box.setPadding(new Insets(5));
    box.setAlignment(Pos.CENTER_LEFT);
    rightMenu.setSpacing(4);
    rightMenu.getChildren().add(0, box);

    files = new FileNamesParameter("MS data files",
        "Please select the data files you want to process.",
        AllSpectralDataImportParameters.extensions);
    filesComponent = files.createEditingComponent();

    pnParametersMS.add(filesComponent, 0, 3, 1, 1);
    pnParametersMS.layout();

    cbIonMobility.disableProperty().bind(rbTOF.selectedProperty().not());
    cbMobilityType.disableProperty().bind(rbTOF.selectedProperty().not());
    rbOrbitrap.setSelected(true);
    rbOrbitrap.selectedProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue) {
        cbIonMobility.setSelected(false);
      }
    }));

    cbPolarity.setItems(FXCollections.observableArrayList(Polarity.values()));
    cbPolarity.setValue(Polarity.Positive);

    cbMobilityType.setItems(FXCollections.observableArrayList(MobilityType.values()));
    cbMobilityType.setValue(MobilityType.TIMS);

    rbUHPLC.setSelected(true);
  }

  public void onSetMsDefaults() {
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
    }
    if (rbOrbitrap.isSelected() && cbPolarity.getValue() == Polarity.Negative) {
      DefaultMsParameters.defaultOrbitrapNegativeParameters.setToParameterSet(msParameters);
      msDialog.setParameterValuesToComponents();
    }
  }

  public void onSetLcDefaults() {
    if (rbUHPLC.isSelected()) {
      DefaultLcParameters.uhplc.setToParameterSet(hplcParameters);
      hplcDialog.setParameterValuesToComponents();
      return;
    }
    if (rbHPLC.isSelected()) {
      DefaultLcParameters.hplc.setToParameterSet(hplcParameters);
      hplcDialog.setParameterValuesToComponents();
    }
    // TODO add GC workflow
    //    if (rbGC.isSelected()) {
    //      DefaultLcParameters.gc.setToParameterSet(hplcParameters);
    //      hplcDialog.setParameterValuesToComponents();
    //    }
  }

  public void onRunPressed() {
    List<String> errorMessages = new ArrayList<>();
    msDialog.updateParameterSetFromComponents();
    msParameters.checkParameterValues(errorMessages);

    hplcDialog.updateParameterSetFromComponents();
    hplcParameters.checkParameterValues(errorMessages);

    final var pathParam = wizardParam.getParameter(BatchWizardParameters.exportPath);
    pathParam.setValueFromComponent(exportPathComponent);
    final boolean useExport = pathParam.getValue();
    final File exportPath = useExport ? pathParam.getEmbeddedParameter().getValue() : null;

    files.setValueFromComponent(filesComponent);
    files.checkValue(errorMessages);

    if (!errorMessages.isEmpty()) {
      MZmineCore.getDesktop().displayErrorMessage("Please check the parameters.\n" + errorMessages);
      return;
    }

    final BatchQueue q = createQueue(useExport, exportPath);
    BatchModeParameters batchModeParameters = (BatchModeParameters) MZmineCore.getConfiguration()
        .getModuleParameters(BatchModeModule.class);
    batchModeParameters.getParameter(BatchModeParameters.batchQueue).setValue(q);
    if (batchModeParameters.showSetupDialog(false) == ExitCode.OK) {
      MZmineCore.runMZmineModule(BatchModeModule.class, batchModeParameters.cloneParameterSet());
    }

    // keep old settings
    MZmineCore.getConfiguration()
        .setModuleParameters(BatchWizardModule.class, wizardParam.cloneParameterSet());
  }

  private BatchQueue createQueue(boolean useExport, File exportPath) {
    final BatchQueue q = new BatchQueue();
    q.add(makeImportTask(files));
    q.add(makeMassDetectionStep(msParameters, 1));
    q.add(makeMassDetectionStep(msParameters, 2));

    if (cbIonMobility.isSelected() && Arrays.stream(filesComponent.getValue())
        .anyMatch(file -> file.getName().toLowerCase().endsWith(".mzml"))) {
      q.add(makeMobilityScanMergerStep(msParameters));
    }

    q.add(makeAdapStep(msParameters, hplcParameters));

    q.add(makeSmoothingStep(hplcParameters, true, false));
    q.add(makeRtResolvingStep(msParameters, hplcParameters,
        cbIonMobility.isSelected() && cbMobilityType.getValue() == MobilityType.TIMS));
    if (cbIonMobility.isSelected()) {
      q.add(makeImsExpanderStep(msParameters, hplcParameters));
      q.add(makeSmoothingStep(hplcParameters, false, true));
      q.add(makeMobilityResolvingStep(msParameters, hplcParameters));
      q.add(makeSmoothingStep(hplcParameters, true, true));
    }

    q.add(makeDeisotopingStep(msParameters, hplcParameters, cbIonMobility.isSelected(),
        cbMobilityType.getValue()));
    q.add(makeIsotopeFinderStep(msParameters));
    q.add(makeAlignmentStep(msParameters, hplcParameters, cbIonMobility.isSelected(),
        cbMobilityType.getValue()));
    final var rowsFilter = makeRowFilterStep(msParameters, hplcParameters);
    if (rowsFilter != null) {
      q.add(rowsFilter);
    }
    q.add(makeGapFillStep(msParameters, hplcParameters));
    if (!cbIonMobility.isSelected()) { // might filter IMS resolved isomers
      q.add(makeDuplicateRowFilterStep(msParameters, hplcParameters));
    }
    q.add(makeMetaCorrStep(msParameters, hplcParameters));
    q.add(makeIinStep(msParameters, cbPolarity.getValue()));
    if (useExport && exportPath != null) {
      q.add(makeIimnGnpsExportStep(exportPath));
      q.add(makeSiriusExportStep(exportPath));
    }
    return q;
  }

  @Nullable
  private MZmineProcessingStep<MZmineProcessingModule> makeRowFilterStep(ParameterSet msParameters,
      ParameterSet hplcParameters) {
    final int minSamples = hplcParameters.getValue(BatchWizardHPLCParameters.minNumberOfSamples);
    final boolean filter13C = hplcParameters.getValue(BatchWizardHPLCParameters.filter13C);

    if (!filter13C && minSamples < 2) {
      return null;
    }

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(RowsFilterModule.class).cloneParameterSet();
    param.setParameter(RowsFilterParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    String suffix = (filter13C ? "13C" : "");
    if (minSamples > 1) {
      suffix = suffix + (suffix.isEmpty() ? "" : " ") + "peak";
    }
    param.setParameter(RowsFilterParameters.SUFFIX, suffix);
    param.setParameter(RowsFilterParameters.MIN_FEATURE_COUNT, minSamples > 1);
    param.getParameter(RowsFilterParameters.MIN_FEATURE_COUNT).getEmbeddedParameter()
        .setValue((double) minSamples);
    // use the new isotope filter instead of two isotope peaks
    param.setParameter(RowsFilterParameters.MIN_ISOTOPE_PATTERN_COUNT, false);
    param.setParameter(RowsFilterParameters.ISOTOPE_FILTER_13C, true);

    final Isotope13CFilterParameters filterIsoParam = param.getParameter(
        RowsFilterParameters.ISOTOPE_FILTER_13C).getEmbeddedParameters();
    filterIsoParam.setParameter(Isotope13CFilterParameters.mzTolerance, msParameters.getValue(BatchWizardMassSpectrometerParameters.featureToFeatureMzTolerance));
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
    param.setParameter(RowsFilterParameters.handleOriginal, hplcParameters.getValue(BatchWizardHPLCParameters.handleOriginalFeatureLists));

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(RowsFilterModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeGapFillStep(ParameterSet msParameters,
      ParameterSet hplcParameters) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MultiThreadPeakFinderModule.class).cloneParameterSet();

    param.setParameter(MultiThreadPeakFinderParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // going back into scans so rather use scan mz tol
    param.setParameter(MultiThreadPeakFinderParameters.MZTolerance,
        msParameters.getValue(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance));
    param.setParameter(MultiThreadPeakFinderParameters.RTTolerance,
        hplcParameters.getValue(BatchWizardHPLCParameters.intraSampleRTTolerance));
    param.setParameter(MultiThreadPeakFinderParameters.intTolerance, 0.2);
    param.setParameter(MultiThreadPeakFinderParameters.handleOriginal,
        hplcParameters.getValue(BatchWizardHPLCParameters.handleOriginalFeatureLists));
    param.setParameter(MultiThreadPeakFinderParameters.suffix, "gaps");

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MultiThreadPeakFinderModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeDuplicateRowFilterStep(
      ParameterSet msParameters, ParameterSet hplcParameters) {
    // reduced rt tolerance - after gap filling the rt difference should be very small
    RTTolerance rtTol = hplcParameters.getValue(
        BatchWizardHPLCParameters.approximateChromatographicFWHM);
    rtTol = new RTTolerance(rtTol.getTolerance() * 0.7f, rtTol.getUnit());

    MZTolerance mzTol = msParameters.getValue(
        BatchWizardMassSpectrometerParameters.featureToFeatureMzTolerance);
    mzTol = new MZTolerance(mzTol.getMzTolerance() / 2f, mzTol.getPpmTolerance() / 2f);

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(DuplicateFilterModule.class).cloneParameterSet();

    param.setParameter(DuplicateFilterParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    // going back into scans so rather use scan mz tol
    param.setParameter(DuplicateFilterParameters.mzDifferenceMax, mzTol);
    param.setParameter(DuplicateFilterParameters.rtDifferenceMax, rtTol);
    param.setParameter(DuplicateFilterParameters.handleOriginal,
        hplcParameters.getValue(BatchWizardHPLCParameters.handleOriginalFeatureLists));
    param.setParameter(DuplicateFilterParameters.suffix, "dup");
    param.setParameter(DuplicateFilterParameters.requireSameIdentification, false);
    param.setParameter(DuplicateFilterParameters.filterMode, FilterMode.NEW_AVERAGE);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(DuplicateFilterModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeImportTask(FileNamesParameter files) {
    // todo make auto mass detector work, so we can use it here.
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(AllSpectralDataImportModule.class).cloneParameterSet();
    param.getParameter(AllSpectralDataImportParameters.advancedImport).setValue(false);
    param.getParameter(AllSpectralDataImportParameters.fileNames).setValue(files.getValue());
    // for now import no libraries
    param.getParameter(SpectralLibraryImportParameters.dataBaseFiles).setValue(new File[0]);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(AllSpectralDataImportModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMassDetectionStep(
      @NotNull final ParameterSet msParameters, int msLevel) {

    final ParameterSet detectorParam = MZmineCore.getConfiguration()
        .getModuleParameters(AutoMassDetector.class).cloneParameterSet();
    detectorParam.getParameter(AutoMassDetectorParameters.noiseLevel).setValue(
        msLevel == 1 ? msParameters.getParameter(
            BatchWizardMassSpectrometerParameters.ms1NoiseLevel).getValue()
            : msParameters.getParameter(BatchWizardMassSpectrometerParameters.ms2NoiseLevel)
                .getValue());
    // per default do not detect isotope signals below noise. this might introduce too many signals
    // for the isotope finder later on and confuse users
    detectorParam.setParameter(AutoMassDetectorParameters.detectIsotopes, false);
    final DetectIsotopesParameter detectIsotopesParameter = detectorParam.getParameter(
        AutoMassDetectorParameters.detectIsotopes).getEmbeddedParameters();
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
    param.getParameter(MassDetectionParameters.scanTypes).setValue(SelectedScanTypes.SCANS);
    param.getParameter(MassDetectionParameters.massDetector).setValue(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(AutoMassDetector.class),
            detectorParam));

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(MassDetectionModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMobilityScanMergerStep(
      @NotNull final ParameterSet msParameters) {

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MobilityScanMergerModule.class).cloneParameterSet();

    param.setParameter(MobilityScanMergerParameters.mzTolerance,
        msParameters.getValue(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance));
    param.setParameter(MobilityScanMergerParameters.scanSelection, new ScanSelection());
    param.setParameter(MobilityScanMergerParameters.noiseLevel,
        0d); // the noise level of the mass detector already did all the filtering we want (at least in the wizard)
    param.setParameter(MobilityScanMergerParameters.mergingType, MergingType.SUMMED);
    param.setParameter(MobilityScanMergerParameters.weightingType, Weighting.LINEAR);

    final RawDataFilesSelection rawDataFilesSelection = new RawDataFilesSelection(
        RawDataFilesSelectionType.BATCH_LAST_FILES);
    param.setParameter(MobilityScanMergerParameters.rawDataFiles, rawDataFilesSelection);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MobilityScanMergerModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeAdapStep(
      @NotNull final ParameterSet msParameters, ParameterSet hplcParameters) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ModularADAPChromatogramBuilderModule.class).cloneParameterSet();
    param.setParameter(ADAPChromatogramBuilderParameters.dataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    // crop rt range
    param.setParameter(ADAPChromatogramBuilderParameters.scanSelection, new ScanSelection(
        RangeUtils.toFloatRange(hplcParameters.getValue(BatchWizardHPLCParameters.cropRtRange)),
        1));
    param.setParameter(ADAPChromatogramBuilderParameters.minimumScanSpan,
        hplcParameters.getValue(BatchWizardHPLCParameters.minNumberOfDataPoints));
    param.setParameter(ADAPChromatogramBuilderParameters.mzTolerance,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance)
            .getValue());
    param.setParameter(ADAPChromatogramBuilderParameters.suffix, "chroms");
    param.setParameter(ADAPChromatogramBuilderParameters.minGroupIntensity,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.ms1NoiseLevel).getValue());
    param.setParameter(ADAPChromatogramBuilderParameters.minHighestPoint,
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.minimumFeatureHeight)
            .getValue());

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ModularADAPChromatogramBuilderModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeImsExpanderStep(
      @NotNull final ParameterSet msParameters, @NotNull final ParameterSet hplcParameters) {
    ParameterSet param = MZmineCore.getConfiguration().getModuleParameters(ImsExpanderModule.class)
        .cloneParameterSet();

    param.setParameter(ImsExpanderParameters.handleOriginal,
        hplcParameters.getValue(BatchWizardHPLCParameters.handleOriginalFeatureLists));
    param.setParameter(ImsExpanderParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ImsExpanderParameters.useRawData, true);
    param.getParameter(ImsExpanderParameters.useRawData).getEmbeddedParameter().setValue(1E1);
    param.setParameter(ImsExpanderParameters.mzTolerance, true);
    param.getParameter(ImsExpanderParameters.mzTolerance).getEmbeddedParameter().setValue(
        msParameters.getValue(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance));
    param.setParameter(ImsExpanderParameters.mobilogramBinWidth, false);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ImsExpanderModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeSmoothingStep(
      ParameterSet hplcParameters, final boolean rt, final boolean mobility) {
    final Integer minDP = hplcParameters.getValue(BatchWizardHPLCParameters.minNumberOfDataPoints);

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(SmoothingModule.class).cloneParameterSet();
    param.setParameter(SmoothingParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));

    ParameterSet sgParam = MZmineCore.getConfiguration()
        .getModuleParameters(SavitzkyGolaySmoothing.class).cloneParameterSet();
    sgParam.setParameter(SavitzkyGolayParameters.rtSmoothing, rt);
    sgParam.getParameter(SavitzkyGolayParameters.rtSmoothing).getEmbeddedParameter()
        .setValue(minDP > 5 ? 7 : 5);
    sgParam.setParameter(SavitzkyGolayParameters.mobilitySmoothing, mobility);
    sgParam.getParameter(SavitzkyGolayParameters.mobilitySmoothing).getEmbeddedParameter()
        .setValue(13);

    param.getParameter(SmoothingParameters.smoothingAlgorithm).setValue(
        new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(SavitzkyGolaySmoothing.class),
            sgParam));
    param.setParameter(SmoothingParameters.handleOriginal,
        hplcParameters.getValue(BatchWizardHPLCParameters.handleOriginalFeatureLists));
    param.setParameter(SmoothingParameters.suffix, "sm");

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(SmoothingModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeRtResolvingStep(
      @NotNull final ParameterSet msParameters, @NotNull final ParameterSet hplcParam,
      boolean hasIMS) {
    final Integer minDP = hplcParam.getValue(BatchWizardHPLCParameters.minNumberOfDataPoints);
    final Range<Double> rtRange = hplcParam.getValue(BatchWizardHPLCParameters.cropRtRange);
    final double totalRtWidth = rtRange.upperEndpoint() - rtRange.lowerEndpoint();
    final float fwhm = hplcParam.getValue(BatchWizardHPLCParameters.approximateChromatographicFWHM)
        .getToleranceInMinutes();
    final int maxIsomers = hplcParam.getValue(
        BatchWizardHPLCParameters.maximumIsomersInChromatogram);

    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, "r");
    param.setParameter(MinimumSearchFeatureResolverParameters.handleOriginal,
        hplcParam.getValue(BatchWizardHPLCParameters.handleOriginalFeatureLists));

    param.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, true);
    final GroupMS2SubParameters groupParam = param.getParameter(
        MinimumSearchFeatureResolverParameters.groupMS2Parameters).getEmbeddedParameters();
    groupParam.setParameter(GroupMS2SubParameters.mzTol,
        msParameters.getValue(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance));
    groupParam.setParameter(GroupMS2SubParameters.combineTimsMsMs, false);
    boolean limitByRTEdges = minDP >= 4;
    groupParam.setParameter(GroupMS2SubParameters.limitRTByFeature, limitByRTEdges);
    groupParam.setParameter(GroupMS2SubParameters.lockMS2ToFeatureMobilityRange, true);
    // rt tolerance is +- while FWHM is the width. still the MS2 might be triggered very early
    // change rt tol depending on number of datapoints
    groupParam.setParameter(GroupMS2SubParameters.rtTol,
        new RTTolerance(limitByRTEdges ? fwhm * 3 : fwhm, Unit.MINUTES));
    groupParam.setParameter(GroupMS2SubParameters.outputNoiseLevel, hasIMS);
    groupParam.getParameter(GroupMS2SubParameters.outputNoiseLevel).getEmbeddedParameter().setValue(
        msParameters.getParameter(BatchWizardMassSpectrometerParameters.ms2NoiseLevel).getValue()
        * 2);

    param.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.RETENTION_TIME);
    // should be relatively high - unless user suspects many same m/z peaks in chromatogram
    // e.g., isomers or fragments in GC-EI-MS
    // 10 isomers, 0.05 min FWHM, 10 min total time = 0.90 threshold
    // ranges from 0.3 - 0.9
    final double thresholdPercent = MathUtils.within(1d - fwhm * maxIsomers / totalRtWidth * 2d,
        0.3, 0.9, 3);

    param.setParameter(MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL,
        thresholdPercent);
    param.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE, (double) fwhm);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT, 0d);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT,
        msParameters.getValue(BatchWizardMassSpectrometerParameters.minimumFeatureHeight));
    final double ratioTopToEdge = minDP == 3 ? 1.4 : (minDP == 4 ? 1.8 : 2);
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO, ratioTopToEdge);
    param.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION,
        Range.closed(0d, fwhm * 30d));
    param.setParameter(MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS, minDP);

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
    param.setParameter(MinimumSearchFeatureResolverParameters.handleOriginal,
        hplcParam.getValue(BatchWizardHPLCParameters.handleOriginalFeatureLists));

    param.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, true);
    param.getParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters)
        .getEmbeddedParameters().setParameter(GroupMS2SubParameters.mzTol,
            msParameters.getParameter(BatchWizardMassSpectrometerParameters.scanToScanMzTolerance)
                .getValue());
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
        .getEmbeddedParameter().setValue(
            msParameters.getParameter(BatchWizardMassSpectrometerParameters.ms2NoiseLevel).getValue()
            * 2);

    param.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.MOBILITY);
    param.setParameter(MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL,
        0.80);
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
    param.setParameter(IsotopeGrouperParameters.keepAllMS2, true);
    param.setParameter(IsotopeGrouperParameters.maximumCharge, 2);
    param.setParameter(IsotopeGrouperParameters.representativeIsotope,
        IsotopeGrouperParameters.ChooseTopIntensity);
    param.setParameter(IsotopeGrouperParameters.handleOriginal,
        hplcParameters.getValue(BatchWizardHPLCParameters.handleOriginalFeatureLists));

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IsotopeGrouperModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeIsotopeFinderStep(
      ParameterSet msParameters) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IsotopeFinderModule.class).cloneParameterSet();

    param.setParameter(IsotopeFinderParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(IsotopeFinderParameters.isotopeMzTolerance,
        msParameters.getValue(BatchWizardMassSpectrometerParameters.featureToFeatureMzTolerance));
    param.setParameter(IsotopeFinderParameters.maxCharge, 1);
    param.setParameter(IsotopeFinderParameters.scanRange, ScanRange.SINGLE_MOST_INTENSE);
    param.setParameter(IsotopeFinderParameters.elements,
        List.of(new Element("H"), new Element("C"), new Element("N"), new Element("O"),
            new Element("S")));

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(IsotopeFinderModule.class),
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
    param.setParameter(JoinAlignerParameters.MZWeight, 3d);
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
    param.setParameter(JoinAlignerParameters.handleOriginal,
        hplcParameters.getValue(BatchWizardHPLCParameters.handleOriginalFeatureLists));

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(JoinAlignerModule.class),
        param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeMetaCorrStep(final ParameterSet msParam,
      final ParameterSet hplcParam) {

    final double ms1NoiseLevel = msParam.getValue(
        BatchWizardMassSpectrometerParameters.ms1NoiseLevel);
    final int minSamples = hplcParam.getValue(BatchWizardHPLCParameters.minNumberOfSamples);
    final int minDP = hplcParam.getValue(BatchWizardHPLCParameters.minNumberOfDataPoints);
    final boolean stableIonization = hplcParam.getValue(
        BatchWizardHPLCParameters.stableIonizationAcrossSamples);
    final boolean useCorrGrouping = minDP > 3;
    RTTolerance rtTol = hplcParam.getValue(
        BatchWizardHPLCParameters.approximateChromatographicFWHM);
    rtTol = new RTTolerance(rtTol.getTolerance() * (useCorrGrouping ? 1.1f : 0.7f),
        rtTol.getUnit());

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(CorrelateGroupingModule.class);
    param.setParameter(CorrelateGroupingParameters.PEAK_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(CorrelateGroupingParameters.RT_TOLERANCE, rtTol);
    param.setParameter(CorrelateGroupingParameters.GROUPSPARAMETER, false);
    param.setParameter(CorrelateGroupingParameters.MIN_HEIGHT, 0d);
    param.setParameter(CorrelateGroupingParameters.NOISE_LEVEL, ms1NoiseLevel);
    param.setParameter(CorrelateGroupingParameters.MIN_SAMPLES_FILTER, true);

    // min samples
    var minSampleP = param.getParameter(CorrelateGroupingParameters.MIN_SAMPLES_FILTER)
        .getEmbeddedParameters();
    minSampleP.setParameter(MinimumFeaturesFilterParameters.MIN_SAMPLES_GROUP,
        new AbsoluteNRelativeInt(0, 0, Mode.ROUND_DOWN));
    minSampleP.setParameter(MinimumFeaturesFilterParameters.MIN_SAMPLES_ALL,
        new AbsoluteNRelativeInt(minSamples, 0, Mode.ROUND_DOWN));
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
    param.setParameter(CorrelateGroupingParameters.IMAX_CORRELATION, stableIonization);
    var interSampleCorrParam = param.getParameter(CorrelateGroupingParameters.IMAX_CORRELATION)
        .getEmbeddedParameters();
    interSampleCorrParam.setParameter(InterSampleHeightCorrParameters.MIN_CORRELATION, 0.7);
    interSampleCorrParam.setParameter(InterSampleHeightCorrParameters.MIN_DP, 2);
    interSampleCorrParam.setParameter(InterSampleHeightCorrParameters.MEASURE,
        SimilarityMeasure.PEARSON);

    return new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(CorrelateGroupingModule.class), param);
  }

  private MZmineProcessingStep<MZmineProcessingModule> makeIinStep(ParameterSet msParam,
      Polarity polarity) {

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(IonNetworkingModule.class);
    param.setParameter(IonNetworkingParameters.MIN_HEIGHT, 0d);
    param.setParameter(IonNetworkingParameters.MZ_TOLERANCE,
        msParam.getValue(BatchWizardMassSpectrometerParameters.featureToFeatureMzTolerance));
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

  // export
  private MZmineProcessingStep<MZmineProcessingModule> makeIimnGnpsExportStep(File exportPath) {
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

  private MZmineProcessingStep<MZmineProcessingModule> makeSiriusExportStep(File exportPath) {
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
