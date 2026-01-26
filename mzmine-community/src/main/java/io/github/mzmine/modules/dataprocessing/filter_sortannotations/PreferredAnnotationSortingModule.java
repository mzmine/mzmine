package io.github.mzmine.modules.dataprocessing.filter_sortannotations;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.TaskPerFeatureListModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreferredAnnotationSortingModule extends TaskPerFeatureListModule {

  public PreferredAnnotationSortingModule() {
    super("Sort preferred annotations", PreferredAnnotationSortingParameters.class,
        MZmineModuleCategory.FEATURELISTFILTERING, false,
        "Define how preferred annotations are sorted in a feature list.");
  }

  @Override
  public @NotNull Task createTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable MemoryMapStorage storage,
      @NotNull FeatureList featureList) {
    return new PreferredAnnotationSortingTask(storage, moduleCallDate,
        (PreferredAnnotationSortingParameters) parameters, this.getClass(), featureList);
  }
}
