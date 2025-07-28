/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.AlignmentScoreMemorySegmentColumn;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.MobilityAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.scores.RateType;
import io.github.mzmine.datamodel.features.types.numbers.scores.WeightedDistanceScore;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterModule;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Saves scores on the alignment.
 * <p>
 * Important: Check record creation in {@link AlignmentScoreMemorySegmentColumn} when changing the
 * order of fields. Use proper refactoring tool instead of just changing the order manually.
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
    return DataTypes.getAll(RateType.class, AlignedFeaturesNType.class,
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

    return new AlignmentScores(average(rate, other.rate, total, otherTotal),
        alignedFeatures + other.alignedFeatures, extraFeatures + other.extraFeatures,
        average(weightedDistanceScore, other.weightedDistanceScore, total, otherTotal), //
        average(mzPpmDelta, other.mzPpmDelta, total, otherTotal),
        // maximum distances
        max(maxMzDelta, other.maxMzDelta), //
        max(maxRtDelta, other.maxRtDelta), //
        max(maxMobilityDelta, other.maxMobilityDelta));
  }


  /**
   * Recalculate alignment. Some fields cannot fully be recalculated like the extra features and the
   * alignment scores as the alignment parameters are missing. But this can provide better insights
   * into the final feature list after {@link DuplicateFilterModule}.
   *
   * @param row        the row to recalculate
   * @param rowScore   the row score
   * @param otherScore an optional other score like when two rows are merged into row and the second
   *                   alignment score will be used to calculate averages and maxima of properties
   * @return A merged alignment score
   */
  @NotNull
  public static AlignmentScores recalculateForRow(@NotNull FeatureListRow row,
      @NotNull AlignmentScores rowScore, @Nullable AlignmentScores otherScore) {
    final List<ModularFeature> detected = row.streamFeatures()
        .filter(f -> f.getFeatureStatus() == FeatureStatus.DETECTED).toList();

    final int detectedFeatures = detected.size();
    final int totalFeatures = row.getNumberOfFeatures();

    final float rate = (float) detectedFeatures / totalFeatures;
    int maxExtraFeatures = rowScore.extraFeatures();
    float avgDistanceScore = rowScore.weightedDistanceScore();

    final DoubleSummaryStatistics rtSummary = detected.stream().filter(f -> f.getRT() != null)
        .mapToDouble(ModularFeature::getRT).summaryStatistics();
    Float maxRtDelta =
        rtSummary.getCount() == 0 ? 0f : (float) (rtSummary.getMax() - rtSummary.getMin());

    final DoubleSummaryStatistics mobilitySummary = detected.stream()
        .map(ModularFeature::getMobility).filter(Objects::nonNull).mapToDouble(Float::doubleValue)
        .summaryStatistics();
    final Float maxMobilityDelta = mobilitySummary.getCount() == 0 ? 0f
        : (float) (mobilitySummary.getMax() - mobilitySummary.getMin());

    final DoubleSummaryStatistics mzSummary = detected.stream().filter(f -> f.getMZ() != null)
        .mapToDouble(ModularFeature::getMZ).summaryStatistics();
    final double maxMzDelta =
        mzSummary.getCount() == 0 ? 0d : (mzSummary.getMax() - mzSummary.getMin());
    final Float ppmMzDelta = (float) (maxMzDelta / row.getAverageMZ() * 1_000_000f);

    if (otherScore != null) {
      maxExtraFeatures = Math.max(maxExtraFeatures, otherScore.extraFeatures());
      // average of distance score
      avgDistanceScore = average(avgDistanceScore, otherScore.weightedDistanceScore(),
          rowScore.alignedFeatures, otherScore.alignedFeatures);
    }

    return new AlignmentScores(rate, detectedFeatures, maxExtraFeatures, avgDistanceScore,
        ppmMzDelta, maxMzDelta, maxRtDelta, maxMobilityDelta);
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

  private static Double average(final Double a, final Double b, final int total,
      final int otherTotal) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return (a * total + b * otherTotal) / (total + otherTotal);
  }

  private static Float average(final Float a, final Float b, final int total,
      final int otherTotal) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return (a * total + b * otherTotal) / (total + otherTotal);
  }
}
