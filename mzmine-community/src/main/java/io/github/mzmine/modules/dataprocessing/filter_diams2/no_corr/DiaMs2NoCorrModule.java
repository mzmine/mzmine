/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.filter_diams2.no_corr;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaCorrelationModule;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaMs2CorrParameters;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaMs2CorrTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.operations.TaskSubProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiaMs2NoCorrModule implements DiaCorrelationModule {

  @Override
  public @NotNull String getName() {
    return "DIA pseudo MS2 builder (no correlation) (experimental)";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return DiaMs2NoCorrParameters.class;
  }

  @Override
  public TaskSubProcessor createLogicTask(@NotNull ModularFeatureList flist,
      @NotNull DiaMs2CorrParameters mainParam, @NotNull ParameterSet moduleParam,
      @NotNull DiaMs2CorrTask mainTask) {
    return new DiaMs2NoCorrTask(flist, mainParam, moduleParam, mainTask);
  }
}
