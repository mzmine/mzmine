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

package io.github.mzmine.taskcontrol.threadpools;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * ThreadPoolTask (virtual, fixed, provided) that can be used to wrap a list of tasks, e.g., when
 * running many tasks, a ThreadPoolTask will show up as an additional task with the combined
 * progress of how many sub tasks are finished. The main task will wait for sub tasks to complete.
 * <p>
 * General cases use {@link #createDefaultTaskManagerPool(String, List)} and the thread pool of the
 * {@link TaskController}
 * <p>
 * Use {@link FixedThreadPoolTask} to limit the number of threads.
 * <p>
 * Use
 */
public sealed abstract class ThreadPoolTask extends AbstractTask permits FixedThreadPoolTask,
    ProvidedThreadPoolTask, VirtualThreadPoolTask {

  private static final Logger logger = Logger.getLogger(FixedThreadPoolTask.class.getName());
  private final AtomicInteger finishedTasks = new AtomicInteger(0);
  private final int totalTasks;
  private final String description;
  private final List<WrappedTask> tasks;
  private final TaskPriority priority;

  protected ThreadPoolTask(String description, List<? extends Task> tasks) {
    // time is not used
    super(null, Instant.now());
    this.description = description;
    this.tasks = tasks.stream().map(task -> new WrappedTask(task, task.getTaskPriority())).toList();
    priority = this.tasks.stream().map(WrappedTask::getTaskPriority).findFirst()
        .orElse(TaskPriority.NORMAL);
    totalTasks = tasks.size();

    addTaskStatusListener((_a, _b, _c) -> applyNewStatusToFutures());
  }

  @Override
  public TaskPriority getTaskPriority() {
    return priority;
  }

  /**
   * Implementation that uses the task controller internal thread pool to limit threads. This should
   * be generally used unless more constraints are needed to run a task. Then create a new
   * {@link FixedThreadPoolTask} or for blocking calls {@link VirtualThreadPoolTask}
   */
  public static ThreadPoolTask createDefaultTaskManagerPool(final String description,
      final List<? extends Task> tasks) {
    return new ProvidedThreadPoolTask(description, getTaskControllerThreadPool(),
        false, tasks);
  }

  @NotNull
  private static ThreadPoolExecutor getTaskControllerThreadPool() {
    return MZmineCore.getTaskController().getExecutor();
  }

  private void applyNewStatusToFutures() {
    // apply error and cancel
    if (!isCanceled()) {
      return;
    }
    for (final var task : tasks) {
      task.cancel();
    }
  }

  public abstract ExecutorService createThreadPool();

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    ExecutorService threadPool = null;
    try {
      // do not auto close as we are usually using the TaskController thread pool
      threadPool = createThreadPool();

      int numThreads = MZmineCore.getConfiguration().getNumOfThreads();

      if (threadPool instanceof ThreadPoolExecutor threadPoolExecutor) {
        // threads are usually defined by the threadPool used for normal tasks
        numThreads = threadPoolExecutor.getCorePoolSize();
      }

      // this executor is only used for high priority tasks, core thread pool size is zero
      try (ExecutorService highPrioExecutor = TaskController.createCachedHighPriorityThreadPool(
          numThreads)) {

        // execute
        for (WrappedTask task : tasks) {
          task.addTaskStatusListener(
              (thistask, newStatus, oldStatus) -> handleSubTaskStatusChanged(thistask, newStatus));

          Runnable runnable = trackProgressTask(task);

          var executor =
              task.getTaskPriority() == TaskPriority.HIGH ? highPrioExecutor : threadPool;
          Future<?> future = executor.submit(runnable);
          task.setFuture(future);

          MZmineCore.getTaskController().addSubmittedTasksToView(task);

          // only shutdown if this was not a provided executor
          if (this instanceof ProvidedThreadPoolTask provided && provided.autoShutdownExecutor()) {
            threadPool.shutdown();
          }
        }

        // wait for finish
        for (WrappedTask task : tasks) {
          if (isCanceled()) {
            break;
          }
          Future<?> future = task.getFuture();
          assert future != null;
          // wait for execution end
          var result = future.get();
        }
      }
    } catch (InterruptedException e) {
      cancel();
      logger.log(Level.SEVERE, STR."Task \{description} did not finish and timedout.");
    } catch (ExecutionException e) {
      setStatus(TaskStatus.ERROR);
      throw new RuntimeException(e);
    } catch (CancellationException e) {
      cancel();
    } finally {
      // only shutdown if this was not a provided executor
      if (this instanceof ProvidedThreadPoolTask provided && provided.autoShutdownExecutor()
          && threadPool != null) {
        threadPool.shutdown();
      }
    }

    if (!isCanceled()) {
      setStatus(TaskStatus.FINISHED);
    }
  }


  private void handleSubTaskStatusChanged(final Task thistask, final TaskStatus newStatus) {
    switch (newStatus) {
      case CANCELED -> cancel();
      case ERROR -> {
        logger.info(STR."""
            Subtask had an error. Cancelling all sub tasks now.
            \{thistask.toString()}""");
        cancel();
        setStatus(TaskStatus.ERROR);
      }
      case FINISHED, PROCESSING, WAITING -> {
        // nothing to do here
      }
    }
  }

  /**
   * Track progress when tasks are finished
   */
  private @NotNull Runnable trackProgressTask(final Task task) {
    return () -> {
      task.run();
      finishedTasks.getAndIncrement();
    };
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return totalTasks == 0 ? 1d : (double) finishedTasks.get() / totalTasks;
  }

}
