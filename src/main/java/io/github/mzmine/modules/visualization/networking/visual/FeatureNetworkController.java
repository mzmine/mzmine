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


package io.github.mzmine.modules.visualization.networking.visual;

import static io.github.mzmine.modules.visualization.networking.visual.enums.GraphStyleAttribute.COLOR;
import static io.github.mzmine.modules.visualization.networking.visual.enums.GraphStyleAttribute.LABEL;
import static io.github.mzmine.modules.visualization.networking.visual.enums.GraphStyleAttribute.SIZE;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.networking.visual.enums.EdgeAtt;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphElementAttr;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphObject;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphStyleAttribute;
import io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.TextFields;
import org.graphstream.graph.Node;
import org.jetbrains.annotations.NotNull;

/**
 * Loads FeatureNetworkPane.fxml
 */
public class FeatureNetworkController {

  private static final Logger logger = Logger.getLogger(FeatureNetworkController.class.getName());

  // those rows are focussed - usually showing its neighbors
  public SearchableComboBox<NodeAtt> comboNodeColor;
  public SearchableComboBox<NodeAtt> comboNodeSize;
  public SearchableComboBox<NodeAtt> comboNodeLabel;
  public SearchableComboBox<EdgeAtt> comboEdgeColor;
  public SearchableComboBox<EdgeAtt> comboEdgeSize;
  public SearchableComboBox<EdgeAtt> comboEdgeLabel;
  public ToggleSwitch cbCollapseIons;
  //  public CheckComboBox<String> cbComboVisibleEdgeTypes;
  public Spinner<Integer> spinnerNodeNeighbors;
  public BorderPane mainPane;
  public TextField txtFilterAnnotations;
  public Button btnFocusSelectedNodes;

  private FeatureNetworkPane networkPane;

  /**
   * Load fxml and create network
   *
   * @throws IOException
   */
  public static FeatureNetworkController create(@NotNull FeatureList flist,
      @NotNull ObservableList<FeatureListRow> focussedRows) throws IOException {
    // Load the window FXML
    FXMLLoader loader = new FXMLLoader(
        FeatureNetworkController.class.getResource("FeatureNetworkPane.fxml"));
    BorderPane rootPane = loader.load();
    FeatureNetworkController controller = loader.getController();

    controller.setFeatureListCreateNetworkPane(flist, focussedRows);
    return controller;
  }

  @FXML
  public void initialize() {
    spinnerNodeNeighbors.setValueFactory(
        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 2));
  }

  private void setFeatureListCreateNetworkPane(final @NotNull FeatureList flist,
      final @NotNull ObservableList<FeatureListRow> focussedRows) {
    // create graph and add to center
    FeatureNetworkGenerator generator = new FeatureNetworkGenerator();
    var fullGraph = generator.createNewGraph(flist, true, true, false);
    networkPane = new FeatureNetworkPane(this, flist, focussedRows, generator, fullGraph);
    mainPane.setCenter(networkPane);

    addMenuOptions();
    addAnnotationFilterOptions(flist);
    addBindings();
  }

  private void addBindings() {
    getSelectedNodes().addListener(
        (ListChangeListener<? super Node>) c -> btnFocusSelectedNodes.setText(
            "Focus %d nodes".formatted(c.getList().size())));
  }

  private void addAnnotationFilterOptions(final FeatureList flist) {
    var annotations = flist.stream().map(FeatureListRow::getAllFeatureAnnotations)
        .flatMap(Collection::stream).map(Object::toString).toList();
    TextFields.bindAutoCompletion(txtFilterAnnotations, annotations);
    PauseTransition delayedFilter = new PauseTransition(Duration.seconds(1));
    delayedFilter.setOnFinished(event -> {
      // finally filter and select nodes
      networkPane.selectNodesByAnnotation(txtFilterAnnotations.getText());
    });
    txtFilterAnnotations.textProperty()
        .addListener((observable, oldValue, newValue) -> delayedFilter.playFromStart());
  }

  public ObservableList<FeatureListRow> getFocussedRows() {
    return networkPane.getFocussedRows();
  }

  public ObservableList<Node> getSelectedNodes() {
    return networkPane.getSelectedNodes();
  }

  public ObjectProperty<Integer> neighborDistanceProperty() {
    return spinnerNodeNeighbors.getValueFactory().valueProperty();
  }


  private void addMenuOptions() {
    // defaults
    addComboOptions(comboNodeColor, COLOR, NodeAtt.values(), NodeAtt.RT);
    addComboOptions(comboNodeLabel, LABEL, NodeAtt.values(), NodeAtt.ANNOTATION);
    addComboOptions(comboNodeSize, SIZE, NodeAtt.values(), NodeAtt.LOG10_SUM_INTENSITY);
    addComboOptions(comboEdgeColor, COLOR, EdgeAtt.values(), EdgeAtt.NEIGHBOR_DISTANCE);
    addComboOptions(comboEdgeSize, SIZE, EdgeAtt.values(), EdgeAtt.SCORE);
    addComboOptions(comboEdgeLabel, LABEL, EdgeAtt.values(), EdgeAtt.NONE);

    cbCollapseIons.selectedProperty()
        .addListener((observable, oldValue, newValue) -> networkPane.collapseIonNodes(newValue));

//    cbComboVisibleEdgeTypes.getItems().addAll(networkPane.getUniqueEdgeTypes());
    // TODO check all and bind visibility

    // #######################################################
    // add buttons
//
//    ToggleButton toggleShowMS2SimEdges = new ToggleButton("Show MS2 sim");
//    toggleShowMS2SimEdges.setMaxWidth(Double.MAX_VALUE);
//    toggleShowMS2SimEdges.setSelected(true);
//    toggleShowMS2SimEdges.selectedProperty()
//        .addListener((o, old, value) -> setShowMs2SimEdges(toggleShowMS2SimEdges.isSelected()));
//
//    ToggleButton toggleShowRelations = new ToggleButton("Show relational edges");
//    toggleShowRelations.setMaxWidth(Double.MAX_VALUE);
//    toggleShowRelations.setSelected(true);
//    toggleShowRelations.selectedProperty()
//        .addListener((o, old, value) -> setConnectByNetRelations(toggleShowRelations.isSelected()));
//
//    ToggleButton toggleShowIonIdentityEdges = new ToggleButton("Show ion edges");
//    toggleShowIonIdentityEdges.setMaxWidth(Double.MAX_VALUE);
//    toggleShowIonIdentityEdges.setSelected(true);
//    toggleShowIonIdentityEdges.selectedProperty().addListener(
//        (o, old, value) -> showIonIdentityEdges(toggleShowIonIdentityEdges.isSelected()));
//
//    ToggleButton toggleShowEdgeLabel = new ToggleButton("Show edge label");
//    toggleShowEdgeLabel.setMaxWidth(Double.MAX_VALUE);
//    toggleShowEdgeLabel.setSelected(showEdgeLabels);
//    toggleShowEdgeLabel.selectedProperty()
//        .addListener((o, old, value) -> showEdgeLabels(toggleShowEdgeLabel.isSelected()));
//
//    ToggleButton toggleShowNodeLabel = new ToggleButton("Show node label");
//    toggleShowNodeLabel.setMaxWidth(Double.MAX_VALUE);
//    toggleShowNodeLabel.setSelected(showNodeLabels);
//    toggleShowNodeLabel.selectedProperty()
//        .addListener((o, old, value) -> showNodeLabels(toggleShowNodeLabel.isSelected()));
//
//    Button showGNPSMatches = new Button("GNPS matches");
//    showGNPSMatches.setMaxWidth(Double.MAX_VALUE);
//    showGNPSMatches.setOnAction(e -> showGNPSMatches());
//
//    Button showLibraryMatches = new Button("Library matches");
//    showLibraryMatches.setMaxWidth(Double.MAX_VALUE);
//    showLibraryMatches.setOnAction(e -> showLibraryMatches());
//
//    Button updateGraphButton = new Button("Update graph");
//    updateGraphButton.setMaxWidth(Double.MAX_VALUE);
//    updateGraphButton.setOnAction(e -> updateGraph());
//
//    Button showOnlyConnectedNodesButton = new Button("Show only connected nodes");
//    showOnlyConnectedNodesButton.setMaxWidth(Double.MAX_VALUE);
//    showOnlyConnectedNodesButton.setOnAction(e -> visualizeConnectedNodesOnly());
//
//    // finally add buttons
//    VBox pnRightMenu = new VBox(4, toggleCollapseIons, toggleShowMS2SimEdges, toggleShowRelations,
//        toggleShowIonIdentityEdges, toggleShowEdgeLabel, toggleShowNodeLabel, showGNPSMatches,
//        showLibraryMatches, l, nodeNeighbours, updateGraphButton, showOnlyConnectedNodesButton);
//    pnRightMenu.setSpacing(10);
//    pnRightMenu.setPadding(new Insets(0, 20, 10, 20));
//    this.setRight(pnRightMenu);
  }

  private <T extends GraphElementAttr> void addComboOptions(final SearchableComboBox<T> combo,
      final GraphStyleAttribute attribute, final T[] values, @NotNull final T selectedValue) {
    combo.setItems(FXCollections.observableArrayList(values));
    combo.setButtonCell(new ListCell<>() {
      @Override
      protected void updateItem(final T item, final boolean empty) {
        super.updateItem(item, empty);
        if (item != null) {
          setText(attribute + ": " + item);
        }
      }
    });
    combo.valueProperty().addListener((observable, oldValue, newValue) -> {
      networkPane.setAttributeForAllElements(attribute, newValue);
    });
    GraphObject go = selectedValue.getGraphObject();
    combo.setTooltip(new Tooltip(go + " " + attribute)); // e.g., Node color
    combo.getSelectionModel().select(selectedValue);
  }

  public void updateGraph() {
    networkPane.updateGraph();
  }

  public FeatureNetworkPane getNetworkPane() {
    return networkPane;
  }

  public Pane getMainPane() {
    return mainPane;
  }

  public void onFocusSelectedNodes(final ActionEvent e) {
    networkPane.focusSelectedNodes();
  }

  public void onShowAllNodes(final ActionEvent actionEvent) {
    networkPane.showFullGraph();
  }

  public void onZoomSelectedNodes(final ActionEvent actionEvent) {
    networkPane.zoomOnSelectedNodes();
  }
}
