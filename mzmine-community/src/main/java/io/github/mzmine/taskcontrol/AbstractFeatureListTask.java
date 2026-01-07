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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link AbstractSimpleTask} that produces feature lists
 */
public abstract class AbstractFeatureListTask extends AbstractSimpleTask {


  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   */
  protected AbstractFeatureListTask(@Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
  }

  /**
   * Automatically adds the applied method to all the raw data files
   *
   * @return the processed raw data files
   */
  @Override
  @NotNull
  protected List<RawDataFile> getProcessedDataFiles() {
    return List.of();
  }

}
