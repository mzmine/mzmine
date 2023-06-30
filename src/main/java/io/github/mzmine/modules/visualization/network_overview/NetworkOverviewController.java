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

package io.github.mzmine.modules.visualization.network_overview;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.visualization.compdb.CompoundDatabaseMatchTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableTab;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkController;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkPane;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowFXML;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsWindowFX;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.ToggleSwitch;
import org.graphstream.graph.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetworkOverviewController {

  private static final Logger logger = Logger.getLogger(NetworkOverviewController.class.getName());
  private final ObservableList<FeatureListRow> focussedRows;
  private boolean setUpCalled = false;

  private FeatureNetworkController networkController;
  private FeatureTableFX internalTable;
  private MirrorScanWindowController mirrorScanController;
  private SpectraIdentificationResultsWindowFX spectralMatchesController;
  private CompoundDatabaseMatchTab compoundMatchController;

  public BorderPane pnNetwork;
  public Tab tabAnnotations;
  public Tab tabSimilarity;
  public Tab tabFeatureChrom;
  public Tab tabNodes;
  public Tab tabEdges;
  public ToggleSwitch cbUpdateOnSelection;
  public GridPane gridAnnotations;

  public NetworkOverviewController() {
    this.focussedRows = FXCollections.observableArrayList();
  }

  public void setUp(@NotNull ModularFeatureList featureList, @Nullable FeatureTableFX externalTable,
      @Nullable List<? extends FeatureListRow> focussedRows) throws IOException {
    if (setUpCalled) {
      throw new IllegalStateException(
          "Cannot setup NetworkOverviewController twice. Create a new one.");
    }
    setUpCalled = true;

    // create internal table
    FeatureTableTab tempTab = new FeatureTableTab(featureList);
    internalTable = tempTab.getFeatureTable();

    // create network
    networkController = FeatureNetworkController.create(featureList, this.focussedRows);
    pnNetwork.setCenter(networkController.getMainPane());
    linkFeatureTableSelections(internalTable, externalTable);

    // create edges

    // create annotations tab
    spectralMatchesController = new SpectraIdentificationResultsWindowFX(internalTable);
    compoundMatchController = new CompoundDatabaseMatchTab(internalTable);
    gridAnnotations.add(spectralMatchesController.getContent(), 0, 0);
    gridAnnotations.add(compoundMatchController.getContent(), 0, 1);

    // create mirror scan tab
    var mirrorScanTab = new MirrorScanWindowFXML();
    mirrorScanController = mirrorScanTab.getController();

    // set content to panes
    tabNodes.setContent(tempTab.getMainPane());
    // tabEdges.

    tabSimilarity.setContent(mirrorScanController.getMainPane());
    tabAnnotations.setContent(gridAnnotations);

    // add callbacks
    networkController.getNetworkPane().getSelectedNodes()
        .addListener(this::handleSelectedNodesChanged);

    // set focussed rows last
    if (focussedRows != null) {
      this.focussedRows.setAll(focussedRows);
    }
  }


  private void linkFeatureTableSelections(final @NotNull FeatureTableFX internal,
      final @Nullable FeatureTableFX external) {
    // just apply selections in network
    internal.getSelectedTableRows()
        .addListener((ListChangeListener<? super TreeItem<ModularFeatureListRow>>) c -> {
          var list = c.getList().stream().map(TreeItem::getValue).toList();
          var networkPane = networkController.getNetworkPane();
          networkPane.getSelectedNodes().setAll(networkPane.getNodes(list));
        });
    // external directly sets new focussed rows - and then selected rows in the internal table
    if (external != null) {
      external.getSelectedTableRows()
          .addListener((ListChangeListener<? super TreeItem<ModularFeatureListRow>>) c -> {
            var list = c.getList().stream().map(TreeItem::getValue).toList();
            focussedRows.setAll(list);
          });
    }
  }

  protected void handleSelectedNodesChanged(final Change<? extends Node> change) {
    var selectedRows = FeatureNetworkPane.getRowsFromNodes(change.getList());
    if (selectedRows.size() >= 1) {
      showAnnotations(selectedRows.get(0));
    }
    if (selectedRows.size() >= 2) {
      showSimilarityMirror(selectedRows.get(0), selectedRows.get(1));
    }
  }

  public void showAnnotations(final FeatureListRow row) {
    spectralMatchesController.setMatches(row.getSpectralLibraryMatches());
    if (row instanceof ModularFeatureListRow mod) {
      compoundMatchController.setFeatureRow(mod);
    }
  }

  /**
   * Run the MSMS-MirrorScan module whenever user clicks on edges
   */
  public void showSimilarityMirror(FeatureListRow a, FeatureListRow b) {
    mirrorScanController.setScans(a.getMostIntenseFragmentScan(), b.getMostIntenseFragmentScan());
  }
}
