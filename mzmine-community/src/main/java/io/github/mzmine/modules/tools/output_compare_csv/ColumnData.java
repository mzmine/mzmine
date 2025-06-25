package io.github.mzmine.modules.tools.output_compare_csv;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import io.github.mzmine.modules.tools.output_compare_csv.MZmineModularCsv.Column;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.maths.Precision;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ColumnData in a ModularCSV
 *
 * @param <T> value types
 */
sealed interface ColumnData<T> {

  record StringColumnData(Column col, List<String> data) implements ColumnData<String> {

  }

  record NumberColumnData(Column col, List<Double> data) implements ColumnData<Double> {

    @Override
    public boolean checkEqualValue(@NotNull final Double a, @NotNull final Double b) {
      return Precision.equalDoubleSignificance(a, b); // allow a bit if variance in output
    }
  }

  // creation

  static ColumnData create(Column col, List<String> data) {
    if (col.type() instanceof NumberType<?>) {
      return new NumberColumnData(col,
          data.stream().map(s -> StringUtils.parseDoubleOrElse(s, null)).toList());
    }
    return new StringColumnData(col, data);
  }

  /**
   * @param idPair
   * @param other     other column to compare
   * @param maxErrors maximum errors to collect before ending. -1 for unlimited issues
   * @return list of issues
   */
  default List<CheckResult> checkEqual(final @Nullable ColumnData[] idPair, final ColumnData other,
      int maxErrors) {
    List<CheckResult> results = new ArrayList<>();
    final var col = col();

    if (!getTypeDescription().equals(other.getTypeDescription())) {
      results.add(CheckResult.create(col.header(), Severity.ERROR, col.type(), getTypeDescription(),
          other.getTypeDescription(),
          "Column types do not equal. This was %s and other was %s".formatted(getTypeDescription(),
              other.getTypeDescription())));
      return results;
    }

    // max rows
    int numRows = Math.max(numRows(), other.numRows());

    for (int row = 0; row < numRows; row++) {
      // get value is safe for out of index --> null
      final T a = getValue(row);
      final T b;
      try {
        b = (T) other.getValue(row);
      } catch (Exception e) {
        results.add(
            CheckResult.create(col.header(), Severity.ERROR, col.type(), getTypeDescription(),
                other.getTypeDescription(),
                "%sColumn types do not equal - conversion of value failed. This was %s and other was %s".formatted(
                    getRowDescription(idPair, row), getTypeDescription(),
                    other.getTypeDescription())));
        return results;
      }
      if (a == null && b == null) {
        continue;
      } else if (a == null) {
        results.add(CheckResult.create(col.header(), Severity.ERROR, col.type(), "null", b,
            rowValueUnequalMessage(idPair, row)));
      } else if (b == null) {
        results.add(CheckResult.create(col.header(), Severity.ERROR, col.type(), a, "null",
            rowValueUnequalMessage(idPair, row)));
      } else if (!checkEqualValue(a, b)) {
        results.add(CheckResult.create(col.header(), Severity.ERROR, col.type(), a, b,
            rowValueUnequalMessage(idPair, row)));
      }
      if (maxErrors >= 0 && results.size() > maxErrors) {
        return results;
      }
    }
    return results;
  }

  private @NotNull String rowValueUnequalMessage(final @Nullable ColumnData[] idPair,
      final int row) {
    return getRowDescription(idPair, row) + getTypeDescription() + " value does not equal";
  }

  private @NotNull String getRowDescription(@Nullable ColumnData[] idPair, int row) {
    if (idPair == null) {
      return "Row %d: ".formatted(row);
    }
    Object id1 = requireNonNullElse(idPair[0].getValue(row), "null");
    Object id2 = requireNonNullElse(idPair[1].getValue(row), "null");
    // read as double
    if (id1 instanceof Number n) {
      id1 = n.intValue();
    }
    if (id2 instanceof Number n) {
      id2 = n.intValue();
    }

    if (Objects.equals(id1, id2)) {
      return "Row %d with ID %s: ".formatted(row, id1);
    }
    return "Row %d with ID %s and %s: ".formatted(row, id1, id2);
  }

  Column col();

  List<T> data();

  default int numRows() {
    return data().size();
  }

  default String getIdentifier() {
    return col().type() != null ? col().type().getUniqueID() : col().uniqueTypeId();
  }

  default String getTypeDescription() {
    return switch (this) {
      case ColumnData.StringColumnData _ -> "String";
      case ColumnData.NumberColumnData _ -> "Number";
    };
  }

  default boolean checkEqualValue(@NotNull T a, @NotNull T b) {
    return Objects.equals(a, b);
  }

  default T getValue(int row) {
    if (row < 0 || row >= numRows()) {
      return null;
    }
    return data().get(row);
  }


}
