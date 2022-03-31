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
import java.io.File;
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
    final File path = param.getValue(BioTransformerParameters.bioPath);

    final ExitCode exitCode = param.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.getTaskController()
          .addTask(new SingleRowPredictionTask(row, smiles, prefix, param, Instant.now()));
    }
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }
}
