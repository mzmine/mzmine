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
 * The full carbon-averagine isotope finder algorithm: estimates the carbon count from the searched
 * m/z to model the 13C envelope, with heavy-isotope-aware upper bounds. Requires no formula
 * prediction and exposes every parameter of the detection run.
 */
public class CarbonAveragineAlgorithmModule implements IsotopeFinderAlgorithmModule {

  @Override
  public @NotNull String getName() {
    return "Carbon-averagine";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return CarbonAveragineAlgorithmParameters.class;
  }

  @Override
  public @NotNull List<Task> createTasks(@NotNull final MZmineProject project,
      @NotNull final ModularFeatureList[] featureLists, @NotNull final ParameterSet parameters,
      @NotNull final ParameterSet topParameters, @NotNull final Instant moduleCallDate) {
    // clone so a task never reads the live GUI/config instance while it is running
    final CarbonAveragineAlgorithmParameters algo = (CarbonAveragineAlgorithmParameters) parameters.cloneParameterSet();

    final List<Task> tasks = new ArrayList<>(featureLists.length);
    for (final ModularFeatureList featureList : featureLists) {
      tasks.add(new IsotopeFinderTask(project, featureList, topParameters, algo, getName(),
          moduleCallDate));
    }
    return tasks;
  }
}
