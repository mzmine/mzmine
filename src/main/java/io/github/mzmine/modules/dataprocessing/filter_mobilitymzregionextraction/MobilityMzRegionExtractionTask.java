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

package io.github.mzmine.modules.dataprocessing.filter_mobilitymzregionextraction;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.visualization.ims_mobilitymzplot.PlotType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 */
public class MobilityMzRegionExtractionTask extends AbstractTask {

  private final List<List<Point2D>> pointsLists;
  private final String suffix;
  private final ParameterSet parameterSet;
  private final ModularFeatureList originalFeatureList;
  private final MZmineProject project;
  private final PlotType ccsOrMobility;
  private double progress;

  public MobilityMzRegionExtractionTask(ParameterSet parameterSet,
      ModularFeatureList originalFeatureList, MZmineProject project,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.originalFeatureList = originalFeatureList;
    this.parameterSet = parameterSet;
    pointsLists = parameterSet.getParameter(MobilityMzRegionExtractionParameters.regions)
        .getValue();
    this.project = project;
    ccsOrMobility = parameterSet.getParameter(MobilityMzRegionExtractionParameters.ccsOrMobility)
        .getValue();
    suffix = parameterSet.getParameter(MobilityMzRegionExtractionParameters.suffix).getValue();
  }

  public static Path2D getShape(List<Point2D> points) {
    if (points.isEmpty()) {
      return new Path2D.Double();
    }
    Path2D path = new Path2D.Double();
    path.moveTo(points.get(0).getX(), points.get(0).getY());

    for (int i = 1; i < points.size(); i++) {
      path.lineTo(points.get(i).getX(), points.get(i).getY());
    }
    path.closePath();
    return path;
  }

  @Override
  public String getTaskDescription() {
    return "Extraction of mm/z mobility regions.";
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    List<Path2D> regions = new ArrayList<>();
    pointsLists.forEach(list -> regions.add(getShape(list)));

    ModularFeatureList newFeatureList = originalFeatureList
        .createCopy(originalFeatureList.getName() + " " + suffix, getMemoryMapStorage(), false);

    final double numberOfRows = newFeatureList.getNumberOfRows();
    int processedFeatures = 0;

    final List<FeatureListRow> rowsToRemove = new ArrayList<>();
    for (FeatureListRow r : newFeatureList.getRows()) {
      ModularFeatureListRow row = (ModularFeatureListRow) r;
      boolean contained = (ccsOrMobility == PlotType.MOBILITY) ? IonMobilityUtils
          .isRowWithinMzMobilityRegion(row, regions)
          : IonMobilityUtils.isRowWithinMzCCSRegion(row, regions);
      if (!contained) {
        rowsToRemove.add(row);
      }
      processedFeatures++;
      progress = processedFeatures / numberOfRows;

      if (isCanceled()) {
        return;
      }
    }

    for (FeatureListRow r : newFeatureList.getRows()) {
      if (r.getNumberOfFeatures() == 0) {
        rowsToRemove.add(r);
      }
    }
    rowsToRemove.forEach(newFeatureList::removeRow);

    newFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(MobilityMzRegionExtractionModule.class, parameterSet, getModuleCallDate()));

    project.addFeatureList(newFeatureList);

    setStatus(TaskStatus.FINISHED);
  }
}
