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

package io.github.mzmine.modules.dataprocessing.filter_diams2;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.operations.TaskSubProcessor;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiaMs2CorrTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiaMs2CorrTask.class.getName());

  private final ModularFeatureList flist;
  private final DiaMs2CorrParameters parameters;
  private final TaskSubProcessor logic;

  protected DiaMs2CorrTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      ModularFeatureList flist, ParameterSet parameters) {
    super(storage, moduleCallDate);

    this.flist = flist;
    this.parameters = (DiaMs2CorrParameters) parameters;

    final ValueWithParameters<DiaCorrelationOptions> corrModule = parameters.getParameter(
        DiaMs2CorrParameters.algorithm).getValueWithParameters();
    logic = corrModule.value()
        .createLogicTask(flist, this.parameters, corrModule.parameters(), this);
  }

  @Override
  public String getTaskDescription() {
    return logic.getTaskDescription();
  }

  @Override
  public double getFinishedPercentage() {
    return logic.getFinishedPercentage();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (flist.getNumberOfRawDataFiles() != 1) {
      error(
          "Cannot build DIA MS2 for feature lists with more than one raw data file. Run DIA pseudo MS2 builder before alignment.");
      return;
    }

    logic.process();

    flist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(DiaMs2CorrModule.class, parameters,
            getModuleCallDate()));
    setStatus(TaskStatus.FINISHED);

  }
}
