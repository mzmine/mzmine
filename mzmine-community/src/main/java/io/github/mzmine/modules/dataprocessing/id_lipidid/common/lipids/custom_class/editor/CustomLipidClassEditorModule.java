package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Opens the custom lipid class editor.
 */
public class CustomLipidClassEditorModule implements MZmineRunnableModule {

  public static final String NAME = "Custom lipid class editor";
  private static final String DESCRIPTION = """
      Build, test and save custom lipid classes for lipid annotation. \
      Browse, download and share classes in the community lipid library.""";

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    CustomLipidClassEditorTab.showTab();
    return ExitCode.OK;
  }

  @Override
  public @NotNull String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.TOOLS;
  }

  @Override
  public @NotNull String getName() {
    return NAME;
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return CustomLipidClassEditorParameters.class;
  }
}
