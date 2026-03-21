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

package io.github.mzmine.modules.dataprocessing.filter_diams2;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_diams2.no_corr.DiaMs2NoCorrModule;
import io.github.mzmine.modules.dataprocessing.filter_diams2.rt_corr.DiaMs2RtCorrModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import io.github.mzmine.taskcontrol.operations.TaskSubProcessor;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public enum DiaCorrelationOptions implements ModuleOptionsEnum<DiaCorrelationModule> {
  RT_CORRELATION, NO_CORRELATION;

  public static String getDescriptions() {
    return Arrays.stream(values()).map(v -> "- " + v.toString() + ": " + v.getDescription())
        .collect(Collectors.joining("\n"));
  }

  @Override
  public Class<? extends DiaCorrelationModule> getModuleClass() {
    return switch (this) {
      case NO_CORRELATION -> DiaMs2NoCorrModule.class;
      case RT_CORRELATION -> DiaMs2RtCorrModule.class;
    };
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case NO_CORRELATION -> "no_corr";
      case RT_CORRELATION -> "rt_corr";
    };
  }

  public TaskSubProcessor createLogicTask(@NotNull final ModularFeatureList flist,
      @NotNull final DiaMs2CorrParameters mainParam, @NotNull final ParameterSet moduleParam,
      @NotNull final DiaMs2CorrTask mainTask) {
    DiaCorrelationModule module = MZmineCore.getModuleInstance(getModuleClass());
    return module.createLogicTask(flist, mainParam, moduleParam, mainTask);
  }

  public String getDescription() {
    return switch (this) {
      case NO_CORRELATION ->
          "Pairs the closest MS2 spectrum based on retention time and ion mobility dimension without filtering for feature shape correlation. (Quadrupole aware)";
      case RT_CORRELATION ->
          "Correlates feature shapes of potential fragments in RT dimension and filters by ion mobility dimension, if available. (Quadrupole aware)";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case NO_CORRELATION -> "No correlation";
      case RT_CORRELATION -> "RT correlation";
    };
  }
}
