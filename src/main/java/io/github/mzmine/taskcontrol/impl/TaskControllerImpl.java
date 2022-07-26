/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.taskcontrol.impl;

import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumOfThreadsParameter;
import io.github.mzmine.main.GoogleAnalyticsTracker;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskControlListener;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Task controller implementation
 */
public class TaskControllerImpl implements TaskController, Runnable {

  private static final Logger logger = Logger.getLogger(TaskControllerImpl.class.getName());
  /**
   * Update the task progress window every 300 ms
   */
  private final int TASKCONTROLLER_THREAD_SLEEP = 300;
  ArrayList<TaskControlListener> listeners = new ArrayList<>();
  private Thread taskControllerThread;

  private TaskQueue taskQueue;

  /**
   * This vector contains references to all running threads of NORMAL priority. Maximum number of
   * concurrent threads is specified in the preferences dialog.
   */
  private Vector<WorkerThread> runningThreads;

  /**
   * Initialize the task controller
   */
  public void initModule() {

    logger.finest("Starting task controller thread");
    taskQueue = new TaskQueue();

    runningThreads = new Vector<>();

    // Create a low-priority thread that will manage the queue and start
    // worker threads for tasks
    taskControllerThread = new Thread(this, "Task controller thread");
    taskControllerThread.setPriority(Thread.MIN_PRIORITY);
    taskControllerThread.start();

  }

  @Override
  public TaskQueue getTaskQueue() {
    return taskQueue;
  }

  @Override
  public void addTask(Task task) {
    addTask(task, task.getTaskPriority());
  }

  /**
   * Override the standard task priority of all tasks with a specific
   */
  @Override
  public void addTask(Task task, TaskPriority priority) {
    addTasks(new Task[]{task}, new TaskPriority[]{priority});
  }

  @Override
  public WrappedTask[] addTasks(Task[] tasks) {
    if (tasks == null || tasks.length == 0) {
      return new WrappedTask[0];
    }

    TaskPriority[] prio = Arrays.stream(tasks).map(Task::getTaskPriority)
        .toArray(TaskPriority[]::new);
    return addTasks(tasks, prio);
  }

  @Override
  public WrappedTask[] addTasks(Task[] tasks, TaskPriority[] priorities) {
    // It can sometimes happen during a batch that no tasks are actually
    // executed --> tasks[] array may be empty
    if ((tasks == null) || (tasks.length == 0)) {
      return new WrappedTask[0];
    }

    WrappedTask[] wrappedTasks = new WrappedTask[tasks.length];
    for (int i = 0; i < tasks.length; i++) {
      Task task = tasks[i];
      TaskPriority priority = priorities[i];
      WrappedTask newQueueEntry = new WrappedTask(task, priority);
      taskQueue.addWrappedTask(newQueueEntry);
      wrappedTasks[i] = newQueueEntry;
      // logger.finest("Added wrapped task for " +
      // task.getTaskDescription());
    }

    // Wake up the task controller thread
    synchronized (this) {
      this.notifyAll();
    }
    return wrappedTasks;
  }

  /**
   * Task controller thread main method.
   */
  @Override
  public void run() {

    int previousQueueSize = -1;
    int previousPercentDone = -1;

    while (true) {

      final int waitingTasks = taskQueue.getNumOfWaitingTasks();
      final int percentDone = taskQueue.getTotalPercentComplete();
      if ((waitingTasks != previousQueueSize) || (percentDone != previousPercentDone)) {
        previousQueueSize = waitingTasks;
        previousPercentDone = percentDone;
        for (TaskControlListener listener : listeners) {
          listener.numberOfWaitingTasksChanged(waitingTasks, percentDone);
        }
      }

      // If the queue is empty, we can sleep. When new task is added into
      // the queue, we will be awaken by notify()
      synchronized (this) {
        while (taskQueue.isEmpty()) {
          try {
            this.wait(100);
          } catch (InterruptedException e) {
            // Ignore
          }
        }
      }

      // Check if all tasks in the queue are finished
      if (taskQueue.allTasksFinished()) {
        taskQueue.clear();
        continue;
      }

      // Remove already finished threads from runningThreads
      Iterator<WorkerThread> threadIterator = runningThreads.iterator();
      while (threadIterator.hasNext()) {
        WorkerThread thread = threadIterator.next();
        if (thread.isFinished()) {
          threadIterator.remove();
        }
      }

      // Get a snapshot of the queue
      WrappedTask[] queueSnapshot = taskQueue.getQueueSnapshot();

      // Obtain the settings of max concurrent threads
      NumOfThreadsParameter parameter = MZmineCore.getConfiguration().getPreferences()
          .getParameter(MZminePreferences.numOfThreads);
      int maxRunningThreads;
      if (parameter.isAutomatic() || (parameter.getValue() == null)) {
        maxRunningThreads = Runtime.getRuntime().availableProcessors();
      } else {
        maxRunningThreads = parameter.getValue();
      }

      // Check all tasks in the queue
      for (WrappedTask task : queueSnapshot) {

        // Skip assigned and canceled tasks
        if (task.isAssigned() || (task.getActualTask().getStatus() == TaskStatus.CANCELED)) {
          continue;
        }

        // Create a new thread if the task is high-priority or if we
        // have less then maximum # of threads running
        if ((task.getPriority() == TaskPriority.HIGH) || (runningThreads.size()
            < maxRunningThreads)) {
          WorkerThread newThread = new WorkerThread(task);

          // track task use
          GoogleAnalyticsTracker.trackTaskRun(task.getActualTask());

          if (task.getPriority() == TaskPriority.NORMAL) {
            runningThreads.add(newThread);
          }

          newThread.start();
        }
      }

      // Refresh the tasks window
      Desktop desktop = MZmineCore.getDesktop();
      if ((desktop != null) && (!(desktop instanceof HeadLessDesktop))) {
        desktop.getTasksView().refresh();
      }

      // Sleep for a while until next update
      try {
        Thread.sleep(TASKCONTROLLER_THREAD_SLEEP);
      } catch (InterruptedException e) {
        // Ignore
      }

    }

  }

  @Override
  public void setTaskPriority(Task task, TaskPriority priority) {

    // Get a snapshot of current task queue
    WrappedTask[] currentQueue = taskQueue.getQueueSnapshot();

    // Find the requested task
    for (WrappedTask wrappedTask : currentQueue) {

      if (wrappedTask.getActualTask() == task) {
        logger.finest(
            "Setting priority of task \"" + task.getTaskDescription() + "\" to " + priority);
        wrappedTask.setPriority(priority);
      }
    }

    // Refresh the tasks window
    Desktop desktop = MZmineCore.getDesktop();
    if ((desktop != null) && (!(desktop instanceof HeadLessDesktop))) {
      desktop.getTasksView().refresh();
    }

  }

  @Override
  public void addTaskControlListener(TaskControlListener listener) {
    listeners.add(listener);
  }

  public boolean isTaskInstanceRunningOrQueued(Class<? extends AbstractTask> clazz) {
    final WrappedTask[] snapshot = taskQueue.getQueueSnapshot();
    for (WrappedTask wrappedTask : snapshot) {
      if (clazz.isInstance(wrappedTask.getActualTask())) {
        return true;
      }
    }

    var running = runningThreads.toArray(WorkerThread[]::new);
    for (WorkerThread runningThread : running) {
      if (clazz.isInstance(runningThread.getWrappedTask().getActualTask())) {
        return true;
      }
    }

    return false;
  }

}
