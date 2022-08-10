/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Set;
import org.graphstream.graph.Node;

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
}
