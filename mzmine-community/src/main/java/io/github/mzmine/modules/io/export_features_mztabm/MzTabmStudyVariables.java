package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lifstools.mztab2.model.Assay;
import org.lifstools.mztab2.model.Parameter;
import org.lifstools.mztab2.model.StudyVariable;

/**
 * Maps the selected raw data metadata grouping column to exported study variables.
 */
final class MzTabmStudyVariables {

  /**
   * Utility class.
   */
  private MzTabmStudyVariables() {
  }

  /**
   * Groups exported assays by the selected project column, retaining files with missing values.
   *
   * @param metadata metadata belonging to the exported project
   * @param assays exported raw files and their assays, in export order
   * @param selectedColumn metadata column selected for raw data grouping, or null for no grouping
   * @return study variables in numeric then deterministic text order, with missing values last
   */
  static @NotNull List<StudyVariable> create(@NotNull final MetadataTable metadata,
      @NotNull final Map<RawDataFile, Assay> assays,
      @Nullable final MetadataColumn<?> selectedColumn) {
    if (assays.isEmpty()) {
      return List.of();
    }
    if (selectedColumn == null) {
      return List.of(createVariable(1, null, null, new ArrayList<>(assays.values()),
          "All assays; no metadata grouping column is selected"));
    }
    // decision: Resolve the selection against the owning project rather than another active project.
    final MetadataColumn<?> column = metadata.getColumnByName(selectedColumn.getTitle());
    if (column == null) {
      return List.of(createVariable(1, null, null, new ArrayList<>(assays.values()),
          "All assays; selected metadata column " + selectedColumn.getTitle()
              + " is unavailable in the project"));
    }
    final Map<Object, List<Assay>> grouped = new LinkedHashMap<>();
    for (final Map.Entry<RawDataFile, Assay> entry : assays.entrySet()) {
      final Object value = metadata.getValue(column, entry.getKey());
      // decision: A zero value is a real group; only absent or blank values are missing.
      final Object factor = value instanceof String text && text.isBlank() ? null : value;
      grouped.computeIfAbsent(factor, ignored -> new ArrayList<>()).add(entry.getValue());
    }
    final List<Map.Entry<Object, List<Assay>>> groups = new ArrayList<>(grouped.entrySet());
    groups.sort((left, right) -> compareFactors(left.getKey(), right.getKey()));
    final List<StudyVariable> variables = new ArrayList<>();
    for (final Map.Entry<Object, List<Assay>> group : groups) {
      final Object factor = group.getKey();
      final String description = factor == null
          ? "Assays without a " + column.getTitle() + " metadata value"
          : column.getTitle() + " = " + factor;
      variables.add(createVariable(variables.size() + 1, column, factor, group.getValue(), description));
    }
    return variables;
  }

  /**
   * Declares the grouping factor and the statistics used for its corresponding abundance cells.
   *
   * @param id within-file study-variable identifier
   * @param column selected metadata column, or null when no column is available
   * @param factor actual metadata value, or null for the undefined group
   * @param assays assays belonging to this group
   * @param description human-readable grouping provenance
   * @return study variable containing the original factor and assay references
   */
  private static @NotNull StudyVariable createVariable(final int id,
      @Nullable final MetadataColumn<?> column, @Nullable final Object factor,
      @NotNull final List<Assay> assays, @NotNull final String description) {
    final String value = factor == null ? null : factor.toString();
    final StudyVariable variable = new StudyVariable().id(id)
        .name(value == null ? "undefined" : value).description(description)
        .assayRefs(new ArrayList<>(assays))
        .averageFunction(new Parameter().cvLabel("MS").cvAccession("MS:1002962").name("mean"))
        .variationFunction(new Parameter().cvLabel("MS").cvAccession("MS:1002963")
            .name("variation coefficient"));
    if (value != null && column != null) {
      // decision: Explicit factors preserve a genuine single-group name in mzTab-M.
      variable.addFactorsItem(new Parameter().name(column.getTitle()).value(value));
    }
    return variable;
  }

  /**
   * Orders numeric factors naturally, with a stable order for other values and missing data last.
   *
   * @param left first factor value
   * @param right second factor value
   * @return ordering of the factor values
   */
  private static int compareFactors(@Nullable final Object left, @Nullable final Object right) {
    if (left == null) {
      return right == null ? 0 : 1;
    }
    if (right == null) {
      return -1;
    }
    final BigDecimal leftNumber = numericValue(left);
    final BigDecimal rightNumber = numericValue(right);
    if (leftNumber != null && rightNumber != null) {
      final int numericOrder = leftNumber.compareTo(rightNumber);
      if (numericOrder != 0) {
        return numericOrder;
      }
    } else if (leftNumber != null) {
      return -1;
    } else if (rightNumber != null) {
      return 1;
    }
    final int typeOrder = left.getClass().getName().compareTo(right.getClass().getName());
    return typeOrder == 0 ? left.toString().compareTo(right.toString()) : typeOrder;
  }

  /**
   * Recognizes numeric metadata without changing its displayed value or grouping identity.
   *
   * @param value actual metadata value
   * @return numeric sort value, or null for text and date values
   */
  private static @Nullable BigDecimal numericValue(@NotNull final Object value) {
    try {
      return new BigDecimal(value.toString());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
