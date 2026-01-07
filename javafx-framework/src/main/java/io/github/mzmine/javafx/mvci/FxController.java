/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import javafx.scene.layout.Region;
import javafx.util.Duration;
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

  protected final LatestTaskScheduler scheduler = new LatestTaskScheduler();
  @NotNull
  protected final ViewModelClass model;

  protected FxController(@NotNull ViewModelClass model) {
    this.model = model;
  }

  protected LatestTaskScheduler getScheduler() {
    return scheduler;
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
   * Creates a view and sets the cached internal instance
   */
  public @NotNull Region buildView() {
    return getViewBuilder().build();
  }

  // TASK scheduling

  /**
   * Run task on GUI thread
   */
  public void onGuiThread(Runnable task) {
    scheduler.onGuiThread(task);
  }

  /**
   * Run a task on a separate thread - for GUI updates after completion use {@link FxUpdateTask}.
   * See {@link #getScheduler()} for more options in the {@link LatestTaskScheduler}.
   *
   * @param task primary task run on separate thread
   */
  public void onTaskThread(final @NotNull Runnable task) {
    scheduler.onTaskThread(task);
  }

  /**
   * Stop old tasks with this unique name, restart a timer that calls the task on finish. This
   * accumulates multiple calls into a single task run. Run a task on a separate thread - for GUI
   * updates after completion use {@link FxUpdateTask}. Default delay. See {@link #getScheduler()}
   * for more options in the {@link LatestTaskScheduler}.
   *
   * @param task primary task run on separate thread. Update GUI after complete
   */
  public void onTaskThreadDelayed(final @NotNull FxUpdateTask<?> task) {
    scheduler.onTaskThreadDelayed(task);
  }

  /**
   * Stop old tasks with this unique name, restart a timer that calls the task on finish. This
   * accumulates multiple calls into a single task run. Run a task on a separate thread - for GUI
   * updates after completion use {@link FxUpdateTask}. See {@link #getScheduler()} for more options
   * in the {@link LatestTaskScheduler}.
   *
   * @param task  primary task run on separate thread. Update GUI after complete
   * @param delay the delay for call accumulation, the timer is always reset after each call.
   */
  public void onTaskThreadDelayed(final @NotNull FxUpdateTask<?> task,
      final @NotNull Duration delay) {
    scheduler.onTaskThreadDelayed(task, delay);
  }

  /**
   * Stop old tasks with this unique name, restart a timer that calls the task on finish. This
   * accumulates multiple calls into a single task run. Run a task on a separate thread - for GUI
   * updates after completion use {@link FxUpdateTask} and
   * {@link #onTaskThreadDelayed(FxUpdateTask, Duration)}. See {@link #getScheduler()} for more
   * options in the {@link LatestTaskScheduler}.
   *
   * @param task           primary task run on separate thread
   * @param uniqueTaskName the unique task name is used to accumulate update calls and stop older
   *                       tasks
   */
  public void onTaskThreadDelayed(final @NotNull Runnable task, final String uniqueTaskName) {
    scheduler.onTaskThreadDelayed(task, uniqueTaskName);
  }

  /**
   * Stop old tasks with this unique name, restart a timer that calls the task on finish. This
   * accumulates multiple calls into a single task run. Run a task on a separate thread - for GUI
   * updates after completion use {@link FxUpdateTask} and
   * {@link #onTaskThreadDelayed(FxUpdateTask, Duration)}. See {@link #getScheduler()} for more
   * options in the {@link LatestTaskScheduler}.
   *
   * @param task           primary task run on separate thread
   * @param uniqueTaskName the unique task name is used to accumulate update calls and stop older
   *                       tasks
   * @param delay          the delay for call accumulation, the timer is always reset after each
   *                       call.
   */
  public void onTaskThreadDelayed(final @NotNull Runnable task, final String uniqueTaskName,
      final @NotNull Duration delay) {
    scheduler.onTaskThreadDelayed(task, uniqueTaskName, delay);
  }

  /**
   * Cancel all tasks, except {@link FxUpdateTask} which define that they should keep on running
   */
  public void cancelTasks() {
    scheduler.cancelTasks();
  }

  /**
   * Default close behavior like cancelling all tasks. Closing the view does not automatically close
   * the controller. But if the controller is part of a tab this close method can be called from the
   * onClosed callback of the tab.
   */
  public void close() {
    cancelTasks();
  }

}
