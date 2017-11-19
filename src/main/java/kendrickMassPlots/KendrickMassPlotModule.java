package kendrickMassPlots;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotModule;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotParameters;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotWindow;
import net.sf.mzmine.modules.visualization.intensityplot.ParameterWrapper;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsSelectionType;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class KendrickMassPlotModule implements MZmineRunnableModule{

    private static final String MODULE_NAME = "Kendrick mass plot";
    private static final String MODULE_DESCRIPTION = "Kendrick mass plot."; // TODO

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
        KendrickMassPlotWindow newFrame = new KendrickMassPlotWindow(parameters);
        newFrame.setVisible(true);
        return ExitCode.OK;
    }

    public static void showKendrickMassPlot(@Nonnull MZmineProject project,
            PeakList peakList, PeakListRow rows[]) {

        ParameterSet parameters = MZmineCore.getConfiguration()
                .getModuleParameters(KendrickMassPlotModule.class);

        parameters.getParameter(KendrickMassPlotParameters.peakList).setValue(
                PeakListsSelectionType.SPECIFIC_PEAKLISTS,
                new PeakList[] { peakList });

        parameters.getParameter(KendrickMassPlotParameters.dataFiles)
                .setChoices(peakList.getRawDataFiles());

        parameters.getParameter(KendrickMassPlotParameters.dataFiles)
                .setValue(peakList.getRawDataFiles());

        parameters.getParameter(KendrickMassPlotParameters.selectedRows)
                .setValue(rows);

        UserParameter<?, ?> projectParams[] = project.getParameters();
       

        ExitCode exitCode = parameters.showSetupDialog(null, true);

        if (exitCode == ExitCode.OK) {
            PeakListRow selectedRows[] = parameters
                    .getParameter(KendrickMassPlotParameters.selectedRows)
                    .getMatchingRows(peakList);
            if (selectedRows.length == 0) {
                MZmineCore.getDesktop().displayErrorMessage(null,
                        "No rows selected");
                return;
            }

            KendrickMassPlotWindow newFrame = new KendrickMassPlotWindow(
                    parameters.cloneParameterSet());
            newFrame.setVisible(true);
        }

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
