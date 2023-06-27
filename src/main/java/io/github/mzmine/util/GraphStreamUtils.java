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

package io.github.mzmine.util;


import static io.github.mzmine.modules.visualization.networking.visual.NodeAtt.COMMUNITY_ID;

import io.github.mzmine.modules.dataprocessing.group_spectral_networking.NetworkCluster;
import io.github.mzmine.modules.visualization.networking.visual.NodeAtt;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.graphstream.algorithm.community.EpidemicCommunityAlgorithm;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.jetbrains.annotations.NotNull;

public class GraphStreamUtils {

  /**
   * Unique list of node neighbors within edge distance
   *
   * @param node         node to visit and all its neighbors
   * @param edgeDistance number of consecutive edges connecting neighbors
   * @return list of all neighbors + the initial node
   */
  public static Set<Node> getNodeNeighbors(Node node, int edgeDistance) {
    Object2IntOpenHashMap<Node> visited = new Object2IntOpenHashMap<>();
    visited.put(node, edgeDistance);
    addNodeNeighbors(visited, node, edgeDistance);
    return visited.keySet();
  }

  /**
   * Add all neighbors to the visited list and check for higher edgeDistance to really capture all
   * neighbors
   *
   * @param visited      map that tracks visited nodes and their edgeDistance
   * @param node
   * @param edgeDistance
   */
  private static void addNodeNeighbors(Object2IntOpenHashMap<Node> visited, Node node,
      int edgeDistance) {
    final int nextDistance = edgeDistance - 1;
    node.neighborNodes().forEach(neighbor -> {
      if (visited.getOrDefault(neighbor, -1) < edgeDistance) {
        // was never visited or was visited with lower edgeDistance - visit this time
        visited.put(neighbor, nextDistance);
        if (edgeDistance > 1) {
          addNodeNeighbors(visited, neighbor, nextDistance);
        }
      }
    });
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
        addAllNodeNeighbors(clusterIds, node, id);
      }
    });

    // create clusters
    Map<Integer, List<Node>> clusters = new HashMap<>();
    clusterIds.forEach((node, id) -> {
      List<Node> nodes = clusters.computeIfAbsent(id, __ -> new ArrayList<>());
      nodes.add(node);
    });

    nextClusterId.set(1);
    var sortedClusters = clusters.values().stream().sorted(Comparator.comparingInt(List::size))
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
}
