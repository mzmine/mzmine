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

package io.github.mzmine.javafx.mvci;

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.utils.TaskUtils;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MVCI Controller base class. The Controller is the interface to other parts of the framework and
 * initializes the ViewModel that contains only observable properties. The {@link FxViewBuilder}
 * creates the layout and adds bindings to the model. The {@link FxInteractor} interacts with
 * business logic and is called by the {@link FxController}, which handles the calling thread to be
 * GUI or other managed thread. Use {@link FxUpdateTask} to run a heavy process on a separate thread
 * and then update the model on GUI thread.
 */
public abstract class FxController<ViewModelClass> {

  private final CloseableReentrantReadWriteLock taskLock = new CloseableReentrantReadWriteLock();
  private Map<String, Task> runningTasks;

  @NotNull
  protected final ViewModelClass model;

  protected FxController(@NotNull ViewModelClass model) {
    this.model = model;
  }

  /**
   * Interactor is optional and only used to separate logic from the controller. Also see
   * {@link FxUpdateTask} if updates need to run on a separate task.
   */
  protected @Nullable FxInteractor<ViewModelClass> getInteractor() {
    return null;
  }

  protected abstract @NotNull FxViewBuilder<ViewModelClass> getViewBuilder();

  /**
   * Run task on GUI thread
   */
  public void onGuiThread(Runnable task) {
    FxThread.runLater(task);
  }


  /**
   * Run a task on a separate thread - for GUI updates after completion use {@link FxUpdateTask}
   *
   * @param task primary task run on separate thread
   */
  public void onTaskThread(final @NotNull Runnable task) {
    onTaskThread(task, TaskPriority.NORMAL);
  }

  /**
   * Run a task on a separate thread - for GUI updates after completion use {@link FxUpdateTask}
   *
   * @param task primary task run on separate thread
   */
  public void onTaskThread(final @NotNull Runnable task, final @NotNull TaskPriority priority) {
    final Task runningTask = TaskUtils.wrapTask(task);

    // if gui is updated after - add checks for latest task completion
    if (task instanceof FxUpdateTask<?> fxUpdateTask) {
      if (!fxUpdateTask.checkPreConditions()) {
        return;
      }

      String uniqueTaskName = fxUpdateTask.getName();

      try (var _ = taskLock.lockWrite()) {
        if (runningTasks == null) {
          runningTasks = new HashMap<>();
        }
        var oldTask = runningTasks.put(uniqueTaskName, runningTask);
        if (oldTask != null) {
          oldTask.cancel();
        }
      }

      runningTask.setOnFinished(() -> {
        // remove the old task from map and compare with the running task if equal
        final Task oldTask = removeOldTask(uniqueTaskName);
        if (oldTask != null && oldTask.isFinished() && Objects.equals(oldTask, runningTask)) {
          // only update gui if still latest task
          FxThread.runLater(fxUpdateTask::updateGuiModel);
        }
      });
    }
    // schedule
    TaskService.getController().addTask(runningTask, priority);
  }


  /**
   * removes the task with a write lock
   *
   * @param taskName the name of the task in map
   * @return the old task in map
   */
  private Task removeOldTask(final @NotNull String taskName) {
    try (var _ = taskLock.lockWrite()) {
      return runningTasks.remove(taskName);
    }
  }

  /**
   * Creates a view and sets the cached internal instance
   */
  public @NotNull Region buildView() {
    return getViewBuilder().build();
  }

}
