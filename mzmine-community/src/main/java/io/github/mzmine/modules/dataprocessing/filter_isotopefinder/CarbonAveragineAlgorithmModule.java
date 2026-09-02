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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The full carbon-averagine isotope finder algorithm: estimates the carbon count from the searched
 * m/z to model the 13C envelope, with heavy-isotope-aware upper bounds. Requires no formula
 * prediction and exposes every parameter of the detection run.
 */
public class CarbonAveragineAlgorithmModule implements IsotopeFinderAlgorithmModule {

  @Override
  public @NotNull String getName() {
    return "Carbon model";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return CarbonAveragineAlgorithmParameters.class;
  }

  @Override
  public @NotNull List<Task> createTasks(@NotNull final MZmineProject project,
      @NotNull final ModularFeatureList[] featureLists, @NotNull final ParameterSet parameters,
      @NotNull final ParameterSet topParameters, @NotNull final Instant moduleCallDate) {
    // clone so a task never reads the live GUI/config instance while it is running
    final CarbonAveragineAlgorithmParameters algo = (CarbonAveragineAlgorithmParameters) parameters.cloneParameterSet();

    final List<Task> tasks = new ArrayList<>(featureLists.length);
    for (final ModularFeatureList featureList : featureLists) {
      tasks.add(new IsotopeFinderTask(project, featureList, topParameters, algo, getName(),
          moduleCallDate));
    }
    return tasks;
  }
}
