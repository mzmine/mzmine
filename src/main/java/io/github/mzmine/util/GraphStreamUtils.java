package io.github.mzmine.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import org.graphstream.graph.Node;

public class GraphStreamUtils {

  /**
   * Unique list of node neighbors within edge distance
   *
   * @param node         node to visit and all its neighbors
   * @param edgeDistance number of consecutive edges connecting neighbors
   * @return list of all neighbors + the initial node
   */
  public static List<Node> getNodeNeighbors(Node node, int edgeDistance) {
    Object2IntOpenHashMap<Node> visited = new Object2IntOpenHashMap<>();
    visited.put(node, edgeDistance);
    addNodeNeighbors(visited, node, edgeDistance);
    return new ArrayList<>(visited.keySet());
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
}
