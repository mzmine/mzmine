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
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeGenerationThread extends AbstractTask {

  private static final Logger logger = Logger.getLogger(NodeGenerationThread.class.getName());

  private final int numberOfRows;
  private int rows = 0;
  private FeatureList flist;

  private Map<Node, Node> nodeChartMap = new HashMap<>();

  public NodeGenerationThread(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      FeatureList flist) {
    super(storage, moduleCallDate);
    this.flist = flist;
    numberOfRows = flist.getNumberOfRows();
  }

  @Override
  public String getTaskDescription() {
    return "Creating charts for row " + rows + "/" + numberOfRows;
  }

  @Override
  public double getFinishedPercentage() {
    return numberOfRows != 0 ? rows / (double) numberOfRows : 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
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
        return;
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
    setStatus(TaskStatus.FINISHED);
  }
}
