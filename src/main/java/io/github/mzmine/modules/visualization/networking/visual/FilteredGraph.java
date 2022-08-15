package io.github.mzmine.modules.visualization.networking.visual;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

public class FilteredGraph extends MultiGraph {

  private final MultiGraph fullGraph;

  public FilteredGraph(String id) {
    super(id);
    this.fullGraph = new MultiGraph("Full-Graph");
  }

  public void setInitialGraph(MultiGraph graph1,MultiGraph graph2) {
    graph1.nodes().forEach(aNode -> {
      Node n = graph2.addNode(aNode.getId());
      aNode.attributeKeys().forEach(attribute -> n.setAttribute(attribute, aNode.getAttribute(attribute)));
    });
    graph1.edges().forEach(anEdge -> {
      Edge e;
      e = graph2.addEdge(anEdge.getId(), anEdge.getSourceNode().getId(),
          anEdge.getTargetNode().getId(), anEdge.isDirected());
      anEdge.attributeKeys().forEach(attribute -> e.setAttribute(attribute, anEdge.getAttribute(attribute)));
    });
  }
  public MultiGraph getFullGraph() {
    return fullGraph;
  }
}
