package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

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

public class CompoundGrouperModule implements MZmineProcessingModule {

  public static final String NAME = "Compound grouping";

  private static final String DESCRIPTION =
      "Groups feature list rows that represent the same chemical compound (adducts, isotopologues, "
          + "in-source fragments, correlation members) into compound rows. Requires Ion Identity "
          + "Networking and/or Correlation Grouping output as input.";

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
    return CompoundGrouperParameters.class;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    final ModularFeatureList[] featureLists = parameters.getValue(
        CompoundGrouperParameters.FEATURE_LISTS).getMatchingFeatureLists();
    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (final ModularFeatureList flist : featureLists) {
      tasks.add(new CompoundGrouperTask(flist, parameters, storage, moduleCallDate));
    }
    return ExitCode.OK;
  }
}
