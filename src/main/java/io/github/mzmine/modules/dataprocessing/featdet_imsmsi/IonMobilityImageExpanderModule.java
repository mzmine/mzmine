package io.github.mzmine.modules.dataprocessing.featdet_imsmsi;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IonMobilityImageExpanderModule implements
    MZmineProcessingModule {

  @Nonnull
  @Override
  public String getName() {
    return "Ion mobility image expander";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return IonMobilityImageExpanderParameters.class;
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "Expands images into the mobility dimension";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    for (ModularFeatureList matchingFeatureList : parameters
        .getParameter(IonMobilityImageExpanderParameters.featureLists).getValue()
        .getMatchingFeatureLists()) {
      tasks.add(new IonMobilityImageExpanderTask(project, parameters, matchingFeatureList));
    }

    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return null;
  }
}
