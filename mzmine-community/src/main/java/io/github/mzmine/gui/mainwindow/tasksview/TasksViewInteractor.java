/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TasksChangedEvent;
import io.github.mzmine.taskcontrol.TasksChangedEvent.TasksAddedEvent;
import io.github.mzmine.taskcontrol.TasksChangedEvent.TasksRemovedEvent;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import java.util.List;
import java.util.Set;
import javafx.event.ActionEvent;

/**
 * The interactor updates the data model based on a binding to the TaskController submitted tasks
 * list via a binding that is created in {@link TasksViewController}. It also interacts with other
 * MZmineCore classes like the TaskController itself
 */
public class TasksViewInteractor extends FxInteractor<TasksViewModel> {

  TasksViewInteractor(final TasksViewModel model) {
    super(model);
  }

  private static boolean isDone(final WrappedTaskModel wt) {
    var task = wt.getTask().getActualTask();
    return task.isFinished() || task.isCanceled();
  }

  public void onSubmittedTasksChanged(TasksChangedEvent event) {
    FxThread.runLater(() -> {
      switch (event) {
        case TasksAddedEvent(List<WrappedTask> _, List<WrappedTask> added) -> {
          model.addTasks(added.stream().map(WrappedTaskModel::new).toList());
        }
        case TasksRemovedEvent(List<WrappedTask> _, Set<WrappedTask> removed) -> {
          model.getTasks().removeIf(task -> removed.contains(task.getTask()));
        }
      }
    });
  }

  @Override
  public void updateModel() {
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
    TaskService.getController().cancelAllTasks();
  }

  void cancelBatchTasks(ActionEvent actionEvent) {
    TaskService.getController().cancelAllTasks(BatchTask.class);
  }

  void showTasksView(ActionEvent actionEvent) {
    MZmineCore.getDesktop().handleShowTaskView();
  }

}
