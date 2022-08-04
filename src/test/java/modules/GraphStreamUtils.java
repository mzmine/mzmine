package modules;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class GraphStreamUtils {

  public Graph createGraph()
  {
    Graph graph = new MultiGraph("Test-Graph");
    graph.setStrict(false);
    graph.setAutoCreate(true);
    graph.addEdge("AB", "A", "B");
    graph.addEdge("AC", "A", "C");
    graph.addEdge("CD", "C", "D");
    graph.addEdge("BD", "B", "D");
    graph.addEdge("BE", "B", "E");
    graph.addEdge("BF", "B", "F");
    graph.addEdge("BC", "B", "C");
    graph.addEdge("GF", "G", "F");
    graph.addEdge("HI", "H", "I");
    graph.addEdge("FE", "F", "E");
    graph.addEdge("FK", "F", "K");
    graph.addEdge("FI", "F", "I");
    graph.addEdge("KL", "K", "L");
    graph.addEdge("JL", "J", "L");
    graph.addEdge("IJ", "I", "J");
    graph.addEdge("FJ", "F", "J");
    graph.addEdge("CJ", "C", "J");
    graph.addEdge("KJ", "K", "J");
    for (Node node : graph) {
      node.setAttribute("ui.label", node.getId());
    }
    return graph;
  }

/**
 * Unique list of node neighbors within edge distance
 *
 * @param node         node to visit and all its neighbors
 * @param edgeDistance number of consecutive edges connecting neighbors
 * @return list of all neighbors + the initial node
 */
  public List<Node> getNodeNeighbors(Node node, int edgeDistance) {
    Object2IntOpenHashMap<Node> visited = new Object2IntOpenHashMap<>();
    visited.put(node, edgeDistance);
    addNodeNeighbors(visited, node, edgeDistance);
    return new ArrayList<>(visited.keySet());
  }

  /**
   * Add all neighbors to the visited list and check for higher edgeDistance to really capture all
   * neighbors
   *
   * @param visited map that tracks visited nodes and their edgeDistance
   * @param node
   * @param edgeDistance
   */
  private void addNodeNeighbors(Object2IntOpenHashMap<Node> visited, Node node, int edgeDistance) {
    final int nextDistance = edgeDistance-1;
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
  @Test
  public void testmethod()
  {
    String traversingSequence=getNodeNeighbors(createGraph().getNode("A"),2).toString();
    Assertions.assertEquals("FABJCED",traversingSequence);
  }
}
