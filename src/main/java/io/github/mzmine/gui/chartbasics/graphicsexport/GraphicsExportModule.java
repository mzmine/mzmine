package io.github.mzmine.gui.chartbasics.graphicsexport;

import java.util.Collection;
import org.jfree.chart.JFreeChart;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

public class GraphicsExportModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "Graphics Export";
  private static final String MODULE_DESCR = "Exports a plot to a file.";
  
  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return GraphicsExportParameters.class;
  }

  @Override
  public String getDescription() {
    return null;
  }

  public ExitCode openDialog(JFreeChart chart, ParameterSet parameters) {
    return ((GraphicsExportParameters)parameters).showSetupDialog(true, chart);
  }

  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.TOOLS;
  }

  @Override
  public ExitCode runModule(MZmineProject project, ParameterSet parameters,
      Collection<Task> tasks) {
    // TODO Auto-generated method stub
    return null;
  }

}
