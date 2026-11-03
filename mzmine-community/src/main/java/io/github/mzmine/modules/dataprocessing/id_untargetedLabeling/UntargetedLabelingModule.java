/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_untargetedLabeling;

import static io.github.mzmine.gui.mainwindow.MZmineTab.logger;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
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
 * This module implements untargeted isotope labeling analysis based on aligned feature lists
 * containing both labeled and unlabeled samples.
 */
public class UntargetedLabelingModule implements MZmineProcessingModule {

  public static final String MODULE_NAME = "Untargeted isotope labeling (metadata-based)";
  public static final String MODULE_DESCRIPTION =
      "This module detects isotopically labeled compounds by analyzing aligned feature lists "
          + "containing both labeled and unlabeled samples, using metadata to distinguish sample groups.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ISOTOPES;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return UntargetedLabelingParameters.class;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    // Get feature lists
    FeatureList[] featureLists = parameters.getParameter(UntargetedLabelingParameters.featureLists)
        .getValue().getMatchingFeatureLists();

    // Get metadata grouping parameters
    String metadataColumnName = parameters.getParameter(
        UntargetedLabelingParameters.metadataGrouping).getValue();
    String unlabeledValue = parameters.getParameter(
        UntargetedLabelingParameters.unlabeledGroupValue).getValue();
    String labeledValue = parameters.getParameter(UntargetedLabelingParameters.labeledGroupValue)
        .getValue();

    // Ensure we have feature lists
    if (featureLists.length == 0) {
      logger.warning("No feature lists selected");
      return ExitCode.ERROR;
    }

    // Validate metadata column is selected
    if (metadataColumnName == null || metadataColumnName.trim().isEmpty()) {
      logger.warning("No metadata column selected for sample grouping");
      return ExitCode.ERROR;
    }

    // Validate group values are provided
    if (unlabeledValue == null || unlabeledValue.trim().isEmpty()) {
      logger.warning("No unlabeled group value specified");
      return ExitCode.ERROR;
    }

    if (labeledValue == null || labeledValue.trim().isEmpty()) {
      logger.warning("No labeled group value specified");
      return ExitCode.ERROR;
    }

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

    // Create task for each feature list
    for (FeatureList featureList : featureLists) {
      Task newTask = new UntargetedLabelingTask(project, featureList, metadataColumnName,
          unlabeledValue, labeledValue, parameters, storage, moduleCallDate);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }
}