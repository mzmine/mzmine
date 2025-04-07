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

package io.github.mzmine.taskcontrol;

import org.jetbrains.annotations.Nullable;

/**
 *
 */
public interface Task extends Runnable {

  String getTaskDescription();

  double getFinishedPercentage();

  TaskStatus getStatus();

  void setStatus(TaskStatus newStatus);

  /**
   * Convenience method for determining if this task has been canceled. Also returns true if the
   * task encountered an error.
   *
   * @return true if this task has been canceled or stopped due to an error
   */
  default boolean isCanceled() {
    TaskStatus status = getStatus();
    return (status == TaskStatus.CANCELED) || (status == TaskStatus.ERROR);
  }

  /**
   * Convenience method for determining if this task has been completed
   *
   * @return true if this task is finished
   */
  default boolean isFinished() {
    TaskStatus status = getStatus();
    return status == TaskStatus.FINISHED;
  }

  /**
   * Set status to error and sets error message
   *
   * @param message error message
   */
  default void error(String message) {
    this.error(message, null);
  }

  void error(@Nullable String message, @Nullable Exception exceptionToLog);

  String getErrorMessage();

  /**
   * The standard TaskPriority assign to this task
   *
   * @return
   */
  TaskPriority getTaskPriority();

  /**
   * Cancel a running task by user request. Status FINISHED and ERROR cannot be overwritten by
   * cancel.
   */
  void cancel();

  void addTaskStatusListener(TaskStatusListener list);

  boolean removeTaskStatusListener(TaskStatusListener list);

  void clearTaskStatusListener();

  /**
   * Executes a runnable on the task thread if this task's status changes to
   * {@link TaskStatus#FINISHED}. The runnable is not executed if this task is canceled or fails.
   */
  default void setOnFinished(Runnable runnable) {
    addTaskStatusListener((task, newStatus, oldStatus) -> {
      if (newStatus == TaskStatus.FINISHED) {
        runnable.run();
      }
    });
  }
}
