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

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.GraphStreamUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import static java.util.Objects.requireNonNullElse;
import java.util.function.Consumer;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.jetbrains.annotations.Nullable;

public class FilterableGraph extends MultiGraph {

  private final MultiGraph fullGraph;
  private final IntegerProperty distanceProperty = new SimpleIntegerProperty(1);
  private final List<Consumer<FilterableGraph>> graphChangeListener = new ArrayList<>();
  private final ObservableList<Node> centered = FXCollections.observableArrayList();
  private EdgeTypeFilter edgeFilter;
  private @Nullable MultiGraph edgeFilteredGraph;
  private boolean fullGraphLayoutApplied = false;
  private boolean fullGraphLayoutFinished = false;
  private boolean edgeGraphLayoutApplied = false;
  private boolean edgeGraphLayoutFinished = false;

  public FilterableGraph(String id, final MultiGraph fullGraph, boolean showFullNetwork) {
    super(id);
    this.fullGraph = fullGraph;
    setAutoCreate(true);
    setStrict(false);
    if (showFullNetwork) {
      showFullNetwork();
    }
    centered.addListener((ListChangeListener<? super Node>) c -> filterCenterNeighbors());
  }

  private void filterEdges(boolean update) {
    edgeGraphLayoutApplied = false;
    edgeGraphLayoutFinished = false;
    if (edgeFilter == null) {
      edgeFilteredGraph = null;
      return;
    }
    List<Edge> edges = fullGraph.edges().filter(e -> edgeFilter.accept(e)).toList();
    edgeFilteredGraph = GraphStreamUtils.createFilteredCopy(fullGraph, edges);

    // show graph
    if (update) {
      filterCenterNeighbors();
    }
  }

  private void filterCenterNeighbors() {
    MultiGraph mainGraph = getMainGraph();
    if (centered.isEmpty()) {
      showNetwork(mainGraph);
    } else {
      List<Node> correctNodes = centered.stream().map(Element::getId).map(mainGraph::getNode)
          .toList();
      setNodeFilter(
          GraphStreamUtils.getNodeNeighbors(mainGraph, correctNodes, distanceProperty.get()),
          correctNodes.get(0));
    }
  }

  private void applyLayout(final @Nullable Node frozen, final MultiGraph gl,
      boolean externalThread) {
    Task task = new NetworkLayoutParallelComputeTask(gl);
    if (externalThread) {
      task.addTaskStatusListener((task1, newStatus, oldStatus) -> {
        if (newStatus == TaskStatus.FINISHED) {
          if (Objects.equals(gl, edgeFilteredGraph)) {
            edgeGraphLayoutFinished = true;
          } else {
            fullGraphLayoutFinished = true;
          }
          showNetwork(gl);
        }
      });
      MZmineCore.getTaskController().addTask(task, TaskPriority.HIGH);
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

  public MultiGraph getMainGraph() {
    return requireNonNullElse(edgeFilteredGraph, fullGraph);
  }

  public MultiGraph getEdgeFilteredGraph() {
    return edgeFilteredGraph;
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
    if (!isMainGraphLayoutApplied()) {
      setMainGraphLayoutApplied(true);
      applyLayout(null, getMainGraph(), true);
    } else if (isMainGraphLayoutFinished()) {
      showNetwork(getMainGraph());
    }
  }

  public boolean isMainGraphLayoutApplied() {
    return edgeFilteredGraph == null ? fullGraphLayoutApplied : edgeGraphLayoutApplied;
  }

  private void setMainGraphLayoutApplied(final boolean state) {
    if (edgeFilteredGraph == null) {
      fullGraphLayoutApplied = state;
    } else {
      edgeGraphLayoutApplied = state;
    }
  }

  public boolean isMainGraphLayoutFinished() {
    return edgeFilteredGraph == null ? fullGraphLayoutFinished : edgeGraphLayoutFinished;
  }

  private void setMainGraphLayoutFinished(final boolean state) {
    if (edgeFilteredGraph == null) {
      fullGraphLayoutFinished = state;
    } else {
      edgeGraphLayoutFinished = state;
    }
  }

  public void showNetwork(final MultiGraph source) {
    // make sure its javafx
    FxThread.runLater(() -> {
      this.clear();
      GraphStreamUtils.copyGraphContent(source, this);
      graphChangeListener.forEach(listener -> listener.accept(this));
    });
  }

  public void setNodeNeighborFilter(final List<Node> central, final int distance) {
    // make sure the correct nodes are selected from full graph
    distanceProperty.set(distance);
    centered.setAll(central);
  }

  public void setEdgeTypeFilter(final EdgeTypeFilter filter, final boolean update) {
    edgeFilter = filter;
    filterEdges(update);
  }
}
