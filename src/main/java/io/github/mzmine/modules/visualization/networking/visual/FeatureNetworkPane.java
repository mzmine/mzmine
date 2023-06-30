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


package io.github.mzmine.modules.visualization.networking.visual;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureNetworkPane extends NetworkPane {

  private static final Logger logger = Logger.getLogger(FeatureNetworkPane.class.getName());
  /**
   * Max width in graph units. 1 is the distance between nodes
   */
  public static final float MAX_NODE_WIDTH_GU = 0.3f;
  public static final float MIN_NODE_WIDTH_GU = 0.02f;

  // currently set dynamic node styles like color, size, label
  private final EnumMap<GraphStyleAttribute, NodeAtt> dynamicNodeStyle = new EnumMap<>(
      GraphStyleAttribute.class);
  private final EnumMap<GraphStyleAttribute, EdgeAtt> dynamicEdgeStyle = new EnumMap<>(
      GraphStyleAttribute.class);
  // style values need to be set as float - double crashes in the javafx thread for graphstream
  private final Map<NodeAtt, Range<Float>> attributeRanges = new HashMap<>();
  // for non numeric values: store all objects and provide indexes
  private final Map<NodeAtt, Map<String, Integer>> attributeCategoryValuesMap = new HashMap<>();

  // store all node annotations here for quick selections
  private final NodeAnnotationsFilter annotationsFilter;

  // the network generator
  private final FeatureNetworkGenerator generator;
  private final FeatureNetworkController controller;
  // data
  private final FeatureList featureList;
  private final List<FeatureListRow> rows; // might be a filtered subset

  // those rows are focussed - usually showing its neighbors
  private final @NotNull ObservableList<FeatureListRow> focussedRows;
  private final @NotNull ObjectProperty<Integer> neighborDistance;
  private boolean showNetRelationsEdges;
  private boolean collapse = true;
  private boolean showIonEdges = true;
  private boolean showMs2SimEdges;
  private boolean ms1FeatureShapeEdges = false;

  public FeatureNetworkPane(final FeatureNetworkController controller,
      final @NotNull FeatureList featureList,
      final @NotNull ObservableList<FeatureListRow> focussedRows,
      @NotNull ObjectProperty<Integer> neighborDistance,
      final @NotNull FeatureNetworkGenerator generator, final @NotNull MultiGraph fullGraph) {
    super("Molecular Networks", false, fullGraph);
    this.controller = controller;
    this.featureList = featureList;
    this.rows = featureList.getRows();
    this.focussedRows = focussedRows;
    this.generator = generator;
    this.neighborDistance = neighborDistance;
    addMenu();
    focussedRows.addListener(this::handleFocussedRowsChanged);
    annotationsFilter = new NodeAnnotationsFilter(this);

  }

  /**
   * Called by changes to {@link #focussedRows}.
   *
   * @param c change
   */
  private void handleFocussedRowsChanged(final Change<? extends FeatureListRow> c) {
    //select nodes
    var nodes = getNodes(c.getList());
    selectedNodes.setAll(nodes);
    showNodesNeighbors(nodes);
  }

  private void showNodesNeighbors(final List<Node> selected) {
    if (selected.isEmpty()) {
      return;
    }
    filterNodeNeighbors(selected, neighborDistance.get());
    applyDynamicStyles();
  }


  @Override
  protected void onGraphClicked(final @NotNull MouseEvent e, final @Nullable Node node,
      final @Nullable Edge edge) {
    super.onGraphClicked(e, node, edge);
    if (!selectedNodes.isEmpty()) {
      focusSelectedNodes();
    }
  }

  public @NotNull ObservableList<FeatureListRow> getFocussedRows() {
    return focussedRows;
  }

  public @NotNull ObjectProperty<Integer> neighborDistanceProperty() {
    return neighborDistance;
  }

  @Nullable
  public Node getNode(FeatureListRow row) {
    return generator.getRowNode(row, false);
  }

  @NotNull
  public List<Node> getNodes(List<? extends FeatureListRow> rows) {
    return rows.stream().map(this::getNode).filter(Objects::nonNull).toList();
  }

//  public void createNewGraph(List<FeatureListRow> rows) {
//    this.rows = rows;
//    attributeRanges.clear();
//    attributeCategoryValuesMap.clear();
//    clear();
//    generator.createNewGraph(rows, graph.getFullGraph(), onlyBest, relationMaps,
//        ms1FeatureShapeEdges);
//
//    clearNodeSelections();
//    showEdgeLabels(showEdgeLabels);
//    showNodeLabels(showNodeLabels);
//
//    // last state
//    collapseIonNodes(collapse);
//
//    // apply dynamic style
//    applyDynamicStyles();
//    graph.setFullGraph(graph.getFullGraph());
//  }


  public static FeatureListRow getRowFromNode(final Node a) {
    return (FeatureListRow) a.getAttribute(NodeAtt.ROW.toString());
  }

  public static List<FeatureListRow> getRowsFromNodes(final List<? extends Node> nodes) {
    return nodes.stream().map(FeatureNetworkPane::getRowFromNode).toList();
  }

  private void addMenu() {
    Pane menu = getPnSettings();
    menu.setVisible(true);

    showEdgeLabels = false;
    showNodeLabels = true;
    collapse = true;

    // defaults
    dynamicNodeStyle.put(GraphStyleAttribute.COLOR, NodeAtt.RT);
    dynamicNodeStyle.put(GraphStyleAttribute.SIZE, NodeAtt.LOG10_SUM_INTENSITY);
    dynamicNodeStyle.put(GraphStyleAttribute.LABEL, NodeAtt.LABEL);
    dynamicNodeStyle.put(GraphStyleAttribute.CLASS, null);

    menu.getChildren().add(new Label("Color:"));
    ComboBox<NodeAtt> comboNodeColor = new ComboBox<>(
        FXCollections.observableArrayList(NodeAtt.values()));
    comboNodeColor.setTooltip(new Tooltip("Node color"));
    comboNodeColor.getSelectionModel().select(NodeAtt.RT);
    menu.getChildren().add(comboNodeColor);
    comboNodeColor.setOnAction(e -> {
      NodeAtt selectedItem = comboNodeColor.getSelectionModel().getSelectedItem();
      setAttributeForAllNodes(GraphStyleAttribute.COLOR, selectedItem);
    });

    menu.getChildren().add(new Label("Size:"));
    ComboBox<NodeAtt> comboNodeSize = new ComboBox<>(
        FXCollections.observableArrayList(NodeAtt.values()));
    comboNodeSize.setTooltip(new Tooltip("Node size"));
    comboNodeSize.getSelectionModel().select(NodeAtt.LOG10_SUM_INTENSITY);
    menu.getChildren().add(comboNodeSize);
    comboNodeSize.setOnAction(e -> {
      NodeAtt selectedItem = comboNodeSize.getSelectionModel().getSelectedItem();
      setAttributeForAllNodes(GraphStyleAttribute.SIZE, selectedItem);
    });

    menu.getChildren().add(new Label("Label:"));
    ComboBox<NodeAtt> comboNodeLabel = new ComboBox<>(
        FXCollections.observableArrayList(NodeAtt.values()));
    comboNodeLabel.setTooltip(new Tooltip("Node label"));
    comboNodeLabel.getSelectionModel().select(NodeAtt.LABEL);
    menu.getChildren().add(comboNodeLabel);
    comboNodeLabel.setOnAction(e -> {
      NodeAtt selectedItem = comboNodeLabel.getSelectionModel().getSelectedItem();
      setAttributeForAllNodes(GraphStyleAttribute.LABEL, selectedItem);
    });

    menu.getChildren().add(new Label("Edge:"));
    ComboBox<EdgeAtt> comboEdgeLabel = new ComboBox<>(
        FXCollections.observableArrayList(EdgeAtt.values()));
    comboEdgeLabel.setTooltip(new Tooltip("Edge label"));
    comboEdgeLabel.getSelectionModel().select(EdgeAtt.LABEL);
    menu.getChildren().add(comboEdgeLabel);
    comboEdgeLabel.setOnAction(e -> {
      EdgeAtt selectedItem = comboEdgeLabel.getSelectionModel().getSelectedItem();
      setAttributeForAllEdges(GraphStyleAttribute.LABEL, selectedItem);
    });

    menu.setStyle("-fx-padding: 15; -fx-spacing: 15;");

    // #######################################################
    // add buttons
    ToggleButton toggleCollapseIons = new ToggleButton("Collapse ions");
    toggleCollapseIons.setMaxWidth(Double.MAX_VALUE);
    toggleCollapseIons.setSelected(collapse);
    toggleCollapseIons.selectedProperty()
        .addListener((o, old, value) -> collapseIonNodes(toggleCollapseIons.isSelected()));

    ToggleButton toggleShowMS2SimEdges = new ToggleButton("Show MS2 sim");
    toggleShowMS2SimEdges.setMaxWidth(Double.MAX_VALUE);
    toggleShowMS2SimEdges.setSelected(true);
    toggleShowMS2SimEdges.selectedProperty()
        .addListener((o, old, value) -> setShowMs2SimEdges(toggleShowMS2SimEdges.isSelected()));

    ToggleButton toggleShowRelations = new ToggleButton("Show relational edges");
    toggleShowRelations.setMaxWidth(Double.MAX_VALUE);
    toggleShowRelations.setSelected(true);
    toggleShowRelations.selectedProperty()
        .addListener((o, old, value) -> setConnectByNetRelations(toggleShowRelations.isSelected()));

    ToggleButton toggleShowIonIdentityEdges = new ToggleButton("Show ion edges");
    toggleShowIonIdentityEdges.setMaxWidth(Double.MAX_VALUE);
    toggleShowIonIdentityEdges.setSelected(true);
    toggleShowIonIdentityEdges.selectedProperty().addListener(
        (o, old, value) -> showIonIdentityEdges(toggleShowIonIdentityEdges.isSelected()));

    ToggleButton toggleShowEdgeLabel = new ToggleButton("Show edge label");
    toggleShowEdgeLabel.setMaxWidth(Double.MAX_VALUE);
    toggleShowEdgeLabel.setSelected(showEdgeLabels);
    toggleShowEdgeLabel.selectedProperty()
        .addListener((o, old, value) -> showEdgeLabels(toggleShowEdgeLabel.isSelected()));

    ToggleButton toggleShowNodeLabel = new ToggleButton("Show node label");
    toggleShowNodeLabel.setMaxWidth(Double.MAX_VALUE);
    toggleShowNodeLabel.setSelected(showNodeLabels);
    toggleShowNodeLabel.selectedProperty()
        .addListener((o, old, value) -> showNodeLabels(toggleShowNodeLabel.isSelected()));

    Button showGNPSMatches = new Button("GNPS matches");
    showGNPSMatches.setMaxWidth(Double.MAX_VALUE);
    showGNPSMatches.setOnAction(e -> showGNPSMatches());

    Button showLibraryMatches = new Button("Library matches");
    showLibraryMatches.setMaxWidth(Double.MAX_VALUE);
    showLibraryMatches.setOnAction(e -> showLibraryMatches());

    Spinner<Integer> nodeNeighbours = new Spinner<>(1, Integer.MAX_VALUE, 3, 1);
    Label l = new Label("No. of node neighbours:");
    neighborDistance.bind(nodeNeighbours.valueProperty());

    Button updateGraphButton = new Button("Update graph");
    updateGraphButton.setMaxWidth(Double.MAX_VALUE);
    updateGraphButton.setOnAction(e -> updateGraph());

    Button showOnlyConnectedNodesButton = new Button("Show only connected nodes");
    showOnlyConnectedNodesButton.setMaxWidth(Double.MAX_VALUE);
    showOnlyConnectedNodesButton.setOnAction(e -> visualizeConnectedNodesOnly());

    // finally add buttons
    VBox pnRightMenu = new VBox(4, toggleCollapseIons, toggleShowMS2SimEdges, toggleShowRelations,
        toggleShowIonIdentityEdges, toggleShowEdgeLabel, toggleShowNodeLabel, showGNPSMatches,
        showLibraryMatches, l, nodeNeighbours, updateGraphButton, showOnlyConnectedNodesButton);
    pnRightMenu.setSpacing(10);
    pnRightMenu.setPadding(new Insets(0, 20, 10, 20));
    this.setRight(pnRightMenu);
  }


  public void setAttributeForAllElements(GraphObject go, GraphStyleAttribute attribute,
      Object prop) {
    switch (go) {
      case NODE -> setAttributeForAllNodes(attribute, (NodeAtt) prop);
      case EDGE -> setAttributeForAllEdges(attribute, (EdgeAtt) prop);
    }
  }

  public void setAttributeForAllNodes(GraphStyleAttribute attribute, NodeAtt prop) {
    dynamicNodeStyle.put(attribute, prop);
    switch (attribute) {
      case COLOR -> applyNodeColorStyle();
      case LABEL -> applyLabelStyle(GraphObject.NODE);
      case SIZE -> applyNodeSizeStyle();
    }
  }

  public void setAttributeForAllEdges(GraphStyleAttribute attribute, EdgeAtt prop) {
    dynamicEdgeStyle.put(attribute, prop);
    switch (attribute) {
      case COLOR -> applyNodeColorStyle();
      case LABEL -> applyLabelStyle(GraphObject.EDGE);
      case SIZE -> applyNodeSizeStyle();
    }
  }

  public void updateGraph() {
    if (getMouseClickedNode() == null) {
      Alert alert = new Alert(AlertType.INFORMATION);
      alert.setContentText("Please click on any node First!!");
      alert.showAndWait();
    } else {
      filterNodeNeighbors(List.of(getMouseClickedNode()), neighborDistance.get());
    }
  }

  public void filterRowNeighbors(final List<FeatureListRow> center, final int distance) {
    filterNodeNeighbors(getNodes(center), distance);
  }

  private void filterNodeNeighbors(final List<Node> center, final int distance) {
    graph.setNodeNeighborFilter(center, distance);
    resetZoom();
  }

  /**
   * Visualize only the cluster (all connected nodes)
   */
  private void visualizeConnectedNodesOnly() {
    List<Node> isolatedNodes = graph.nodes()
        .filter(n -> (n.getInDegree() == 0 || n.getOutDegree() == 0)).toList();
    for (Node n : isolatedNodes) {
      graph.removeNode(n);
    }
  }

  /**
   * Show GNPS library match
   */
  private void showGNPSMatches() {
    int n = 0;
    for (Node node : graph) {
      String name = (String) node.getAttribute(GNPSLibraryMatch.ATT.COMPOUND_NAME.getKey());
      if (name != null) {
        node.setAttribute("ui.label", name);
        n++;
      }
    }
    logger.info("Show " + n + " GNPS library matches");
  }

  /**
   * Show spectral library matches
   */
  private void showLibraryMatches() {
    int n = 0;
    for (Node node : graph) {
      String name = (String) node.getAttribute(NodeAtt.SPECTRAL_LIB_MATCH_SUMMARY.toString());
      if (name != null) {
        node.setAttribute("ui.label", name);
        n++;
      }
    }
    logger.info("Show " + n + " spectral library matches");
  }

  private void showIonIdentityEdges(boolean selected) {
    showIonEdges = selected;
    collapseIonNodes(collapse);
  }

  public void collapseIonNodes(boolean collapse) {
    this.collapse = collapse;
    for (Node node : graph) {
      NodeType type = (NodeType) node.getAttribute(NodeAtt.TYPE.toString());
      if (type != null) {
        switch (type) {
          case NEUTRAL_LOSS_CENTER:
          case ION_FEATURE:
            setVisible(node, !collapse);
            break;
          case NEUTRAL_M:
            break;
          case SINGLE_FEATURE:
            break;
          default:
            break;
        }
      }
    }

    graph.edges().forEach(edge -> {
      EdgeType type = (EdgeType) edge.getAttribute(EdgeAtt.TYPE.toString());
      if (type != null) {
        switch (type) {
          case ION_IDENTITY -> setVisible(edge, !collapse && showIonEdges);
          case MS2_SIMILARITY_NEUTRAL_M_TO_FEATURE, MS2_SIMILARITY_NEUTRAL_M, MS2_SIMILARITY ->
              setVisible(edge, showMs2SimEdges);
          case NETWORK_RELATIONS -> setVisible(edge, showNetRelationsEdges);
          default -> {
          }
        }
      }
      // only if both nodes are visible
      if (!isVisible(edge.getSourceNode()) || !isVisible(edge.getTargetNode())) {
        setVisible(edge, false);
      }
    });
  }

  @Override
  public void clear() {
    super.clear();
  }


  private void applyDynamicStyles() {
    applyNodeSizeStyle();
    applyNodeColorStyle();
    applyLabelStyle(GraphObject.NODE);

    // edges
    applyLabelStyle(GraphObject.EDGE);

  }

  private void applyNodeSizeStyle() {
    NodeAtt nodeAttSize = dynamicNodeStyle.get(GraphStyleAttribute.SIZE);
    // min / max values of the specific attributes
    final Range<Float> sizeValueRange =
        nodeAttSize.isNumber() ? attributeRanges.computeIfAbsent(nodeAttSize,
            nodeAtt -> computeValueRange(rows, nodeAttSize)) : null;
    // for non numeric values - give each Object an index
    final Map<String, Integer> sizeValueMap = nodeAttSize.isNumber() ? null
        : attributeCategoryValuesMap.computeIfAbsent(nodeAttSize,
            att -> indexAllValues(nodeAttSize));
    final int numSizeValues = sizeValueMap == null ? 0 : sizeValueMap.size();

    for (Node node : graph) {
      NodeType type = (NodeType) node.getAttribute(NodeAtt.TYPE.toString());
      if (type == NodeType.NEUTRAL_M || type == NodeType.NEUTRAL_LOSS_CENTER) {
        continue;
      }
      // set size
      try {
        Object sizeValue = node.getAttribute(nodeAttSize.toString());
        if (sizeValue != null) {
          // differentiate between numeric values and a list of discrete values
          float size = 0;
          if (sizeValueRange != null) {
            size = interpolateIntensity(Float.parseFloat(sizeValue.toString()),
                sizeValueRange.lowerEndpoint(), sizeValueRange.upperEndpoint());
          } else if (sizeValueMap != null) {
            // non numeric values - use index
            int index = sizeValueMap.getOrDefault(sizeValue.toString(), 0);
            size = index / (float) numSizeValues;
          }
          size = Math.max(MIN_NODE_WIDTH_GU, size * MAX_NODE_WIDTH_GU);
          // set as graphical units for zoom effect
          // otherwise use fixed number of pixels
          node.setAttribute("ui.size", size + "gu");
        }
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Error while setting size attribute. " + ex.getMessage(), ex);
      }
    }
  }

  private void applyNodeColorStyle() {
    NodeAtt nodeAttColor = dynamicNodeStyle.get(GraphStyleAttribute.COLOR);
    final Range<Float> colorValueRange =
        nodeAttColor.isNumber() ? attributeRanges.computeIfAbsent(nodeAttColor,
            nodeAtt -> computeValueRange(rows, nodeAttColor)) : null;

    final Map<String, Integer> colorValueMap = nodeAttColor.isNumber() ? null
        : attributeCategoryValuesMap.computeIfAbsent(nodeAttColor,
            att -> indexAllValues(nodeAttColor));
    final int numColorValues = colorValueMap == null ? 0 : colorValueMap.size();

    for (Node node : graph) {
      NodeType type = (NodeType) node.getAttribute(NodeAtt.TYPE.toString());
      if (type == NodeType.NEUTRAL_M || type == NodeType.NEUTRAL_LOSS_CENTER) {
        continue;
      }
      try {
        if (nodeAttColor == NodeAtt.NONE) {
          node.removeAttribute("ui.class");
        } else {
          // make colors a gradient
          Object colorValue = node.getAttribute(nodeAttColor.toString());
          if (colorValue != null) {
            node.setAttribute("ui.class", "GRADIENT");
            // differentiate between numeric values and a list of discrete values
            if (colorValueRange != null) {
              final float interpolated = interpolateIntensity(
                  Float.parseFloat(colorValue.toString()), colorValueRange.lowerEndpoint(),
                  colorValueRange.upperEndpoint());
              node.setAttribute("ui.color", interpolated);
            } else if (colorValueMap != null) {
              // non numeric values - use index
              int index = colorValueMap.getOrDefault(colorValue.toString(), 0);
              node.setAttribute("ui.color", index / (float) numColorValues);
            }
          }
        }
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Error while setting color attribute. " + ex.getMessage(), ex);
        logger.log(Level.SEVERE, ex.getMessage(), ex);
      }
    }
  }

  private void applyLabelStyle(GraphObject target) {
    final String att = getStyleAttribute(target, GraphStyleAttribute.LABEL);
    target.stream(graph).forEach(node -> {
      try {
        node.setAttribute("ui.label", getOrElseString(node, att, ""));
      } catch (Exception ex) {
        logger.log(Level.SEVERE, "Error while setting label attribute. " + ex.getMessage(), ex);
      }
    });
  }

  /**
   * Get style attribute
   *
   * @param target   the target object to style
   * @param styleAtt the styling attribute of the node or edge
   * @return either a {@link NodeAtt} or {@link EdgeAtt}
   */
  public String getStyleAttribute(GraphObject target, GraphStyleAttribute styleAtt) {
    return Objects.toString(getDynamicStyle(target).get(styleAtt), null);
  }

  /**
   * get the dynamic style map for target
   *
   * @param target edge or node as target
   * @return style map
   */
  public EnumMap<GraphStyleAttribute, ?> getDynamicStyle(GraphObject target) {
    return switch (target) {
      case NODE -> dynamicNodeStyle;
      case EDGE -> dynamicEdgeStyle;
    };
  }

  public EnumMap<GraphStyleAttribute, NodeAtt> getDynamicNodeStyle() {
    return dynamicNodeStyle;
  }

  public EnumMap<GraphStyleAttribute, EdgeAtt> getDynamicEdgeStyle() {
    return dynamicEdgeStyle;
  }

  /**
   * Index all objects found in all rows for an attribute
   *
   * @param attribute the node attribute for this row
   * @return map of all objects found and their idexes in their original order
   */
  private Map<String, Integer> indexAllValues(NodeAtt attribute) {
    Map<String, Integer> map = new HashMap<>();
    int currentIndex = 0;
    for (Node node : graph) {
      try {
        String object = Objects.requireNonNullElse(node.getAttribute(attribute.toString()), "")
            .toString();
        if (object.isEmpty()) {
          continue;
        }
        if (!map.containsKey(object)) {
          map.put(object, currentIndex);
          currentIndex++;
        }
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex.getMessage(), ex);
      }
    }
    return map;
  }

  private Range<Float> computeValueRange(List<FeatureListRow> rows, NodeAtt attribute) {
    float min = Float.POSITIVE_INFINITY;
    float max = Float.NEGATIVE_INFINITY;

    for (FeatureListRow row : rows) {
      try {
        Object object = attribute.getValue(row);
        if (object == null) {
          continue;
        }
        float value = Float.parseFloat(object.toString());
        if (value < min) {
          min = value;
        }
        if (value > max) {
          max = value;
        }
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex.getMessage(), ex);
      }
    }

    if (Float.compare(Float.POSITIVE_INFINITY, min) == 0) {
      min = 0;
    }
    if (Float.compare(Float.NEGATIVE_INFINITY, max) == 0) {
      max = 1;
    }
    return Range.closed(min, max);
  }

  /**
   * ratio (0-1) between min and maxIntensity
   *
   * @param value the intensity value
   * @return a value between 0-1 (including)
   */
  protected float interpolateIntensity(float value, float min, float max) {
    return (float) Math.min(1.0, Math.max(0.0, (value - min) / (max - min)));
  }

  public void setConnectByNetRelations(boolean connectByNetRelations) {
    this.showNetRelationsEdges = connectByNetRelations;
    collapseIonNodes(collapse);
  }

  public void dispose() {
    graph.clear();
  }

  public void setShowMs2SimEdges(boolean ms2SimEdges) {
    this.showMs2SimEdges = ms2SimEdges;
    collapseIonNodes(collapse);
  }

  public FeatureList getFeatureList() {
    return featureList;
  }

  public void setUseMs1FeatureShapeEdges(boolean ms1FeatureShapeEdges) {
    this.ms1FeatureShapeEdges = ms1FeatureShapeEdges;
  }

  public void focusSelectedNodes() {
    // will automatically trigger an update
    focussedRows.setAll(getRowsFromNodes(selectedNodes));
  }

  public void selectNodesByAnnotation(final String annotationFilter) {
    List<Node> nodes = annotationsFilter.findNodes(annotationFilter);
    logger.fine(
        "Selecting %d nodes by annotations filter: %s".formatted(nodes.size(), annotationFilter));
    selectedNodes.setAll(nodes);
  }
}
