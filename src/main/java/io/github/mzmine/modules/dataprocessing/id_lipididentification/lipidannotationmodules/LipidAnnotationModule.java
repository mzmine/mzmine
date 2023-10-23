package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public abstract class LipidAnnotationModule implements MZmineProcessingModule {

  @Override
  public abstract @NotNull String getName();

  @Override
  public abstract @NotNull String getDescription();

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project, ParameterSet parameters,
      @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    FeatureList[] featureLists = parameters.getParameter(getFeatureListsParameter()).getValue()
        .getMatchingFeatureLists();

    for (FeatureList featureList : featureLists) {
      Task newTask = createAnnotationTask(parameters, featureList, moduleCallDate);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }

  @Override
  public abstract Class<? extends ParameterSet> getParameterSetClass();

  public abstract FeatureListsParameter getFeatureListsParameter();

  public abstract Task createAnnotationTask(ParameterSet parameters, FeatureList featureList,
      Instant moduleCallDate);
}