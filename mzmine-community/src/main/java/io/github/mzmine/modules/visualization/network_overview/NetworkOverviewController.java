/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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
import io.github.mzmine.gui.framework.fx.FeatureRowInterfaceFx;
import io.github.mzmine.modules.visualization.compdb.CompoundDatabaseMatchTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableTab;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkController;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.LipidAnnotationMatchTabOld;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowFXML;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.SpectraStackVisualizerPane;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsWindowFX;
import io.github.mzmine.util.javafx.WeakAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import org.controlsfx.control.ToggleSwitch;
import org.graphstream.graph.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetworkOverviewController {

  private static final Logger logger = Logger.getLogger(NetworkOverviewController.class.getName());
  private final ObservableList<FeatureListRow> focussedRows;
  private final @NotNull WeakAdapter weak = new WeakAdapter();
  public ToggleSwitch cbBindToExternalTable;
  public BorderPane pnNetwork;
  public Tab tabAnnotations;
  public Tab tabSimilarity;
  public Tab tabAllMs2;
  public Tab tabNodes;
  public Tab tabEdges;
  public GridPane gridAnnotations;
  private boolean setUpCalled = false;
  private FeatureNetworkController networkController;
  private FeatureTableFX internalTable;

  /**
   * all interfaces that listen for changes to selected rows
   */
  private @NotNull List<FeatureRowInterfaceFx> featureRowInterfaces;
  private @NotNull List<FeatureRowInterfaceFx> annotationInterfaces;
  private SpectraIdentificationResultsWindowFX spectralMatchesController;

  public NetworkOverviewController() {
    this.focussedRows = FXCollections.observableArrayList();
  }

  public void setUp(@NotNull ModularFeatureList featureList, @Nullable FeatureTableFX externalTable,
      @Nullable List<? extends FeatureListRow> focussedRows, final NetworkOverviewFlavor flavor)
      throws IOException {
    if (setUpCalled) {
      throw new IllegalStateException(
          "Cannot setup NetworkOverviewController twice. Create a new one.");
    }
    setUpCalled = true;

    // create network
    networkController = FeatureNetworkController.create(featureList, this.focussedRows, flavor);
    pnNetwork.setCenter(networkController.getMainPane());

    // create edge table
    createEdgeTable();

    // create internal table
    createInternalTable(featureList);
    linkFeatureTableSelections(internalTable, externalTable);

    // all MS2
    SpectraStackVisualizerPane allMs2Pane = new SpectraStackVisualizerPane();

    // create annotations tab
    spectralMatchesController = new SpectraIdentificationResultsWindowFX(internalTable);
    CompoundDatabaseMatchTab compoundMatchController = new CompoundDatabaseMatchTab(internalTable);

    // create mirror scan tab
    var mirrorScanTab = new MirrorScanWindowFXML();
    MirrorScanWindowController mirrorScanController = mirrorScanTab.getController();

    LipidAnnotationMatchTabOld lipidAnnotationMatchTabOld = new LipidAnnotationMatchTabOld(
        internalTable);

    // set content to panes
    // tabEdges.

    tabSimilarity.setContent(mirrorScanController.getMainPane());
    tabAnnotations.setContent(gridAnnotations);
    tabAllMs2.setContent(allMs2Pane);

    // all content that listens to selected feature changes
    featureRowInterfaces = List.of(spectralMatchesController, compoundMatchController, allMs2Pane,
        mirrorScanController, lipidAnnotationMatchTabOld);
    // only annotation interfaces to control visibility
    annotationInterfaces = List.of(spectralMatchesController, compoundMatchController,
        lipidAnnotationMatchTabOld);
    layoutAnnotations();

    // add callbacks
    weak.addListChangeListener(networkController,
        networkController.getNetworkPane().getSelectedNodes(), c -> handleSelectedNodesChanged(c));

    // set focussed rows last
    if (focussedRows != null) {
      this.focussedRows.setAll(focussedRows);
    } else {
      networkController.getNetworkPane().showFullGraph();
    }
  }

  private void layoutAnnotations() {
    gridAnnotations.getChildren().clear();
    List<RowConstraints> rows = new ArrayList<>();
    for (final FeatureRowInterfaceFx inter : annotationInterfaces) {
      if (inter.isEmptyContent() || !(inter instanceof Tab tab)) {
        continue;
      }

      gridAnnotations.add(tab.getContent(), 0, rows.size());
      RowConstraints row = new RowConstraints();
      row.setFillHeight(true);
      row.setVgrow(Priority.SOMETIMES);
      rows.add(row);
    }
    gridAnnotations.getRowConstraints().setAll(rows);
  }


  private void createEdgeTable() {
    try {
      // Load the window FXML
      FXMLLoader loader = new FXMLLoader(getClass().getResource("EdgeTable.fxml"));
      BorderPane rootPane = loader.load();
      EdgeTableController edgeTableController = loader.getController();
      edgeTableController.setGraph(networkController.getNetworkPane().getGraph());

      tabEdges.setContent(rootPane);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Could not load EdgeTable.fxml " + ex.getMessage(), ex);
    }
  }

  private void createInternalTable(final @NotNull ModularFeatureList featureList) {
    FeatureTableTab tempTab = new FeatureTableTab(featureList);
    internalTable = tempTab.getFeatureTable();
    tabNodes.setContent(tempTab.getMainPane());

    var tabController = tempTab.getController();
    weak.addListChangeListener(networkController,
        networkController.getNetworkPane().getVisibleRows(), c -> {
          if (weak.isDisposed()) {
            return;
          }
          ObservableList<? extends FeatureListRow> visible = c.getList();
          tabController.getIdSearchField().setText(
              visible.stream().map(FeatureListRow::getID).map(Object::toString)
                  .collect(Collectors.joining(",")));
        });
  }


  private void linkFeatureTableSelections(final @NotNull FeatureTableFX internal,
      final @Nullable FeatureTableFX external) {
    // just apply selections in network
    weak.addListChangeListener(internal, internal.getSelectedTableRows(), c -> {
      if (weak.isDisposed()) {
        return;
      }
      var list = c.getList().stream().map(TreeItem::getValue).toList();
      var networkPane = networkController.getNetworkPane();
      networkPane.getSelectedNodes().setAll(networkPane.getNodes(list));
    });
    // external directly sets new focussed rows - and then selected rows in the internal table
    if (external != null) {
      weak.addListChangeListener(external, external.getSelectedTableRows(), c -> {
        if (weak.isDisposed()) {
          return;
        }
        if (cbBindToExternalTable.isSelected()) {
          var list = c.getList().stream().map(TreeItem::getValue).toList();
          focussedRows.setAll(list);
        }
      });
    }
  }

  protected void handleSelectedNodesChanged(final Change<? extends Node> change) {
    if (weak.isDisposed()) {
      return;
    }

    var selectedRows = networkController.getNetworkPane().getRowsFromNodes(change.getList());

    for (final FeatureRowInterfaceFx interfaceFx : featureRowInterfaces) {
      interfaceFx.setFeatureRows(selectedRows);
    }
    layoutAnnotations();
  }

  public void close() {
    // need to dispose of listeners to be garbage collected
    weak.dipose();
  }
}
