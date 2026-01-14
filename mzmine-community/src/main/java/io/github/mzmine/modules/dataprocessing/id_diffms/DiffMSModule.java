/*
 * Copyright (c) 2004-2026 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_diffms;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiffMSModule implements MZmineProcessingModule {

  public static void showSelectedRowsDialog(final List<FeatureListRow> rows, final FeatureTableFX table,
      @NotNull final Instant moduleCallDate) {
    final ParameterSet parameters = new SelectedRowsDiffMSParameters();
    if (parameters.showSetupDialog(true) != ExitCode.OK) {
      return;
    }
    final var flist = table.getFeatureList();
    final var selected = rows.stream().map(r -> {
      if (r instanceof ModularFeatureListRow mr) {
        return mr;
      }
      throw new IllegalStateException("DiffMS selected-rows requires ModularFeatureListRow.");
    }).toList();
    MZmineCore.getTaskController().addTask(
        new DiffMSTask(ProjectService.getProjectManager().getCurrentProject(), parameters, flist,
            selected, moduleCallDate));
  }

  @Override
  public @NotNull String getName() {
    return "DiffMS (MS/MS → structure)";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return DiffMSParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Generates candidate molecular structures from MS/MS and an assigned molecular formula "
        + "using pretrained DiffMS checkpoints.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    for (var flist : parameters.getValue(DiffMSParameters.flists).getMatchingFeatureLists()) {
      tasks.add(new DiffMSTask(project, parameters, flist, moduleCallDate));
    }
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }
}

