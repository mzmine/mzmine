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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple task that may produce feature lists or run on raw data files. Applied methods will be
 * added automatically to the processed files and feature lists.
 * <p>
 * Logic should be implemented in {@link #process()}. Progress is calculated from
 * {@link #totalItems} and {@link #finishedItems} and incremented via
 * {@link #incrementFinishedItems()}.
 */
public abstract class AbstractSimpleTask extends AbstractTask {

  protected final ParameterSet parameters;
  private final Class<? extends MZmineModule> moduleClass;
  protected long totalItems;
  protected AtomicLong finishedItems = new AtomicLong(0);

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   */
  protected AbstractSimpleTask(@Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
    this.moduleClass = moduleClass;
  }

  @Override
  public double getFinishedPercentage() {
    return totalItems != 0 ? finishedItems.get() / (double) totalItems : 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    process();

    if (!isCanceled()) {
      addAppliedMethod();
      setStatus(TaskStatus.FINISHED);
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

  /**
   * Automatically adds the applied method to all the feature lists
   *
   * @return the processed feature lists
   */
  @NotNull
  protected abstract List<FeatureList> getProcessedFeatureLists();

  /**
   * Automatically adds the applied method to all the raw data files
   *
   * @return the processed raw data files
   */
  @NotNull
  protected abstract List<RawDataFile> getProcessedDataFiles();

  protected void addAppliedMethod() {
    SimpleFeatureListAppliedMethod appliedMethod = new SimpleFeatureListAppliedMethod(moduleClass,
        parameters, moduleCallDate);
    for (final var flist : getProcessedFeatureLists()) {
      flist.addDescriptionOfAppliedTask(appliedMethod);
    }
    for (final var raw : getProcessedDataFiles()) {
      raw.getAppliedMethods().add(appliedMethod);
    }
  }

  public ParameterSet getParameters() {
    return parameters;
  }

  public Class<? extends MZmineModule> getModuleClass() {
    return moduleClass;
  }
}
