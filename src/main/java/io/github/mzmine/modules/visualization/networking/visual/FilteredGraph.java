package io.github.mzmine.modules.visualization.networking.visual;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

public class FilteredGraph extends MultiGraph {

  private MultiGraph fullGraph;

  public FilteredGraph(String id) {
    super(id);
    this.fullGraph=new MultiGraph(id);
  }

  public void setFullGraph(MultiGraph Graph) {
    Graph.nodes().forEach(aNode -> {
      Node n = this.fullGraph.addNode(aNode.getId());
      aNode.attributeKeys().forEach(attribute -> n.setAttribute(attribute, aNode.getAttribute(attribute)));
    });
    Graph.edges().forEach(anEdge -> {
      Edge e;
      e = this.fullGraph.addEdge(anEdge.getId(), anEdge.getSourceNode().getId(),
          anEdge.getTargetNode().getId(), anEdge.isDirected());
      anEdge.attributeKeys().forEach(attribute -> e.setAttribute(attribute, anEdge.getAttribute(attribute)));
    });
  }

  public MultiGraph getFullGraph() {
    return fullGraph;
  }
}
