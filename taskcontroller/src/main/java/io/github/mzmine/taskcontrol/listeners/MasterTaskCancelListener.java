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

package io.github.mzmine.taskcontrol.listeners;

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.taskcontrol.impl.WrappedTask;

/**
 * Listener that is added to a master thread to also cancel or error out sub tasks
 */
public class MasterTaskCancelListener implements TaskStatusListener {

  private final WrappedTask[] subTasks;

  public MasterTaskCancelListener(WrappedTask[] subTasks) {
    this.subTasks = subTasks;
  }

  @Override
  public void taskStatusChanged(final Task master, final TaskStatus newStatus,
      final TaskStatus oldStatus) {
    if (master.isCanceled()) {
      for (final WrappedTask wrappedTask : subTasks) {
        if (wrappedTask.isFinished()) {
          continue;
        }

        if (wrappedTask.getActualTask() instanceof AbstractTask at) {
          at.setStatus(newStatus);
        } else {
          wrappedTask.cancel();
        }
      }
    }
  }
}
