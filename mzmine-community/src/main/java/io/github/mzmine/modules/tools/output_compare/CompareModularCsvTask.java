package io.github.mzmine.modules.tools.output_compare;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.modules.tools.output_compare.MZmineModularCsv.ColumnData;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import io.github.mzmine.util.objects.ObjectUtils;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class CompareModularCsvTask extends AbstractSimpleToolTask {

  private static final Logger logger = Logger.getLogger(CompareModularCsvTask.class.getName());
  private final String baseName = "Base table";
  private final String compareName = "Compare table";

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
    // only potential issues are listed
    List<String> warnings = new ArrayList<>();

    // log general info for tables
    logBaseInformation(info, baseName, baseTab);
    logBaseInformation(info, compareName, compareTab);

    // different num rows
    if (baseTab.numRows() == compareTab.numRows()) {
      info.add("Same number of rows in both tables");
    } else {
      warnings.add("Different number of rows in both tables: %d to %d".formatted(baseTab.numRows(),
          compareTab.numRows()));
    }

    // compare sorting - it might be different
    // TODO sort by rowID? or by mz, rt, mobility? not all tables have all
    // apply sorting to all columns by argsort
    // similar to CollectionUtils.argsortReversed()
//    compareAndFixSorting

    // first compare row types
    compareTypeColumns("row types", info, warnings, baseTab.pairRowColumns(compareTab));

    // then compare feature types per sample
    compareTypeColumns("feature types", info, warnings, baseTab.pairFeatureColumns(compareTab));

    // log all info
    logger.info("""
        CSV table comparison:
        %s
        
        Warnings:
        %s""".formatted(String.join("\n", info), String.join("\n", warnings)));
  }

  private void logBaseInformation(final List<String> info, final String name,
      final MZmineModularCsv tab) {
    final List<DataType> rowTypes = tab.getRowTypes();
    final List<DataType> featureTypes = tab.getFeatureTypes();
    info.add(
        "Info for %s with %d columns and %d rows, %d row types, %d feature types".formatted(name,
            tab.numColumns(), tab.numRows(), rowTypes.size(), featureTypes.size()));
    info.add("Row types: %s".formatted(
        rowTypes.stream().map(DataType::getUniqueID).collect(Collectors.joining(", "))));
    info.add("Feature types: %s".formatted(
        featureTypes.stream().map(DataType::getUniqueID).collect(Collectors.joining(", "))));
    info.add("Raw files: %s".formatted(String.join(", ", tab.getUniqueRawFiles())));
    info.add("\n");
  }

  private void compareTypeColumns(final String columnDefinition, final List<String> info,
      final List<String> warnings, final List<ColumnData[]> grouped) {

    // general comparison
    info.add("\nComparing columns for %s".formatted(columnDefinition));

    // singles are only present in one table pairs in both
    final List<ColumnData> singles = grouped.stream()
        .filter(p -> ObjectUtils.countNonNull((Object[]) p) == 1)
        .map(p -> p[0] != null ? p[0] : p[1]).toList();
    final List<ColumnData[]> pairs = grouped.stream()
        .filter(p -> ObjectUtils.countNonNull((Object[]) p) == 2).toList();

    if (singles.isEmpty()) {
      info.add("Num %s columns are %d and equal for both tables:".formatted(columnDefinition,
          pairs.size()));
    } else {
      // list all single columns that will be skipped
      warnings.add("Downstream analysis will skip %d columns: %s".formatted(singles.size(),
          singles.stream().map(col -> col.col().type()).filter(Objects::nonNull)
              .map(DataType::getUniqueID).collect(Collectors.joining(", "))));

      // add info
      final long colsA = grouped.stream().filter(g -> g[0] != null).count();
      final long colsB = grouped.stream().filter(g -> g[1] != null).count();

      info.add("Num %s columns are %d and %d:".formatted(columnDefinition, colsA, colsB));
    }

    // compare data values
    compareData(pairs, info, warnings);
  }

  private void compareData(final List<ColumnData[]> pairs, final List<String> info,
      final List<String> warnings) {
    for (final ColumnData[] pair : pairs) {
      final ColumnData base = pair[0];
      final ColumnData compare = pair[1];

      base.checkEqual(compare, warnings);
    }
  }


  @Override
  public String getTaskDescription() {
    return "";
  }
}
