package io.github.mzmine.modules.tools.output_compare;

import io.github.mzmine.modules.tools.output_compare.MZmineModularCsv.ColumnData;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class CompareModularCsvTask extends AbstractSimpleToolTask {

  private static final Logger logger = Logger.getLogger(CompareModularCsvTask.class.getName());

  public CompareModularCsvTask(final @NotNull Instant moduleCallDate,
      final @NotNull ParameterSet parameters) {
    super(moduleCallDate, parameters);
  }

  @Override
  protected void process() {
    final File baseFile = parameters.getValue(CompareModularCsvParameters.baseFile);
    final File compareFile = parameters.getValue(CompareModularCsvParameters.compareFile);

    final MZmineModularCsv baseTab = MZmineModularCsv.parseFile(baseFile);
    final MZmineModularCsv compareTab = MZmineModularCsv.parseFile(compareFile);
    compareTables(baseTab, compareTab);
  }

  private void compareTables(final MZmineModularCsv baseTab, final MZmineModularCsv compareTab) {
    List<String> info = new ArrayList<>();

    // check the same order and general size
    int colsA = baseTab.numColumns();
    int colsB = compareTab.numColumns();
    info.add("Num columns %d and %d are %s".formatted(colsA, colsB,
        colsA == colsB ? "equal" : "not equal"));

    final List<ColumnData[]> pairs = baseTab.pairColumns(compareTab);
    if (colsA != colsB) {

    }

    List<String> warnings = new ArrayList<>();
    baseTab.checkEqual(compareTab, warnings);

    if (warnings.isEmpty()) {
      logger.info("Tables are the same");
    } else {
      logger.info("""
          Tables are different:
          %s""".formatted(String.join("\n", warnings)));
    }
  }


  @Override
  public String getTaskDescription() {
    return "";
  }
}
