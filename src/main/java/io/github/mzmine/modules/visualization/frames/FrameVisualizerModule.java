package io.github.mzmine.modules.visualization.frames;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrameVisualizerModule implements MZmineRunnableModule {

  @Override
  public @NotNull String getName() {
    return "Frame visualizer";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return FrameVisualizerParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Visualizes frames from ion mobility raw data files.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    final RawDataFile file = parameters.getValue(FrameVisualizerParameters.files)
        .getMatchingRawDataFiles()[0];
    if (file instanceof IMSRawDataFile) {
      final FrameVisualizerTab tab = new FrameVisualizerTab("IMS Frame visualizer", true);
      tab.onRawDataFileSelectionChanged(List.of(file));
      MZmineCore.getDesktop().addTab(tab);
    } else {
      MZmineCore.getDesktop()
          .displayMessage(file.getName() + "is not an IMS file. Cannot run frame visualizer.");
    }
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }
}
