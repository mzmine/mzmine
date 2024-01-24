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

package io.github.mzmine.taskcontrol;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class InternalThreadPoolTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ThreadPoolTask.class.getName());
  private final AtomicInteger finishedTasks = new AtomicInteger(0);
  private final int totalTasks;
  private final String description;
  private final List<Task> tasks;

  protected InternalThreadPoolTask(String description, List<Task> tasks) {
    // time is not used
    super(null, Instant.now());
    this.description = description;
    this.tasks = tasks;
    totalTasks = tasks.size();
  }

  public abstract ExecutorService createThreadPool();

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final AbstractTask parentTask = this;

    try (ExecutorService threadPool = createThreadPool()) {
      for (final Task task : tasks) {
        if (isCanceled()) {
          return;
        }
        threadPool.submit(() -> {
          task.run();
          finishedTasks.getAndIncrement();
        });
      }
      // remove tasks so that they can be collected by GC
      tasks.clear();
      //
      threadPool.shutdown();
      if (!threadPool.awaitTermination(1, TimeUnit.DAYS)) {
        logger.log(Level.SEVERE, STR."Task \{description} did not finish and timedout.");
      }
    } catch (InterruptedException e) {
      logger.log(Level.SEVERE, STR."Task \{description} did not finish and timedout.");
    }

    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return totalTasks == 0 ? 1d : (double) finishedTasks.get() / totalTasks;
  }

  public static class VirtualThreadPoolTask extends InternalThreadPoolTask {


    public VirtualThreadPoolTask(final String description, List<Task> tasks) {
      super(description, tasks);
    }

    @Override
    public ExecutorService createThreadPool() {
      return Executors.newVirtualThreadPerTaskExecutor();
    }
  }

  public static class ThreadPoolTask extends InternalThreadPoolTask {

    private final int threads;

    public ThreadPoolTask(final String description, int threads, List<Task> tasks) {
      super(description, tasks);
      this.threads = threads;
    }

    @Override
    public ExecutorService createThreadPool() {
      return Executors.newFixedThreadPool(threads);
    }
  }
}
