package io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RecursiveIMSBuilderModule implements MZmineProcessingModule {

  @Nonnull
  @Override
  public String getName() {
    return "IMS Builder";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return RecursiveIMSBuilderParameters.class;
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "Builds m/z traces for ion mobility spectrometry data";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (RawDataFile rawDataFile : parameters
        .getParameter(RecursiveIMSBuilderParameters.rawDataFiles).getValue().getMatchingRawDataFiles()) {
      if (rawDataFile instanceof IMSRawDataFile) {
        tasks.add(new RecursiveIMSBuilderTask(storage, (IMSRawDataFile) rawDataFile, parameters, project));
      }
    }
    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.EIC_DETECTION;
  }
}
