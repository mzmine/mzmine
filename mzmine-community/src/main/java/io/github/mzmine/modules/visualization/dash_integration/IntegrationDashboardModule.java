package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.SimpleRunnableTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class IntegrationDashboardModule extends AbstractRunnableModule {

  public IntegrationDashboardModule() {
    super("Integration dashboard", IntegrationDashboardParameters.class,
        MZmineModuleCategory.VISUALIZATIONFEATURELIST,
        "Visualize and modify integrations across multiple raw files.");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    final SimpleRunnableTask task = new SimpleRunnableTask(() -> {
      final IntegrationDashboardTab tab = new IntegrationDashboardTab();
      tab.onFeatureListSelectionChanged(Arrays.asList(
          parameters.getValue(IntegrationDashboardParameters.flists).getMatchingFeatureLists()));
      ((MZmineGUI) DesktopService.getDesktop()).addTab(tab);
    });
    tasks.add(task);
    return ExitCode.OK;
  }
}
