/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.dataprocessing.norm_intensity.NormalizationFunction;
import io.github.mzmine.modules.dataprocessing.norm_intensity.RawFileNormalizationFunction;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to define the abundance of features
 */
public enum AbundanceMeasure implements UniqueIdSupplier {
  Height(HeightType.class), Area(AreaType.class), NORMALIZED_HEIGHT(
      NormalizedHeightType.class), NORMALIZED_AREA(NormalizedAreaType.class);

  final Class<? extends DataType<Float>> type;

  AbundanceMeasure(Class<? extends DataType<Float>> type) {
    this.type = type;
  }

  public static @NotNull AbundanceMeasure[] rawValues() {
    return new AbundanceMeasure[]{Height, Area};
  }

  public Class<? extends DataType<Float>> type() {
    return type;
  }

  /**
   * @param featureOrRow The feature or row
   * @return The abundance or null if the feature/row is null or no abundance is set.
   */
  @Nullable
  public Float get(@Nullable ModularDataModel featureOrRow) {
    if (featureOrRow == null) {
      return null;
    }
    return featureOrRow.get(type);
  }

  public float getOrNaN(@Nullable ModularDataModel featureOrRow) {
    if (featureOrRow == null) {
      return Float.NaN;
    }
    return Objects.requireNonNullElse(featureOrRow.get(type), Float.NaN);
  }

  /**
   * Applies a normalization function to the value
   *
   * @param featureOrRow          the model
   * @param normalizationFunction a function
   * @return the normalized value or raw value if function is null or NaN if feature or value is
   * null or not a finite number
   */
  public float getOrNaN(@Nullable ModularDataModel featureOrRow,
      @Nullable RawFileNormalizationFunction normalizationFunction) {
    return getOrNaN(featureOrRow,
        normalizationFunction == null ? null : normalizationFunction.function());
  }

  /**
   * Applies a normalization function to the value
   *
   * @param featureOrRow          the model
   * @param normalizationFunction a function
   * @return the normalized value or raw value if function is null or NaN if feature or value is
   * null or not a finite number
   */
  public float getOrNaN(@Nullable ModularDataModel featureOrRow,
      @Nullable NormalizationFunction normalizationFunction) {
    if (isNormalized()) {
      throw new IllegalStateException("""
          Cannot apply normalization on already normalized values. The idea is to have composite \
          normalization functions that apply multiply steps to the raw area or height values.""");
    }

    if (featureOrRow == null) {
      return Float.NaN;
    }
    final Float value = featureOrRow.get(type);
    if (value == null || !Double.isFinite(value)) { // handles NaN as well
      return Float.NaN;
    }
    if (normalizationFunction == null) {
      return value;
    }
    final Float rt = featureOrRow.get(RTType.class);
    final Double mz = featureOrRow.get(MZType.class);
    return (float) (value * normalizationFunction.getNormalizationFactor(mz, rt));
  }


  /**
   * @return true if normalized
   */
  public boolean isNormalized() {
    return this == NORMALIZED_AREA || this == NORMALIZED_HEIGHT;
  }


  @Override
  public String toString() {
    return switch (this) {
      case NORMALIZED_HEIGHT -> "Normalized height";
      case NORMALIZED_AREA -> "Normalized area";
      case Area -> "Area";
      case Height -> "Height";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case NORMALIZED_AREA -> NormalizedAreaType.UNIQUE_ID;
      case NORMALIZED_HEIGHT -> NormalizedHeightType.UNIQUE_ID;
      case Area -> "Area"; // upper case for compatibility
      case Height -> "Height"; // upper case for compatibility
    };
  }
}
