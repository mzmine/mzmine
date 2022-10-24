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
import java.util.List;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeGenerationThread extends AbstractTask {

  private final int numberOfRows;
  private int rows = 0;
  private FeatureList flist;

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
        new FeaturesGraphicalNodeTask(rowType.getNodeClass(), new StackPane(),
            (ModularFeatureListRow) row, rowType.getHeaderString()).run();
      }

      for (LinkedGraphicalType featureType : featureTypes) {
        for (ModularFeature feature : row.getFeatures()) {
          if (feature != null && feature.getFeatureStatus() != FeatureStatus.UNKNOWN) {
            new FeatureGraphicalNodeTask(featureType.getNodeClass(), new StackPane(), feature,
                featureType.getHeaderString()).run();
          }
        }
      }
      rows++;
    }
    setStatus(TaskStatus.FINISHED);
  }
}
