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

package io.github.mzmine.modules.example;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * A Module creates tasks which are then added queue
 */
public class EmptyFeatureListModule implements MZmineProcessingModule {
  // #################################################################
  // IMPORTANT
  // add your module to the {@link MainMenu.fxml}
  // do not forget to put your module in the BatchModeModulesList
  // in the package: io.github.mzmine.main
  // #################################################################

  private static final String MODULE_NAME = "module";
  private static final String MODULE_DESCRIPTION = "describe";

  // this method is called after user selects the parameters and clicks okay (or in batch mode)
  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    // get parameters here only needed to run one task per featureList
    FeatureList[] featureLists = parameters.getParameter(LearnerParameters.featureLists).getValue()
        .getMatchingFeatureLists();

    // create and start one task for each feature list
    for (final FeatureList featureList : featureLists) {
      Task newTask = new EmptyFeatureListTask(project, featureList, parameters, null,
          moduleCallDate);
      // task is added to TaskManager that schedules later
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    /*
     * Change category - used in the batch list module to group processing steps
     */
    return MZmineModuleCategory.FEATURELISTFILTERING;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return EmptyFeatureListParameters.class;
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }
}
