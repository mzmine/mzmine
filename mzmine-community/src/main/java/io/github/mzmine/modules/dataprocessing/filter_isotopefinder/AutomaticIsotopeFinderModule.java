package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The simplified isotope finder algorithm: exposes only m/z tolerance, the 13C requirement, and the
 * maximum charge, and runs the same detection as the {@link CarbonAveragineAlgorithmModule} with
 * defaults for everything else.
 */
public class AutomaticIsotopeFinderModule implements IsotopeFinderAlgorithmModule {

  @Override
  public @NotNull String getName() {
    return "Automatic";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return AutomaticIsotopeFinderParameters.class;
  }

  @Override
  public @NotNull List<Task> createTasks(@NotNull final MZmineProject project,
      @NotNull final ModularFeatureList[] featureLists, @NotNull final ParameterSet parameters,
      @NotNull final ParameterSet topParameters, @NotNull final Instant moduleCallDate) {
    final CarbonAveragineAlgorithmParameters algo = AutomaticIsotopeFinderParameters.toCarbonAveragineParameters(
        parameters);

    final List<Task> tasks = new ArrayList<>(featureLists.length);
    for (final ModularFeatureList featureList : featureLists) {
      tasks.add(new IsotopeFinderTask(project, featureList, topParameters, algo, getName(),
          moduleCallDate));
    }
    return tasks;
  }
}
