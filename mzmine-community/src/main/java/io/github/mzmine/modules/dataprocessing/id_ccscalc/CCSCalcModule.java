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

package io.github.mzmine.modules.dataprocessing.id_ccscalc;

import io.github.mzmine.datamodel.MZmineProject;
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
 * Calculates feature specific collision cross section values based on ion mobility values and the
 * charge of a feature. For calculation, a mobility value {@link io.github.mzmine.datamodel.features.types.numbers.MobilityType},
 * the unit of that mobility value {@link io.github.mzmine.datamodel.MobilityType} and the charge
 * {@link io.github.mzmine.datamodel.features.types.numbers.ChargeType} of an ion are necessary.
 *
 * @author https://github.com/SteffenHeu
 * @see CCSCalcTask
 * @see io.github.mzmine.datamodel.features.types.numbers.CCSType
 * @see https://en.wikipedia.org/wiki/Cross_section_(physics)#Collision_among_gas_particles
 * @see https://doi.org/10.1002/jssc.201700919
 * @see https://arxiv.org/ftp/arxiv/papers/1709/1709.02953.pdf
 * @see https://www.waters.com/webassets/cms/library/docs/720005374en.pdf or
 * https://www.sciencedirect.com/science/article/abs/pii/S1367593117301229?via%3Dihub
 */
public class CCSCalcModule implements MZmineProcessingModule {

  public static final String NAME = "CCS calculation module";

  @NotNull
  @Override
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return CCSCalcParameters.class;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Calculates CCS values for features.";
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    Task task = new CCSCalcTask(project, parameters, MemoryMapStorage.forFeatureList(), moduleCallDate);
    tasks.add(task);
    return ExitCode.OK;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }
}
