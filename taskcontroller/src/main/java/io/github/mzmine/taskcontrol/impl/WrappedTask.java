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

package io.github.mzmine.taskcontrol.impl;

import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper class for Tasks that stores additional information
 */
public class WrappedTask implements Task {

  private static final Logger logger = Logger.getLogger(WrappedTask.class.getName());
  private Task task;
  private final Property<TaskPriority> priority;
  private boolean running = false;
  private @Nullable Future<?> future;

  public WrappedTask(Task task, TaskPriority priority) {
    this.task = task;
    this.priority = new SimpleObjectProperty<>(priority);
    final WrappedTask wrapped = this;
    task.addTaskStatusListener((_, newStatus, _) -> {
      if (newStatus == TaskStatus.CANCELED) {
        wrapped.cancel();
      } else if (newStatus == TaskStatus.ERROR) {
        if (future != null) {
          future.cancel(true);
        }
      }
    });
  }

  @Nullable
  public Future<?> getFuture() {
    return future;
  }

  public void setFuture(final @Nullable Future<?> future) {
    this.future = future;
    if (future != null && isCanceled()) {
      future.cancel(true);
    }
  }

  @Override
  public String getTaskDescription() {
    return task.getTaskDescription();
  }

  @Override
  public double getFinishedPercentage() {
    return task.getFinishedPercentage();
  }

  @Override
  public TaskStatus getStatus() {
    return task.getStatus();
  }

  @Override
  public void error(@Nullable final String message, @Nullable final Exception exceptionToLog) {
    task.error(message, exceptionToLog);
  }

  @Override
  public String getErrorMessage() {
    return task.getErrorMessage();
  }

  /**
   * @return Returns the priority.
   */
  @Override
  public TaskPriority getTaskPriority() {
    return priority.getValue();
  }

  @Override
  public void cancel() {
    task.cancel();
    if (future != null) {
      future.cancel(true);
    }
  }

  @Override
  public void addTaskStatusListener(final TaskStatusListener list) {
    task.addTaskStatusListener(list);
  }

  @Override
  public boolean removeTaskStatusListener(final TaskStatusListener list) {
    return task.removeTaskStatusListener(list);
  }

  @Override
  public void clearTaskStatusListener() {
    task.clearTaskStatusListener();
  }

  /**
   * @param priority The priority to set.
   */
  public void setPriority(TaskPriority priority) {
    this.priority.setValue(priority);
  }

  public Property<TaskPriority> priorityProperty() {
    return priority;
  }

  /**
   * @return Returns the task.
   */
  public synchronized Task getActualTask() {
    return task;
  }

  public synchronized String toString() {
    return task.getTaskDescription();
  }

  synchronized void removeTaskReference() {
    task = new FinishedTask(task);
  }

  public void run() {
    try {
      running = true;
      Task actualTask = getActualTask();
      try {

        // Log the start (INFO level events go to the Status bar, too)
        logger.info("Starting processing of task " + actualTask.getTaskDescription());

        // Process the actual task
        actualTask.run();

        // Check if task finished with an error
        if (actualTask.getStatus() == TaskStatus.ERROR) {

          String errorMsg = actualTask.getErrorMessage();
          if (errorMsg == null) {
            errorMsg = "Unspecified error in " + actualTask.getClass();
          }

          // Log the error
          logger.severe("Error of task " + actualTask.getTaskDescription() + ": " + errorMsg);

//        DesktopService.getDesktop().displayErrorMessage(errorMsg);
        } else {
          // Log the finish
          logger.info("Processing of task " + actualTask.getTaskDescription() + " done, status "
                      + actualTask.getStatus());
        }

      } catch (Throwable e) {
        /*
         * This should never happen, it means the task did not handle its exception properly, or there
         * was some severe error, like OutOfMemoryError
         */

        logger.log(Level.SEVERE, "Unhandled exception " + e + " while processing task "
                                 + actualTask.getTaskDescription());

        if (e instanceof Exception exception) {
          actualTask.error(e.getMessage(), exception);
        } else {
          actualTask.error(e.getMessage());
        }

//      DesktopService.getDesktop().displayErrorMessage(
//          "Unhandled exception in task " + actualTask.getTaskDescription() + ": "
//          + ExceptionUtils.exceptionToString(e));

      }

      /*
       * This is important to allow the garbage collector to remove the task, while keeping the task
       * description in the "Tasks in progress" window
       */
      removeTaskReference();

      /*
       * Mark this thread as finished
       */
    } finally {
      running = false;
    }
  }

  /**
   * if run method is complete its finished the state of the task may be finished, canceled or error
   * though
   */
  public boolean isWorkFinished() {
    return (future != null && future.isDone()) ||
           // task is not running but also not waiting anymore
           (!running && getActualTask().getStatus() != TaskStatus.WAITING);
  }

  @Override
  public void setStatus(TaskStatus newStatus) {
    task.setStatus(newStatus);
  }
}
