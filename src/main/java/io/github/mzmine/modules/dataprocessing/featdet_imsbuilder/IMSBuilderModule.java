package io.github.mzmine.modules.dataprocessing.featdet_imsbuilder;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IMSBuilderModule implements MZmineProcessingModule {

  @Nonnull
  @Override
  public String getName() {
    return "IMS Builder";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return IMSBuilderParameters.class;
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "Builds m/z traces for ion mobility spectrometry data";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {
    return null;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.EIC_DETECTION;
  }
}
