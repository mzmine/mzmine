package io.github.mzmine.modules.tools.output_compare_csv;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.iin.PartnerIdsType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import io.github.mzmine.util.objects.ObjectUtils;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompareModularCsvTask extends AbstractSimpleToolTask {

  private static final Logger logger = Logger.getLogger(CompareModularCsvTask.class.getName());
  private final String baseName = "Base table";
  private final String compareName = "Compare table";


  // describe why it is ok that types do not equal after processing
  private final Set<DataType> excludedTypes = Set.copyOf(DataTypes.getAll(
      // community detection has some randomness and does not number the communities the same order
      // actually managed to get stable community ids by sorting nodes during their creation.
      // networks require same order of nodes and edges - but hashmaps were unordered before
//      MolNetCommunityIdType.class, MolNetCommunitySizeType.class,
      // parner ids in ion identity are not ordered. This could be changed but maybe better to remove this all together later
      PartnerIdsType.class));

  private final List<CheckResult> checks = new ArrayList<>();

  public CompareModularCsvTask(final @NotNull Instant moduleCallDate,
      final @NotNull ParameterSet parameters) {
    super(moduleCallDate, parameters);
  }

  @Override
  protected void process() {
    final File baseFile = parameters.getValue(CompareModularCsvParameters.baseFile);
    final File compareFile = parameters.getValue(CompareModularCsvParameters.compareFile);

    checks.add(CheckResult.create("file paths", Severity.INFO, baseFile.getAbsolutePath(),
        compareFile.getAbsolutePath(), "Modular CSV file paths"));
    checks.add(CheckResult.create("file names", Severity.INFO, baseFile.getName(),
        compareFile.getAbsolutePath(), "Modular CSV file names"));

    final MZmineModularCsv baseTab = MZmineModularCsv.parseFile(baseFile);
    final MZmineModularCsv compareTab = MZmineModularCsv.parseFile(compareFile);

    compareTablesAndLog(baseTab, compareTab);
  }

  public void compareTablesAndLog(final MZmineModularCsv baseTab,
      final MZmineModularCsv compareTab) {
    if (baseTab == null || compareTab == null) {
      checks.add(
          CheckResult.create("file parsing", Severity.ERROR, baseTab == null, compareTab == null,
              "Failed to parse file (true failed, false success)"));
      return;
    }

    // compare and add results to checks
    compareTables(baseTab, compareTab);

    // filter checks
    final Severity filter = parameters.getValue(CompareModularCsvParameters.filterLevel);
    final File outFile = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        CompareModularCsvParameters.outFile, null);

    CheckResultWriter.filterAndLogResults(filter, outFile, checks);
  }

  private void compareTables(final MZmineModularCsv baseTab, final MZmineModularCsv compareTab) {
    // log general info for tables
    logBaseInformation(baseName, baseTab);
    logBaseInformation(compareName, compareTab);

    // different num rows
    if (baseTab.numRows() == compareTab.numRows()) {
      checks.add(CheckResult.create("rows", Severity.INFO, baseTab.numRows(), compareTab.numRows(),
          "Same number of rows"));
    } else {
      checks.add(CheckResult.create("rows", Severity.ERROR, baseTab.numRows(), compareTab.numRows(),
          "Different Number of rows in both tables"));
    }

    // compare sorting - it might be different
    // TODO sort by rowID? or by mz, rt, mobility? not all tables have all
    // apply sorting to all columns by argsort
    // similar to CollectionUtils.argsortReversed()
//    compareAndFixSorting

    // pair row columns and find id column
    final List<ColumnData[]> rowPairs = baseTab.pairRowColumns(compareTab);

    final String uniqueID = new IDType().getUniqueID();
    final ColumnData[] idPair = findIdColumn(rowPairs);
    if (idPair == null) {
      checks.add(CheckResult.create("ID column missing", Severity.ERROR,
          "No row ID columns found. Should have header %s".formatted(uniqueID)));
    }

    // first compare row types
    compareTypeColumns("row types", rowPairs, idPair);

    // then compare feature types per sample
    compareTypeColumns("feature types", baseTab.pairFeatureColumns(compareTab), idPair);
  }

  private void logBaseInformation(final String name, final MZmineModularCsv tab) {
    final List<DataType> rowTypes = tab.getRowTypes();
    final List<DataType> featureTypes = tab.getFeatureTypes();
    final String summary = "Info for %s with %d columns and %d rows, %d row types, %d feature types, %d samples".formatted(
        name, tab.numColumns(), tab.numRows(), rowTypes.size(), featureTypes.size(),
        tab.getUniqueRawFiles().size());

    checks.add(CheckResult.create(name + ": summary", Severity.INFO, summary));
    checks.add(CheckResult.create(name + ": row types", Severity.INFO,
        rowTypes.stream().map(DataType::getUniqueID).collect(Collectors.joining(", "))));
    checks.add(CheckResult.create(name + ": feature types", Severity.INFO,
        featureTypes.stream().map(DataType::getUniqueID).collect(Collectors.joining(", "))));
    checks.add(CheckResult.create(name + ": raw files", Severity.INFO,
        String.join(", ", tab.getUniqueRawFiles())));
  }

  private void compareTypeColumns(final String columnDefinition, final List<ColumnData[]> grouped,
      final ColumnData @Nullable [] idPair) {
    // singles are only present in one table pairs in both
    final List<ColumnData> singles = grouped.stream()
        .filter(p -> ObjectUtils.countNonNull((Object[]) p) == 1)
        .map(p -> p[0] != null ? p[0] : p[1]).toList();
    final List<ColumnData[]> pairs = grouped.stream()
        .filter(p -> ObjectUtils.countNonNull((Object[]) p) == 2).toList();

    // add info
    final long colsA = grouped.stream().filter(g -> g[0] != null).count();
    final long colsB = grouped.stream().filter(g -> g[1] != null).count();

    if (singles.isEmpty()) {
      checks.add(CheckResult.create(columnDefinition + ": num columns", Severity.INFO, colsA, colsB,
          "equal number of columns"));
    } else {
      // add info
      checks.add(CheckResult.create(columnDefinition + ": num columns", Severity.WARN, colsA, colsB,
          "unequal number of columns"));

      // list all single columns that will be skipped
      checks.add(CheckResult.create("skip %s columns".formatted(columnDefinition), Severity.WARN,
          "Downstream analysis will skip %d columns: %s".formatted(singles.size(),
              singles.stream().map(ColumnData::getIdentifier).collect(Collectors.joining(", ")))));
    }

    // compare data values
    compareData(pairs, idPair);
  }

  private void compareData(final List<ColumnData[]> pairs, final ColumnData[] idPair) {
    for (final ColumnData[] pair : pairs) {
      final ColumnData base = pair[0];
      final ColumnData compare = pair[1];

      // skip columns that are known to be unstable in their values
      if (excludedTypes.contains(base.col().type())) {
        continue;
      }

      checks.addAll(base.checkEqual(idPair, compare, 10));
    }
  }

  private static ColumnData @Nullable [] findIdColumn(final List<ColumnData[]> pairs) {
    final String uniqueID = new IDType().getUniqueID();
    return pairs.stream().filter(p -> p[0] != null && Objects.equals(p[0].col().header(), uniqueID))
        .findFirst().orElse(null);
  }


  @Override
  public String getTaskDescription() {
    return "Comparing two modular CSV output files";
  }

  public List<CheckResult> getChecks() {
    return checks;
  }
}
