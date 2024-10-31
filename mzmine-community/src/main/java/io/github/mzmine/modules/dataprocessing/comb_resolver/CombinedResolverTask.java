package io.github.mzmine.modules.dataprocessing.comb_resolver;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CombinedResolverTask extends AbstractFeatureListTask {

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   */
  protected CombinedResolverTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
  }

  @java.lang.Override
  protected void process() {

  }

  @java.lang.Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return null;
  }

  @java.lang.Override
  public String getTaskDescription() {
    return null;
  }
}
