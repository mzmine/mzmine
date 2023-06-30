/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.networking.visual;

import io.github.mzmine.util.GraphStreamUtils;
import java.util.List;
import java.util.Set;
import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FilterableGraph extends MultiGraph {

  private final MultiGraph fullGraph;

  public FilterableGraph(String id, final MultiGraph fullGraph, boolean showFullNetwork) {
    super(id);
    this.fullGraph = fullGraph;
    setAutoCreate(true);
    setStrict(false);
    if (showFullNetwork) {
      showFullNetwork();
    }
  }

  @Override
  public void setAttribute(final String attribute, final Object... values) {
    super.setAttribute(attribute, values);
    fullGraph.setAttribute(attribute, values);
  }

  @Override
  public void setStrict(final boolean on) {
    super.setStrict(on);
    fullGraph.setStrict(on);
  }

  @Override
  public void setAutoCreate(final boolean on) {
    super.setAutoCreate(on);
    fullGraph.setAutoCreate(on);
  }

  public MultiGraph getFullGraph() {
    return fullGraph;
  }

  @Override
  public void clear() {
    var ccs = getAttribute("ui.stylesheet");
    super.clear();
    setAttribute("ui.stylesheet", ccs);
  }

  public void setNodeFilter(Set<Node> neighboringNodes, @Nullable Node frozen) {
    clear();

    MultiGraph gl = copyNetworkApplyLayout(neighboringNodes, frozen);

    // copy network over
    copyNetwork(gl, this);
  }

  /**
   * show full network without filters
   */
  public void showFullNetwork() {
    copyNetwork(fullGraph, this);
  }

  private void copyNetwork(final MultiGraph source, final FilterableGraph target) {
    source.nodes().forEach(n -> {
      addCopy(target, n);
    });
    source.edges().forEach(e -> {
      addCopy(target, e);
    });
  }

  @NotNull
  private MultiGraph copyNetworkApplyLayout(final Set<Node> neighboringNodes,
      final @Nullable Node frozen) {
    MultiGraph gl = new MultiGraph("layout_graph");
    Layout layout = new SpringBox(false);
    layout.setForce(1);
    layout.setQuality(0.75);

    for (Node n : neighboringNodes) {
      addCopy(gl, n);
    }
    for (Node n : neighboringNodes) {
      n.enteringEdges().forEach(edge -> {
        // need to contain both nodes
        if (neighboringNodes.contains(edge.getSourceNode()) && neighboringNodes.contains(
            edge.getTargetNode())) {
          addCopy(gl, edge);
        }
      });
    }

    Toolkit.computeLayout(gl, layout, 0.75);

    final double[] origin =
        frozen == null ? new double[]{0, 0} : Toolkit.nodePosition(gl.getNode(frozen.getId()));

    // set position to center origin
    gl.nodes().forEach(n -> {
      double[] pos = Toolkit.nodePosition(n);
      n.setAttribute("xy", pos[0] - origin[0], pos[1] - origin[1]);
    });
    return gl;
  }

  private Element addCopy(final MultiGraph g, final Element element) {
    if (element instanceof Node n) {
      var node = g.addNode(n.getId());
      copyAttributes(n, node);
      return node;
    }
    if (element instanceof Edge e) {
      if (g.getEdge(e.getId()) != null) {
        return null;
      }
      var source = g.getNode(e.getSourceNode().getId());
      var target = g.getNode(e.getTargetNode().getId());
      if (source == null || target == null) {
        return null;
      }
      var edge = g.addEdge(e.getId(), source, target);
      copyAttributes(e, edge);
      return edge;
    }
    return null;
  }

  private static void copyAttributes(final Element source, final Element target) {
    source.attributeKeys().forEach(att -> {
      target.setAttribute(att, source.getAttribute(att));
    });
  }


  public void setNodeNeighborFilter(final List<Node> central, final int distance) {
    // make sure the correct nodes are selected from full graph
    List<Node> correctNodes = central.stream().map(Element::getId).map(fullGraph::getNode).toList();
    setNodeFilter(GraphStreamUtils.getNodeNeighbors(correctNodes, distance), correctNodes.get(0));
  }
}
