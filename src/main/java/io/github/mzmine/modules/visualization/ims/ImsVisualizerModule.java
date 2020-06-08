package io.github.mzmine.modules.visualization.ims;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * ims plot module
 *
 * @author Nakul Bharti (knakul853@gmail.com)
 */
public class ImsVisualizerModule implements MZmineRunnableModule {
  private static final String MODULE_NAME = "IMS visualizer";
  private static final String MODULE_DESCRIPTION = "IMS visualizer";

  @Nonnull
  @Override
  public String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Nonnull
  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @Nonnull
  @Override
  public ExitCode runModule(
      @Nonnull MZmineProject project,
      @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    Task newTask = new ImsVisualizerTask(parameters);
    tasks.add(newTask);
    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return ImsVisualizerParameters.class;
  }
}
