package io.github.mzmine.modules.dataprocessing.comb_resolver;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.TaskPerFeatureListModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CombinedResolverModule extends TaskPerFeatureListModule {

  /**
   * @param name                  name of the module in the menu and quick access
   * @param parameterSetClass     the class of the parameters
   * @param moduleCategory        module category for quick access and batch mode
   * @param requiresMemoryMapping if true and if memory mapping is activated, task will get a memory
   *                              map storage
   * @param description           the description of the task
   */
  public CombinedResolverModule(@NotNull String name,
      @Nullable Class<? extends ParameterSet> parameterSetClass,
      @NotNull MZmineModuleCategory moduleCategory, boolean requiresMemoryMapping,
      @NotNull String description) {
    super(name, parameterSetClass, moduleCategory, requiresMemoryMapping, description);
  }

  @java.lang.Override
  public @NotNull Task createTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable MemoryMapStorage storage,
      @NotNull FeatureList featureList) {
    return null;
  }
}
