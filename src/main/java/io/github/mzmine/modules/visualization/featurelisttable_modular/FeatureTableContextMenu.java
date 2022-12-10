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


package io.github.mzmine.modules.visualization.featurelisttable_modular;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.fx.ColumnType;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_manual.XICManualPickerModule;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.FormulaPredictionModule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchModule;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDBSearchModule;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchModule;
import io.github.mzmine.modules.io.export_features_gnps.masst.GnpsMasstSubmitModule;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportModule;
import io.github.mzmine.modules.io.export_image_csv.ImageToCsvExportModule;
import io.github.mzmine.modules.io.spectraldbsubmit.view.MSMSLibrarySubmissionWindow;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerTab;
import io.github.mzmine.modules.visualization.compdb.CompoundDatabaseMatchTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.export.IsotopePatternExportModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.export.MSMSExportModule;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerModule;
import io.github.mzmine.modules.visualization.image.ImageVisualizerModule;
import io.github.mzmine.modules.visualization.image.ImageVisualizerParameters;
import io.github.mzmine.modules.visualization.image.ImageVisualizerTab;
import io.github.mzmine.modules.visualization.ims_featurevisualizer.IMSFeatureVisualizerTab;
import io.github.mzmine.modules.visualization.ims_mobilitymzplot.IMSMobilityMzPlotModule;
import io.github.mzmine.modules.visualization.intensityplot.IntensityPlotModule;
import io.github.mzmine.modules.visualization.rawdataoverviewims.IMSRawDataOverviewModule;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidSpectrumTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.MultiSpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowFXML;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.SpectraStackVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsModule;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.components.ConditionalMenuItem;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Conditions should be chosen in a way that exceptions are impossible when the item is clicked. On
 * click events should not be longer than a few lines and should be moved to helper classes if
 * otherwise.
 */
public class FeatureTableContextMenu extends ContextMenu {

  private static final Logger logger = Logger.getLogger(FeatureTableContextMenu.class.getName());
  final Menu showMenu;
  final Menu searchMenu;
  final Menu idsMenu;
  final Menu exportMenu;
  private final FeatureTableFX table;
  @Nullable ModularFeatureListRow selectedRow;
  private Set<DataType<?>> selectedRowTypes;
  private Set<DataType<?>> selectedFeatureTypes;
  private Set<RawDataFile> selectedFiles;
  private List<ModularFeature> selectedFeatures;
  private List<ModularFeatureListRow> selectedRows;
  @Nullable
  private ModularFeature selectedFeature;
  private List<FeatureIdentity> copiedIDs;

  FeatureTableContextMenu(final FeatureTableFX table) {
    this.table = table;

    showMenu = new Menu("Show");
    searchMenu = new Menu("Search");
    exportMenu = new Menu("Export");
    idsMenu = new Menu("Identification");
    this.getItems().addAll(showMenu, searchMenu, idsMenu, exportMenu);

    this.setOnShowing(event -> onShown());

    initShowMenu();
    initSearchMenu();
    initExportMenu();
    initIdentitiesMenu();

    final MenuItem deleteRowsItem = new ConditionalMenuItem("Delete row(s)",
        () -> !selectedRows.isEmpty());
    deleteRowsItem.setOnAction(
        e -> selectedRows.forEach(row -> table.getFeatureList().removeRow(row)));

    // final MenuItem addNewRowItem;
    final MenuItem manuallyDefineItem = new ConditionalMenuItem("Define manually",
        () -> selectedRows.size() == 1 && selectedFeature != null);
    manuallyDefineItem.setOnAction(
        e -> XICManualPickerModule.runManualDetection(selectedFeature.getRawDataFile(),
            selectedRows.get(0), table.getFeatureList()));

    getItems().addAll(new SeparatorMenuItem(), manuallyDefineItem, deleteRowsItem);
  }

  private void initIdentitiesMenu() {
    final MenuItem openCompoundIdUrl = new ConditionalMenuItem("Open compound ID URL (disabled)",
        () -> false);

    final MenuItem copyIdsItem = new ConditionalMenuItem("Copy identities",
        () -> !selectedRows.isEmpty() && !selectedRows.get(0).getPeakIdentities().isEmpty());
    copyIdsItem.setOnAction(e -> copiedIDs = selectedRows.get(0).getPeakIdentities());

    final MenuItem pasteIdsItem = new ConditionalMenuItem("Paste identities",
        () -> !selectedRows.isEmpty() && copiedIDs != null);
    pasteIdsItem.setOnAction(e -> {
      ObservableList<FeatureIdentity> copy = FXCollections.observableArrayList();
      FXCollections.copy(copy, copiedIDs);
      selectedRows.get(0).setPeakIdentities(copy);
    });

    final MenuItem clearIdsItem = new ConditionalMenuItem("Clear identities",
        () -> selectedRows.size() > 0 && selectedRows.get(0).getPeakIdentities().size() > 0);
    clearIdsItem.setOnAction(e -> selectedRows.forEach(
        row -> row.setPeakIdentities(FXCollections.observableArrayList())));

    // add the same menu for all datatypes
    final Menu clearAnnotationsMenu = new Menu("Clear annotations");
    DataTypes.getInstances().stream().forEach(dt -> {
      if (!(dt instanceof ListWithSubsType<?> listType && dt instanceof AnnotationType)) {
        return;
      }
      if (dt instanceof IonIdentityListType) {
        // disabled for now, remove operation requires further implementations
        return;
      }

      final MenuItem deleteTopAnnotation = new ConditionalMenuItem(
          "Delete selected " + listType.getHeaderString(), () -> selectedRow.get(listType) != null);
      deleteTopAnnotation.setOnAction(e -> {
        var value = selectedRow.get(listType);
        List<?> newList = new ArrayList<>(value);
        newList.remove(0);
        selectedRow.set(dt, newList);
        table.refresh();
      });
      final MenuItem clearAllAnnotations = new ConditionalMenuItem(
          "Clear all " + listType.getHeaderString(), () -> selectedRow.get(listType) != null);
      clearAllAnnotations.setOnAction(e -> {
        selectedRow.set(listType, null);
        table.refresh();
      });
      clearAnnotationsMenu.getItems()
          .addAll(deleteTopAnnotation, clearAllAnnotations, new SeparatorMenuItem());
    });

    idsMenu.getItems()
        .addAll(openCompoundIdUrl, copyIdsItem, pasteIdsItem, clearIdsItem, clearAnnotationsMenu);
  }

  private void initExportMenu() {
    final MenuItem exportIsotopesItem = new ConditionalMenuItem("Export isotope pattern",
        () -> selectedRows.size() == 1 && selectedRows.get(0).getBestIsotopePattern() != null);
    exportIsotopesItem.setOnAction(
        e -> IsotopePatternExportModule.exportIsotopePattern(selectedRows.get(0)));

    final MenuItem exportMSMSItem = new ConditionalMenuItem("Export MS/MS pattern",
        () -> selectedRows.size() == 1 && selectedRows.get(0).getMostIntenseFragmentScan() != null);
    exportMSMSItem.setOnAction(e -> MSMSExportModule.exportMSMS(selectedRows.get(0)));

    final MenuItem exportToSirius = new ConditionalMenuItem("Export to Sirius",
        () -> !selectedRows.isEmpty());
    exportToSirius.setOnAction(
        e -> SiriusExportModule.exportSingleRows(selectedRows.toArray(new ModularFeatureListRow[0]),
            Instant.now()));

    final MenuItem exportMS1Library = new ConditionalMenuItem("Export to MS1 library",
        () -> !selectedRows.isEmpty());
    exportMS1Library.setOnAction(e -> MZmineCore.runLater(() -> {
      MSMSLibrarySubmissionWindow window = new MSMSLibrarySubmissionWindow();
      window.setData(selectedRows.toArray(new ModularFeatureListRow[0]), SortingProperty.MZ,
          SortingDirection.Ascending, false);
      window.show();
    }));

    final MenuItem exportMSMSLibrary = new ConditionalMenuItem("Export to MS/MS library",
        () -> !selectedRows.isEmpty());
    exportMSMSLibrary.setOnAction(e -> MZmineCore.runLater(() -> {
      MSMSLibrarySubmissionWindow window = new MSMSLibrarySubmissionWindow();
      window.setData(selectedRows.toArray(new ModularFeatureListRow[0]), SortingProperty.MZ,
          SortingDirection.Ascending, true);
      window.show();
    }));

    final MenuItem exportImageToCsv = new ConditionalMenuItem("Export image to .csv",
        () -> !selectedRows.isEmpty() && selectedRows.get(0).hasFeatureType(ImageType.class));
    exportImageToCsv.setOnAction(
        e -> ImageToCsvExportModule.showExportDialog(selectedRows, Instant.now()));

    // export menu
    exportMenu.getItems()
        .addAll(exportIsotopesItem, exportMSMSItem, exportToSirius, new SeparatorMenuItem(),
            exportMS1Library, exportMSMSLibrary, new SeparatorMenuItem(), exportImageToCsv);
  }

  private void initSearchMenu() {
    final MenuItem onlineDbSearchItem = new ConditionalMenuItem("Online compound database search",
        () -> selectedRows.size() == 1);
    onlineDbSearchItem.setOnAction(
        e -> OnlineDBSearchModule.showSingleRowIdentificationDialog(selectedRows.get(0),
            Instant.now()));

    final MenuItem spectralDbSearchItem = new ConditionalMenuItem("Spectral library search",
        () -> selectedRows.size() >= 1);
    spectralDbSearchItem.setOnAction(
        e -> SpectralLibrarySearchModule.showSelectedRowsIdentificationDialog(
            new ArrayList<>(selectedRows), table, Instant.now()));

    final MenuItem nistSearchItem = new ConditionalMenuItem("NIST MS search",
        () -> selectedRows.size() == 1);
    nistSearchItem.setOnAction(
        e -> NistMsSearchModule.singleRowSearch(table.getFeatureList(), selectedRows.get(0)));

    // submit GNPS MASST search job
    final MenuItem masstSearch = new ConditionalMenuItem(
        "Submit MASST public data search (on GNPS)",
        () -> selectedRows.size() == 1 && getNumberOfRowsWithFragmentScans(selectedRows) >= 1);
    masstSearch.setOnAction(e -> submitMasstGNPSSearch(selectedRows));

    final MenuItem formulaPredictionItem = new ConditionalMenuItem("Predict molecular formula",
        () -> selectedRows.size() == 1);
    formulaPredictionItem.setOnAction(
        e -> FormulaPredictionModule.showSingleRowIdentificationDialog(selectedRows.get(0)));

    searchMenu.getItems()
        .addAll(onlineDbSearchItem, spectralDbSearchItem, nistSearchItem, new SeparatorMenuItem(),
            formulaPredictionItem, new SeparatorMenuItem(), masstSearch);
  }

  private void initShowMenu() {
    final MenuItem showXICItem = new ConditionalMenuItem("XIC (quick)",
        () -> !selectedRows.isEmpty());
    showXICItem.setOnAction(
        e -> ChromatogramVisualizerModule.visualizeFeatureListRows(selectedRows));

    final MenuItem showXICSetupItem = new ConditionalMenuItem("XIC (dialog)",
        () -> !selectedRows.isEmpty());
    showXICSetupItem.setOnAction(
        e -> ChromatogramVisualizerModule.setUpVisualiserFromFeatures(selectedRows,
            selectedFeature != null ? selectedFeature.getRawDataFile() : null));

    final MenuItem showIMSFeatureItem = new ConditionalMenuItem("Ion mobility trace",
        () -> !selectedRows.isEmpty() && selectedFeature != null
            && selectedFeature.getRawDataFile() instanceof IMSRawDataFile);
    showIMSFeatureItem.setOnAction(
        e -> MZmineCore.getDesktop().addTab(new IMSFeatureVisualizerTab(selectedFeature)));

    final MenuItem showImageFeatureItem = new ConditionalMenuItem("Image",
        () -> !selectedRows.isEmpty() && selectedFeature != null
            && selectedFeature.getRawDataFile() instanceof ImagingRawDataFile);
    showImageFeatureItem.setOnAction(e -> {
      ParameterSet params = MZmineCore.getConfiguration()
          .getModuleParameters(ImageVisualizerModule.class).cloneParameterSet();
      params.setParameter(ImageVisualizerParameters.imageNormalization,
          MZmineCore.getConfiguration().getImageNormalization()); // same as in feature table.
      MZmineCore.getDesktop().addTab(new ImageVisualizerTab(selectedFeature, params));
    });

    final MenuItem show2DItem = new ConditionalMenuItem("Feature in 2D",
        () -> selectedFeature != null);
    show2DItem.setOnAction(
        e -> TwoDVisualizerModule.show2DVisualizerSetupDialog(selectedFeature.getRawDataFile(),
            selectedFeature.getRawDataPointsMZRange(), selectedFeature.getRawDataPointsRTRange()));

    final MenuItem show3DItem = new ConditionalMenuItem("Feature in 3D",
        () -> selectedFeature != null);
    show3DItem.setOnAction(
        e -> Fx3DVisualizerModule.setupNew3DVisualizer(selectedFeature.getRawDataFile(),
            selectedFeature.getRawDataPointsMZRange(), selectedFeature.getRawDataPointsRTRange(),
            selectedFeature));

    final MenuItem showIntensityPlotItem = new ConditionalMenuItem(
        "Plot using Intensity plot module",
        () -> !selectedRows.isEmpty() && selectedFeature != null);
    showIntensityPlotItem.setOnAction(e -> IntensityPlotModule.showIntensityPlot(
        MZmineCore.getProjectManager().getCurrentProject(), selectedFeature.getFeatureList(),
        selectedRows.toArray(new ModularFeatureListRow[0])));

    final MenuItem showInIMSRawDataOverviewItem = new ConditionalMenuItem(
        "Show m/z ranges in IMS raw data overview",
        () -> selectedFeature != null && selectedFeature.getRawDataFile() instanceof IMSRawDataFile
            && !selectedFeatures.isEmpty());
    showInIMSRawDataOverviewItem.setOnAction(
        e -> IMSRawDataOverviewModule.openIMSVisualizerTabWithFeatures(
            getFeaturesFromSelectedRaw(selectedFeatures)));

    final MenuItem showInMobilityMzVisualizerItem = new ConditionalMenuItem(
        "Plot mobility/CCS vs. m/z", () -> !selectedRows.isEmpty());
    showInMobilityMzVisualizerItem.setOnAction(e -> {
      IMSMobilityMzPlotModule.visualizeFeaturesInNewTab(selectedRows, false);
    });

    final MenuItem showSpectrumItem = new ConditionalMenuItem("Mass spectrum",
        () -> selectedFeature != null && selectedFeature.getRepresentativeScan() != null);
    showSpectrumItem.setOnAction(
        e -> SpectraVisualizerModule.addNewSpectrumTab(selectedFeature.getRawDataFile(),
            selectedFeature.getRepresentativeScan(), selectedFeature));

    final MenuItem showFeatureFWHMMs1Item = new ConditionalMenuItem(
        "Accumulated mass spectrum (FWHM)",
        () -> selectedFeature != null && selectedFeature.getFeatureData() != null);
    showFeatureFWHMMs1Item.setOnAction(e -> {
      final Float fwhm = selectedFeature.getFWHM();
      if (fwhm != null) {
        final Range<Float> range = Range.closed(selectedFeature.getRT() - fwhm / 2,
            selectedFeature.getRT() + fwhm / 2);
        List<Scan> scans = (List<Scan>) selectedFeature.getFeatureData().getSpectra().stream()
            .filter(s -> range.contains(s.getRetentionTime())).toList();
        MergedMassSpectrum spectrum = SpectraMerging.mergeSpectra(scans,
            SpectraMerging.defaultMs1MergeTol, MergingType.ALL, null);
        SpectraVisualizerModule.addNewSpectrumTab(spectrum);
      }
    });

    final MenuItem showBestMobilityScanItem = new ConditionalMenuItem("Best mobility scan",
        () -> selectedFeature != null && selectedFeature.getRepresentativeScan() instanceof Frame
            && selectedFeature.getFeatureData() instanceof IonMobilogramTimeSeries);
    showBestMobilityScanItem.setOnAction(e -> SpectraVisualizerModule.addNewSpectrumTab(
        IonMobilityUtils.getBestMobilityScan(selectedFeature)));

    final MenuItem extractSumSpectrumFromMobScans = new ConditionalMenuItem(
        "Extract spectrum from mobility FWHM", () -> selectedFeature != null
        && selectedFeature.getFeatureData() instanceof IonMobilogramTimeSeries);
    extractSumSpectrumFromMobScans.setOnAction(e -> {
      Range<Float> fwhm = IonMobilityUtils.getMobilityFWHM(
          ((IonMobilogramTimeSeries) selectedFeature.getFeatureData()).getSummedMobilogram());
      if (fwhm != null) {
        MergedMassSpectrum spectrum = SpectraMerging.extractSummedMobilityScan(selectedFeature,
            SpectraMerging.defaultMs1MergeTol, fwhm, null);
        SpectraVisualizerModule.addNewSpectrumTab(selectedFeature.getRawDataFile(), spectrum,
            selectedFeature);
      }
    });

    // TODO this should display selected features instead of rows. MultiMSMSWindow does not support
    // that, however.
    final MenuItem showMSMSItem = new ConditionalMenuItem("Most intense MS/MS",
        () -> (selectedRow != null && getNumberOfFeaturesWithFragmentScans(selectedRow) >= 1) || (
            selectedFeature != null && selectedFeature.getMostIntenseFragmentScan() != null) || (
            selectedRows.size() > 1 && getNumberOfRowsWithFragmentScans(selectedRows) > 1));
    showMSMSItem.setOnAction(e -> {
      if (selectedFeature != null && selectedFeature.getMostIntenseFragmentScan() != null) {
        SpectraVisualizerModule.addNewSpectrumTab(selectedFeature.getMostIntenseFragmentScan());
      } else if (selectedRows.size() > 1 && getNumberOfRowsWithFragmentScans(selectedRows) > 1) {
        SpectraStackVisualizerModule.addMsMsStackVisualizer(selectedRows,
            table.getFeatureList().getRawDataFiles(), selectedRows.get(0).getRawDataFiles().get(0));
      } else if (selectedRow != null && selectedRow.getMostIntenseFragmentScan() != null) {
        SpectraVisualizerModule.addNewSpectrumTab(selectedRow.getMostIntenseFragmentScan());
      }
    });

    final MenuItem showDiaIons = new ConditionalMenuItem("Show DIA ion shapes",
        () -> selectedFeature != null
            && selectedFeature.getMostIntenseFragmentScan() instanceof PseudoSpectrum);
    showDiaIons.setOnAction(e -> showDiaMsMsIons());

    final MenuItem showDiaMirror = new ConditionalMenuItem(
        "DIA spectral mirror: Correlated-to-all signals",
        () -> selectedFeature != null && selectedFeature.getRawDataFile() instanceof IMSRawDataFile
            && selectedFeature.getFeatureData() instanceof IonMobilogramTimeSeries
            && selectedFeature.getMostIntenseFragmentScan() instanceof PseudoSpectrum);
    showDiaMirror.setOnAction(e -> showDiaMirror());

    final MenuItem showMSMSMirrorItem = new ConditionalMenuItem("Mirror MS/MS (2 rows)",
        () -> selectedRows.size() == 2 && getNumberOfRowsWithFragmentScans(selectedRows) == 2);
    showMSMSMirrorItem.setOnAction(e -> {
      MirrorScanWindowFXML mirrorScanTab = new MirrorScanWindowFXML();
      mirrorScanTab.getController().setScans(selectedRows.get(0).getMostIntenseFragmentScan(),
          selectedRows.get(1).getMostIntenseFragmentScan());
      mirrorScanTab.show();
    });

    final MenuItem showAllMSMSItem = new ConditionalMenuItem("All MS/MS",
        () -> !selectedRows.isEmpty() && !selectedRows.get(0).getAllFragmentScans().isEmpty());
    showAllMSMSItem.setOnAction(
        e -> MultiSpectraVisualizerTab.addNewMultiSpectraVisualizerTab(selectedRows.get(0)));

    final MenuItem showIsotopePatternItem = new ConditionalMenuItem("Isotope pattern",
        () -> selectedFeature != null && selectedFeature.getIsotopePattern() != null);
    showIsotopePatternItem.setOnAction(
        e -> SpectraVisualizerModule.addNewSpectrumTab(selectedFeature.getRawDataFile(),
            selectedFeature.getRepresentativeScan(), selectedFeature.getIsotopePattern()));

    final MenuItem showCompoundDBResults = new ConditionalMenuItem("Compound DB search results",
        () -> selectedRow != null && !selectedRow.getCompoundAnnotations().isEmpty());
    showCompoundDBResults.setOnAction(e -> CompoundDatabaseMatchTab.addNewTab(table));

    final MenuItem showSpectralDBResults = new ConditionalMenuItem("Spectral DB search results",
        () -> !selectedRows.isEmpty() && rowHasSpectralLibraryMatches(selectedRows));
    showSpectralDBResults.setOnAction(
        e -> SpectraIdentificationResultsModule.showNewTab(selectedRows));

    final MenuItem showMatchedLipidSignals = new ConditionalMenuItem("Matched lipid signals",
        () -> !selectedRows.isEmpty() && rowHasMatchedLipidSignals(selectedRows.get(0)));
    showMatchedLipidSignals.setOnAction(e -> {
      List<MatchedLipid> matchedLipids = selectedRows.get(0).get(LipidMatchListType.class);
      if (matchedLipids != null && !matchedLipids.isEmpty()) {
        MatchedLipidSpectrumTab matchedLipidSpectrumTab = new MatchedLipidSpectrumTab(
            matchedLipids.get(0).getLipidAnnotation().getAnnotation() + " Matched Signals",
            new LipidSpectrumChart(selectedRows.get(0), null));
        MZmineCore.getDesktop().addTab(matchedLipidSpectrumTab);
      }
    });

    final MenuItem showPeakRowSummaryItem = new ConditionalMenuItem("Row(s) summary", () ->
        /* !selectedRows.isEmpty() */ false); // todo, not implemented yet

    showMenu.getItems()
        .addAll(showXICItem, showXICSetupItem, showIMSFeatureItem, showImageFeatureItem,
            new SeparatorMenuItem(), show2DItem, show3DItem, showIntensityPlotItem,
            showInIMSRawDataOverviewItem, showInMobilityMzVisualizerItem, new SeparatorMenuItem(),
            showSpectrumItem, showFeatureFWHMMs1Item, showBestMobilityScanItem,
            extractSumSpectrumFromMobScans, showMSMSItem, showMSMSMirrorItem, showAllMSMSItem,
            showDiaIons, showDiaMirror, new SeparatorMenuItem(), showIsotopePatternItem,
            showCompoundDBResults, showSpectralDBResults, showMatchedLipidSignals,
            new SeparatorMenuItem(), showPeakRowSummaryItem);
  }

  private void onShown() {
    selectedRowTypes = table.getSelectedDataTypes(ColumnType.ROW_TYPE);
    selectedFeatureTypes = table.getSelectedDataTypes(ColumnType.FEATURE_TYPE);
    selectedFiles = table.getSelectedRawDataFiles();
    selectedFeatures = table.getSelectedFeatures();
    selectedRows = table.getSelectedRows();
    selectedFeature = table.getSelectedFeature();
    selectedRow = table.getSelectedRow();

    // for single-raw-file-feature-lists it's intuitive to be able to click on the row columns, too
    if (selectedFeature == null && selectedRows.size() == 1
        && selectedRows.get(0).getRawDataFiles().size() == 1) {
      selectedFeature = selectedRows.get(0)
          .getFeature(selectedRows.get(0).getRawDataFiles().get(0));
    }

    for (MenuItem item : getItems()) {
      updateItem(item);
    }
  }

  /**
   * Mass spectrometry search tool job on GNPS
   */
  private void submitMasstGNPSSearch(List<ModularFeatureListRow> rows) {
    // single
    if (rows.size() == 1) {
      final ModularFeatureListRow row = rows.get(0);
      final Scan ms2 = row.getMostIntenseFragmentScan();
      if (ms2 != null) {
        if (ms2.getMassList() == null) {
          logger.warning("Missing mass list. Run mass detection on MS2 scans to run MASST search");
          return;
        }
        GnpsMasstSubmitModule.submitSingleMASSTJob(row, row.getAverageMZ(), ms2.getMassList());
      }
    }
  }

  private void updateItem(MenuItem item) {
    if (item instanceof ConditionalMenuItem conditionalMenuItem) {
      conditionalMenuItem.updateVisibility();
    }
    if (item instanceof Menu menu) {
      menu.getItems().forEach(this::updateItem);
    }
  }

  private int getNumberOfRowsWithFragmentScans(Collection<ModularFeatureListRow> rows) {
    if (rows.isEmpty()) {
      return 0;
    }
    int numFragmentScans = 0;
    for (ModularFeatureListRow row : rows) {
      if (row.getMostIntenseFragmentScan() != null) {
        numFragmentScans++;
      }
    }
    return numFragmentScans;
  }

  private int getNumberOfFeaturesWithFragmentScans(@Nullable ModularFeatureListRow row) {
    if (row == null) {
      return 0;
    }

    int num = 0;
    for (Feature feature : row.getFeatures()) {
      if (feature != null && feature.getFeatureStatus() != FeatureStatus.UNKNOWN
          && feature.getMostIntenseFragmentScan() != null) {
        num++;
      }
    }
    return num;
  }

  private boolean rowHasSpectralLibraryMatches(List<ModularFeatureListRow> rows) {
    for (ModularFeatureListRow row : rows) {
      if (!row.getSpectralLibraryMatches().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private boolean rowHasMatchedLipidSignals(ModularFeatureListRow row) {
    List<MatchedLipid> matches = row.get(LipidMatchListType.class);
    return matches != null && !matches.isEmpty();
  }

  @NotNull
  private List<ModularFeature> getFeaturesFromSelectedRaw(Collection<ModularFeature> features) {
    if (selectedFeature == null || selectedFeature.getRawDataFile() == null) {
      return Collections.emptyList();
    }
    final RawDataFile file = selectedFeature.getRawDataFile();
    return features.stream().filter(f -> f.getRawDataFile() == file).collect(Collectors.toList());
  }

  private void showDiaMsMsIons() {
    final Scan msms = selectedFeature.getMostIntenseFragmentScan();
    final RawDataFile file = selectedFeature.getRawDataFile();
    ScanSelection selection = new ScanSelection(
        Range.closed(selectedFeature.getRawDataPointsRTRange().lowerEndpoint() - 1,
            selectedFeature.getRawDataPointsRTRange().upperEndpoint() + 1), 2);
    final List<Scan> matchingScans = selection.getMatchingScans(file.getScans());
    MZTolerance tol = new MZTolerance(0.005, 15);

    TICVisualizerTab window = new TICVisualizerTab(new RawDataFile[]{file}, TICPlotType.BASEPEAK,
        new ScanSelection(1), tol.getToleranceRange(selectedFeature.getMZ()), null, null);

    final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    for (int i = 0; i < msms.getNumberOfDataPoints(); i++) {
      TICDataSet dataSet = new TICDataSet(file, matchingScans,
          tol.getToleranceRange(msms.getMzValue(i)), null, TICPlotType.BASEPEAK);
      dataSet.setCustomSeriesKey(String.format("m/z %s", mzFormat.format(msms.getMzValue(i))));
      window.getTICPlot().addTICDataSet(dataSet,
          ColorUtils.getContrastPaletteColorAWT(file.getColor(),
              MZmineCore.getConfiguration().getDefaultColorPalette()));
    }

    MZmineCore.getDesktop().addTab(window);
  }

  private void showDiaMirror() {
    final Scan msms = selectedFeature.getMostIntenseFragmentScan();
    final RawDataFile file = selectedFeature.getRawDataFile();

    final MirrorScanWindowFXML window = new MirrorScanWindowFXML();
    final MirrorScanWindowController controller = window.getController();

    final IonTimeSeries<? extends Scan> featureData = selectedFeature.getFeatureData();
    if (!(featureData instanceof IonMobilogramTimeSeries ims)
        || !(selectedFeature.getRawDataFile() instanceof IMSRawDataFile imsFile)) {
      return;
    }

    final Range<Float> mobilityFWHM = IonMobilityUtils.getMobilityFWHM(ims.getSummedMobilogram());
    ScanSelection scanSelection = new ScanSelection(selectedFeature.getRawDataPointsRTRange(), 2);
    List<Scan> ms2Scans = scanSelection.getMatchingScans(imsFile.getScans());

    final List<MobilityScan> mobilityScans = ms2Scans.stream().<MobilityScan>mapMulti((f, c) -> {
      Frame frame = (Frame) f;
      for (MobilityScan ms : frame.getMobilityScans()) {
        if (mobilityFWHM.contains((float) ms.getMobility())) {
          c.accept(ms);
        }
      }
    }).toList();

    final MergedMassSpectrum uncorrelatedSpectrum = SpectraMerging.mergeSpectra(mobilityScans,
        SpectraMerging.pasefMS2MergeTol, MergingType.ALL, null);

    controller.setScans(selectedFeature.getMZ(), ScanUtils.extractDataPoints(msms),
        selectedFeature.getMZ(), ScanUtils.extractDataPoints(uncorrelatedSpectrum), " (correlated)",
        " (no correlation)");

    window.show();
  }
}
