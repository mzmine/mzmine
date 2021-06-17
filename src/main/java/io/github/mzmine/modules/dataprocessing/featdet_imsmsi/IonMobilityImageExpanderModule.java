package io.github.mzmine.modules.dataprocessing.featdet_imsmsi;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IonMobilityImageExpanderModule implements
    MZmineProcessingModule {

  @NotNull
  @Override
  public String getName() {
    return "Ion mobility image expander";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return IonMobilityImageExpanderParameters.class;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Expands images into the mobility dimension";
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks) {

    for (ModularFeatureList matchingFeatureList : parameters
        .getParameter(IonMobilityImageExpanderParameters.featureLists).getValue()
        .getMatchingFeatureLists()) {
      tasks.add(new IonMobilityImageExpanderTask(project, parameters, matchingFeatureList,
          MemoryMapStorage.forFeatureList()));
    }

    return ExitCode.OK;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.EIC_DETECTION;
  }
}
