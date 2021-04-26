/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */


package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.fx.ColumnType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_manual.XICManualPickerModule;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.FormulaPredictionModule;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchModule;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDBSearchModule;
import io.github.mzmine.modules.dataprocessing.id_sirius.SiriusIdentificationModule;
import io.github.mzmine.modules.dataprocessing.id_spectraldbsearch.LocalSpectralDBSearchModule;
import io.github.mzmine.modules.io.export_sirius.SiriusExportModule;
import io.github.mzmine.modules.io.spectraldbsubmit.view.MSMSLibrarySubmissionWindow;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.export.IsotopePatternExportModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.export.MSMSExportModule;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerModule;
import io.github.mzmine.modules.visualization.ims_featurevisualizer.IMSFeatureVisualizerTab;
import io.github.mzmine.modules.visualization.ims_mobilitymzplot.IMSMobilityMzPlotModule;
import io.github.mzmine.modules.visualization.intensityplot.IntensityPlotModule;
import io.github.mzmine.modules.visualization.rawdataoverviewims.IMSRawDataOverviewModule;
import io.github.mzmine.modules.visualization.spectra.multimsms.MultiMsMsTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.MultiSpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowFX;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsModule;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.components.ConditionalMenuItem;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.SwingUtilities;

/**
 * Conditions should be chosen in a way that exceptions are impossible when the item is clicked. On
 * click events should not be longer than a few lines and should be moved to helper classes if
 * otherwise.
 */
public class FeatureTableContextMenu extends ContextMenu {

  final Menu showMenu;
  final Menu searchMenu;
  final Menu idsMenu;
  final Menu exportMenu;
  private final FeatureTableFX table;

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
    deleteRowsItem
        .setOnAction(e -> selectedRows.forEach(row -> table.getFeatureList().removeRow(row)));

//    final MenuItem addNewRowItem;
    final MenuItem manuallyDefineItem = new ConditionalMenuItem("Define manually",
        () -> selectedRows.size() == 1 && selectedFeature != null);
    manuallyDefineItem.setOnAction(e -> XICManualPickerModule
        .runManualDetection(selectedFeature.getRawDataFile(), selectedRows.get(0),
            table.getFeatureList(), table));

    getItems().addAll(new SeparatorMenuItem(), manuallyDefineItem, deleteRowsItem);
  }

  private void initIdentitiesMenu() {
    final MenuItem openCompoundIdUrl = new ConditionalMenuItem("Open compound ID URL (disabled)",
        () -> false);

    final MenuItem copyIdsItem = new ConditionalMenuItem("Copy identities", () ->
        !selectedRows.isEmpty() && !selectedRows.get(0).getPeakIdentities().isEmpty());
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
    clearIdsItem.setOnAction(e -> selectedRows
        .forEach(row -> row.setPeakIdentities(FXCollections.observableArrayList())));

    idsMenu.getItems().addAll(openCompoundIdUrl, copyIdsItem, pasteIdsItem, clearIdsItem);
  }

  private void initExportMenu() {
    final MenuItem exportIsotopesItem = new ConditionalMenuItem("Export isotope pattern",
        () -> selectedRows.size() == 1 && selectedRows.get(0).getBestIsotopePattern() != null);
    exportIsotopesItem
        .setOnAction(e -> IsotopePatternExportModule.exportIsotopePattern(selectedRows.get(0)));

    final MenuItem exportMSMSItem = new ConditionalMenuItem("Export MS/MS pattern",
        () -> selectedRows.size() == 1 && selectedRows.get(0).getBestFragmentation() != null);
    exportMSMSItem.setOnAction(e -> MSMSExportModule.exportMSMS(selectedRows.get(0)));

    final MenuItem exportToSirius = new ConditionalMenuItem("Export to Sirius",
        () -> !selectedRows.isEmpty());
    exportToSirius.setOnAction(e -> SiriusExportModule
        .exportSingleRows(selectedRows.toArray(new ModularFeatureListRow[0])));

    final MenuItem exportMS1Library = new ConditionalMenuItem("Export to MS1 library (Swing)",
        () -> !selectedRows.isEmpty());
    exportMS1Library.setOnAction(e -> SwingUtilities.invokeLater(() -> {
      MSMSLibrarySubmissionWindow window = new MSMSLibrarySubmissionWindow();
      window.setData(selectedRows.toArray(new ModularFeatureListRow[0]), SortingProperty.MZ,
          SortingDirection.Ascending, false);
      window.setVisible(true);
    }));

    final MenuItem exportMSMSLibrary = new ConditionalMenuItem("Export to MS/MS library (Swing)",
        () -> !selectedRows.isEmpty());
    exportMSMSLibrary.setOnAction(e -> SwingUtilities.invokeLater(() -> {
      MSMSLibrarySubmissionWindow window = new MSMSLibrarySubmissionWindow();
      window.setData(selectedRows.toArray(new ModularFeatureListRow[0]), SortingProperty.MZ,
          SortingDirection.Ascending, true);
      window.setVisible(true);
    }));

    exportMenu.getItems()
        .addAll(exportIsotopesItem, exportMSMSItem, exportToSirius, new SeparatorMenuItem(),
            exportMS1Library, exportMSMSLibrary);
  }

  private void initSearchMenu() {
    final MenuItem onlineDbSearchItem = new ConditionalMenuItem("Online compound database search",
        () -> selectedRows.size() == 1);
    onlineDbSearchItem.setOnAction(
        e -> OnlineDBSearchModule.showSingleRowIdentificationDialog(selectedRows.get(0)));

    final MenuItem spectralDbSearchItem = new ConditionalMenuItem("Local spectral database search",
        () -> selectedRows.size() >= 1);
    spectralDbSearchItem.setOnAction(e -> LocalSpectralDBSearchModule
        .showSelectedRowsIdentificationDialog(selectedRows.toArray(new ModularFeatureListRow[0]),
            table));

    final MenuItem nistSearchItem = new ConditionalMenuItem("NIST MS search",
        () -> selectedRows.size() == 1);
    nistSearchItem.setOnAction(
        e -> NistMsSearchModule.singleRowSearch(table.getFeatureList(), selectedRows.get(0)));

    // Comments from MZmine 2 source:
    // TODO: what is going on here?
    // TODO: probably remove singlerowidentificationDialog as Sirius works with spectrum, not 1
    final MenuItem siriusItem = new ConditionalMenuItem("Sirius structure prediction",
        () -> selectedRows.size() == 1);
    siriusItem.setOnAction(
        e -> SiriusIdentificationModule.showSingleRowIdentificationDialog(selectedRows.get(0)));

    final MenuItem formulaPredictionItem = new ConditionalMenuItem("Predict molecular formula",
        () -> selectedRows.size() == 1);
    formulaPredictionItem.setOnAction(
        e -> FormulaPredictionModule.showSingleRowIdentificationDialog(selectedRows.get(0)));

    searchMenu.getItems()
        .addAll(onlineDbSearchItem, spectralDbSearchItem, nistSearchItem, siriusItem,
            new SeparatorMenuItem(), formulaPredictionItem);
  }

  private void initShowMenu() {
    final MenuItem showXICItem = new ConditionalMenuItem("XIC (quick)",
        () -> !selectedRows.isEmpty());
    showXICItem
        .setOnAction(e -> ChromatogramVisualizerModule.visualizeFeatureListRows(selectedRows));

    final MenuItem showXICSetupItem = new ConditionalMenuItem("XIC (dialog)",
        () -> !selectedRows.isEmpty());
    showXICSetupItem.setOnAction(e -> ChromatogramVisualizerModule
        .setUpVisualiserFromFeatures(selectedRows, selectedFeature != null ? selectedFeature.getRawDataFile() : null));

    final MenuItem showIMSFeatureItem = new ConditionalMenuItem("Ion mobility trace",
        () -> !selectedRows.isEmpty() && selectedFeature != null && selectedFeature
            .getRawDataFile() instanceof IMSRawDataFile);
    showIMSFeatureItem.setOnAction(
        e -> MZmineCore.getDesktop().addTab(new IMSFeatureVisualizerTab(selectedFeature)));

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
    showIntensityPlotItem.setOnAction(e ->
        IntensityPlotModule.showIntensityPlot(MZmineCore.getProjectManager().getCurrentProject(),
            selectedFeature.getFeatureList(), selectedRows.toArray(new ModularFeatureListRow[0])));

    final MenuItem showInIMSRawDataOverviewItem = new ConditionalMenuItem(
        "Show m/z ranges in IMS raw data overview",
        () -> selectedFeature != null && selectedFeature
            .getRawDataFile() instanceof IMSRawDataFile && !selectedFeatures.isEmpty());
    showInIMSRawDataOverviewItem.setOnAction(e -> IMSRawDataOverviewModule
        .openIMSVisualizerTabWithFeatures(getFeaturesFromSelectedRaw(selectedFeatures)));

    final MenuItem showInMobilityMzVisualizerItem = new ConditionalMenuItem(
        "Plot mobility/CCS vs. m/z", () -> !selectedRows.isEmpty());
    showInMobilityMzVisualizerItem.setOnAction(e -> {
      boolean useMobilograms = true;
      if (selectedFeatures.size() > 1000) {
        useMobilograms = MZmineCore.getDesktop()
            .displayConfirmation("You selected " + selectedFeatures.size()
                    + " to visualize. This might take a long time or crash MZmine.\nWould you like to "
                    + "visualize points instead of mobilograms for features?", ButtonType.YES,
                ButtonType.NO) == ButtonType.NO;
      }
      IMSMobilityMzPlotModule.visualizeFeaturesInNewTab(selectedRows, useMobilograms);
    });

    final MenuItem showSpectrumItem = new ConditionalMenuItem("Mass spectrum",
        () -> selectedFeature != null && selectedFeature.getRepresentativeScan() != null);
    showSpectrumItem.setOnAction(
        e -> SpectraVisualizerModule.addNewSpectrumTab(selectedFeature.getRawDataFile(),
            selectedFeature.getRepresentativeScan(), selectedFeature));

    // TODO this should display selected features instead of rows. MultiMSMSWindow does not support that, however.
    final MenuItem showMSMSItem = new ConditionalMenuItem("Most intense MS/MS",
        () -> getNumberOfRowsWithFragmentScans(selectedRows) >= 1 && selectedFeature != null);
    showMSMSItem.setOnAction(e -> {
      if (getNumberOfRowsWithFragmentScans(selectedRows) > 1) {
        MultiMsMsTab multi = new MultiMsMsTab(selectedRows,
            table.getFeatureList().getRawDataFiles(), selectedRows.get(0).getRawDataFiles().get(0));
        MZmineCore.getDesktop().addTab(multi);
      } else {
        SpectraVisualizerModule.addNewSpectrumTab(selectedFeature.getMostIntenseFragmentScan());
      }
    });

    final MenuItem showMSMSMirrorItem = new ConditionalMenuItem("Mirror MS/MS (2 rows)",
        () -> selectedRows.size() == 2 && getNumberOfRowsWithFragmentScans(selectedRows) == 2);
    showMSMSMirrorItem.setOnAction(e -> {
      MirrorScanWindowFX mirrorScanTab = new MirrorScanWindowFX();
      mirrorScanTab.setScans(selectedRows.get(0).getBestFragmentation(),
          selectedRows.get(1).getBestFragmentation());
      mirrorScanTab.show();
    });

    // TODO this is still a Swing window :(
    final MenuItem showAllMSMSItem = new ConditionalMenuItem("All MS/MS (still Swing)",
        () -> !selectedRows.isEmpty() && !selectedRows.get(0).getAllMS2Fragmentations().isEmpty());
    showAllMSMSItem.setOnAction(
        e -> MultiSpectraVisualizerTab.addNewMultiSpectraVisualizerTab(selectedRows.get(0)));

    final MenuItem showIsotopePatternItem = new ConditionalMenuItem("Isotope pattern",
        () -> selectedFeature != null && selectedFeature.getIsotopePattern() != null);
    showIsotopePatternItem.setOnAction(e -> SpectraVisualizerModule
        .addNewSpectrumTab(selectedFeature.getRawDataFile(),
            selectedFeature.getRepresentativeScan(), selectedFeature.getIsotopePattern()));

    final MenuItem showSpectralDBResults = new ConditionalMenuItem("Spectral DB search results",
        () -> !selectedRows.isEmpty() && rowHasSpectralDBMatchResults(selectedRows.get(0)));
    showSpectralDBResults
        .setOnAction(e -> SpectraIdentificationResultsModule.showNewTab(selectedRows.get(0)));

    final MenuItem showPeakRowSummaryItem = new ConditionalMenuItem("Row(s) summary", () ->
        /*!selectedRows.isEmpty()*/ false); // todo, not implemented yet

    showMenu.getItems()
        .addAll(showXICItem, showXICSetupItem, showIMSFeatureItem, new SeparatorMenuItem(),
            show2DItem, show3DItem, showIntensityPlotItem, showInIMSRawDataOverviewItem,
            showInMobilityMzVisualizerItem,
            new SeparatorMenuItem(),
            showSpectrumItem, showMSMSItem, showMSMSMirrorItem, showAllMSMSItem,
            new SeparatorMenuItem(), showIsotopePatternItem, showSpectralDBResults,
            new SeparatorMenuItem(), showPeakRowSummaryItem);
  }

  private void onShown() {
    selectedRowTypes = table.getSelectedDataTypes(ColumnType.ROW_TYPE);
    selectedFeatureTypes = table.getSelectedDataTypes(ColumnType.FEATURE_TYPE);
    selectedFiles = table.getSelectedRawDataFiles();
    selectedFeatures = table.getSelectedFeatures();
    selectedRows = table.getSelectedRows();
    selectedFeature = table.getSelectedFeature();

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
      if (row.getBestFragmentation() != null) {
        numFragmentScans++;
      }
    }
    return numFragmentScans;
  }

  private boolean rowHasSpectralDBMatchResults(ModularFeatureListRow row) {
    return row.getPeakIdentities().stream()
        .filter(pi -> pi instanceof SpectralDBFeatureIdentity)
        .map(pi -> ((SpectralDBFeatureIdentity) pi)).count() > 0;
  }

  @Nonnull
  private List<ModularFeature> getFeaturesFromSelectedRaw(Collection<ModularFeature> features) {
    if (selectedFeature == null || selectedFeature.getRawDataFile() == null) {
      return Collections.emptyList();
    }
    final RawDataFile file = selectedFeature.getRawDataFile();
    return features.stream().filter(f -> f.getRawDataFile() == file).collect(Collectors.toList());
  }
}
