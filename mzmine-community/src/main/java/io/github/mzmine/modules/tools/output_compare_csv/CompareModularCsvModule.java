package io.github.mzmine.modules.tools.output_compare_csv;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractRunnableModule;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class CompareModularCsvModule extends AbstractRunnableModule {

  public CompareModularCsvModule() {
    final String supported = String.join(", ", CSVExportModularModule.MODULE_NAME);
    super("Compare mzmine CSV output", CompareModularCsvParameters.class,
        MZmineModuleCategory.TOOLS, """
            Compares two csv files exported from mzmine. Supported files are from those modules:
            %s""".formatted(supported));
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    tasks.add(new CompareModularCsvTask(moduleCallDate, parameters));
    return ExitCode.OK;
  }
}
