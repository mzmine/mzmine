package io.github.mzmine.datamodel.identities.fx;

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

public class IonTypeCreatorModule implements MZmineRunnableModule {

  public static final String NAME = "Define global ions";
  private static final String DESCRIPTION = """
      Define global ions types, adducts, and in-source fragments.
      Create and modify lists of ion types used by Ion Identity Networking and other tools.""";

  public IonTypeCreatorModule() {
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    IonTypeCreatorTab.showTab();
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
    return IonTypeCreatorParameters.class;
  }
}
