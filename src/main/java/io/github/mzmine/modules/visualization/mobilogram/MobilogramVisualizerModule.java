package io.github.mzmine.modules.visualization.mobilogram;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobilogramVisualizerModule implements MZmineRunnableModule {

  @Nonnull
  @Override
  public String getDescription() {
    return "Visualizes mobilograms of frames";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project,
      @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    MobilogramVisualizerTab tab = new MobilogramVisualizerTab();
    RawDataFile[] files = parameters.getParameter(MobilogramVisualizerParameters.rawFiles)
        .getValue().getMatchingRawDataFiles();
    MZmineCore.getDesktop().addTab(tab);
    tab.onRawDataFileSelectionChanged(Arrays.asList(files));

    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Nonnull
  @Override
  public String getName() {
    return "Mobilogram visualizer";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return MobilogramVisualizerParameters.class;
  }
}
