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

package io.github.mzmine.datamodel.features.annotationpriority;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnnotationPriority {

  /// This list does **not** represent an absolute order of annotation priorities, but may be used
  /// for rough pre-grouping if required.
  public static final List<DataType> types = DataTypes.getAll(CompoundDatabaseMatchesType.class,
      LipidMatchListType.class, SpectralLibraryMatchesType.class);

  /**
   *
   * @param topN The maximum number of annotations per type.
   * @return A list of all feature annotations in no particular order.
   */
  public static @NotNull List<@NotNull FeatureAnnotation> getFeatureAnnotations(
      @Nullable ModularDataModel row, int topN) {
    if (row == null || row.isEmpty()) {
      return List.of();
    }

    final List<@NotNull FeatureAnnotation> results = new ArrayList<>(types.size());
    for (final DataType type : types) {
      final Object value = row.get(type);
      if (value == null) {
        continue;
      }

      if (value instanceof List list) {
        if (list.isEmpty()) {
          continue;
        }
        for (int i = 0; i < list.size() && i < topN; i++) {
          Object o = list.get(i);
          if (o instanceof FeatureAnnotation a) {
            results.add(a);
          }
        }
      } else if (value instanceof FeatureAnnotation a) {
        results.add(a);
      }
    }
    return results;
  }

  /**
   *
   * @return A list of all feature annotations in no particular order.
   */
  public static @NotNull List<@NotNull FeatureAnnotation> getAllFeatureAnnotations(
      @Nullable ModularDataModel row) {
    return getFeatureAnnotations(row, Integer.MAX_VALUE);
  }

  /**
   *
   * @param model
   * @return A list of the top annotation per type. Not sorted by overall confidence.
   */
  public static @NotNull List<@NotNull FeatureAnnotation> getTopAnnotationsPerType(
      @Nullable final ModularDataModel model) {
    return getFeatureAnnotations(model, 1);
  }

  public static @NotNull Map<@NotNull Class<? extends DataType>, @NotNull FeatureAnnotation> getTopAnnotationsPerTypeMap(
      @Nullable ModularDataModel model) {
    return getTopAnnotationsPerType(model).stream()
        .collect(Collectors.toMap(FeatureAnnotation::getDataType, a -> a));
  }

  public static @Nullable AnnotationSummary getBestAnnotationSummary(
      @Nullable final FeatureListRow model) {
    if (model == null) {
      return null;
    }
    return getTopAnnotationsPerType(model).stream().map(a -> AnnotationSummary.of(model, a))
        .min(AnnotationSummary.HIGH_TO_LOW_CONFIDENCE).orElse(null);
  }

  public static @Nullable FeatureAnnotation getBestFeatureAnnotation(@Nullable FeatureListRow row) {
    AnnotationSummary bestSummary = getBestAnnotationSummary(row);
    return bestSummary != null ? bestSummary.annotation() : null;
  }

  public static @Nullable DataType<?> getBestAnnotationType(@Nullable FeatureListRow row) {
    FeatureAnnotation annotation = getBestFeatureAnnotation(row);
    return annotation != null ? DataTypes.get(annotation.getDataType()) : null;
  }

  public static @NotNull List<@NotNull FeatureAnnotation> getFeatureAnnotationsByDescendingConfidence(
      @Nullable final FeatureListRow row, int topN) {
    if (row == null) {
      return List.of();
    }
    return getFeatureAnnotations(row, topN).stream().map(a -> AnnotationSummary.of(row, a))
        .sorted(AnnotationSummary.HIGH_TO_LOW_CONFIDENCE).map(AnnotationSummary::annotation)
        //.filter(Objects::nonNull) // cannot be null because input is not null
        .toList();
  }

  public static @NotNull List<@NotNull FeatureAnnotation> getAllFeatureAnnotationsByDescendingConfidence(
      @Nullable final FeatureListRow row) {
    return getFeatureAnnotationsByDescendingConfidence(row, Integer.MAX_VALUE);
  }

  public static @NotNull Stream<@NotNull AnnotationSummary> streamBestAnnotationSummaries(
      @NotNull List<@NotNull FeatureListRow> rows, final boolean includeUnannotated) {
    return rows.stream().map(row -> Objects.requireNonNullElse(getBestAnnotationSummary(row),
        AnnotationSummary.of(row, null))).filter(s -> s.annotation() != null || includeUnannotated);
  }

  public static Map<@NotNull FeatureListRow, @NotNull AnnotationSummary> mapRowsToBestAnnotationSummary(
      @NotNull List<@NotNull FeatureListRow> rows, final boolean includeUnannotated) {
    return AnnotationPriority.streamBestAnnotationSummaries(rows, includeUnannotated)
        .collect(Collectors.toMap(AnnotationSummary::row, a -> a));
  }

  public static <T> Map<@NotNull T, @NotNull AnnotationSummary> mapRowsToBestAnnotationSummary(
      @NotNull List<@NotNull T> data, @NotNull Function<T, @NotNull FeatureListRow> mapper,
      final boolean includeUnannotated) {

    final Map<T, AnnotationSummary> result = new HashMap<>();
    for (T d : data) {
      AnnotationSummary summary = getBestAnnotationSummary(mapper.apply(d));
      if (summary == null && !includeUnannotated) {
        continue;
      }
      result.put(d, summary);
    }

    return result;
  }
}


