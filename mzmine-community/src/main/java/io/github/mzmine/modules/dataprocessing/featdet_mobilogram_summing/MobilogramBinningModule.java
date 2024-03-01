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

package io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Steffen https://github.com/SteffenHeu
 */
public class MobilogramBinningModule implements MZmineProcessingModule {

  @NotNull
  @Override
  public String getName() {
    return "Mobilogram summing";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return MobilogramBinningParameters.class;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Bins intensities within given mobility ranges to correct for noise in mobilograms.";
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    final ModularFeatureList[] flists = parameters
        .getParameter(MobilogramBinningParameters.featureLists).getValue()
        .getMatchingFeatureLists();

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (ModularFeatureList flist : flists) {
      tasks.add(new MobilogramBinningTask(storage, flist, parameters, project, moduleCallDate));
    }

    return ExitCode.OK;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.EIC_DETECTION;
  }
}
