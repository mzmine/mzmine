package io.github.mzmine.modules.dataprocessing.id_cliquems;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import javax.annotation.Nonnull;

public class CliqueMSModule implements MZmineProcessingModule {
  // Name and description.
    public static final String MODULE_NAME = "CliqueMS annotation";
    private static final String MODULE_DESCRIPTION =
        "This method groups features and annotates the groups/cliques using CliqueMS algorithm.";

    @Override
    public @Nonnull
    String getName() {
      return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
      return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
        @Nonnull Collection<Task> tasks) {

      PeakList peakLists[] = parameters.getParameter(CliqueMSParameters.PEAK_LISTS).getValue()
          .getMatchingPeakLists();

      for (PeakList peakList : peakLists) {
        Task newTask = new CliqueMSTask(project, parameters, peakList);
        tasks.add(newTask);
      }

      return ExitCode.OK;
    }

    @Override
    public @Nonnull
    MZmineModuleCategory getModuleCategory() {
      return MZmineModuleCategory.IDENTIFICATION;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
      return CliqueMSParameters.class;
    }

}
