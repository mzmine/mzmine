/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.framework.fx.FeatureRowInterfaceFx;
import io.github.mzmine.modules.visualization.compdb.CompoundDatabaseMatchTab;
import io.github.mzmine.modules.visualization.external_row_html.ExternalRowHtmlVisualizerController;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkController;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkPane;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.LipidAnnotationMatchTabOld;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowFXML;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.SpectraStackVisualizerPane;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsWindowFX;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.javafx.WeakAdapter;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.AnalogCompoundGroup;
import io.github.mzmine.util.spectraldb.entry.AnalogCompoundGroup.RowAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
  public Tab tabAnalogs;
  public Tab tabSimilarity;
  public Tab tabMasst;
  public Tab tabAllMs2;
  public Tab tabNodes;
  public Tab tabEdges;
  public GridPane gridAnnotations;
  private boolean setUpCalled = false;
  private FeatureNetworkController networkController;
  private FeatureTableFX internalTable;

  /**
   * Row-only interfaces that listen for changes to selected rows. The Annotations / Mirror /
   * All-MS2 tabs are dispatched separately because they also consume analog-compound-node data.
   */
  private @NotNull List<FeatureRowInterfaceFx> featureRowInterfaces;
  private @NotNull List<FeatureRowInterfaceFx> annotationInterfaces;
  private SpectraIdentificationResultsWindowFX spectralMatchesController;
  // Separate controller for the new Analogs tab. Same UI as the Annotations panel but driven from
  // the row's getAnalogSpectralLibraryMatches() instead of getSpectralLibraryMatches().
  private SpectraIdentificationResultsWindowFX analogMatchesController;
  private ExternalRowHtmlVisualizerController masstController;
  private MirrorScanWindowController mirrorScanController;
  private SpectraStackVisualizerPane allMs2Pane;

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
    allMs2Pane = new SpectraStackVisualizerPane();

    // Pass null to analog + annotations panel so the panel does NOT register its own row-selection
    // listener on the internal table - the dispatcher in handleSelectedNodesChanged is the single
    // source of data for both panels. Passing internalTable here would let the panel's built-in
    // setFeatureRows (which always reads row.getSpectralLibraryMatches()) overwrite the analog
    // matches we set on the analog tab, producing the "No chart found" symptom on row clicks.
    // Analogs tab uses the same panel implementation but is fed analog matches only
    spectralMatchesController = new SpectraIdentificationResultsWindowFX(null);
    analogMatchesController = new SpectraIdentificationResultsWindowFX(null);
    CompoundDatabaseMatchTab compoundMatchController = new CompoundDatabaseMatchTab(internalTable);

    // create mirror scan tab
    var mirrorScanTab = new MirrorScanWindowFXML();
    mirrorScanController = mirrorScanTab.getController();

    LipidAnnotationMatchTabOld lipidAnnotationMatchTabOld = new LipidAnnotationMatchTabOld(
        internalTable);

    // set content to panes
    // tabEdges.

    tabSimilarity.setContent(mirrorScanController.getMainPane());
    tabAnnotations.setContent(gridAnnotations);
    // Pull the analog controller's UI out of its (unused) Tab wrapper and attach to our tab. The
    // controller's setMatches/setTitle methods continue to work the same way regardless.
    tabAnalogs.setContent(analogMatchesController.getContent());
    tabAllMs2.setContent(allMs2Pane);

    // MASST
    masstController = new ExternalRowHtmlVisualizerController();
    tabMasst.setContent(masstController.buildView());

    // Row-only consumers. The Annotations, Mirror, and All-MS2 tabs are dispatched explicitly in
    // handleSelectedNodesChanged because they also accept analog-compound data and need a richer
    // signature than setFeatureRows.
    featureRowInterfaces = List.of(compoundMatchController, lipidAnnotationMatchTabOld,
        masstController);
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
    final var controller = new FxFeatureTableController();
    controller.setFeatureList(featureList);
    internalTable = controller.getFeatureTable();
    tabNodes.setContent(controller.buildView());

    weak.addListChangeListener(networkController,
        networkController.getNetworkPane().getVisibleRows(), c -> {
          if (weak.isDisposed()) {
            return;
          }
          ObservableList<? extends FeatureListRow> visible = c.getList();
          controller.getFilterModel().setIdFilter(
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

    final List<? extends Node> selectedNodes = change.getList();
    final FeatureNetworkPane networkPane = networkController.getNetworkPane();
    // disjoint partitions of the selection: feature-row nodes vs analog-compound nodes
    final List<FeatureListRow> selectedRows = networkPane.getRowsFromNodes(selectedNodes);
    final List<AnalogCompoundGroup> selectedAnalogs = networkPane.getAnalogGroupsFromNodes(
        selectedNodes);

    // 1) Annotations tab: regular library matches only. Analogs tab: analog matches only.
    //    Split by SpectralDBAnnotation.isAnalogMatch() so each tab shows the matching variant.
    dispatchAnnotationsAndAnalogs(selectedRows, selectedAnalogs);

    // 2) Mirror tab: pair-based. Two rows -> row1.MS2 vs row2.MS2 (existing behavior).
    //    Row + analog -> row.MS2 vs library entry. Two analogs -> two library entries.
    //    Anything other than 2 items clears.
    dispatchMirror(selectedNodes, networkPane);

    // 3) All MS2 stack: row MS2 charts + one extra chart per analog node's representative
    //    library entry, rendered as standalone fragment spectra alongside the rows.
    final List<SpectralDBAnnotation> analogReps = selectedAnalogs.stream()
        .map(AnalogCompoundGroup::representative).toList();
    allMs2Pane.setRowsAndAnalogs(selectedRows, analogReps, false);

    // 4) Other row-only consumers (compound DB, lipid, MASST). They ignore analog nodes.
    for (final FeatureRowInterfaceFx interfaceFx : featureRowInterfaces) {
      interfaceFx.setFeatureRows(selectedRows);
    }
    layoutAnnotations();
  }

  /**
   * Annotations tab + Analogs tab. Both consume {@link SpectralDBAnnotation}s but differ by
   * {@link SpectralDBAnnotation#isAnalogMatch()}:
   * <ul>
   *   <li>Annotations: row's {@code getSpectralLibraryMatches()} + non-analog members of any
   *       selected analog-compound group (cross-type dedup means analog groups may carry direct
   *       matches that share the same compound).</li>
   *   <li>Analogs: row's {@code getAnalogSpectralLibraryMatches()} + analog members of any selected
   *       analog-compound group.</li>
   * </ul>
   * LinkedHashSet preserves insertion order (rows first, then analog members) and dedupes via
   * SpectralDBAnnotation.equals so identical entries don't render twice.
   */
  private void dispatchAnnotationsAndAnalogs(final List<FeatureListRow> rows,
      final List<AnalogCompoundGroup> analogs) {
    final LinkedHashSet<SpectralDBAnnotation> annotationsMatches = new LinkedHashSet<>();
    final LinkedHashSet<SpectralDBAnnotation> analogsMatches = new LinkedHashSet<>();

    for (final FeatureListRow row : rows) {
      annotationsMatches.addAll(row.getSpectralLibraryMatches());
      analogsMatches.addAll(row.getAnalogSpectralLibraryMatches());
    }
    for (final AnalogCompoundGroup group : analogs) {
      for (final RowAnnotation member : group.members()) {
        if (member.annotation().isAnalogMatch()) {
          analogsMatches.add(member.annotation());
        } else {
          annotationsMatches.add(member.annotation());
        }
      }
    }

    final String title = buildAnnotationsTitle(rows, analogs);
    spectralMatchesController.setMatches(new ArrayList<>(annotationsMatches));
    spectralMatchesController.setTitle(title);
    analogMatchesController.setMatches(new ArrayList<>(analogsMatches));
    analogMatchesController.setTitle(title);
  }

  private static @NotNull String buildAnnotationsTitle(final List<FeatureListRow> rows,
      final List<AnalogCompoundGroup> analogs) {
    final List<String> parts = new ArrayList<>();
    rows.stream().map(FeatureUtils::rowToString).forEach(parts::add);
    for (final AnalogCompoundGroup group : analogs) {
      final String key = group.compoundKey();
      final String name = group.representative().getCompoundName();
      parts.add("Library: " + (key != null ? key : (name != null ? name : "analog")));
    }
    return String.join("; ", parts);
  }

  /**
   * Mirror tab: build a pair-item list in selection order (each row contributes its most-intense
   * fragment scan, each analog node contributes its representative library entry), then feed the
   * first two into the existing pair-mode setScans. Fewer than 2 items -> clear.
   */
  private void dispatchMirror(final List<? extends Node> selectedNodes,
      final FeatureNetworkPane networkPane) {
    final List<MirrorPairItem> pairItems = new ArrayList<>();
    for (final Node node : selectedNodes) {
      final FeatureListRow row = networkPane.getRowFromNode(node);
      if (row != null) {
        final Scan scan = row.getMostIntenseFragmentScan();
        if (scan != null) {
          // row MS2 path mirrors what setScans(Scan, Scan) does internally
          pairItems.add(
              new MirrorPairItem(scan.getPrecursorMz(), ScanUtils.extractDataPoints(scan, true),
                  "Row " + row.getID()));
        }
        continue;
      }
      final AnalogCompoundGroup group = networkPane.getAnalogGroupFromNode(node);
      if (group != null) {
        final SpectralLibraryEntry entry = group.representative().getEntry();
        final String compoundName = group.representative().getCompoundName();
        pairItems.add(new MirrorPairItem(entry.getPrecursorMZ(), entry.getDataPoints(),
            "Library: " + (compoundName != null ? compoundName : "analog")));
      }
    }
    if (pairItems.size() < 2) {
      mirrorScanController.clearScans();
      return;
    }
    final MirrorPairItem top = pairItems.get(0);
    final MirrorPairItem bot = pairItems.get(1);
    mirrorScanController.setScans(top.precursorMz(), top.dataPoints(), bot.precursorMz(),
        bot.dataPoints(), "", ""); // top.label(), bot.label());
  }

  /**
   * A single side of the mirror plot, abstracted over rows and library entries so the dispatcher
   * can feed mixed pairs through the existing raw-data {@code setScans} overload.
   */
  private record MirrorPairItem(Double precursorMz, DataPoint[] dataPoints, String label) {

  }

  public void close() {
    // need to dispose of listeners to be garbage collected
    weak.dipose();
  }
}
