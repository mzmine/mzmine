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

package io.github.mzmine.taskcontrol.impl;

import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.main.GoogleAnalyticsTracker;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskControlListener;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Task controller implementation
 */
public class TaskControllerImpl implements TaskController, Runnable {

  private static final Logger logger = Logger.getLogger(TaskControllerImpl.class.getName());
  /**
   * Update the task progress window every 1000 ms
   */
  private static final int TASKCONTROLLER_THREAD_SLEEP = 100;
  private static final int GUI_UPDATE_SLEEP = 950;

  private static final TaskControllerImpl INSTANCE = new TaskControllerImpl();
  private final ArrayList<TaskControlListener> listeners = new ArrayList<>();
  // the executor that runs tasks, may be recreated with different size of pools
  @NotNull
  protected final ThreadPoolExecutor executor;
  // add all tasks here
  private final ConcurrentLinkedDeque<WrappedTask> tasksToSubmit = new ConcurrentLinkedDeque<>();
  // Those tasks show in the GUI and are submitted to a thread pool
  private final TaskQueue submittedTaskQueue;
  // the scheduler tasks that update schedule and GUI
  private final ScheduledFuture<?> schedulerUpdateFuture;
  private final ScheduledFuture<?> guiUpdateFuture;
  protected int previousQueueSize = -1;
  protected int previousPercentDone = -1;
  private int numThreads;

  private TaskControllerImpl() {
    logger.finest("Starting task controller thread");
    submittedTaskQueue = new TaskQueue();

    // create the actual executor that runs the tasks
    numThreads = MZmineCore.getConfiguration().getNumOfThreads();
    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);

    // Create a low-priority thread that will manage the queue and start
    // worker threads for tasks
    try (ScheduledExecutorService managerExecutor = Executors.newSingleThreadScheduledExecutor(
        r -> {
          Thread t = new Thread(r, "MZmine Task controller thread");
          t.setDaemon(true);
          t.setPriority(Thread.MIN_PRIORITY);
          return t;
        })) {

      schedulerUpdateFuture = managerExecutor.scheduleAtFixedRate(this::scheduleTasks, 0,
          TASKCONTROLLER_THREAD_SLEEP, TimeUnit.MILLISECONDS);

      guiUpdateFuture = managerExecutor.scheduleAtFixedRate(this::updateGui, 0, GUI_UPDATE_SLEEP,
          TimeUnit.MILLISECONDS);
    }
  }

  private void setNumberOfThreads(final int numThreads) {
    this.numThreads = numThreads;
    boolean shrink = numThreads < executor.getCorePoolSize();
    if (shrink) {
      executor.setCorePoolSize(numThreads);
      executor.setMaximumPoolSize(numThreads);
    } else {
      executor.setMaximumPoolSize(numThreads);
      executor.setCorePoolSize(numThreads);
    }
  }

  private void updateGui() {
    logger.fine("Updating GUI from task manager");
    Desktop desktop = MZmineCore.getDesktop();
    if ((desktop != null) && (!(desktop instanceof HeadLessDesktop))) {
      desktop.getTasksView().refresh();
    }
  }

  private void scheduleTasks() {
    logger.fine("Scheduling more tasks");

    // Obtain the settings of max concurrent threads
    int maxRunningThreads = MZmineCore.getConfiguration().getNumOfThreads();
    if (maxRunningThreads != numThreads) {
      setNumberOfThreads(maxRunningThreads);
    }

    List<WrappedTask> tasks = new ArrayList<>();
    WrappedTask task;
    while ((task = tasksToSubmit.pollFirst()) != null) {
      tasks.add(task);
      // track task use
      GoogleAnalyticsTracker.trackTaskRun(task.getActualTask());

      // Create a new thread if the task is high-priority or if we
      // have less then maximum # of threads running
      var worker = new WorkerThread(task);

      if (task.getPriority() == TaskPriority.NORMAL) {
        executor.submit(worker);
      }
      if (task.getPriority() == TaskPriority.HIGH) {
        // maybe add a second executor for high priority to not spawn too many threads
        Thread thread = new Thread(worker);
        thread.setName("High priority Task " + worker.getTitle());
        thread.setDaemon(true);
        thread.start();
      }
    }

    // push all to the submitted tasks
    submittedTaskQueue.addAll(tasks);
  }

  public static TaskControllerImpl getInstance() {
    return INSTANCE;
  }

  @Override
  public TaskQueue getSubmittedTaskQueue() {
    return submittedTaskQueue;
  }

  @Override
  public ThreadPoolExecutor getExecutor() {
    return executor;
  }

  @Override
  public WorkerThread runTaskOnThisThread(Task task) {
    var worker = new WorkerThread(new WrappedTask(task, TaskPriority.HIGH));
    runningThreads.add(worker);
    submittedTaskQueue.addWrappedTask(worker.getWrappedTask()); // show in UI
    return worker;
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
      submittedTaskQueue.addWrappedTask(newQueueEntry);
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


  private void checkNumberOfWaitingTasksAndNotify() {
    final int waitingTasks = submittedTaskQueue.getNumOfWaitingTasks();
    final int percentDone = submittedTaskQueue.getTotalPercentComplete();
    if ((waitingTasks != previousQueueSize) || (percentDone != previousPercentDone)) {
      previousQueueSize = waitingTasks;
      previousPercentDone = percentDone;
      for (TaskControlListener listener : listeners) {
        listener.numberOfWaitingTasksChanged(waitingTasks, percentDone);
      }
    }
  }

  @Override
  public void setTaskPriority(Task task, TaskPriority priority) {

    // Get a snapshot of current task queue
    WrappedTask[] currentQueue = submittedTaskQueue.getQueueSnapshot();

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
    final WrappedTask[] snapshot = submittedTaskQueue.getQueueSnapshot();
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
