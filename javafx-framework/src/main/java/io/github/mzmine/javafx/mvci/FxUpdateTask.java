/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public abstract class FxUpdateTask<ViewModelClass> extends AbstractTask {

  private static final Logger logger = Logger.getLogger(FxUpdateTask.class.getName());
  protected final ViewModelClass model;

  protected FxUpdateTask(@NotNull String taskName, ViewModelClass model) {
    super(null, Instant.now());
    setName(taskName);
    this.model = model;
  }


  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      process();
    } catch (Exception ex) {
      logger.log(Level.SEVERE,
          "Unhandled exception " + ex + " while processing task " + getTaskDescription(), ex);
      setStatus(TaskStatus.ERROR);
      throw ex;
    }

    if (!isCanceled()) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  /**
   * Process and generate new data that is stored in this object until {@link #updateGuiModel()} is
   * called on the GUI thread
   */
  protected abstract void process();

  /**
   * Update model is only called if process was successful and if this task is still the latest
   * scheduled task of this type
   */
  protected abstract void updateGuiModel();

  /**
   * @return task is only run if preconditions are met
   */
  public boolean checkPreConditions() {
    return true;
  }

  /**
   * Default is true.
   *
   * @return if task should be cancelled when parent closes. Parent may be a tab and this is most
   * likely controlled in {@link LatestTaskScheduler}
   */
  public boolean isCancelTaskOnParentClosed() {
    return true;
  }

  /**
   * Is called if preconditions are failed
   */
  public void onFailedPreCondition() {
  }
}
