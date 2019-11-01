package net.sf.mzmine.modules.peaklistmethods.filtering.blanksubtraction;

import java.util.Collection;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class PeakListBlankSubtractionModule implements MZmineRunnableModule {

  @Override
  public String getName() {
    return "Peak list blank subtraction";
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return PeakListBlankSubtractionParameters.class;
  }

  @Override
  public String getDescription() {
    return "Subtracts a blank measurements peak list from another peak list.";
  }

  @Override
  public ExitCode runModule(MZmineProject project, ParameterSet parameters,
      Collection<Task> tasks) {
    
    Task task = new PeakListBlankSubtractionMasterTask(project,  (PeakListBlankSubtractionParameters) parameters);
    
    tasks.add(task);
    
    return ExitCode.OK;
  }

  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.PEAKLISTFILTERING;
  }

}
