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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmoothingModule implements MZmineProcessingModule {

  private static final String name = "Smoothing";

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return SmoothingParameters.class;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Smooths intensity along the retention time and/or mobility dimension.";
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks) {

    final ModularFeatureList[] flists = parameters.getParameter(SmoothingParameters.featureLists)
        .getValue().getMatchingFeatureLists();

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (ModularFeatureList flist : flists) {
      tasks.add(new SmoothingTask(project, flist, storage, parameters));
    }

    return ExitCode.OK;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURE_RESOLVING;
  }
}
