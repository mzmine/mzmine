/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.taskcontrol.impl;

import java.util.logging.Logger;

import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * This class stores all tasks (as WrappedTasks) in the queue of task controller and also provides
 * data for TaskProgressWindow (as TableModel).
 */
public class TaskQueue {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * This observable list stores the actual tasks
   */
  private final ObservableList<WrappedTask> queue =
      FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

  public synchronized int getNumOfWaitingTasks() {
    final long numOfWaitingTasks =
        queue.stream().filter(task -> ((task.getActualTask().getStatus() == TaskStatus.PROCESSING)
            || (task.getActualTask().getStatus() == TaskStatus.WAITING))).count();
    return (int) numOfWaitingTasks;
  }

  public synchronized int getTotalPercentComplete() {
    double totalFinished = 0.0;

    WrappedTask snapshot[] = getQueueSnapshot();

    for (WrappedTask task : snapshot) {
      totalFinished += task.getActualTask().getFinishedPercentage();
    }
    final int totalPercentFinished =
        (int) Math.floor(totalFinished / (double) snapshot.length * 100);
    return totalPercentFinished;
  }

  synchronized void addWrappedTask(WrappedTask task) {
    logger.finest("Adding task \"" + task + "\" to the task controller queue");
    queue.add(task);

  }

  synchronized void clear() {
    queue.clear();
  }

  synchronized boolean isEmpty() {
    return queue.isEmpty();
  }

  synchronized boolean allTasksFinished() {
    final int numOfWaitingTasks = getNumOfWaitingTasks();
    return numOfWaitingTasks == 0;
  }

  public synchronized WrappedTask[] getQueueSnapshot() {
    return queue.toArray(new WrappedTask[0]);
  }

  public ObservableList<WrappedTask> getTasks() {
    return queue;
  }

}
