/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.gui.mainwindow.tasksview;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.taskcontrol.impl.TaskControllerImpl;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import java.util.HashSet;
import java.util.logging.Logger;
import javafx.collections.ListChangeListener.Change;
import javafx.event.ActionEvent;

public class TasksViewInteractor {

  private static final Logger logger = Logger.getLogger(TasksViewInteractor.class.getName());
  private final TasksViewModel model;

  public TasksViewInteractor(final TasksViewModel model) {
    this.model = model;
  }

  private static boolean isDone(final WrappedTaskModel wt) {
    var task = wt.getTask().getActualTask();
    return task.isFinished() || task.isCanceled();
  }

  void onSubmittedTasksChanged(final Change<? extends WrappedTask> change) {
    MZmineCore.runLater(() -> {
      while (change.next()) {
        if (change.wasRemoved()) {
          HashSet<? extends WrappedTask> removed = new HashSet<>(change.getRemoved());
          model.getTasks().removeIf(task -> removed.contains(task.getTask()));
        }
        if (change.wasAdded()) {
          var newTasks = change.getAddedSubList().stream().map(WrappedTaskModel::new).toList();
          model.addTasks(newTasks);
        }
      }
    });
  }

  void updateDataModel() {
//    logger.info("Updating tasks view");

    // remove finished tasks
    var tasks = model.getTasks();
    tasks.removeIf(TasksViewInteractor::isDone);

    // update progress and other stats for running tasks
    String batchDescription = null;
    double batchProgress = 0;
    double progress = 0;
    for (final WrappedTaskModel wt : model.getTasks()) {
      wt.updateProperties();

      if (wt.getTask().getActualTask() instanceof BatchTask batchTask) {
        batchDescription = batchTask.getTaskDescription();
        batchProgress = batchTask.getFinishedPercentage();
      } else {
        progress += wt.getProgress();
      }
    }

    var ntasks = model.getTasks().size();
    model.setAllTasksProgress(ntasks == 0 ? 0 : progress / ntasks);
    model.setBatchDescription(batchDescription);
    model.setBatchIsRunning(batchDescription != null);
    model.setBatchProgress(batchProgress);
  }

  void cancelAllTasks(ActionEvent actionEvent) {
    TaskControllerImpl.getInstance().cancelAllTasks();
  }

  void cancelBatchTasks(ActionEvent actionEvent) {
    TaskControllerImpl.getInstance().cancelBatchTasks();
  }

  void showTasksView(ActionEvent actionEvent) {
    MZmineCore.getDesktop().handleShowTaskView();
  }
}
