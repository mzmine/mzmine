/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.filter_mobilitymzregionextraction;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * @author https://github.com/SteffenHeu
 */
public class MobilityMzRegionExtractionTask extends AbstractTask {

  private final List<List<Point2D>> pointsLists;
  private final String suffix;
  private final ParameterSet parameterSet;
  private final ModularFeatureList originalFeatureList;
  private final MZmineProject project;
  private double progress;

  public MobilityMzRegionExtractionTask(ParameterSet parameterSet,
      ModularFeatureList originalFeatureList, MZmineProject project) {
    this.originalFeatureList = originalFeatureList;
    this.parameterSet = parameterSet;
    pointsLists = parameterSet.getParameter(MobilityMzRegionExtractionParameters.regions)
        .getValue();
    this.project = project;
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
        .createCopy(originalFeatureList.getName() + suffix);

    final double numberOfRows = (double) newFeatureList.getNumberOfRows();
    int processedFeatures = 0;

    for (FeatureListRow r : newFeatureList.getRows()) {
      ModularFeatureListRow row = (ModularFeatureListRow) r;
      List<RawDataFile> rawDataFiles = row.getRawDataFiles();
      for (RawDataFile file : rawDataFiles) {

        ModularFeature feature = (ModularFeature) row.getFeature(file);
        boolean contained = IonMobilityUtils.isFeatureWithinMzMobilityRegion(feature, regions);
        if (!contained) {
          // it's okay to remove the feature from the row, but not the row. otherwise we would get
          // concurrent modification exceptions
          row.removeFeature(file);
        }
      }
      processedFeatures++;
      progress = processedFeatures / numberOfRows;

      if (isCanceled()) {
        return;
      }
    }

    List<FeatureListRow> rowsToRemove = new ArrayList<>();
    for (FeatureListRow r : newFeatureList.getRows()) {
      if (r.getNumberOfFeatures() == 0) {
        rowsToRemove.add(r);
      }
    }
    rowsToRemove.forEach(newFeatureList::removeRow);

    newFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(MobilityMzRegionExtractionModule.NAME, parameterSet));

    project.addFeatureList(newFeatureList);

    setStatus(TaskStatus.FINISHED);
  }
}
