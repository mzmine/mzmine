package io.github.mzmine.modules.visualization.kendrickmassplot.regionextraction;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KendrickRegionExtractionModule extends AbstractProcessingModule {

  public KendrickRegionExtractionModule() {
    super("Region extraction (Kendrick and more)", KendrickRegionExtractionParameters.class,
        MZmineModuleCategory.FEATURELISTFILTERING,
        "Extract a region from a multidimensional plot created using the Kendrick plot module.");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    tasks.add(new KendrickRegionExtractionTask(parameters, project, moduleCallDate));

    return ExitCode.OK;
  }
}
