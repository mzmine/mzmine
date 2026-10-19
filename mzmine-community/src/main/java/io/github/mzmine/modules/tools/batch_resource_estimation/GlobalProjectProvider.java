package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.project.ProjectService;
import org.jetbrains.annotations.NotNull;

/**
 * Runs every subset on the cleared global project, as the batch task always uses the global
 * project. Discards the user's project, so callers must confirm this first.
 */
public final class GlobalProjectProvider implements EstimationProjectProvider {

  @Override
  public @NotNull MZmineProject projectForRun() {
    ProjectService.getProjectManager().clearProject();
    return ProjectService.getProject();
  }

  @Override
  public void release(@NotNull final MZmineProject project) {
    ProjectService.getProjectManager().clearProject();
  }
}
