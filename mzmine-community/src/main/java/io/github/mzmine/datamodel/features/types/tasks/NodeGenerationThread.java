/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.types.tasks;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.Timer;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeGenerationThread extends AbstractTask {

  private static final Logger logger = Logger.getLogger(NodeGenerationThread.class.getName());
  private final Queue<NodeRequest<?>> nodeRequestQueue = new ConcurrentLinkedQueue<>();
  private final Queue<FinishedNodePair> finishedNodes = new ConcurrentLinkedQueue<>();
  private final AtomicLong requestedNodesCount = new AtomicLong(0);
  private FeatureList flist;
  private double progress = 0;

  public NodeGenerationThread(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      FeatureList flist) {
    super(storage, moduleCallDate);
    this.flist = flist;
  }

  @Override
  public String getTaskDescription() {
    return "Creating charts for row %d rows".formatted(requestedNodesCount.get());
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // if there was no chart request for this time, stop the thread
    final Timer waitTimer = new Timer(500, () -> setStatus(TaskStatus.FINISHED));

    try {
      while (getStatus() == TaskStatus.PROCESSING) {

        final NodeRequest<?> request = nodeRequestQueue.poll();
        if (request == null || !(request.type() instanceof GraphicalColumType graphicalType)) {
          try {
            TimeUnit.MILLISECONDS.sleep(10);
            waitTimer.tick();
          } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            setStatus(TaskStatus.CANCELED);
            return;
          }
          continue;
        }

        // There was a chart requested, so keep going
        waitTimer.restart();

        var row = request.row();
        DataType type = request.type();
        try {

          final Node node = graphicalType.createCellContent(row, request.value(), request.raw(),
              new AtomicDouble());
          final Pane parentNode = request.parentNode();
          finishedNodes.add(new FinishedNodePair(parentNode, node));
        } catch (Exception e) {
          // sometimes some exceptions occur during the drawing, catch them here.
          logger.log(Level.FINE, e.getMessage(), e);
        }

        final int numFinishedNodes = finishedNodes.size();
        progress = numFinishedNodes / (double) (numFinishedNodes + nodeRequestQueue.size());

        if ((nodeRequestQueue.isEmpty() && !finishedNodes.isEmpty()) || numFinishedNodes > 10) {
          FxThread.runLater(() -> {
            FinishedNodePair pair = null;
            while ((pair = finishedNodes.poll()) != null) {
              if (pair.child() == null) {
                continue;
              }

              try {
                pair.parent().getChildren().clear();
                pair.parent().getChildren().add(pair.child());
              } catch (Exception e) {
                logger.log(Level.INFO, e.getMessage(), e);
              }
            }
          });
        }
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, ex.getMessage(), ex);
    }

    setStatus(TaskStatus.FINISHED);
  }

  public <T> void requestNode(@NotNull ModularFeatureListRow row, DataType<T> type, T value,
      RawDataFile raw, Pane parentNode) {
    nodeRequestQueue.add(new NodeRequest<>(row, type, value, raw, parentNode));
    requestedNodesCount.incrementAndGet();
  }

  private record FinishedNodePair(Pane parent, Node child) {

  }
}
