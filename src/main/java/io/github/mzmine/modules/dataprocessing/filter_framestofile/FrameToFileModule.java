package io.github.mzmine.modules.dataprocessing.filter_framestofile;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FrameToFileModule implements MZmineRunnableModule {

  @Nonnull
  @Override
  public String getDescription() {
    return "Creates a single raw data file for each frame in a given raw data file.";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {
    RawDataFile[] files = parameters.getParameter(FrameToFileParameters.raw).getValue()
        .getMatchingRawDataFiles();
    for(RawDataFile file : files) {
      MZmineCore.getTaskController().addTask(new FrameToFileTask(file, parameters));
    }
    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATAFILTERING;
  }

  @Nonnull
  @Override
  public String getName() {
    return "Frame to file converter";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return FrameToFileParameters.class;
  }
}
