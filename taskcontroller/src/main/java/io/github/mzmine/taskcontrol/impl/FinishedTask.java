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

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import java.time.Instant;

/**
 * This class serves as a replacement for Task within the task controller queue, after the Task is
 * finished. This allows the garbage collector to remove the memory occupied by the actual Task
 * while keeping the task description in the Tasks in progress window, until all tasks are finished.
 */
public class FinishedTask extends AbstractTask {

  private final String description;
  private final double finishedPercentage;

  public FinishedTask(Task task) {
    super(null, Instant.now()); // date is irrelevant
    setStatus(task.getStatus());
    setErrorMessage(task.getErrorMessage());
    description = task.getTaskDescription();
    finishedPercentage = task.getFinishedPercentage();
  }

  public String getTaskDescription() {
    return description;
  }

  public void run() {
    // ignore any attempt to run this task, because it is finished
  }

  public void cancel() {
    // ignore any attempt to cancel this task, because it is finished
  }

  public double getFinishedPercentage() {
    return finishedPercentage;
  }


}
