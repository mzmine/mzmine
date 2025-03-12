package io.github.mzmine.modules.tools.output_compare;

import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.modules.tools.output_compare.DataCheckResult.Severity;
import io.github.mzmine.modules.tools.output_compare.MZmineModularCsv.Column;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.maths.Precision;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

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
   * @param other     other column to compare
   * @param maxErrors maximum errors to collect before ending. -1 or 0 for unlimited issues
   * @return list of issues
   */
  default List<DataCheckResult> checkEqual(final ColumnData other, int maxErrors) {
    List<DataCheckResult> results = new ArrayList<>();
    final var col = col();

    if (!getTypeDescription().equals(other.getTypeDescription())) {
      results.add(
          DataCheckResult.create(col.type(), Severity.ERROR, col.header(), getTypeDescription(),
              other.getTypeDescription(),
              "Column types do not equal. This was %s and other was %s".formatted(
                  getTypeDescription(), other.getTypeDescription())));
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
            DataCheckResult.create(col.type(), Severity.ERROR, col.header(), getTypeDescription(),
                other.getTypeDescription(),
                "Column types do not equal - conversion of value failed. This was %s and other was %s".formatted(
                    getTypeDescription(), other.getTypeDescription())));
        return results;
      }
      if (a == null && b == null) {
        continue;
      } else if (a == null) {
        results.add(DataCheckResult.create(col.type(), Severity.ERROR, col.header(), "null", b,
            getTypeDescription() + " value does not equal"));
      } else if (b == null) {
        results.add(DataCheckResult.create(col.type(), Severity.ERROR, col.header(), a, "null",
            getTypeDescription() + " value does not equal"));
      } else if (!checkEqualValue(a, b)) {
        results.add(DataCheckResult.create(col.type(), Severity.ERROR, col.header(), a, b,
            getTypeDescription() + " value does not equal"));
      }
      if (maxErrors > 0 && results.size() >= maxErrors) {
        return results;
      }
    }
    return results;
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
      case StringColumnData _ -> "String";
      case NumberColumnData _ -> "Number";
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
