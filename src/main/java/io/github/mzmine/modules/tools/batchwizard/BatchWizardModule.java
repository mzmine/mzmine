package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BatchWizardModule implements MZmineRunnableModule {

  @Override
  public @NotNull String getName() {
    return "Processing wizard";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return BatchWizardParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Creates processing batches with a reduced set of parameters.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks) {
    if (MZmineCore.isHeadLessMode()) {
      return ExitCode.OK;
    }

    showTab();
    return ExitCode.OK;
  }

  public void showTab() {
    MZmineCore.runLater(() -> MZmineCore.getDesktop().addTab(new BatchWizardTab()));
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.TOOLS;
  }
}
