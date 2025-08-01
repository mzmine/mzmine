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
import io.github.mzmine.util.RangeUtils;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
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
    List<MeasuredGraph> graphs = clusters.stream().parallel()
        .map(cluster -> GraphStreamUtils.createFilteredCopy(cluster.nodes())).map(graph -> {
          if (isCanceled()) {
            return null;
          }
          NetworkLayoutComputeTask.applyLayout(null, graph);
          progress.addAndGet(progressStep);
          return measureSize(graph);
        }).filter(Objects::nonNull).toList();

    if (isCanceled()) {
      return;
    }

    // edge length is roughly 1
    final double space = 0.4;
    double maxRowWidth = 30;
    double startx = 0;
    double starty = 0;
    // current row
    int subnetsInRow = 0;
    double maxHeightThisRow = 0;

    // remove all nodes and add copies back in
    mainGraph.clear();

    double[] xyz = new double[3];
    // add one row of graphs
    for (int i = 0; i < graphs.size(); i++) {
      maxHeightThisRow = 0;
      subnetsInRow = 0;
      startx = 0;
      for (int j = i; j < graphs.size(); j++) {
        MeasuredGraph g = graphs.get(j);
        subnetsInRow++;
        maxHeightThisRow = Math.max(g.height(), maxHeightThisRow);
        startx += space + g.width();
        if (startx >= maxRowWidth) {
          if (subnetsInRow == 1) {
            // make maximum width larger to fit the largest network
            maxRowWidth = Math.max(g.width(), maxRowWidth);
          }
          break;
        }
      }

      // reset for this row
      startx = 0;
      starty -= maxHeightThisRow;
      // add rows
      logger.info("Adding new row at " + starty);
      for (; subnetsInRow > 0; i++, subnetsInRow--) {
        MeasuredGraph mg = graphs.get(i);
        MultiGraph g = mg.graph;

        // move network to position
        for (final Node node : g) {
          Toolkit.nodePosition(node, xyz);
          // move to zero and then to the current start
          xyz[0] = startx + xyz[0] - mg.getX();
          xyz[1] = starty + (xyz[1] - mg.getY());
          node.setAttribute("x", xyz[0]);
          node.setAttribute("y", xyz[1]);
        }

        startx += space + mg.width();

        // add graph as copy over
        GraphStreamUtils.copyGraphContent(g, mainGraph);
        progress.addAndGet(progressStep);
      }
      starty -= space;
    }

    //
    setStatus(TaskStatus.FINISHED);
  }

  public MeasuredGraph measureSize(MultiGraph graph) {
    double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
    double[] xyz = new double[3];
    for (final Node node : graph) {
      Toolkit.nodePosition(node, xyz);
      minX = Math.min(minX, xyz[0]);
      maxX = Math.max(maxX, xyz[0]);
      minY = Math.min(minY, xyz[1]);
      maxY = Math.max(maxY, xyz[1]);
    }
    return new MeasuredGraph(graph, Range.closed(minX, maxX), Range.closed(minY, maxY));
  }

  record MeasuredGraph(MultiGraph graph, Range<Double> xrange, Range<Double> yrange) {

    public double height() {
      return RangeUtils.rangeLength(yrange);
    }

    public double width() {
      return RangeUtils.rangeLength(xrange);
    }

    public double getX() {
      return xrange.lowerEndpoint();
    }

    public double getY() {
      return yrange.lowerEndpoint();
    }

    public double getYEnd() {
      return yrange.upperEndpoint();
    }
  }
}
