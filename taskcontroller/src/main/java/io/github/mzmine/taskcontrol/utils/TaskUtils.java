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

package io.github.mzmine.taskcontrol.utils;

import io.github.mzmine.taskcontrol.SimpleCalculationTask;
import io.github.mzmine.taskcontrol.SimpleRunnableTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.taskcontrol.listeners.MasterTaskCancelListener;
import io.github.mzmine.taskcontrol.operations.TaskSubProcessor;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class TaskUtils {

  private static final Logger logger = Logger.getLogger(TaskUtils.class.getName());

  /**
   * Wraps a runnable into a task.
   *
   * @param runnable task, processor or runnable
   * @return a {@link Task} is directly returned. A {@link TaskSubProcessor} is wrapped into
   * {@link SimpleCalculationTask}. A {@link Runnable} is wrapped into {@link SimpleRunnableTask}
   */
  public static Task wrapTask(final @NotNull Runnable runnable) {
    return switch (runnable) {
      case Task task -> task;
      case TaskSubProcessor task -> new SimpleCalculationTask<>(task);
      case Runnable _ -> new SimpleRunnableTask(runnable);
    };
  }

  /**
   * Wait for all subtasks to finish. if a subtask or the master task cancels or errors out - cancel
   * or error on all sub-tasks and the main task.
   *
   * @param masterTask   task that controls the cancel and error status of the subtasks
   * @param wrappedTasks waiting to finish, listens for cancel or error on each subtask
   * @return if master is cancelled or errors out - the masterTask.getStatus. Otherwise FINISHED if
   * no error.
   */
  @NotNull
  public static TaskStatus waitForTasksToFinish(@NotNull final Task masterTask,
      final WrappedTask[] wrappedTasks) {
    // make sure to cancel all subtasks if needed
    TaskStatusListener masterListener = new MasterTaskCancelListener(wrappedTasks);
    masterTask.addTaskStatusListener(masterListener);

    for (final WrappedTask task : wrappedTasks) {
      task.addTaskStatusListener(
          (_, newStatus, _) -> handleSubTaskStatusChanged(masterTask, newStatus));
    }

    for (final WrappedTask task : wrappedTasks) {
      // wait for all to finish
      try {
        task.getFuture().get();
      } catch (InterruptedException | ExecutionException e) {
        if (task.getStatus() != TaskStatus.CANCELED && masterTask.getStatus() != TaskStatus.CANCELED) {
          masterTask.error("Subtask had an error or was interrupted");
        }
        return TaskStatus.ERROR;
      }
    }

    masterTask.removeTaskStatusListener(masterListener);
    if (masterTask.isFinished()) {
      return masterTask.getStatus();
    }
    return TaskStatus.FINISHED;
  }

  private static void handleSubTaskStatusChanged(final Task masterTask,
      final TaskStatus newStatus) {
    switch (newStatus) {
      case CANCELED -> masterTask.cancel();
      case ERROR -> {
        masterTask.error(STR."""
            Subtask had an error. Cancelling all sub tasks now.
                        \{masterTask.toString()}""");
      }
      case FINISHED, PROCESSING, WAITING -> {
        // nothing to do here
      }
    }
  }

}
