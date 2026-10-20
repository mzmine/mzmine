package io.github.mzmine.taskcontrol;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.batchmode.BatchTask;
import org.jetbrains.annotations.Nullable;

/**
 * A task that may result in a different project than the one it was started on, e.g., by loading
 * a project without merging or by clearing the project. Similar to
 * {@link AbstractSimpleTask#getProcessedFeatureLists()} this declares the result of the task.
 * <p>
 * The {@link BatchTask} continues with the result project if it differs from the project the step
 * was started on. Implementations are responsible to replace the project in the
 * {@link io.github.mzmine.project.ProjectManager} so that the GUI follows if the replaced project
 * was the current project.
 */
public interface ProjectResultTask extends Task {

  /**
   * @return the project that holds the results of this task. This is the project the task was
   * started on or a new project that replaces it. Null if the task did not finish.
   */
  @Nullable MZmineProject getResultProject();

}
