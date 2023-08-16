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

import com.google.common.collect.Range;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.NetworkCluster;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.GraphStreamUtils;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

public class NetworkLayoutParallelComputeTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      NetworkLayoutParallelComputeTask.class.getName());
  private final MultiGraph mainGraph;
  private final AtomicDouble progress = new AtomicDouble(0);

  public NetworkLayoutParallelComputeTask(final MultiGraph mainGraph) {
    super(null, Instant.now());
    this.mainGraph = mainGraph;
  }

  @Override
  public String getTaskDescription() {
    return "Computing network layout in parallel";
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    List<NetworkCluster> clusters = GraphStreamUtils.detectClusters(mainGraph, false);

    double progressStep = 0.5 / clusters.size();
    // all graphs with layout applied
    List<MultiGraph> graphs = clusters.stream().parallel()
        .map(cluster -> GraphStreamUtils.createFilteredCopy(cluster.nodes())).peek(graph -> {
          if (isCanceled()) {
            return;
          }
          NetworkLayoutComputeTask.applyLayout(null, graph);
          progress.addAndGet(progressStep);
        }).toList();

    if (isCanceled()) {
      return;
    }

    // bounding boxes from all graphs
    Range<Double>[] xranges = new Range[graphs.size()];
    Range<Double>[] yranges = new Range[graphs.size()];
    double[] widths = new double[graphs.size()];
    double[] heights = new double[graphs.size()];
    double[] xyz = new double[3];
    for (int i = 0; i < graphs.size(); i++) {
      double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
      MultiGraph graph = graphs.get(i);
      for (final Node node : graph) {
        Toolkit.nodePosition(node, xyz);
        minX = Math.min(minX, xyz[0]);
        maxX = Math.max(maxX, xyz[0]);
        minY = Math.min(minY, xyz[1]);
        maxY = Math.max(maxY, xyz[1]);
      }
      xranges[i] = Range.closed(minX, maxX);
      yranges[i] = Range.closed(minY, maxY);
      widths[i] = maxX - minX;
      heights[i] = maxY - minY;
    }

    // edge length is roughly 1
    final double space = 1;
    double maxRowWidth = 60;
    double startx = 0;
    double starty = 0;
    // current row
    int subnetsInRow = 0;
    double maxHeightThisRow = 0;

    // remove all nodes and add copies back in
    mainGraph.clear();
    for (int i = 0; i < graphs.size(); i++) {
      MultiGraph graph = graphs.get(i);
      Range<Double> xrange = xranges[i];
      Range<Double> yrange = yranges[i];
      double width = widths[i];
      double height = heights[i];
      maxHeightThisRow = Math.max(height, maxHeightThisRow);

      for (final Node node : graph) {
        Toolkit.nodePosition(node, xyz);
        // move to zero and then to the current start
        xyz[0] = xyz[0] - xrange.lowerEndpoint() + startx;
        xyz[1] = maxHeightThisRow + (xyz[1] - yrange.lowerEndpoint()) + starty;
        node.setAttribute("x", xyz[0]);
        node.setAttribute("y", xyz[1]);
      }

      // add graph as copy over
      GraphStreamUtils.copyGraphContent(graph, mainGraph);

      progress.addAndGet(progressStep);

      // finished graph
      subnetsInRow++;
      startx += width + space;
      if (startx >= maxRowWidth) {
        if (subnetsInRow == 1) {
          // make maximum width larger to fit the largest network
          maxRowWidth = Math.max(width, maxRowWidth);
        }
        // start new row
        starty -= (maxHeightThisRow + space);
        startx = 0;
        maxHeightThisRow = 0;
        subnetsInRow = 0;
      }
    }

    //
    setStatus(TaskStatus.FINISHED);
  }
}
