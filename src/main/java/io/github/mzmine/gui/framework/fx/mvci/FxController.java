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

package io.github.mzmine.gui.framework.fx.mvci;

import io.github.mzmine.main.MZmineCore;
import javafx.concurrent.Task;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MVCI Controller base class. The Controller is the interface to other parts of the framework and
 * initializes the ViewModel that contains only observable properties. The {@link FxViewBuilder}
 * creates the layout and adds bindings to the model. The {@link FxInteractor} interacts with
 * business logic and is called by the {@link FxController}, which handles the calling thread to be
 * GUI or other managed thread.
 */
public abstract class FxController<ViewModelClass> {

  @NotNull
  protected final ViewModelClass model;

  protected FxController(@NotNull ViewModelClass model) {
    this.model = model;
  }

  protected abstract @Nullable FxInteractor<ViewModelClass> getInteractor();

  protected abstract @NotNull FxViewBuilder<ViewModelClass> getViewBuilder();

  /**
   * Run task on GUI thread
   */
  public void onGuiThread(Runnable task) {
    MZmineCore.runLater(task);
  }

  /**
   * Run a task on a separate thread and then finally update the GUI after success
   *
   * @param task            primary task run on separate thread
   * @param postTaskGuiTask post GUI update task
   */
  public void onTaskThread(@NotNull Runnable task, @Nullable Runnable postTaskGuiTask) {
    // TODO change to taskController once all PR are merged
    Task<Void> fxTask = new Task<>() {
      @Override
      protected Void call() {
        task.run();
        return null; // success return null
      }
    };
    if (postTaskGuiTask != null) {
      fxTask.setOnSucceeded(evt -> {
        postTaskGuiTask.run();
      });
    }
    Thread taskThread = new Thread(fxTask);
    taskThread.setDaemon(true);
    taskThread.start();
  }

  /**
   * Creates a view and sets the cached internal instance
   */
  public @NotNull Region buildView() {
    return getViewBuilder().build();
  }

}
