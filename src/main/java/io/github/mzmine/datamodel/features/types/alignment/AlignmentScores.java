/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.alignment;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.MobilityAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.scores.RateType;
import io.github.mzmine.datamodel.features.types.numbers.scores.WeightedDistanceScore;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Saves scores on the alignment
 *
 * @param rate             the aligned/total samples
 * @param extraFeatures    features that fall within the alignment range but were not aligned with
 *                         this feature
 * @param maxMzDelta
 * @param maxRtDelta
 * @param maxMobilityDelta
 */
public record AlignmentScores(float rate, int alignedFeatures, int extraFeatures,
                              Float weightedDistanceScore, Float mzPpmDelta, Double maxMzDelta,
                              Float maxRtDelta, Float maxMobilityDelta) implements
    ModularDataRecord {

  @SuppressWarnings("rawtypes")
  public static List<DataType> getSubTypes() {
    return DataTypes.getList(RateType.class, AlignedFeaturesNType.class,
        AlignExtraFeaturesType.class, WeightedDistanceScore.class, MzPpmDifferenceType.class,
        MzAbsoluteDifferenceType.class, RtAbsoluteDifferenceType.class,
        MobilityAbsoluteDifferenceType.class);
  }

  public AlignmentScores {
  }

  public AlignmentScores() {
    this(0, 0, 0, null, null, null, null, null);
  }

  public static AlignmentScores create(final @NotNull SimpleModularDataModel values) {
    var rate = requireNonNullElse(values.get(RateType.class), -1f);
    int alignedFeatures = requireNonNullElse(values.get(AlignedFeaturesNType.class), -1);
    var extraFeatures = requireNonNullElse(values.get(AlignExtraFeaturesType.class), -1);
    var weightedDistanceScore = values.get(WeightedDistanceScore.class);
    var mzPpmDelta = values.get(MzPpmDifferenceType.class);
    var mzDelta = values.get(MzAbsoluteDifferenceType.class);
    var rtDelta = values.get(RtAbsoluteDifferenceType.class);
    var mobilityDelta = values.get(MobilityAbsoluteDifferenceType.class);

    return new AlignmentScores(rate, alignedFeatures, extraFeatures, weightedDistanceScore,
        mzPpmDelta, mzDelta, rtDelta, mobilityDelta);
  }


  /**
   * Provides data for data types
   *
   * @param sub data column
   * @return the score for this column
   */
  @Override
  public Object getValue(final DataType sub) {
    return switch (sub) {
      case RateType ignored -> rate;
      case AlignedFeaturesNType ignored -> alignedFeatures;
      case AlignExtraFeaturesType ignored -> extraFeatures;
      case WeightedDistanceScore ignored -> weightedDistanceScore;
      case MzPpmDifferenceType ignored -> mzPpmDelta;
      case MzAbsoluteDifferenceType ignored -> maxMzDelta;
      case RtAbsoluteDifferenceType ignored -> maxRtDelta;
      case MobilityAbsoluteDifferenceType ignored -> maxMobilityDelta;
      default -> throw new IllegalStateException("Unexpected value: " + sub);
    };
  }

  /**
   * Modify one value
   *
   * @param sub   data column
   * @param value new value
   * @return a new object with one modified value
   */
  public <T> AlignmentScores modify(final DataType<T> sub, T value) {
    return switch (sub) {
      case RateType ignored ->
          new AlignmentScores((Float) value, alignedFeatures, extraFeatures, weightedDistanceScore,
              mzPpmDelta, maxMzDelta, maxRtDelta, maxMobilityDelta);
      case AlignedFeaturesNType ignored ->
          new AlignmentScores(rate, (Integer) value, extraFeatures, weightedDistanceScore,
              mzPpmDelta, maxMzDelta, maxRtDelta, maxMobilityDelta);
      case AlignExtraFeaturesType ignored ->
          new AlignmentScores(rate, alignedFeatures, (Integer) value, weightedDistanceScore,
              mzPpmDelta, maxMzDelta, maxRtDelta, maxMobilityDelta);
      case WeightedDistanceScore ignored ->
          new AlignmentScores(rate, alignedFeatures, extraFeatures, (Float) value, mzPpmDelta,
              maxMzDelta, maxRtDelta, maxMobilityDelta);
      case MzPpmDifferenceType ignored ->
          new AlignmentScores(rate, alignedFeatures, extraFeatures, weightedDistanceScore,
              (Float) value, maxMzDelta, maxRtDelta, maxMobilityDelta);
      case MzAbsoluteDifferenceType ignored ->
          new AlignmentScores(rate, alignedFeatures, extraFeatures, weightedDistanceScore,
              mzPpmDelta, (Double) value, maxRtDelta, maxMobilityDelta);
      case RtAbsoluteDifferenceType ignored ->
          new AlignmentScores(rate, alignedFeatures, extraFeatures, weightedDistanceScore,
              mzPpmDelta, maxMzDelta, (Float) value, maxMobilityDelta);
      case MobilityAbsoluteDifferenceType ignored ->
          new AlignmentScores(rate, alignedFeatures, extraFeatures, weightedDistanceScore,
              mzPpmDelta, maxMzDelta, maxRtDelta, (Float) value);
      default -> throw new IllegalStateException("Unexpected value: " + sub);
    };
  }

  /**
   * Merge scores
   *
   * @param other
   * @return merged score
   */
  public AlignmentScores merge(@Nullable AlignmentScores other) {
    if (other == null) {
      return this;
    }
    int total = Math.round(rate * alignedFeatures);
    int otherTotal = Math.round(other.rate * other.alignedFeatures);
    int totalFeatures = total + otherTotal;

    return new AlignmentScores(average(rate, other.rate, total, otherTotal, totalFeatures),
        alignedFeatures + other.alignedFeatures, extraFeatures + other.extraFeatures,
        average(weightedDistanceScore, other.weightedDistanceScore, total, otherTotal,
            totalFeatures), //
        average(mzPpmDelta, other.mzPpmDelta, total, otherTotal, totalFeatures),
        // maximum distances
        max(maxMzDelta, other.maxMzDelta), //
        max(maxRtDelta, other.maxRtDelta), //
        max(maxMobilityDelta, other.maxMobilityDelta));
  }

  public static Double max(final Double a, final Double b) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return Math.max(a, b);
  }

  public static Float max(final Float a, final Float b) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return Math.max(a, b);
  }
  public static Double min(final Double a, final Double b) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return Math.min(a, b);
  }

  public static Float min(final Float a, final Float b) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return Math.min(a, b);
  }

  private Double average(final Double a, final Double b, final int total, final int otherTotal,
      final int totalFeatures) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return (a * total + b * otherTotal) / totalFeatures;
  }

  private Float average(final Float a, final Float b, final int total, final int otherTotal,
      final int totalFeatures) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return (a * total + b * otherTotal) / totalFeatures;
  }
}
