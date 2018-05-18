package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.peaklistmethods.identification.formulapredictionpeaklist.FormulaPredictionPeakListParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.formulapredictionpeaklist.FormulaPredictionPeakListTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class MyModuleModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "MyModule";
    private static final String MODULE_DESCRIPTION = "This method gets the predicted formula for each unknown compound and compares with an isotope pattern";

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.IDENTIFICATION;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return MyModuleParameters.class;
    }

    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    public @Nonnull ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {
        PeakList peakLists[] = parameters
                .getParameter(MyModuleParameters.PEAK_LISTS)
                .getValue().getMatchingPeakLists();

        for (PeakList peakList : peakLists) {
            Task newTask = new MyModuleTask(project, peakList,
                    parameters);
            tasks.add(newTask);
        }
        return ExitCode.OK;
    }

}
