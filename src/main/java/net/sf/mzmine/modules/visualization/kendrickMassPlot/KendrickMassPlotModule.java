package net.sf.mzmine.modules.visualization.kendrickMassPlot;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class KendrickMassPlotModule implements MZmineProcessingModule{

    private static final String MODULE_NAME = "Kendrick mass plot";
    private static final String MODULE_DESCRIPTION = "Kendrick mass plot."; 

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {
       
    	  Task newTask = new KendrickMassPlotTask(parameters);
          tasks.add(newTask);
      
          return ExitCode.OK;  
    }

   

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONPEAKLIST;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return KendrickMassPlotParameters.class;
    }
}
