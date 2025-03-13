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

package io.github.mzmine.modules.dataprocessing.dummy_example;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationParameters;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationTask;
import io.github.mzmine.modules.example.other.LearnerParameters;
import io.github.mzmine.modules.impl.TaskPerFeatureListModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;

/**
 * A Module creates tasks which are then added queue
 * <p>
 * 1. add your module to the io.github.mzmine.gui.mainwindow.MainMenu.fmxl
 * <p>
 * 2. for access in batch mode, put your module in the BatchModeModulesList in the package:
 * io.github.mzmine.main
 */
public class DummyFeatureListModule implements MZmineRunnableModule {
  private static final String MODULE_NAME ="Dummy";
  private  static final String MODULE_DESCRIPTION ="Does nothing";


  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  //How to run the logic
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
                            @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    //create one task for each feature list
    FeatureList[] featureLists = parameters.getParameter(LearnerParameters.featureLists).getValue().getMatchingFeatureLists();

    for (final FeatureList featureList : featureLists) {
      Task newTask = new DummyFeatureListTask(project, featureList, parameters, null, moduleCallDate);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTFILTERING;
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return DummyFeatureListParameters.class;
  }
}
