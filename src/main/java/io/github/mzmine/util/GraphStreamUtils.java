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

package io.github.mzmine.util;


import static io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt.COMMUNITY_ID;

import io.github.mzmine.modules.dataprocessing.group_spectral_networking.NetworkCluster;
import io.github.mzmine.modules.visualization.networking.visual.enums.EdgeAtt;
import io.github.mzmine.modules.visualization.networking.visual.enums.ElementType;
import io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.graphstream.algorithm.community.EpidemicCommunityAlgorithm;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.jetbrains.annotations.NotNull;

public class GraphStreamUtils {

  private static final Logger logger = Logger.getLogger(GraphStreamUtils.class.getName());

  /**
   * Unique list of node neighbors within edge distance
   *
   * @param nodes       nodes to visit and all their neighbors
   * @param maxDistance number of consecutive edges connecting neighbors
   * @return set of all neighbors + the initial node
   */
  public static Set<Node> getNodeNeighbors(Graph graph, List<Node> nodes, int maxDistance) {
    // clear
    graph.edges().forEach(e -> e.removeAttribute(EdgeAtt.NEIGHBOR_DISTANCE.toString()));
    graph.nodes().forEach(n -> n.removeAttribute(NodeAtt.NEIGHBOR_DISTANCE.toString()));

    // retain insert order
    Set<Node> visited = new LinkedHashSet<>();
    for (final Node node : nodes) {
      node.setAttribute(NodeAtt.NEIGHBOR_DISTANCE.toString(), 0);
      visited.add(node);
    }
    if (maxDistance == 0) {
      return visited;
    }
    for (final Node node : nodes) {
      // after adding all initial nodes we add the rest
      addNodeNeighbors(visited, node, 1, maxDistance);
    }
    return visited;
  }

  /**
   * Unique list of node neighbors within edge distance
   *
   * @param node        node to visit and all its neighbors
   * @param maxDistance number of consecutive edges connecting neighbors
   * @return set of all neighbors + the initial node
   */
  public static Set<Node> getNodeNeighbors(Graph graph, Node node, int maxDistance) {
    return getNodeNeighbors(graph, List.of(node), maxDistance);
  }

  /**
   * Add all neighbors to the visited list and check for higher edgeDistance to really capture all
   * neighbors
   *
   * @param visited      map that tracks visited nodes and their edgeDistance
   * @param node         current node to visit
   * @param edgeDistance current distance from nodes
   */
  private static void addNodeNeighbors(Set<Node> visited, Node node, int edgeDistance,
      int maxDistance) {
    String eDistAttr = EdgeAtt.NEIGHBOR_DISTANCE.toString();
    String nDistAttr = NodeAtt.NEIGHBOR_DISTANCE.toString();
    List<Node> checkNeighbors = new ArrayList<>();
    node.edges().forEach(edge -> {
      int currentEdgeDist = GraphStreamUtils.getIntegerOrElse(edge, eDistAttr, Integer.MAX_VALUE);
      if (currentEdgeDist > edgeDistance) {
        edge.setAttribute(eDistAttr, edgeDistance);
        var opposite = edge.getOpposite(node);
        int opDist = GraphStreamUtils.getIntegerOrElse(opposite, nDistAttr, Integer.MAX_VALUE);
        if (opDist > edgeDistance) {
          // refresh node distance
          opposite.setAttribute(nDistAttr, edgeDistance);
          checkNeighbors.add(opposite);
          visited.add(opposite);
        }
      }
    });

    int nextDistance = edgeDistance + 1;
    if (nextDistance <= maxDistance) {
      for (var neighbor : checkNeighbors) {
        addNodeNeighbors(visited, neighbor, nextDistance, maxDistance);
      }
    }
  }


  /**
   * detect communities with {@link EpidemicCommunityAlgorithm} and add attributes to
   * NodeAtt.COMMUNITY_ID
   *
   * @param graph
   */
  public static void detectCommunities(final MultiGraph graph) {
    // detect communitites
    String attribute = COMMUNITY_ID.toString();
    String attributeScore = attribute + ".score";
    EpidemicCommunityAlgorithm detector = new EpidemicCommunityAlgorithm(graph, attribute);
    detector.setRandom(new Random(1789));
    detector.compute();
    String marker = detector.getMarker();
    String markerScore = marker + ".score";

    graph.nodes().forEach(node -> {
      node.setAttribute(attribute, node.getAttribute(marker));
      node.setAttribute(attributeScore, node.getAttribute(markerScore));
      node.removeAttribute(marker);
      node.removeAttribute(markerScore);
    });
  }


  /**
   * detect largest clusters
   *
   * @return list of {@link NetworkCluster} sorted from max size to min
   */
  public static List<NetworkCluster> detectClusters(final MultiGraph graph, boolean addAttribute) {
    // detect communitites
    Map<Node, Integer> clusterIds = new HashMap<>();
    AtomicInteger nextClusterId = new AtomicInteger(1);
    graph.nodes().forEach(node -> {
      Integer id = clusterIds.get(node);
      // not already visited
      if (id == null) {
        id = nextClusterId.getAndIncrement();
        clusterIds.put(node, id);
        // check all connected nodes
        try {
          addAllNodeNeighbors(clusterIds, node, id);
        } catch (StackOverflowError ex) {
          logger.fine("Failed to nubmer clusters correctly. Networks were too large.");
        }
      }
    });

    // create clusters
    Map<Integer, List<Node>> clusters = new HashMap<>();
    clusterIds.forEach((node, id) -> {
      List<Node> nodes = clusters.computeIfAbsent(id, __ -> new ArrayList<>());
      nodes.add(node);
    });

    nextClusterId.set(1);
    var sortedClusters = clusters.values().stream()
        .sorted(Comparator.comparing(List::size, Comparator.reverseOrder()))
        .map(nodes -> new NetworkCluster(nodes, nextClusterId.getAndIncrement())).toList();

    if (addAttribute) {
      for (NetworkCluster(List<Node> nodes, int id) : sortedClusters) {
        for (final Node node : nodes) {
          node.setAttribute(NodeAtt.CLUSTER_ID.toString(), id);
          node.setAttribute(NodeAtt.CLUSTER_SIZE.toString(), nodes.size());
        }
      }
    }
    return sortedClusters;
  }

  private static void addAllNodeNeighbors(Map<Node, Integer> clusterIds, Node node, int clusterId) {
    node.neighborNodes().forEach(neighbor -> {
      Integer id = clusterIds.get(neighbor);
      // not already visited
      if (id == null) {
        clusterIds.put(neighbor, clusterId);
        // check all connected nodes
        addAllNodeNeighbors(clusterIds, neighbor, clusterId);
      }
    });
  }

  /**
   * Community sizes are counted, grouping by COMMUNITY_ID
   *
   * @return map of community to size
   */
  @NotNull
  public static Object2IntMap<Object> getCommunitySizes(final MultiGraph graph) {
    Object2IntMap<Object> communitySizes = new Object2IntOpenHashMap<>();
    graph.nodes().forEach(node -> {
      Object communityId = node.getAttribute(COMMUNITY_ID.toString());
      if (communityId != null) {
        communitySizes.computeInt(communityId,
            (key, communitySize) -> communitySize == null ? 1 : communitySize + 1);
      }
    });
    return communitySizes;
  }


  public static double getDoubleOrElse(Element e, Object attribute, double defaultValue) {
    try {
      var value = e.getAttribute(attribute.toString());
      return Double.parseDouble(value.toString());
    } catch (Exception ex) {
      return defaultValue;
    }
  }

  public static Optional<Double> getDoubleValue(Element e, Object attribute) {
    try {
      var value = e.getAttribute(attribute.toString());
      return Optional.of(Double.parseDouble(value.toString()));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static float getFloatOrElse(Element e, Object attribute, float defaultValue) {
    try {
      var value = e.getAttribute(attribute.toString());
      return Float.parseFloat(value.toString());
    } catch (Exception ex) {
      return defaultValue;
    }
  }

  public static Optional<Float> getFloatValue(Element e, Object attribute) {
    try {
      var value = e.getAttribute(attribute.toString());
      return Optional.of(Float.parseFloat(value.toString()));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static int getIntegerOrElse(Element e, Object attribute, int defaultValue) {
    try {
      var value = e.getAttribute(attribute.toString());
      return Integer.parseInt(value.toString());
    } catch (Exception ex) {
      return defaultValue;
    }
  }

  public static Optional<Integer> getIntegerValue(Element e, Object attribute) {
    try {
      var value = e.getAttribute(attribute.toString());
      return Optional.of(Integer.parseInt(value.toString()));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static String getStringOrElse(Element e, final Object attribute,
      final String defaultValue) {
    return Objects.requireNonNullElse(e.getAttribute(attribute.toString()), defaultValue)
        .toString();
  }

  public static Optional<ElementType> getElementType(final Element element) {
    var o = switch (element) {
      case Node __ -> getAttribute(element, NodeAtt.TYPE);
      case Edge __ -> getAttribute(element, EdgeAtt.TYPE);
      default -> throw new IllegalStateException("Unexpected value: " + element);
    };
    if (o.isPresent() && o.get() instanceof ElementType etype) {
      return Optional.of(etype);
    }
    return Optional.empty();
  }

  /**
   * Creates a filtered graph with only the selected nodes and all edges between those nodes.
   *
   * @param nodes list of filtered nodes
   * @return filtered graph
   */
  @NotNull
  public static MultiGraph createFilteredCopy(final Collection<Node> nodes) {
    MultiGraph gl = new MultiGraph("layout_graph");

    for (Node n : nodes) {
      addCopy(gl, n);
    }
    for (Node n : nodes) {
      n.enteringEdges().forEach(edge -> {
        // need to contain both nodes
        if (nodes.contains(edge.getSourceNode()) && nodes.contains(edge.getTargetNode())) {
          addCopy(gl, edge);
        }
      });
    }

    return gl;
  }

  /**
   * Add copy of element to targetGraph
   *
   * @return the element copy
   */
  public static Element addCopy(final MultiGraph targetGraph, final Element element) {
    if (element instanceof Node n) {
      var node = targetGraph.addNode(n.getId());
      copyAttributes(n, node);
      return node;
    }
    if (element instanceof Edge e) {
      if (targetGraph.getEdge(e.getId()) != null) {
        return null;
      }
      var source = targetGraph.getNode(e.getSourceNode().getId());
      var target = targetGraph.getNode(e.getTargetNode().getId());
      if (source == null || target == null) {
        return null;
      }
      var edge = targetGraph.addEdge(e.getId(), source, target);
      copyAttributes(e, edge);
      return edge;
    }
    return null;
  }

  /**
   * Add all attributes from source to target
   */
  public static void copyAttributes(final Element source, final Element target) {
    source.attributeKeys().forEach(att -> {
      target.setAttribute(att, source.getAttribute(att));
    });
  }

  /**
   * UI class of node or edge
   *
   * @param element node or edge
   * @return ui class or empty
   */
  public static Optional<String> getUiClass(final Element element) {
    return getElementType(element).flatMap(ElementType::getUiClass);
  }

  private static Optional<Object> getAttribute(final Element element, final Object attribute) {
    return Optional.ofNullable(element.getAttribute(attribute.toString()));
  }

  /**
   * All unique values of EdgeAtt.Type
   *
   * @param graph full graph
   * @return set of unique values
   */
  public static Set<String> getUniqueEdgeTypes(final MultiGraph graph) {
    return graph.edges().map(e -> getStringOrElse(e, EdgeAtt.TYPE, "NONE"))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Clears target graph and adds all content from source
   */
  public static void copyGraphContent(final MultiGraph source, final MultiGraph target) {
    source.nodes().forEach(n -> {
      addCopy(target, n);
    });
    source.edges().forEach(e -> {
      addCopy(target, e);
    });
  }
}
