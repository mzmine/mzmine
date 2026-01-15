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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.util.collections.SortOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Priority of feature annotations. Stream annotations in this priority see
 */
public enum FeatureAnnotationPriority {
  MANUAL(ManualAnnotationType.class), SPECTRAL_LIBRARY(SpectralLibraryMatchesType.class), LIPID(
      LipidMatchListType.class), EXACT_COMPOUND(CompoundDatabaseMatchesType.class), FORMULA(
      FormulaListType.class);
  private static final DataType<?>[] dataTypes = Arrays.stream(FeatureAnnotationPriority.values())
      .map(FeatureAnnotationPriority::getAnnotationType).map(DataTypes::get)
      .toArray(DataType<?>[]::new);

  private final DataType<?> type;

  FeatureAnnotationPriority(Class<? extends DataType<?>> clazz) {
    this.type = DataTypes.get(clazz);
  }


  /**
   * Sort a collection of AnnotationTypes and put {@link MissingValueType} at the end (descending)
   * or at the start (ascending). For charts and legend creation use ACSENDING to put the missing
   * color first
   */
  public static Comparator<? super DataType<?>> createSorter(final SortOrder order) {
    Map<DataType<?>, Integer> orderedTypes = new LinkedHashMap<>();
    int c = 0;
    MissingValueType missingValueType = DataTypes.get(MissingValueType.class);
    if (order == SortOrder.ASCENDING) {
      orderedTypes.put(missingValueType, c++);
      for (final DataType<?> type : FeatureAnnotationPriority.getDataTypesInReversedOrder()) {
        orderedTypes.put(type, c++);
      }
    } else {
      for (final DataType<?> type : FeatureAnnotationPriority.getDataTypesInOrder()) {
        orderedTypes.put(type, c++);
      }
      orderedTypes.put(missingValueType, c);
    }

    return Comparator.comparing(orderedTypes::get, Comparator.nullsLast(Comparator.naturalOrder()));
  }

  /**
   * Returns the priority ordinal of the given annotation type in this enum or the lowest priority
   * specified by this enum + 1 (the length of this enum). The smaller the integer, the higher the
   * priority.
   */
  public static <T extends DataType<?> & AnnotationType> int getPriority(Class<T> clazz) {
    return getPriority(DataTypes.get(clazz));
  }

  /**
   * Returns the priority ordinal of the given annotation type in this enum or the lowest priority
   * specified by this enum + 1 (the length of this enum). The smaller the integer, the higher the
   * priority.
   */
  public static <T extends DataType<?> & AnnotationType> int getPriority(T clazz) {
    final FeatureAnnotationPriority prio = Arrays.stream(FeatureAnnotationPriority.values())
        .filter(p -> p.type.getClass().getName().equalsIgnoreCase(clazz.getClass().getName()))
        .findFirst().orElse(null);
    return prio != null ? prio.ordinal() : FeatureAnnotationPriority.values().length;
  }

  /**
   * @return Best first
   */
  public static DataType<?>[] getDataTypesInOrder() {
    return dataTypes;
  }

  /**
   * @return Best last
   */
  public static DataType<?>[] getDataTypesInReversedOrder() {
    DataType<?>[] reversed = new DataType[dataTypes.length];
    for (int i = 0; i < reversed.length; i++) {
      reversed[i] = dataTypes[dataTypes.length - 1 - i];
    }
    return reversed;
  }


  @NotNull
  public List<?> getAll(FeatureListRow row) {
    return switch (this) {
      case MANUAL -> {
        ManualAnnotation annotation = row.getManualAnnotation();
        if (annotation != null && annotation.getCompoundName() != null
            && !annotation.getCompoundName().isBlank()) {
          yield List.of(annotation);
        } else {
          yield List.of();
        }
      }
      case SPECTRAL_LIBRARY -> row.getSpectralLibraryMatches();
      case LIPID -> row.getLipidMatches();
      case EXACT_COMPOUND -> row.getCompoundAnnotations();
      case FORMULA -> row.getFormulas();
    };
  }

  public DataType<?> getAnnotationType() {
    return type;
  }

  @Nullable
  public static DataType<?> getPreferredAnnotationType(@NotNull ModularDataModel model) {
    for (FeatureAnnotationPriority annotationPriority : values()) {
      Object value = model.get(annotationPriority.getAnnotationType());
      if (value != null) {
        if (value instanceof List list) {
          if (!list.isEmpty()) {
            return annotationPriority.getAnnotationType();
          }
        } else {
          return annotationPriority.getAnnotationType();
        }
      }
    }
    return null;
  }
}
