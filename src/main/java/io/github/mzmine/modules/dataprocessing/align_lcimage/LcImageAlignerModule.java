package io.github.mzmine.modules.dataprocessing.align_lcimage;

import io.github.mzmine.datamodel.MZmineProject;
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

public class LcImageAlignerModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getName() {
    return "LC-Image Aligner";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return LcImageAlignerParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Aligns LC and imaging measurements based on m/z and mobility.\nImages are aligned to "
        + "all LC features that match, only the best match is retained.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    tasks.add(new LcImageAlignerTask(storage, moduleCallDate, parameters, project));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ALIGNMENT;
  }
}
