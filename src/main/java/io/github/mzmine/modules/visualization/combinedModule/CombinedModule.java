package io.github.mzmine.modules.visualization.combinedModule;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import javax.annotation.Nonnull;

public class CombinedModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = " ";
  private static final String MODULE_DESCRIPTION = " ";

  @Override
  public @Nonnull
  String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull
  String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    RawDataFile[] dataFiles = parameters.getParameter(CombinedModuleParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();

    CombinedModuleVisualizerWindow newWindow =
        new CombinedModuleVisualizerWindow(dataFiles[0], parameters);
    newWindow.show();

    return ExitCode.OK;
  }

  @Override
  public @Nonnull
  MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Override
  public @Nonnull
  Class<? extends ParameterSet> getParameterSetClass() {
    return CombinedModuleParameters.class;
  }

}
