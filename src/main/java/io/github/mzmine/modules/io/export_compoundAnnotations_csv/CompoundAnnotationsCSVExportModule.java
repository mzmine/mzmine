package io.github.mzmine.modules.io.export_compoundAnnotations_csv;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collection;

public class CompoundAnnotationsCSVExportModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "Export CompoundAnnotations to CSV file";

    private static final String MODULE_DESCRIPTION =
            "This method exports the CompoundAnnotations into a CSV (comma-separated values) file.";

    @Override
    public @NotNull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @NotNull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
        return CompoundAnnotationsCSVExportParameters.class;
    }

    @Override
    public @NotNull ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
        CompoundAnnotationsCSVExportTask task = new CompoundAnnotationsCSVExportTask(parameters, moduleCallDate);
        tasks.add(task);
        return ExitCode.OK;
    }

    @Override
    public @NotNull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.FEATURELISTEXPORT;
    }
}
