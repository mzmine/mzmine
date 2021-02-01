package io.github.mzmine.modules.visualization.spectramergingtest;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SpectraMergingTestModule implements MZmineRunnableModule {

  @Nonnull
  @Override
  public String getName() {
    return "Spectra merging";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return SpectraMergingTestParameters.class;
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "Merges spectra";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {
    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }
}
