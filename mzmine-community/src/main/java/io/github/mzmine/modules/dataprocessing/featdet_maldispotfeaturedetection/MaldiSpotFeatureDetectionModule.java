package io.github.mzmine.modules.dataprocessing.featdet_maldispotfeaturedetection;

import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MaldiSpotFeatureDetectionModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getName() {
    return "MALDI spot feature detection";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return MaldiSpotFeatureDetectionParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Detects features in MALDI spots. Only works for native timsTOF fleX data.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    final RawDataFile[] files = parameters.getParameter(MaldiSpotFeatureDetectionParameters.files)
        .getValue().getMatchingRawDataFiles();
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();
    for (RawDataFile file : files) {
      if (file instanceof IMSImagingRawDataFile imsImagingRawDataFile) {
        tasks.add(new MaldiSpotFeatureDetectionTask(storage, moduleCallDate, parameters, project,
            imsImagingRawDataFile));
      }
    }

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.EIC_DETECTION;
  }
}
