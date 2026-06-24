package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.intensityrepresentation;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class ConfigCompoundRepresentationModule implements MZmineProcessingModule {

  public static final String NAME = "Configure compound representations";

  private static final String DESCRIPTION =
      "Configures how compound rows expose their per-raw-file intensity (area, height, "
          + "normalized variants). Either falls back to the representative row's feature, or "
          + "synthesizes new compound features by summing over all members or only members that "
          + "carry an Ion Identity. Run after Compound Grouping. Re-run any time to switch modes.";

  @Override
  public @NotNull String getName() {
    return NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURE_GROUPING;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ConfigCompoundRepresentationParameters.class;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    final ModularFeatureList[] featureLists = parameters.getValue(
        ConfigCompoundRepresentationParameters.FEATURE_LISTS).getMatchingFeatureLists();
    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (final ModularFeatureList flist : featureLists) {
      tasks.add(new ConfigCompoundRepresentationTask(flist, parameters, storage, moduleCallDate));
    }
    return ExitCode.OK;
  }
}
