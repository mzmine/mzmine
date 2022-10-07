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
