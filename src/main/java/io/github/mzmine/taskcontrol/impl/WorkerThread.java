/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Task controller worker thread, this thread will process one task and then finish
 */
class WorkerThread extends Thread {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private WrappedTask wrappedTask;
  private boolean finished = false;

  WorkerThread(WrappedTask wrappedTask) {
    super("Thread executing task " + wrappedTask);
    this.wrappedTask = wrappedTask;
    wrappedTask.assignTo(this);
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run() {

    Task actualTask = wrappedTask.getActualTask();

    try {

      // Log the start (INFO level events go to the Status bar, too)
      logger.info("Starting processing of task " + actualTask.getTaskDescription());

      // Process the actual task
      actualTask.run();

      // Check if task finished with an error
      if (actualTask.getStatus() == TaskStatus.ERROR) {

        String errorMsg = actualTask.getErrorMessage();
        if (errorMsg == null)
          errorMsg = "Unspecified error";

        // Log the error
        logger.severe("Error of task " + actualTask.getTaskDescription() + ": " + errorMsg);

        MZmineCore.getDesktop().displayErrorMessage(errorMsg);
      } else {
        // Log the finish
        logger.info("Processing of task " + actualTask.getTaskDescription() + " done, status "
            + actualTask.getStatus());
      }

      /*
       * This is important to allow the garbage collector to remove the task, while keeping the task
       * description in the "Tasks in progress" window
       */
      wrappedTask.removeTaskReference();

    } catch (Throwable e) {

      /*
       * This should never happen, it means the task did not handle its exception properly, or there
       * was some severe error, like OutOfMemoryError
       */

      logger.log(Level.SEVERE,
          "Unhandled exception " + e + " while processing task " + actualTask.getTaskDescription(),
          e);

      e.printStackTrace();

      MZmineCore.getDesktop().displayErrorMessage("Unhandled exception in task "
          + actualTask.getTaskDescription() + ": " + ExceptionUtils.exceptionToString(e));

    }

    /*
     * Mark this thread as finished
     */
    finished = true;

  }

  boolean isFinished() {
    return finished;
  }

  public WrappedTask getWrappedTask() {
    return wrappedTask;
  }
}
