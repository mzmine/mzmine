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

package io.github.mzmine.taskcontrol;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple task that may be a tool like a visualizer. Use {@link AbstractSimpleTask} for tasks
 * producing feature lists or run on raw data files.
 * <p>
 * Logic should be implemented in {@link #process()}. Progress is calculated from
 * {@link #totalItems} and {@link #finishedItems} and incremented via
 * {@link #incrementFinishedItems()}.
 */
public abstract class AbstractSimpleToolTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(AbstractSimpleToolTask.class.getName());

  protected final ParameterSet parameters;
  protected long totalItems;
  protected AtomicLong finishedItems = new AtomicLong(0);

  /**
   * @param moduleCallDate the call date of module to order execution order
   */
  protected AbstractSimpleToolTask(@NotNull final Instant moduleCallDate,
      @NotNull ParameterSet parameters) {
    this(null, moduleCallDate, parameters);
  }

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   */
  public AbstractSimpleToolTask(@Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate, @NotNull final ParameterSet parameters) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
  }

  @Override
  public double getFinishedPercentage() {
    return totalItems != 0 ? finishedItems.get() / (double) totalItems : 0;
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);

      process();

      if (!isCanceled()) {
        setStatus(TaskStatus.FINISHED);
      }
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Unhandled exception " + e.getMessage() + " while processing task "
                               + getTaskDescription(), e);

      if (e instanceof Exception exception) {
        error(e.getMessage(), exception);
      } else {
        error(e.getMessage());
      }
    }
  }

  /**
   * Do the actual processing. Is automatically called in the {@link #run()} method
   */
  protected abstract void process();

  /**
   * Add 1 to finished items. Thread safe
   *
   * @return the new finished items number
   */
  protected long incrementFinishedItems() {
    return finishedItems.incrementAndGet();
  }

  /**
   * Add n to finished items. Thread safe
   *
   * @return the new finished items number
   */
  private long incrementFinishedItems(final int add) {
    return finishedItems.addAndGet(add);
  }

  public ParameterSet getParameters() {
    return parameters;
  }

}
