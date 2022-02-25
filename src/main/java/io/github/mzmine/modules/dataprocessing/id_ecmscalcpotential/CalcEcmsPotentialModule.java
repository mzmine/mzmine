package io.github.mzmine.modules.dataprocessing.id_ecmscalcpotential;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CalcEcmsPotentialModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getName() {
    return "Calculate EC-MS potentials";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return CalcEcmsPotentialParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Calculates metabolite formation potentials in EC-MS experiments.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    tasks.add(new CalcEcmsPotentialTask(project, parameters, moduleCallDate));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }
}
