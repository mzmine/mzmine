/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Priority of feature annotations. Stream annotations in this priority see
 * {@link FeatureAnnotationIterator}
 */
public enum FeatureAnnotationPriority {
  MANUAL(ManualAnnotationType.class), LIPID(LipidMatchListType.class), SPECTRAL_LIBRARY(
      SpectralLibraryMatchesType.class), EXACT_COMPOUND(CompoundDatabaseMatchesType.class), FORMULA(
      FormulaListType.class);
  private static final DataType<?>[] dataTypes = Arrays.stream(FeatureAnnotationPriority.values())
      .map(FeatureAnnotationPriority::getAnnotationType).map(DataTypes::get)
      .toArray(DataType<?>[]::new);

  private final DataType<?> type;

  FeatureAnnotationPriority(Class<? extends DataType<?>> clazz) {
    this.type = DataTypes.get(clazz);
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

  public static DataType<?>[] getDataTypesInOrder() {
    return dataTypes;
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
}
