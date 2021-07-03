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

package io.github.mzmine.modules.dataprocessing.filter_interestingfeaturefinder;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.PossibleIsomerType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class AnnotateIsomersTask extends AbstractTask {

  private final MZmineProject project;
  private final ParameterSet parameters;
  private final ModularFeatureList flist;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final boolean requireSingleRaw = true;
  private String description;
  private int processed = 0;
  private int totalRows = 1;

  public AnnotateIsomersTask(MemoryMapStorage storage, @NotNull MZmineProject project,
      @NotNull ParameterSet parameters, ModularFeatureList flist) {
    super(storage);

    this.project = project;
    this.parameters = parameters;
    this.flist = flist;

    totalRows = flist.getNumberOfRows();
    description = "Searching for isomeric features in " + flist.getName() + ".";

    mzTolerance = parameters
        .getParameter(parameters.getParameter(AnnotateIsomersParameters.mzTolerance)).getValue();
    rtTolerance = parameters.getParameter(AnnotateIsomersParameters.rtTolerance).getValue();
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return processed / totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final List<ModularFeatureListRow> rowsByMz = flist.modularStream()
        .sorted(Comparator.comparingDouble(ModularFeatureListRow::getAverageMZ)).toList();

    // sort by decreasing intensity
//    final List<ModularFeatureListRow> rowsByIntensity = flist.modularStream().sorted(
//        (row1, row2) -> -1 * Double
//            .compare(row1.getMaxDataPointIntensity(), row2.getMaxDataPointIntensity())).toList();

    for (final ModularFeatureListRow row : rowsByMz) {
      var possibleRows = FeatureListUtils
          .getRows(rowsByMz, rtTolerance.getToleranceRange(row.getAverageRT()),
              mzTolerance.getToleranceRange(row.getAverageMZ()), true);

      if (possibleRows.isEmpty()) {
        continue;
      }

      row.set(PossibleIsomerType.class,
          possibleRows.stream().map(ModularFeatureListRow::getID).toList());
    }

    flist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(AnnotateIsomersModule.class, parameters));
    setStatus(TaskStatus.FINISHED);
  }
}
