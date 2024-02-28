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

package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BioTransformerModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getName() {
    return "BioTransformer";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return BioTransformerParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Transforms features with a SMILES annotation to their metabolites by BioTransformer "
        + "in-silico computation.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    for (ModularFeatureList flist : parameters.getValue(BioTransformerParameters.flists)
        .getMatchingFeatureLists()) {
      tasks.add(new BioTransformerTask(project, parameters, flist, moduleCallDate));
    }
    return ExitCode.OK;
  }

  /**
   * Runs a prediction for a single smiles code and annotates all rows with matching mz in the
   * feature list.
   *
   * @param row    The row.
   * @param smiles The smiles code.
   * @param prefix A prefix as metabolite name.
   */
  public static void runSingleRowPredection(ModularFeatureListRow row, String smiles,
      String prefix) {
    final ParameterSet param = new BioTransformerParameters(true);

    final ExitCode exitCode = param.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.getTaskController()
          .addTask(new BioTransformerSingleRowTask(row, smiles, prefix, param, Instant.now()));
    }
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }
}
