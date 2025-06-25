package io.github.mzmine.modules.io.export_features_xml;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.TaskPerFeatureListModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExportFeaturesDataModule extends TaskPerFeatureListModule {

  public ExportFeaturesDataModule() {
    super("Export feature data", ExportFeaturesDataParameters.class,
        MZmineModuleCategory.FEATURELISTEXPORT, false,
        "Export the chromatogram data of features to an XML file.");
  }

  @Override
  public @NotNull Task createTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable MemoryMapStorage storage,
      @NotNull FeatureList featureList) {
    return new ExportFeaturesDataTask(storage, moduleCallDate, parameters, this.getClass(),
        (ModularFeatureList) featureList);
  }
}
