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

/**
 * Listens for end of all tasks in the list
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class AllTasksFinishedListener implements TaskStatusListener {

  private List<AbstractTask> tasks;
  private Consumer<List<AbstractTask>> operation;
  private Consumer<List<AbstractTask>> operationOnError;
  private Consumer<List<AbstractTask>> operationOnCancel;
  private boolean stopOnError = false;
  // mark when done
  private boolean done = false;

  private double progress = 0;

  public AllTasksFinishedListener(List<AbstractTask> tasks,
      Consumer<List<AbstractTask>> operation) {
    this(tasks, false, operation);
  }

  public AllTasksFinishedListener(List<AbstractTask> tasks, boolean stopOnError,
      Consumer<List<AbstractTask>> operation) {
    this(tasks, stopOnError, operation, null);
  }

  /**
   * 
   * @param tasks
   * @param stopOnError
   * @param operation gets fired on completion of all tasks
   * @param operationOnError gets fired on error (only once)
   */
  public AllTasksFinishedListener(List<AbstractTask> tasks, boolean stopOnError,
      Consumer<List<AbstractTask>> operation, Consumer<List<AbstractTask>> operationOnError) {
    this(tasks, stopOnError, operation, operationOnError, null);
  }

  public AllTasksFinishedListener(List<AbstractTask> tasks, boolean stopOnError,
      Consumer<List<AbstractTask>> operation, Consumer<List<AbstractTask>> operationOnError,
      Consumer<List<AbstractTask>> operationOnCancel) {
    this.tasks = tasks;
    this.stopOnError = stopOnError;
    this.operationOnCancel = operationOnCancel;
    this.operation = operation;
    this.operationOnError = operationOnError;
    tasks.stream().forEach(t -> t.addTaskStatusListener(this));
  }

  @Override
  public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
    if (done)
      return;
    // if one is cancelled cancel all
    if (tasks.stream().map(Task::getStatus).anyMatch(s -> s.equals(TaskStatus.CANCELED))) {
      tasks.forEach(AbstractTask::cancel);
      if (operationOnCancel != null)
        operationOnCancel.accept(tasks);
      done = true;
      return;
    }

    // stop on error
    if (stopOnError
        && tasks.stream().map(Task::getStatus).anyMatch(s -> s.equals(TaskStatus.ERROR))) {
      if (operationOnError != null)
        operationOnError.accept(tasks);
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
      return;
    }
  }

  public double getProgress() {
    return progress;
  }
}
