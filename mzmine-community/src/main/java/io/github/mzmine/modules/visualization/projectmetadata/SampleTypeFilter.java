/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.StringUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An immutable set of sample types - the free text values of the {@code mzmine_sample_type}
 * metadata column - that is both the <b>matcher</b> used by tasks and the <b>value</b> that
 * {@link io.github.mzmine.parameters.parametertypes.metadata.SampleTypeFilterParameter} stores,
 * displays and writes to batch files.
 * <p>
 * Values are <b>normalized</b> on construction: trimmed, lower cased (see
 * {@link StringUtils#normalizeStripLowerCase(Object)}), blanks dropped, and sorted alphabetically.
 * That normalized form is the only representation there is - it is what {@link #getValues()}
 * returns, what the editing component lists, and what is saved to XML. A column holding both
 * {@code Sample} and {@code sample} therefore offers the user a single item {@code sample} that
 * matches both, and a filter is independent of how the user or an imported metadata sheet spelled
 * the group.
 * <p>
 * Matching is <b>exact</b> on that normalized value; the file name heuristics in
 * {@link SampleType#guessFromName(String)} are deliberately <b>not</b> applied. Once a file has
 * been imported its sample type is whatever the metadata column says, including custom group names
 * the user typed or imported, and that value is authoritative.
 * <p>
 * Besides an explicit list of values the filter has two open ended modes, {@link Mode#ALL} and
 * {@link Mode#NONE}, so that "every sample type" can be expressed without enumerating a set of
 * values that would silently exclude any type added later. That matters for batch files: a batch
 * saved with "all types" as an explicit list would exclude any sample type introduced afterwards,
 * while {@link Mode#ALL} keeps meaning everything.
 */
public final class SampleTypeFilter {

  /**
   * How the filter decides, independent of the number of known sample types.
   */
  public enum Mode implements UniqueIdSupplier {
    /**
     * Matches every file, whatever its sample type is - including types that do not exist yet.
     */
    ALL,
    /**
     * Matches no file at all.
     */
    NONE,
    /**
     * Matches files whose sample type is one of {@link SampleTypeFilter#getValues()}.
     */
    LIST;

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case ALL -> "ALL";
        case NONE -> "NONE";
        case LIST -> "LIST";
      };
    }
  }

  private final @NotNull Mode mode;
  /**
   * Normalized (trimmed, lower cased) values in alphabetical order, only meaningful for
   * {@link Mode#LIST}. Unmodifiable, and the iteration order is part of the contract: it drives the
   * display and the order values are written to XML.
   */
  private final @NotNull Set<String> values;

  private SampleTypeFilter(@NotNull final Mode mode, @NotNull final Collection<String> values) {
    this.mode = mode;
    this.values = mode == Mode.LIST ? normalizeSorted(values) : Set.of();
  }

  /**
   * Trims and lower cases every value, drops blanks, collapses values that only differed in case or
   * surrounding whitespace, and sorts the result alphabetically so that two filters built from the
   * same groups in a different order are equal and serialize identically.
   */
  private static @NotNull Set<String> normalizeSorted(@NotNull final Collection<String> values) {
    final Set<String> normalized = values.stream().map(StringUtils::normalizeStripLowerCase)
        .filter(StringUtils::hasValue).sorted()
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return Collections.unmodifiableSet(normalized);
  }

  /**
   * Matches every file, also those with a sample type that mzmine does not know yet.
   */
  public static SampleTypeFilter all() {
    return new SampleTypeFilter(Mode.ALL, List.of());
  }

  /**
   * Matches no file.
   */
  public static SampleTypeFilter none() {
    return new SampleTypeFilter(Mode.NONE, List.of());
  }

  /**
   * Matches files whose sample type equals any of the values after normalization. Values may be
   * predefined {@link SampleType}s or custom user defined group names. Use {@link #all()} instead
   * of listing every known type.
   */
  public static SampleTypeFilter ofValues(@NotNull final Collection<String> values) {
    return new SampleTypeFilter(Mode.LIST, values);
  }

  /**
   * Matches files whose sample type equals any of the values after normalization.
   */
  public static SampleTypeFilter ofValues(@NotNull final String... values) {
    return ofValues(List.of(values));
  }

  /**
   * An explicit list of the predefined types. Use {@link #all()} instead of listing all of them.
   */
  public static SampleTypeFilter of(@NotNull final SampleType... types) {
    return of(List.of(types));
  }

  /**
   * An explicit list of the predefined types. Use {@link #all()} instead of listing all of them.
   */
  public static SampleTypeFilter of(@NotNull final List<SampleType> types) {
    return ofValues(types.stream().map(SampleType::toString).toList());
  }

  public static SampleTypeFilter of(@NotNull final SampleType type) {
    return of(List.of(type));
  }

  public static SampleTypeFilter qc() {
    return of(SampleType.QC);
  }

  public static SampleTypeFilter blank() {
    return of(SampleType.BLANK);
  }

  public static SampleTypeFilter sample() {
    return of(SampleType.SAMPLE);
  }

  public static SampleTypeFilter calibration() {
    return of(SampleType.CALIBRATION);
  }

  public static SampleTypeFilter sst() {
    return of(SampleType.SST);
  }

  public boolean matches(@Nullable final SampleType type) {
    return type != null && matchesValue(type.toString());
  }

  /**
   * Compares a raw sample type value against this filter, ignoring case and surrounding whitespace.
   *
   * @param value a value of the sample type metadata column, may be null or blank
   */
  public boolean matchesValue(@Nullable final Object value) {
    return switch (mode) {
      case ALL -> true;
      case NONE -> false;
      case LIST -> values.contains(StringUtils.normalizeStripLowerCase(value));
    };
  }

  /**
   * Checks if the sample type metadata column of this file matches the filter. If the column does
   * not exist, falls back to guessing the type from the file name - the same guess that would have
   * been written into the column on import.
   */
  public boolean matches(RawDataFile file) {
    final MetadataTable metadata = ProjectService.getProjectManager().getCurrentProject()
        .getProjectMetadata();
    final MetadataColumn<String> metadataColumn = (MetadataColumn<String>) metadata.getColumnByName(
        MetadataColumn.SAMPLE_TYPE_HEADER);
    if (metadataColumn != null) {
      return matchesValue(metadata.getValue(metadataColumn, file));
    }
    return matchesValue(SampleType.guessFromName(file.getName()).toString());
  }

  /**
   * Filters a list of rows to contain only rows that contain at least one feature of the sample
   * types described by this filter.
   */
  public List<FeatureListRow> filter(final List<FeatureListRow> rows) {
    return rows.stream().filter(row -> row.streamFeatures().anyMatch(this::matches)).toList();
  }

  /**
   * Filters a list of raw data files to those described by this filter.
   */
  public List<RawDataFile> filterFiles(final List<RawDataFile> raws) {
    return raws.stream().filter(this::matches).toList();
  }

  public boolean matches(final Feature feature) {
    return matches(feature.getRawDataFile());
  }

  /**
   * @return true if this filter can never match any file, either because it is {@link Mode#NONE} or
   * because it is an empty list of values
   */
  public boolean isEmpty() {
    return mode == Mode.NONE || (mode == Mode.LIST && values.isEmpty());
  }

  public @NotNull Mode getMode() {
    return mode;
  }

  /**
   * @return Immutable set of the normalized (trimmed, lower cased) sample type values allowed by
   * this filter, in alphabetical order. Empty for {@link Mode#ALL} and {@link Mode#NONE} - check
   * {@link #getMode()} to tell those apart.
   */
  public @NotNull Set<String> getValues() {
    return values;
  }

  /**
   * @return all values of this filter that are not one of the predefined {@link SampleType}s, in
   * alphabetical order
   */
  public @NotNull Set<String> customValues() {
    return values.stream().filter(v -> SampleType.ofExactValue(v) == null)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * A short, human readable form used by the editing component and in log or error messages, e.g.
   * {@code All}, {@code None} or {@code blank, qc}.
   */
  @Override
  public String toString() {
    return switch (mode) {
      case ALL -> "All";
      case NONE -> "None";
      case LIST -> values.isEmpty() ? "None" : String.join(", ", values);
    };
  }

  /**
   * @return the values joined with {@code ", "} but never longer than maxChars, appending how many
   * values were left out. Used for the collapsed display of the editing component.
   */
  public @NotNull String toShortString(final int maxChars) {
    if (mode != Mode.LIST) {
      return toString();
    }
    final List<String> sorted = List.copyOf(values);
    if (sorted.isEmpty()) {
      return "None";
    }

    // the first value is always shown in full, even if it alone exceeds maxChars - a truncated
    // group name would be misleading, and the count below tells the user what is hidden
    final StringBuilder shown = new StringBuilder(sorted.getFirst());
    int used = 1;
    for (String value : sorted.subList(1, sorted.size())) {
      if (shown.length() + value.length() + 2 > maxChars) {
        break;
      }
      shown.append(", ").append(value);
      used++;
    }

    final int remaining = sorted.size() - used;
    // never silently truncate - say how many values are hidden
    return remaining == 0 ? shown.toString() : shown + " (+%d)".formatted(remaining);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof SampleTypeFilter that)) {
      return false;
    }
    return mode == that.mode && values.equals(that.values);
  }

  @Override
  public int hashCode() {
    return 31 * mode.hashCode() + values.hashCode();
  }
}
