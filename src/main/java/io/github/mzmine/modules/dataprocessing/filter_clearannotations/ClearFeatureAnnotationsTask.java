/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.filter_clearannotations;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Filters out feature list rows.
 */
public class ClearFeatureAnnotationsTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      ClearFeatureAnnotationsTask.class.getName());
  private final FeatureList origFeatureList;
  private final ParameterSet parameters;
  private final List<DataType<?>> typesToClear;
  private int processedRows, totalRows;

  /**
   * Create the task.
   *
   * @param list         feature list to process.
   * @param parameterSet task parameters.
   */
  public ClearFeatureAnnotationsTask(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    // Initialize.
    parameters = parameterSet;
    origFeatureList = list;
    processedRows = 0;
    totalRows = 0;

    final Map<DataType<?>, Boolean> annotationTypes = parameterSet.getValue(
        ClearFeatureAnnotationsParameters.clear);

    typesToClear = (List) annotationTypes.entrySet().stream().filter(Entry::getValue)
        .map(Entry::getKey).filter(t -> list.getRowTypes().containsValue(t)).toList();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {

    return "Clearing annotation from feature list";
  }

  @Override
  public void run() {

    try {
      setStatus(TaskStatus.PROCESSING);
      logger.info("Filtering feature list rows");

      totalRows = origFeatureList.getRows().size() * typesToClear.size();

      for (DataType<?> dataType : typesToClear) {
        if (!origFeatureList.getRowTypes().containsKey(dataType.getClass())) {
          continue;
        }
        // Filter the feature list.
        for (FeatureListRow row : origFeatureList.getRows()) {
          if (isCanceled()) {
            return;
          }

          row.set(dataType, null);
          processedRows += 1;
        }
      }

      origFeatureList.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(ClearFeatureAnnotationsModule.class, parameters,
              getModuleCallDate()));

      setStatus(TaskStatus.FINISHED);
      logger.info("Finished feature list clear annotations.");

    } catch (Throwable t) {
      t.printStackTrace();
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE, "Feature list clear annotations error", t);
    }

  }

}
