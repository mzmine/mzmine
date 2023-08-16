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
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.GraphStreamUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
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

  private void applyLayout(final @Nullable Node frozen, final MultiGraph gl,
      boolean externalThread) {
    if (externalThread) {
      Task task = new NetworkLayoutParallelComputeTask(gl);
      MZmineCore.getTaskController().addTask(task, TaskPriority.HIGH);
      task.addTaskStatusListener((task1, newStatus, oldStatus) -> {
        if (newStatus == TaskStatus.FINISHED) {
          fullGraphLayoutFinished = true;
          showNetwork(gl);
        }
      });
    } else {
      Task task = new NetworkLayoutParallelComputeTask(gl);
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
    MultiGraph gl = GraphStreamUtils.createFilteredCopy(neighboringNodes);
    applyLayout(frozen, gl, false);

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
      this.clear();
      GraphStreamUtils.copyGraphContent(source, this);
      graphChangeListener.forEach(listener -> listener.accept(this));
    });
  }

  public void setNodeNeighborFilter(final List<Node> central, final int distance) {
    // make sure the correct nodes are selected from full graph
    List<Node> correctNodes = central.stream().map(Element::getId).map(fullGraph::getNode).toList();
    setNodeFilter(GraphStreamUtils.getNodeNeighbors(fullGraph, correctNodes, distance),
        correctNodes.get(0));
  }
}
