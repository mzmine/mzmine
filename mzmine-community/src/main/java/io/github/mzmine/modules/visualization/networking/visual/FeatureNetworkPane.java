/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import static io.github.mzmine.modules.visualization.networking.visual.enums.GraphObject.EDGE;
import static io.github.mzmine.modules.visualization.networking.visual.enums.GraphObject.NODE;
import static io.github.mzmine.modules.visualization.networking.visual.enums.GraphRepresentation.FILTERED;
import static io.github.mzmine.modules.visualization.networking.visual.enums.GraphRepresentation.FULL;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch;
import io.github.mzmine.modules.visualization.networking.visual.enums.EdgeAtt;
import io.github.mzmine.modules.visualization.networking.visual.enums.EdgeType;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphElementAttr;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphObject;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphRepresentation;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphStyleAttribute;
import io.github.mzmine.modules.visualization.networking.visual.enums.GraphUnits;
import io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt;
import io.github.mzmine.modules.visualization.networking.visual.enums.NodeType;
import io.github.mzmine.modules.visualization.networking.visual.stylers.GraphColorStyler;
import io.github.mzmine.modules.visualization.networking.visual.stylers.GraphLabelStyler;
import io.github.mzmine.modules.visualization.networking.visual.stylers.GraphSizeStyler;
import io.github.mzmine.modules.visualization.networking.visual.stylers.GraphStyler;
import io.github.mzmine.util.GraphStreamUtils;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureNetworkPane extends NetworkPane {

  private static final Logger logger = Logger.getLogger(FeatureNetworkPane.class.getName());

  // currently set dynamic node styles like color, size, label
  private final EnumMap<GraphStyleAttribute, NodeAtt> dynamicNodeStyle = new EnumMap<>(
      GraphStyleAttribute.class);
  private final EnumMap<GraphStyleAttribute, EdgeAtt> dynamicEdgeStyle = new EnumMap<>(
      GraphStyleAttribute.class);
  // style values need to be set as float - double crashes in the javafx thread for graphstream
  private final Map<GraphElementAttr, Range<Float>> attributeRanges = new HashMap<>();
  // for non numeric values: store all objects and provide indexes
  private final Map<GraphElementAttr, Map<String, Integer>> attributeCategoryValuesMap = new HashMap<>();

  // store all node annotations here for quick selections
  private final NodeAnnotationsFilter annotationsFilter;
  private final Set<String> uniqueEdgeTypes;

  // the network generator
  private final FeatureNetworkGenerator generator;
  private final FeatureNetworkController controller;
  /**
   * Max width in graph units. 1 is the distance between nodes
   */
  private final List<GraphStyler> graphStylers = List.of(
      new GraphSizeStyler(NODE, GraphUnits.gu, 0.02f, 0.3f, 0.1f),
      new GraphSizeStyler(EDGE, GraphUnits.px, 0.5f, 7f, 2f),
      // labels
      new GraphLabelStyler(NODE), new GraphLabelStyler(EDGE),
      // colors
      new GraphColorStyler(NODE), new GraphColorStyler(EDGE));

  // data
  private final FeatureList featureList;

  // those rows are focussed - usually showing its neighbors
  private final @NotNull ObservableList<Node> visibleNodes = FXCollections.observableArrayList();
  private final @NotNull ObservableList<FeatureListRow> visibleRows = FXCollections.observableArrayList();
  private final @NotNull ObservableList<FeatureListRow> focussedRows;

  public FeatureNetworkPane(final FeatureNetworkController controller,
      final @NotNull FeatureList featureList,
      final @NotNull ObservableList<FeatureListRow> focussedRows,
      final @NotNull FeatureNetworkGenerator generator, final @NotNull MultiGraph fullGraph) {
    super("Molecular Networks", false, fullGraph);
    this.controller = controller;
    this.featureList = featureList;
    this.focussedRows = focussedRows;
    this.generator = generator;

    uniqueEdgeTypes = GraphStreamUtils.getUniqueEdgeTypes(fullGraph);

    focussedRows.addListener(this::handleFocussedRowsChanged);
    annotationsFilter = new NodeAnnotationsFilter(this);
    graph.addGraphChangeListener(this::graphChanged);
  }

  private void graphChanged(final FilterableGraph graph) {
    var visible = graph.nodes().map(this::getRowFromNode).filter(Objects::nonNull).toList();
    visibleRows.setAll(visible);
    visibleNodes.setAll(graph.nodes().toList());

    clearPrecomputedDynamicAttributeValues();
    applyDynamicStyles();
    collapseIonNodes(controller.cbCollapseIons.isSelected());
  }

  public @NotNull ObservableList<FeatureListRow> getVisibleRows() {
    return visibleRows;
  }

  public @NotNull ObservableList<Node> getVisibleNodes() {
    return visibleNodes;
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
    logger.fine(() -> "Showing neighboring nodes distance %d of selected nodes %d".formatted(
        getNeighborDistance(), selected.size()));
    if (selected.isEmpty()) {
      return;
    }
    filterNodeNeighbors(selected, getNeighborDistance());
  }


  @Override
  protected void onGraphClicked(final @NotNull MouseEvent e, final @Nullable GraphicNode goNode,
      final @Nullable GraphicEdge goEdge, final @Nullable Node node, final @Nullable Edge edge) {
    super.onGraphClicked(e, goNode, goEdge, node, edge);
    if (!selectedNodes.isEmpty() && e.isShortcutDown()) {
      focusSelectedNodes();
    }
  }

  public @NotNull ObservableList<FeatureListRow> getFocussedRows() {
    return focussedRows;
  }

  public int getNeighborDistance() {
    return controller.neighborDistanceProperty().getValue();
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


  public FeatureListRow getRowFromNode(final Node a) {
    return (FeatureListRow) a.getAttribute(NodeAtt.ROW.toString());
  }

  public List<FeatureListRow> getRowsFromNodes(final List<? extends Node> nodes) {
    return nodes.stream().map(super::mapGraphicObjectToGraph).map(this::getRowFromNode)
        .filter(Objects::nonNull).toList();
  }

  public void setAttributeForAllElements(GraphStyleAttribute gsa, GraphElementAttr attribute) {
    if (attribute == null) {
      return;
    }

    var oldValue = switch (attribute.getGraphObject()) {
      case NODE -> dynamicNodeStyle.put(gsa, (NodeAtt) attribute);
      case EDGE -> dynamicEdgeStyle.put(gsa, (EdgeAtt) attribute);
    };
    if (!Objects.equals(oldValue, attribute)) {
      applyStyler(gsa, attribute);
    }
  }

  public EnumMap<GraphStyleAttribute, ? extends GraphElementAttr> getStyleAttributeMap(
      GraphObject go) {
    return switch (go) {
      case NODE -> dynamicNodeStyle;
      case EDGE -> dynamicEdgeStyle;
    };
  }

  @NotNull
  private GraphStyler getStyler(GraphObject go, GraphStyleAttribute gsa) {
    // there are stylers for all styles! otherwise throw
    return graphStylers.stream().filter(st -> st.matches(go, gsa)).findFirst().orElseThrow();
  }

  private void applyStyler(GraphStyleAttribute gsa, final GraphElementAttr attribute) {
    applyStyler(getStyler(attribute.getGraphObject(), gsa), attribute);
  }

  private void applyStyler(final GraphStyler styler) {
    var graph = graph(FILTERED);
    var attribute = getStyleAttribute(styler.getGraphObject(), styler.getGraphStyleAttribute());
    styler.applyStyle(graph, attribute, this::getValueRange, this::getValueMap);
  }

  private void applyStyler(final GraphStyler styler, final GraphElementAttr attribute) {
    var graph = graph(FILTERED);
    styler.applyStyle(graph, attribute, this::getValueRange, this::getValueMap);
  }

  private void applyDynamicStyles() {
    for (final GraphStyler styler : graphStylers) {
      applyStyler(styler);
    }
  }

  public void updateGraph() {
    if (getMouseClickedNode() == null) {
      Alert alert = new Alert(AlertType.INFORMATION);
      alert.setContentText("Please click on any node First!!");
      alert.showAndWait();
    } else {
      filterNodeNeighbors(List.of(getMouseClickedNode()), getNeighborDistance());
    }
  }

  private void filterNodeNeighbors(final List<Node> center, final int distance) {
    graph.setNodeNeighborFilter(center, distance);
    resetZoom();
  }

  private void clearPrecomputedDynamicAttributeValues() {
    removeDynamicAttributes(attributeRanges);
    removeDynamicAttributes(attributeCategoryValuesMap);
  }

  private void removeDynamicAttributes(final Map<GraphElementAttr, ?> map) {
    var toRemove = map.keySet().stream().filter(GraphElementAttr::isChangingDynamically).toList();
    for (var key : toRemove) {
      map.remove(key);
    }
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
  public void showLibraryMatches() {
    int n = 0;
    for (Node node : graph) {
      String name = (String) node.getAttribute(NodeAtt.LIB_MATCH.toString());
      if (name != null) {
        node.setAttribute("ui.label", name);
        n++;
      }
    }
    logger.info("Show " + n + " spectral library matches");
  }

  public void collapseIonNodes(boolean collapse) {
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
          case ION_IDENTITY -> setVisible(edge, !collapse);
          case MS2_MODIFIED_COSINE, NETWORK_RELATIONS, MS2Deepscore, DREAMS -> setVisible(edge, true);
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

  @Nullable
  public Range<Float> getValueRange(final GraphElementAttr attribute) {
    if (!attribute.isNumber()) {
      return null;
    }
    return attributeRanges.computeIfAbsent(attribute, attr -> computeValueRange(attribute));
  }


  /**
   * @param representation full graph or filtered version
   * @return stream of edges
   */
  public Stream<Edge> edges(GraphRepresentation representation) {
    return graph(representation).edges();
  }

  /**
   * @param representation full graph or filtered version
   * @return stream of nodes
   */
  public Stream<Node> nodes(GraphRepresentation representation) {
    return graph(representation).nodes();
  }

  /**
   * @param representation full graph or filtered version
   * @return filtered graph or full graph
   */
  public Graph graph(GraphRepresentation representation) {
    return switch (representation) {
      case FULL -> graph.getFullGraph();
      case FILTERED -> graph;
    };
  }

  @NotNull
  private Map<String, Integer> getValueMap(final GraphElementAttr attribute) {
    return attribute.isNumber() ? Map.of()
        : attributeCategoryValuesMap.computeIfAbsent(attribute, att -> indexAllValues(attribute));
  }

  /**
   * Get style attribute
   *
   * @param go       the target object to style
   * @param styleAtt the styling attribute of the node or edge
   * @return either a {@link NodeAtt} or {@link EdgeAtt}
   */
  public GraphElementAttr getStyleAttribute(GraphObject go, GraphStyleAttribute styleAtt) {
    return getDynamicStyle(go).get(styleAtt);
  }

  /**
   * get the dynamic style map for target
   *
   * @param target edge or node as target
   * @return style map
   */
  public EnumMap<GraphStyleAttribute, ? extends GraphElementAttr> getDynamicStyle(
      GraphObject target) {
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
  @NotNull
  private Map<String, Integer> indexAllValues(GraphElementAttr attribute) {
    Map<String, Integer> map = new HashMap<>();
    AtomicInteger currentIndex = new AtomicInteger(0);
    attribute.getGraphObject().stream(graph(FULL)).forEach(node -> {
      try {
        String object = GraphStreamUtils.getStringOrElse(node, attribute, "").strip().toLowerCase();
        map.computeIfAbsent(object, k -> currentIndex.getAndIncrement());
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex.getMessage(), ex);
      }
    });
    return map;
  }

  @Nullable
  private Range<Float> computeValueRange(GraphElementAttr attribute) {
    GraphObject go = attribute.getGraphObject();
    var summary = go.stream(graph(FULL)).map(e -> GraphStreamUtils.getDoubleValue(e, attribute))
        .filter(Optional::isPresent).mapToDouble(Optional::get).summaryStatistics();
    if (summary.getCount() == 0) {
      return null;
    }

    return Range.closed((float) summary.getMin(), (float) summary.getMax());
  }


  public void dispose() {
    graph.clear();
  }

  public FeatureList getFeatureList() {
    return featureList;
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

  public Set<String> getUniqueEdgeTypes() {
    return uniqueEdgeTypes;
  }
}
