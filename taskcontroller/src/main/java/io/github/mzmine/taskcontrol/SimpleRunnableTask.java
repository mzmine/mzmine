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

package io.github.mzmine.taskcontrol;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the most basic task that runs a {@link Runnable} that can be manged by the MZmine task
 * controller but does not require the functionality of the more sophisticated {@link AbstractTask}
 */
public class SimpleRunnableTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SimpleRunnableTask.class.getName());

  private final Runnable processor;
  private final String description;

  public SimpleRunnableTask(@NotNull Runnable processor) {
    this("", processor);
  }

  public SimpleRunnableTask(@NotNull String description, @NotNull Runnable processor) {
    super(null, Instant.now());
    this.processor = processor;
    this.description = description;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;  // shall only be used for short-lived tasks without progress bar
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    try {
      processor.run();
    } catch (RuntimeException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      return;
    }
    setStatus(TaskStatus.FINISHED);
  }

}
