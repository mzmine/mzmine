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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_match;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class SpectralLibrarySearchModule implements MZmineProcessingModule {

  public static final String MODULE_NAME = "Spectral library search";
  private static final String MODULE_DESCRIPTION =
      "This method searches all feature list rows (from all feature lists) against a local spectral libraries (needs to be loaded first).";

  /**
   * Show dialog for identifying multiple selected peak-list rows.
   *
   * @param rows the feature list row.
   */
  public static void showSelectedRowsIdentificationDialog(final List<FeatureListRow> rows,
      FeatureTableFX table, @NotNull Instant moduleCallDate) {

    final ParameterSet parameters = new SelectedRowsSpectralLibrarySearchParameters();

    if (parameters.showSetupDialog(true) == ExitCode.OK) {

      MZmineCore.getTaskController().addTask(
          new SelectedRowsSpectralLibrarySearchTask(rows, table, parameters.cloneParameterSet(),
              moduleCallDate));
    }
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    final ModularFeatureList[] featureLists = parameters
        .getParameter(SpectralLibrarySearchParameters.peakLists)
        .getValue().getMatchingFeatureLists();
    Task newTask = new SpectralLibrarySearchTask(parameters, featureLists, moduleCallDate);
    tasks.add(newTask);

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return SpectralLibrarySearchParameters.class;
  }

}
