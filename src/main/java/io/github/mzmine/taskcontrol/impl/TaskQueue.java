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

package io.github.mzmine.taskcontrol.impl;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.Arrays;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * This class stores all tasks (as WrappedTasks) in the queue of task controller and also provides
 * data for TaskProgressWindow (as TableModel).
 */
public class TaskQueue {

  /**
   * This observable list stores the actual tasks
   */
  private final ObservableList<WrappedTask> queue =
      FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public int getNumOfWaitingTasks() {
    final WrappedTask snapshot[] = getQueueSnapshot();
    final long numOfWaitingTasks = Arrays.asList(snapshot).stream()
        .filter(task -> ((task.getActualTask().getStatus() == TaskStatus.PROCESSING)
            || (task.getActualTask().getStatus() == TaskStatus.WAITING)))
        .count();
    return (int) numOfWaitingTasks;
  }

  public int getTotalPercentComplete() {
    double totalFinished = 0.0;

    final WrappedTask snapshot[] = getQueueSnapshot();

    for (WrappedTask task : snapshot) {
      totalFinished += task.getActualTask().getFinishedPercentage();
    }
    final int totalPercentFinished = (int) Math.floor(totalFinished / snapshot.length * 100);
    return totalPercentFinished;
  }

  void addWrappedTask(WrappedTask task) {
    logger.finest("Adding task \"" + task + "\" to the task controller queue");
    if (task.getActualTask() instanceof AbstractTask) {
      ((AbstractTask) task.getActualTask()).addTaskStatusListener((t, oldStatus, newStatus) -> {
        if (t.getStatus() == TaskStatus.FINISHED) {
          MZmineCore.runLater(() -> queue.remove(task));
        }
      });
    }

    MZmineCore.runLater(() -> queue.add(task));
  }

  void clear() {
    MZmineCore.runLater(() -> queue.clear());
  }

  boolean isEmpty() {
    return queue.isEmpty();
  }

  boolean allTasksFinished() {
    final int numOfWaitingTasks = getNumOfWaitingTasks();
    return numOfWaitingTasks == 0;
  }

  public WrappedTask[] getQueueSnapshot() {
    return queue.toArray(new WrappedTask[0]);
  }

  public ObservableList<WrappedTask> getTasks() {
    return queue;
  }

}
