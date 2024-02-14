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

import io.github.mzmine.main.GoogleAnalyticsTracker;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 * Task controller implementation
 */
public class TaskControllerImpl implements TaskController {

  private static final Logger logger = Logger.getLogger(TaskControllerImpl.class.getName());
  /**
   *
   */
  private static final int TASKCONTROLLER_THREAD_SLEEP = 350;

  private static TaskControllerImpl INSTANCE;
  // the executor that runs tasks, may be recreated with different size of pools
  /**
   * This is the main executor of this controller. Fixed size of numThreads threads.
   */
  protected final @NotNull ThreadPoolExecutor executor;

  /**
   * This executor has 0 - numThreads threads and caches them for a specified time if inactive.
   */
  private final @NotNull ThreadPoolExecutor highPriorityExecutor;

  // only modify on FX thread
  private final ObservableList<WrappedTask> tasks = FXCollections.observableArrayList();

  /**
   * the scheduler tasks that update schedule and GUI
   */
  private final ScheduledExecutorService updateExecutor;
  private final ScheduledFuture<?> schedulerUpdateFuture;

  // can be set from outside and resizes the ThreadPool
  private int numThreads;

  private TaskControllerImpl(final int numThreads) {
    logger.finest("Starting task controller thread");

    // create the actual executor that runs the tasks
    this.numThreads = numThreads;
    executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.numThreads);

    // this is a cached thread pool that only retains threads for a certain time, if inactive
    highPriorityExecutor = TaskController.createCachedHighPriorityThreadPool(this.numThreads);
    // Create a low-priority thread that will manage the queue and start
    // worker threads for tasks
    updateExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "MZmine Task controller thread");
      t.setDaemon(true);
      t.setPriority(Thread.MIN_PRIORITY);
      return t;
    });

    schedulerUpdateFuture = updateExecutor.scheduleAtFixedRate(this::cleanUpFinishedTasks, 100,
        TASKCONTROLLER_THREAD_SLEEP, TimeUnit.MILLISECONDS);
  }

  public static TaskControllerImpl init(int numThreads) {
    if (INSTANCE == null) {
      INSTANCE = new TaskControllerImpl(numThreads);
    }
    return INSTANCE;
  }

  public static TaskControllerImpl getInstance() {
    if (INSTANCE == null) {
      int numThreads = Runtime.getRuntime().availableProcessors();
      INSTANCE = new TaskControllerImpl(numThreads);
    }
    return INSTANCE;
  }

  private void cleanUpFinishedTasks() {
    tasks.removeIf(WrappedTask::isWorkFinished);
  }

  @Override
  public void cancelAllTasks() {
    var tasks = getTasksSnapshot();
    for (final WrappedTask task : tasks) {
      task.cancel();
    }
  }

  @Override
  public void cancelBatchTasks() {
    var tasks = getTasksSnapshot();
    for (final WrappedTask task : tasks) {
      if (task.getActualTask() instanceof BatchTask batch) {
        batch.cancel();
      }
    }
  }

  @Override
  public ObservableList<WrappedTask> getReadOnlyTasks() {
    return FXCollections.unmodifiableObservableList(tasks);
  }

  /**
   * Should be called at the end
   */
  @Override
  public void close() {
    cancelAllTasks();
    try {
      executor.shutdown();
    } catch (Exception e) {
      logger.warning("Error when shutting down executor");
    }
    try {
      updateExecutor.shutdown();
    } catch (Exception e) {
      logger.warning("Error when shutting down update executor");
    }
    try {
      schedulerUpdateFuture.cancel(true);
    } catch (Exception e) {
      logger.warning("Error when shutting down update executor");
    }

    try {
      executor.awaitTermination(5, TimeUnit.SECONDS); // Wait for threads to finish
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    try {
      updateExecutor.awaitTermination(5, TimeUnit.SECONDS); // Wait for threads to finish
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void setNumberOfThreads(final int numThreads) {
    if (numThreads == this.numThreads) {
      return;
    }
    this.numThreads = numThreads;
    boolean shrink = numThreads < executor.getCorePoolSize();
    if (shrink) {
      executor.setCorePoolSize(numThreads);
      executor.setMaximumPoolSize(numThreads);
    } else {
      executor.setMaximumPoolSize(numThreads);
      executor.setCorePoolSize(numThreads);
    }
    // core pool size for high priority is 0
    highPriorityExecutor.setMaximumPoolSize(numThreads);
  }

  @Override
  public @NotNull ThreadPoolExecutor getExecutor() {
    return executor;
  }

  @Override
  public @NotNull ThreadPoolExecutor getHighPriorityExecutor() {
    return highPriorityExecutor;
  }

  @Override
  public WrappedTask runTaskOnThisThreadBlocking(Task task) {
    WrappedTask worker = new WrappedTask(task, TaskPriority.NORMAL);
    addSubmittedTasksToView(worker);
    worker.run();
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
      Task internalTask = tasks[i];
      TaskPriority priority = priorities[i];
      WrappedTask task = new WrappedTask(internalTask, priority);
      wrappedTasks[i] = task;

      // track task use
      GoogleAnalyticsTracker.trackTaskRun(task.getActualTask());

      // Create a new thread if the task is high-priority or if we
      // have less then maximum # of threads running

      var usedExecutor =
          task.getTaskPriority() == TaskPriority.NORMAL ? executor : highPriorityExecutor;
      Future<?> future = usedExecutor.submit(task);
      task.setFuture(future);
    }

    addSubmittedTasksToView(wrappedTasks);

    return wrappedTasks;
  }

  @Override
  public void addSubmittedTasksToView(final WrappedTask... wrappedTasks) {
    MZmineCore.runLater(() -> tasks.addAll(wrappedTasks));
  }

  @Override
  public void setTaskPriority(Task task, TaskPriority priority) {
    // Get a snapshot of current task queue
    WrappedTask[] currentQueue = getTasksSnapshot();

    // Find the requested task
    for (WrappedTask wrappedTask : currentQueue) {

      if (wrappedTask.getActualTask() == task) {
        logger.finest(
            "Setting priority of task \"" + task.getTaskDescription() + "\" to " + priority);
        wrappedTask.setPriority(priority);
      }
    }
  }

  public WrappedTask[] getTasksSnapshot() {
    return tasks.toArray(WrappedTask[]::new);
  }

  public boolean isTaskInstanceRunningOrQueued(Class<? extends AbstractTask> clazz) {
    final WrappedTask[] snapshot = getTasksSnapshot();
    for (WrappedTask wrappedTask : snapshot) {
      if (clazz.isInstance(wrappedTask.getActualTask())) {
        return true;
      }
    }
    return false;
  }


}
