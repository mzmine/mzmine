package modules;

import io.github.mzmine.util.GraphStreamUtils;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GraphStreamUtilsTest {

  @Before
  public static Graph createGraph() {
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

  @Test
  public void testMethod() {
    String traversingSequence = GraphStreamUtils.getNodeNeighbors(createGraph().getNode("A"), 2)
        .toString();
    Assertions.assertEquals("[F, A, B, J, C, E, D]", traversingSequence);
  }
}
