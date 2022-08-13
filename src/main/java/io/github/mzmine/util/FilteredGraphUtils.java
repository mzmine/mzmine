package io.github.mzmine.util;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class FilteredGraphUtils {

  /**
   * Copies the edges and nodes of the original_graph into the filtered_graph
   *
   * @param originalGraph the original Graph
   * @param filteredGraph the filtered Graph
   */

  public void addContentsOfFullGraph(Graph originalGraph, Graph filteredGraph) {
    originalGraph.nodes().forEach(aNode -> {
      Node n = filteredGraph.addNode(aNode.getId());
      aNode.attributeKeys().forEach(attribute -> {
        n.setAttribute(attribute, aNode.getAttribute(attribute));
      });
    });
    originalGraph.edges().forEach(anEdge -> {
      Edge e;
      e = filteredGraph.addEdge(anEdge.getId(), anEdge.getSourceNode().getId(),
          anEdge.getTargetNode().getId(), anEdge.isDirected());
      anEdge.attributeKeys().forEach(attribute -> {
        e.setAttribute(attribute, anEdge.getAttribute(attribute));
      });
    });
  }

}
