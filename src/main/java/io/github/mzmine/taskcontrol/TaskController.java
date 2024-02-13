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

import io.github.mzmine.taskcontrol.impl.WrappedTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public interface TaskController {

  @NotNull
  static ThreadPoolExecutor createCachedHighPriorityThreadPool(int maxNumThreads) {
    ThreadFactory threadFac = r -> {
      Thread t = new Thread(r, "High priority sub task thread");
      t.setDaemon(true);
      t.setPriority(8); // elevated priority
      return t;
    };
    return new ThreadPoolExecutor(0, maxNumThreads, 120L, TimeUnit.SECONDS,
        new SynchronousQueue<>(), threadFac);
  }

  /**
   * The executor that schedules the tasks
   */
  @NotNull ThreadPoolExecutor getExecutor();

  /**
   * The executor that schedules high priority tasks on a separate cached number of threads. This
   * avoids long waits for threads on the {@link #getExecutor()} that runs all general tasks
   */
  @NotNull ThreadPoolExecutor getHighPriorityExecutor();

  /**
   * Add a task to the task list (will be in the TaskView) and run it on the calling thread.
   *
   * @param task the task to be added and run
   * @return the wrapped task after finishing the Task.run method
   */
  WrappedTask runTaskOnThisThreadBlocking(Task task);

  void addTask(Task task);

  WrappedTask[] addTasks(Task[] tasks);

  void addTask(Task task, TaskPriority priority);

  WrappedTask[] addTasks(Task[] tasks, TaskPriority[] priority);

  void addSubmittedTasksToView(WrappedTask... wrappedTasks);

  void setTaskPriority(Task task, TaskPriority priority);

  void cancelBatchTasks();

  /**
   * Keep an instance of the read only list view as long as the other object is alive and listening
   * to changes.
   *
   * @return read only list view
   */
  ObservableList<WrappedTask> getReadOnlyTasks();

  void close();

  void setNumberOfThreads(int numThreads);

  boolean isTaskInstanceRunningOrQueued(Class<? extends AbstractTask> clazz);

  void cancelAllTasks();

}
