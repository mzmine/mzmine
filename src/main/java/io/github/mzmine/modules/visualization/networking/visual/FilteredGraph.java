package io.github.mzmine.modules.visualization.networking.visual;

import org.graphstream.graph.implementations.MultiGraph;


public class FilteredGraph extends MultiGraph {

  private MultiGraph filteredGraph;

  public FilteredGraph(String id) {
    super(id);
    this.filteredGraph= new MultiGraph("Filtered-Graph");
  }

  public MultiGraph getFilteredGraph()
  {
    return filteredGraph;
  }
}
