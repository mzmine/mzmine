package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * An isotope finder algorithm, selected by {@link IsotopeFinderModeOptions}. The algorithm owns both
 * its parameters and the processing it performs: it creates the tasks for the selected feature lists,
 * so it is free to run one task per feature list, a single task for all of them, or a different
 * pipeline altogether. Implementations must provide a public no-args constructor (they are
 * instantiated by reflection in {@link io.github.mzmine.main.MZmineCore#getModuleInstance}).
 */
public interface IsotopeFinderAlgorithmModule extends MZmineModule {

  /**
   * @param project        the current project.
   * @param featureLists   the feature lists to process.
   * @param parameters     the embedded parameters of this algorithm option.
   * @param topParameters  the top-level {@link IsotopeFinderParameters}, to be stored as the applied
   *                       method so the run can be reproduced and re-inspected.
   * @param moduleCallDate the module call date of the applied method.
   * @return the tasks to run, may be empty.
   */
  @NotNull List<Task> createTasks(@NotNull MZmineProject project,
      @NotNull ModularFeatureList[] featureLists, @NotNull ParameterSet parameters,
      @NotNull ParameterSet topParameters, @NotNull Instant moduleCallDate);
}
