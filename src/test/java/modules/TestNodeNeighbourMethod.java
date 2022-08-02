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
    Generator gen = new FullGenerator();
    gen.addSink(graph);
    gen.begin();
    while (graph.getNodeCount() < 50 && gen.nextEvents());
    gen.end();
    int i=0;
    for (Node node : graph) {
      node.setAttribute("ui.label",(i+1)+"");
      i++;
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
  public void printNodes()
  {
    streamNodeNeighborsBreadthFirst(CreateGraph().getNode("1"),3).forEach(s -> System.out.println(s.getId()));
  }
}
