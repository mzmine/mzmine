package modules;

import java.util.stream.Stream;
import org.graphstream.algorithm.generator.FullGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.junit.Test;
public class TestNodeNeighbourMethod {

  public Graph CreateGraph()
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
  @Test
  public Stream<Node> streamNodeNeighborsBreadthFirst(Node node, int edgeDistance) {
    if (edgeDistance < 0) {
      throw new IllegalArgumentException("Distance cannot be negative, value=" + edgeDistance);
    }
    return switch (edgeDistance) {
      case 0 -> Stream.of();
      case 1 -> node.neighborNodes();
      default -> {
        Stream<Node> stream = node.neighborNodes();
        Stream<Node> nextLevel = node.neighborNodes().flatMap(
            neighbor -> streamNodeNeighborsBreadthFirst(neighbor, edgeDistance - 1).distinct());
        yield Stream.concat(stream, nextLevel);
      }
    };
  }
  @Test
  public void testmethod()
  {
    System.setProperty("org.graphstream.ui","javafx");
    TestNodeNeighbourMethod t = new TestNodeNeighbourMethod();
    t.streamNodeNeighborsBreadthFirst(t.CreateGraph().getNode("A"),2).forEach(s -> System.out.print(s.getId()));
  }
}
