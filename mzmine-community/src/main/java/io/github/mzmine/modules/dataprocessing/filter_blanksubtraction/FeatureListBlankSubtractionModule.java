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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction;

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

/**
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class FeatureListBlankSubtractionModule implements MZmineProcessingModule {

  public static final String MODULE_NAME = "Feature list blank subtraction";

  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return FeatureListBlankSubtractionParameters.class;
  }

  @Override
  public String getDescription() {
    return "Subtracts features appearing in (procedural) blank measurements feature list from an aligned feature list. Additionally, removed features can be saved to another feature list for inspection. ";
  }

  @Override
  public ExitCode runModule(MZmineProject project, ParameterSet parameters, Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    Task task = new FeatureListBlankSubtractionTask(project,
        (FeatureListBlankSubtractionParameters) parameters, MemoryMapStorage.forFeatureList(),
        moduleCallDate);

    tasks.add(task);

    return ExitCode.OK;
  }

  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTFILTERING;
  }

}
