package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.compoundlist.CompoundComponentizerModule;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy selection for compound componentization. Each value points to a
 * {@link CompoundComponentizerModule} that knows how to instantiate its
 * {@link io.github.mzmine.datamodel.features.compoundlist.CompoundComponentizerStrategy}.
 */
public enum CompoundComponentizerType implements ModuleOptionsEnum<CompoundComponentizerModule> {

  SimpleSeeder("Simple IIN + correlation", "simple_iin_correlation",
      SimpleSeederComponentizerModule.class),

  WeightedGraph("Weighted multi-evidence graph", "weighted_graph",
      WeightedGraphComponentizerModule.class);

  private final String name;
  private final String stableId;
  private final Class<? extends CompoundComponentizerModule> moduleClass;

  CompoundComponentizerType(@NotNull final String name, @NotNull final String stableId,
      @NotNull final Class<? extends CompoundComponentizerModule> moduleClass) {
    this.name = name;
    this.stableId = stableId;
    this.moduleClass = moduleClass;
  }

  @Override
  public @NotNull Class<? extends CompoundComponentizerModule> getModuleClass() {
    return moduleClass;
  }

  @Override
  public @NotNull String getStableId() {
    return stableId;
  }

  @Override
  public String toString() {
    return name;
  }
}
