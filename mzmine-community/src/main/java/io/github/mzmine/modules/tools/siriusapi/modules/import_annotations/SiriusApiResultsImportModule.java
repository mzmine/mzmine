package io.github.mzmine.modules.tools.siriusapi.modules.import_annotations;

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

public class SiriusApiResultsImportModule extends TaskPerFeatureListModule {

  public SiriusApiResultsImportModule() {
    super("Import results from Sirius project (API)", SiriusApiResultsImportParameters.class,
        MZmineModuleCategory.ANNOTATION, false, "Import results from a Sirius project");
  }

  @Override
  public @NotNull Task createTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable MemoryMapStorage storage,
      @NotNull FeatureList featureList) {
    return new SiriusApiResultsImportTask(moduleCallDate, parameters, featureList);
  }
}
