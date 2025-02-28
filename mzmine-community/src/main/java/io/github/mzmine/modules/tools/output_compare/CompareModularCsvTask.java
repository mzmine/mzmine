package io.github.mzmine.modules.tools.output_compare;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.io.CSVUtils;
import io.github.mzmine.util.io.CsvReader;
import java.io.File;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;

public class CompareModularCsvTask extends AbstractSimpleToolTask {

  public CompareModularCsvTask(final @NotNull Instant moduleCallDate,
      final @NotNull ParameterSet parameters) {
    super(moduleCallDate, parameters);
  }

  @Override
  protected void process() {
    final File baseFile = parameters.getValue(CompareModularCsvParameters.baseFile);
    final File compareFile = parameters.getValue(CompareModularCsvParameters.compareFile);

    MZmineModularCsv.parseFile(baseFile)
    MZmineModularCsv.parseFile(baseFile)
    compareCsvFiles(baseFile, compareFile);
  }

  private void compareCsvFiles(final File baseFile, final File compareFile) {

  }

  @Override
  public String getTaskDescription() {
    return "";
  }
}
