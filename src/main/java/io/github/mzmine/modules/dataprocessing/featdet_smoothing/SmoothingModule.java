package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SmoothingModule implements MZmineProcessingModule {

  private static final String name = "Smoothing";

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return SmoothingParameters.class;
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "Smooths intensity along the retention time and/or mobility dimension.";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    final ModularFeatureList[] flists = parameters.getParameter(SmoothingParameters.featureLists)
        .getValue().getMatchingFeatureLists();

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (ModularFeatureList flist : flists) {
      tasks.add(new SmoothingTask(project, flist, storage, parameters));
    }

    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATUREDETECTION;
  }
}
