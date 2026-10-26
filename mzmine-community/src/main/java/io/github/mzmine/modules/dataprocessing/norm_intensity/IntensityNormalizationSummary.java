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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.settings.FeatureListSettings;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * This is a storable version that uses weak references in {@link RawDataFilePlaceholder}. The
 * searchable version is used during normalization building
 * {@link IntensityNormalizationSearchableSummary}
 */
public record IntensityNormalizationSummary(@NotNull List<RawFileNormalizationFunction> functions,
                                            @NotNull List<String> messages,
                                            @NotNull List<FeatureQcProvenance> featureQc) {

  public IntensityNormalizationSummary {
    functions = List.copyOf(functions);
    messages = List.copyOf(messages);
    featureQc = List.copyOf(featureQc);
  }

  public IntensityNormalizationSummary(@NotNull final List<RawFileNormalizationFunction> functions,
      @NotNull final List<String> messages) {
    this(functions, messages, List.of());
  }

  public static final @NotNull IntensityNormalizationSummary EMPTY = new IntensityNormalizationSummary(
      List.of(), List.of());

  /**
   * Messages are plain strings so that they stay easy to save and to read. A message that reports a
   * problem the user should look at contains this marker, usually as part of a step specific prefix
   * like {@code "IS warning: "}. Used to count and highlight them in {@link #toString()} and to log
   * them at the right level.
   */
  public static final @NotNull String WARNING_MARKER = "warning: ";

  @NotNull
  public IntensityNormalizationSummary copy() {
    return new IntensityNormalizationSummary(functions, messages, featureQc);
  }

  public IntensityNormalizationSummary(
      @NotNull List<RawFileNormalizationFunction> functions) {
    this(functions, new ArrayList<>());
  }

  public int size() {
    return functions.size();
  }

  public boolean isEmpty() {
    return functions.isEmpty();
  }

  @NotNull
  public NormalizationFunction get(int index) {
    if (index < 0 || index >= size()) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
    }
    return functions.get(index).function();
  }
  @NotNull
  public RawFileNormalizationFunction getRawFileFunction(int index) {
    if (index < 0 || index >= size()) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
    }
    return functions.get(index);
  }

  /**
   * Rendered in the applied methods pane of the feature list summary, see
   * {@code FeatureListSummaryController.parameterToString}. Reports what the individual
   * normalization steps did, e.g. skipped internal standards or skipped files.
   */
  @Override
  public String toString() {
    // the applied method only keeps the diagnostics, the functions are in the feature list settings
    final StringBuilder sb = new StringBuilder(functions.isEmpty() ? ""
        : "%d normalized files, ".formatted(functions.size()));
    sb.append("%d messages, %d of them warnings".formatted(messages.size(), countWarnings()));
    for (final String message : messages) {
      sb.append("\n").append(message);
    }
    for (final FeatureQcProvenance qc : featureQc) {
      final FeatureQcDiagnostics diagnostics = qc.diagnostics();
      sb.append("\n").append(diagnostics.settings().description())
          .append("; modeled ").append(qc.modeledAbundance())
          .append("; the same factors apply to both area and height.")
          .append("\n").append(diagnostics.solver()).append("; ").append(diagnostics.coordinatePolicy())
          .append("\n").append(diagnostics.scalePolicy())
          .append("\nSettings: ").append(diagnostics.settings())
          .append("\nAvailability: ").append(diagnostics.availability());
    }
    return sb.toString();
  }

  /**
   * @return number of messages that report a problem, see {@link #WARNING_MARKER}
   */
  public long countWarnings() {
    return messages.stream().filter(msg -> msg.contains(WARNING_MARKER)).count();
  }

  /**
   * @return true if any function resolves its factor by feature-list row ID
   */
  public boolean isRowDependent() {
    return functions.stream().anyMatch(RawFileNormalizationFunction::isRowDependent);
  }

  /**
   * @return only the functions, without messages and feature-QC provenance. This is the active
   * normalization state of a feature list, see {@link FeatureListSettings}.
   */
  public @NotNull IntensityNormalizationSummary withFunctionsOnly() {
    return new IntensityNormalizationSummary(functions, List.of(), List.of());
  }

  /**
   * @return only messages and feature-QC provenance, without the functions. Kept in the applied
   * method as diagnostics, the functions are stored once in the {@link FeatureListSettings}.
   */
  public @NotNull IntensityNormalizationSummary withDiagnosticsOnly() {
    return new IntensityNormalizationSummary(List.of(), messages, featureQc);
  }

  /**
   * @param oldToNewIds old row ID -> new row ID. Factors of rows missing from the map are dropped.
   * @see NormalizationFunction#remapRowIds(Map)
   */
  public @NotNull IntensityNormalizationSummary remapRowIds(
      @NotNull final Map<Integer, Integer> oldToNewIds) {
    return new IntensityNormalizationSummary(
        functions.stream().map(function -> function.remapRowIds(oldToNewIds)).toList(), messages,
        featureQc);
  }

  /**
   * Resolves the functions for the given files once for fast lookups. Matches the placeholders
   * against the given files instead of {@link RawDataFilePlaceholder#getMatchingFile()}, which may
   * resolve a cached wrapper instead of the source file of a feature list during project loading.
   *
   * @param files usually the raw data files of the feature list. Files without a function are not
   *              part of the result.
   */
  public @NotNull IntensityNormalizationSearchableSummary toSearchableSummary(
      @NotNull final List<RawDataFile> files) {
    final Map<RawDataFile, RawFileNormalizationFunction> byFile = HashMap.newHashMap(files.size());
    for (final RawDataFile file : files) {
      functions.stream().filter(function -> function.rawDataFilePlaceholder().matches(file))
          .findFirst().ifPresent(function -> byFile.put(file, function));
    }
    return new IntensityNormalizationSearchableSummary(Map.copyOf(byFile), messages, featureQc);
  }
}
