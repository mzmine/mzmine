package io.github.mzmine.modules.visualization.networking.visual;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

public class FilteredGraph extends MultiGraph {

  private MultiGraph fullGraph;

  public FilteredGraph(String id) {
    super(id);
    this.fullGraph= new MultiGraph("Full-Graph");
  }

  public void setFullGraph(MultiGraph graph) {
    this.fullGraph = graph;
  }

  public void setNodeFilter(Set<Node> neighboringNodes) {
    Set<String> neighborIDS = neighboringNodes.stream().map(Element::getId)
        .collect(Collectors.toSet());
    List<Node> removeNodes = fullGraph.nodes().filter(n -> !neighborIDS.contains(n.getId()))
        .toList();
    for (Node n : removeNodes) {
      fullGraph.removeNode(n);
    }
  }

  public MultiGraph getFullGraph() {
    return fullGraph;
  }
}
