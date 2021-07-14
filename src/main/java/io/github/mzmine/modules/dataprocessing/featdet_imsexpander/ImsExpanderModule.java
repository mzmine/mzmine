package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImsExpanderModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getName() {
    return "Ims expander 2";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ImsExpanderParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Expands ims";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks) {

    final Integer numThreads = MZmineCore.getConfiguration().getPreferences()
        .getParameter(MZminePreferences.numOfThreads).getValue();
    final ModularFeatureList[] featureLists = parameters
        .getParameter(ImsExpanderParameters.featureLists).getValue().getMatchingFeatureLists();

    final int threadsPerFlist = Math.max(2, numThreads / Math.max(featureLists.length, 1));

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (ModularFeatureList featureList : featureLists) {
      tasks.add(new ImsExpanderTask(storage, parameters, featureList, project, threadsPerFlist));
    }

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.EIC_DETECTION;
  }
}
