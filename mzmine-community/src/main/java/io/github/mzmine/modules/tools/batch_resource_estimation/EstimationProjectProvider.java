package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.datamodel.MZmineProject;
import org.jetbrains.annotations.NotNull;

/**
 * Provides the empty project that a subset run of the resource estimation runs on.
 * <p>
 * The batch currently always runs on the global project, so {@link GlobalProjectProvider} clears
 * it. Once batches can run on their own project, an implementation can return a new isolated
 * project and leave the user's project untouched.
 */
public interface EstimationProjectProvider {

  /**
   * @return an empty project for the next subset run
   */
  @NotNull MZmineProject projectForRun();

  /**
   * Releases the data of a finished run so that the next run starts from the baseline.
   */
  void release(@NotNull MZmineProject project);
}
