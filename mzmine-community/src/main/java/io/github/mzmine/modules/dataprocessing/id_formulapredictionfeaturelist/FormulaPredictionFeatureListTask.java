/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
package io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist;

import com.google.common.collect.Lists;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.taskcontrol.utils.TaskResultSummary;
import io.github.mzmine.taskcontrol.utils.TaskResultSummary.ErrorMessageHandling;
import io.github.mzmine.taskcontrol.utils.TaskUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class FormulaPredictionFeatureListTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      FormulaPredictionFeatureListTask.class.getName());

  private final ModularFeatureList flist;
  private final ParameterSet parameters;
  private String message;
  private int totalRows;
  private WrappedTask[] wrappedTasks;

  /**
   * @param parameters
   * @param featureList
   */
  FormulaPredictionFeatureListTask(ModularFeatureList featureList, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.flist = featureList;
    message = "Formula Prediction for feature list " + featureList;
    this.parameters = parameters;
  }

  @Override
  public double getFinishedPercentage() {
    if (wrappedTasks == null) {
      return 0.0;
    }
    return Arrays.stream(wrappedTasks).filter(Objects::nonNull)
        .mapToDouble(WrappedTask::getFinishedPercentage).average().orElse(0d);
  }

  @Override
  public String getTaskDescription() {
    return message;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    totalRows = flist.getNumberOfRows();

    flist.addRowType(DataTypes.get(
        io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType.class));

    final int numSubtasks = Math.max(TaskService.getController().getNumberOfThreads() - 2, 1);

    // we could also use Lists.partition and split the rows that way instead of using a single queue
    // that is accessed by all threads, but then we need to properly distribute high-mass features
    // across the different lists. Just keeping all threads active until all rows are processed is easier
    final var rows = new ConcurrentLinkedQueue<>(flist.getRowsCopy());

    final List<FormulaPredictionSubTask> subTasks = new ArrayList<>();
    for (int i = 0; i < numSubtasks; i++) {
      final FormulaPredictionSubTask task = new FormulaPredictionSubTask(parameters, moduleCallDate,
          rows);
      subTasks.add(task);
    }
    wrappedTasks = TaskService.getController().addTasks(subTasks.toArray(Task[]::new));
    TaskUtils.waitForTasksToFinish(this, wrappedTasks);
    final TaskResultSummary worstResult = TaskUtils.findWorstResult(
        ErrorMessageHandling.COMBINE_UNIQUE, wrappedTasks);
    worstResult.applyToTask(this);

    if (isCanceled()) {
      return;
    }

    flist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(FormulaPredictionFeatureListModule.class, parameters,
            getModuleCallDate()));

    logger.finest("Finished formula search for all the features");

    setStatus(TaskStatus.FINISHED);
  }

}
