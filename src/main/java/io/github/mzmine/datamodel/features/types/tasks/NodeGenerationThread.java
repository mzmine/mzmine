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

package io.github.mzmine.datamodel.features.types.tasks;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeGenerationThread extends AbstractTask {

  private static final Logger logger = Logger.getLogger(NodeGenerationThread.class.getName());

  private FeatureList flist;

  private Map<Node, Node> nodeChartMap = new ConcurrentHashMap<>();

  private final Queue<NodeRequest<?>> nodeQueue = new ConcurrentLinkedQueue<>();

  private double progress = 0;

  public NodeGenerationThread(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      FeatureList flist) {
    super(storage, moduleCallDate);
    this.flist = flist;
  }

  @Override
  public String getTaskDescription() {
    return "Creating charts for row %d rows".formatted(nodeQueue.size());
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // generateAllCharts();

    while (!isCanceled()) {

      final NodeRequest<?> request = nodeQueue.poll();
      if (request == null || !(request.type() instanceof GraphicalColumType graphicalType)) {
        try {
          TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        continue;
      }

      if (request.raw() == null) {
        var row = request.row();
        DataType type = request.type();
        try {

          final Node node = graphicalType.createCellContent(row, request.value(), request.raw(),
              new AtomicDouble());
          final Pane parentNode = request.parentNode();
          nodeChartMap.put(parentNode, node);
        } catch (Exception e) {
          // sometimes some exceptions occur during the drawing, catch them here.
          logger.log(Level.FINE, e.getMessage(), e);
        }
      }

      progress = nodeChartMap.size() / (double) (nodeChartMap.size() + nodeQueue.size());

      if ((nodeQueue.isEmpty() && !nodeChartMap.isEmpty()) || nodeChartMap.size() > 10) {
        MZmineCore.runLater(() -> {
          logger.info("Updating %d charts.".formatted(nodeChartMap.size()));
          final ArrayList<Node> updated = new ArrayList<>(nodeChartMap.size());
          nodeChartMap.forEach((pane, chart) -> {
            if (chart == null) {
              return;
            }

            try {
              ((Pane) pane).getChildren().clear();
              ((Pane) pane).getChildren().add(chart);
              updated.add(pane);
            } catch (ClassCastException e) {
              logger.log(Level.INFO, e.getMessage(), e);
            }
          });
          updated.forEach(nodeChartMap::remove);
        });
      }
    }

    setStatus(TaskStatus.FINISHED);
  }

/*  private boolean generateAllCharts() {
    final List<LinkedGraphicalType> rowTypes = flist.getRowTypes().values().stream()
        .filter(type -> type instanceof LinkedGraphicalType).map(t -> (LinkedGraphicalType) t)
        .toList();
    final List<LinkedGraphicalType> featureTypes = flist.getFeatureTypes().values().stream()
        .filter(type -> type instanceof LinkedGraphicalType).map(t -> (LinkedGraphicalType) t)
        .toList();

    for (FeatureListRow row : flist.getRows()) {
      for (LinkedGraphicalType rowType : rowTypes) {
        final String header = rowType.getHeaderString();
        Node base = ((ModularFeatureListRow) row).getBufferedColChart(header);
        if (base == null) {
          base = new StackPane(new Label("Preparing content..."));
          ((ModularFeatureListRow) row).addBufferedColChart(header, base);
        }
        try {
          var chart = rowType.createCellContent((ModularFeatureListRow) row, row.get(rowType), null,
              new AtomicDouble());
          nodeChartMap.put(base, chart);
        } catch (Exception ex) {
          logger.log(Level.WARNING, "Cannot create chart " + ex.getMessage(), ex);
          nodeChartMap.put(base, new Label("Failed to create chart"));
        }
      }

      for (LinkedGraphicalType featureType : featureTypes) {
        for (ModularFeature feature : row.getFeatures()) {
          if (feature != null && feature.getFeatureStatus() != FeatureStatus.UNKNOWN) {
            final String header = featureType.getHeaderString();
            Node base = ((ModularFeature) feature).getBufferedColChart(header);
            if (base == null) {
              base = new StackPane(new Label("Preparing content..."));
              ((ModularFeature) feature).addBufferedColChart(header, base);
            }
            try {
              var chart = featureType.createCellContent((ModularFeatureListRow) row,
                  row.get(featureType), feature.getRawDataFile(), new AtomicDouble());
              nodeChartMap.put(base, chart);
            } catch (Exception ex) {
              logger.log(Level.WARNING, "Cannot create chart " + ex.getMessage(), ex);
              nodeChartMap.put(base, new Label("Failed to create chart"));
            }
          }
        }
      }
      if (isCanceled()) {
        return true;
      }
      rows++;
    }

    Platform.runLater(() -> {
      logger.info("Updating all charts.");
      nodeChartMap.forEach((node, chart) -> {
        if (chart == null) {
          return;
        }
        try {
          ((StackPane) node).getChildren().clear();
          ((StackPane) node).getChildren().add(chart);
        } catch (ClassCastException e) {
          logger.log(Level.INFO, e.getMessage(), e);
        }
      });
      logger.info("All charts updated.");
    });
    return false;
  }*/

  public <T> void requestNode(@NotNull ModularFeatureListRow row, DataType<T> type, T value,
      RawDataFile raw, Pane parentNode) {
    nodeQueue.add(new NodeRequest<>(row, type, value, raw, parentNode));
  }
}
