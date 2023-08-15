/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.GraphStreamUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FilterableGraph extends MultiGraph {

  private final MultiGraph fullGraph;
  private final List<Consumer<FilterableGraph>> graphChangeListener = new ArrayList<>();
  private boolean fullGraphLayoutApplied = false;
  private boolean fullGraphLayoutFinished = false;

  public FilterableGraph(String id, final MultiGraph fullGraph, boolean showFullNetwork) {
    super(id);
    this.fullGraph = fullGraph;
    setAutoCreate(true);
    setStrict(false);
    if (showFullNetwork) {
      showFullNetwork();
    }
  }

  private static void copyAttributes(final Element source, final Element target) {
    source.attributeKeys().forEach(att -> {
      target.setAttribute(att, source.getAttribute(att));
    });
  }

  private void applyLayout(final @Nullable Node frozen, final MultiGraph gl,
      boolean externalThread) {
    NetworkLayoutComputeTask task = new NetworkLayoutComputeTask(frozen, gl);
    if (externalThread) {
      MZmineCore.getTaskController().addTask(task, TaskPriority.HIGH);
      task.addTaskStatusListener((task1, newStatus, oldStatus) -> {
        if (newStatus == TaskStatus.FINISHED) {
          fullGraphLayoutFinished = true;
          showNetwork(gl);
        }
      });
    } else {
      task.run();
    }
  }

  public void addGraphChangeListener(Consumer<FilterableGraph> listener) {
    graphChangeListener.add(listener);
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

  public void setNodeFilter(Collection<Node> neighboringNodes, @Nullable Node frozen) {
    MultiGraph gl = copyNetworkApplyLayout(neighboringNodes, frozen);

    // copy network over
    showNetwork(gl);
  }

  /**
   * show full network without filters
   */
  public synchronized void showFullNetwork() {
    if (!fullGraphLayoutApplied) {
      fullGraphLayoutApplied = true;
      applyLayout(null, fullGraph, true);
    } else if (fullGraphLayoutFinished) {
      showNetwork(fullGraph);
    }
  }

  public void showNetwork(final MultiGraph source) {
    // make sure its javafx
    MZmineCore.runLater(() -> {
      clear();

      source.nodes().forEach(n -> {
        addCopy(this, n);
      });
      source.edges().forEach(e -> {
        addCopy(this, e);
      });
      graphChangeListener.forEach(listener -> listener.accept(this));
    });
  }

  @NotNull
  private MultiGraph copyNetworkApplyLayout(final Collection<Node> neighboringNodes,
      final @Nullable Node frozen) {
    MultiGraph gl = new MultiGraph("layout_graph");

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

    applyLayout(frozen, gl, false);

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

  public void setNodeNeighborFilter(final List<Node> central, final int distance) {
    // make sure the correct nodes are selected from full graph
    List<Node> correctNodes = central.stream().map(Element::getId).map(fullGraph::getNode).toList();
    setNodeFilter(GraphStreamUtils.getNodeNeighbors(fullGraph, correctNodes, distance),
        correctNodes.get(0));
  }
}
