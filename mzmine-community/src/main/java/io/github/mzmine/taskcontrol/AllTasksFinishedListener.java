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

package io.github.mzmine.taskcontrol;

import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

/**
 * Listens for end of all tasks in the list
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class AllTasksFinishedListener implements TaskStatusListener {

  private final List<? extends Task> tasks;
  private final Consumer<List<? extends Task>> operation;
  private final Consumer<List<? extends Task>> operationOnError;
  private final Consumer<List<? extends Task>> operationOnCancel;
  private boolean stopOnError = false;
  // mark when done
  private boolean done = false;

  private double progress = 0;

  public AllTasksFinishedListener(List<? extends Task> tasks,
      Consumer<List<? extends Task>> operation) {
    this(tasks, false, operation);
  }

  public AllTasksFinishedListener(List<? extends Task> tasks, boolean stopOnError,
      Consumer<List<? extends Task>> operation) {
    this(tasks, stopOnError, operation, null);
  }

  /**
   * @param tasks
   * @param stopOnError
   * @param operation        gets fired on completion of all tasks
   * @param operationOnError gets fired on error (only once)
   */
  public AllTasksFinishedListener(List<? extends Task> tasks, boolean stopOnError,
      Consumer<List<? extends Task>> operation, Consumer<List<? extends Task>> operationOnError) {
    this(tasks, stopOnError, operation, operationOnError, null);
  }

  public AllTasksFinishedListener(List<? extends Task> tasks, boolean stopOnError,
      Consumer<List<? extends Task>> operation, Consumer<List<? extends Task>> operationOnError,
      Consumer<List<? extends Task>> operationOnCancel) {
    this.tasks = tasks;
    this.stopOnError = stopOnError;
    this.operationOnCancel = operationOnCancel;
    this.operation = operation;
    this.operationOnError = operationOnError;
    tasks.forEach(t -> t.addTaskStatusListener(this));
  }

  public static void registerCallbacks(List<? extends Task> tasks, boolean stopOnError,
      Consumer<List<? extends Task>> operation, Consumer<List<? extends Task>> operationOnError) {
    new AllTasksFinishedListener(tasks, stopOnError, operation, operationOnError);
  }

  public static void registerCallbacks(List<? extends Task> tasks, boolean stopOnError,
      Consumer<List<? extends Task>> operation, Consumer<List<? extends Task>> operationOnError,
      Consumer<List<? extends Task>> operationOnCancel) {
    new AllTasksFinishedListener(tasks, stopOnError, operation, operationOnError,
        operationOnCancel);
  }

  /**
   * Simple callbacks for on finish, on error, on cancel
   *
   * @param tasks       the list of tasks
   * @param stopOnError stop all tasks if one fails
   * @param onFinish    called once all tasks are finished
   * @param onError     called on errors
   * @param onCancel    called on cancelled tasks
   */
  public static void registerCallbacks(final List<Task> tasks, final boolean stopOnError,
      @Nullable final Runnable onFinish, @Nullable final Runnable onError,
      @Nullable final Runnable onCancel) {
    new AllTasksFinishedListener(tasks, stopOnError, success -> {
      if (onFinish != null) {
        onFinish.run();
      }
    }, error -> {
      if (onError != null) {
        onError.run();
      }
    }, cancel -> {
      if (onCancel != null) {
        onCancel.run();
      }
    });
  }

  @Override
  public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
    if (done) {
      return;
    }
    // if one is cancelled cancel all
    if (tasks.stream().map(Task::getStatus).anyMatch(s -> s.equals(TaskStatus.CANCELED))) {
      tasks.forEach(Task::cancel);
      if (operationOnCancel != null) {
        operationOnCancel.accept(tasks);
      }
      done = true;
      return;
    }

    // stop on error
    if (stopOnError && tasks.stream().map(Task::getStatus)
        .anyMatch(s -> s.equals(TaskStatus.ERROR))) {
      if (operationOnError != null) {
        operationOnError.accept(tasks);
      }
      done = true;
      return;
    }
    // is one still running?
    long stillRunning = tasks.stream().map(Task::getStatus)
        .filter(s -> (s.equals(TaskStatus.WAITING) || s.equals(TaskStatus.PROCESSING))).count();
    progress = (tasks.size() - stillRunning) / (double) tasks.size();
    if (stillRunning == 0) {
      // all done
      operation.accept(tasks);
      done = true;
    }
  }

  public double getProgress() {
    return progress;
  }
}
