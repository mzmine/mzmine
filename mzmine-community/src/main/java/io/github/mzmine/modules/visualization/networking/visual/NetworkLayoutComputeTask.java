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

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Random;
import java.util.logging.Logger;
import org.graphstream.algorithm.Toolkit;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.GraphReplay;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetworkLayoutComputeTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(NetworkLayoutComputeTask.class.getName());
  private final Node frozen;
  private final MultiGraph gl;
  private final AtomicDouble progress = new AtomicDouble(0);

  /**
   * TODO add option to calculate layout for all sub networks in parallel by splitting them.
   * Then stitch together all subnetworks into a similar network view like cytoscape. Biggest first, then second, etc
   */
  public NetworkLayoutComputeTask(final Node frozen, final MultiGraph gl) {
    super(null, Instant.now());
    this.frozen = frozen;
    this.gl = gl;
  }

  /**
   * optimized from {@link Toolkit#computeLayout} as in Toolkit.computeLayout(g, layout, stab);
   */
  public static void applyLayout(final @Nullable Node frozen, final @NotNull MultiGraph g) {
    applyLayout(frozen, g, null, null);
  }

  /**
   * optimized from {@link Toolkit#computeLayout} as in Toolkit.computeLayout(g, layout, stab);
   */
  public static void applyLayout(final @Nullable Node frozen, final @NotNull MultiGraph g,
      @Nullable Task task, @Nullable AtomicDouble progress) {

    double nodeWeight = 0.1d;
    g.nodes().forEach(node -> node.setAttribute("layout.weight", nodeWeight));
    int size = g.getNodeCount();
//  quality opiton seems slow on large networks still!
//    int quality = size<200? 3 : size<500? 2 : 0;
//    g.setAttribute("layout.quality", quality);

    Layout layout = new SpringBox(false, new Random(42));
    layout.setForce(1);
    // less precision for large networks
    double stab = size < 200 ? 0.9 : 0.75;

    if (size > 20) {
      logger.fine("Layout of %d nodes, stabilization at %.2f".formatted(size, stab));
    }

    layout.setQuality(stab);

    GraphReplay r = new GraphReplay(g.getId());
    layout.addAttributeSink(g);
    r.addSink(layout);
    r.replay(g);
    r.removeSink(layout);

    layout.shake();
    layout.compute();

    do {
      if (task != null && task.isCanceled()) {
        break;
      }
      layout.compute();
      if (progress != null) {
        progress.set(layout.getStabilization());
      }
    } while (layout.getStabilization() < stab);

    layout.removeAttributeSink(g);

    // finished code copy
    final double[] origin =
        frozen == null ? new double[]{0, 0} : Toolkit.nodePosition(g.getNode(frozen.getId()));

    // set position to center origin
    g.nodes().forEach(n -> {
      double[] pos = Toolkit.nodePosition(n);
      n.setAttribute("xy", pos[0] - origin[0], pos[1] - origin[1]);
    });
  }

  @Override
  public String getTaskDescription() {
    return "Computing network layout";
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      applyLayout(frozen, gl, this, progress);
    } catch (Exception ex) {
      setErrorMessage("During network layout compute: ex.getMessage()");
      setStatus(TaskStatus.ERROR);
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }
}
