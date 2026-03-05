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

package io.github.mzmine.modules.dataprocessing.norm_standardcompound;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.dataprocessing.norm_linear.NormalizationFunction;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.time.LocalDateTime;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * File-specific standard compound normalization function.
 */
public class StandardCompoundNormalizationFunction implements NormalizationFunction {

  private final RawDataFilePlaceholder referenceFilePlaceholder;
  private final LocalDateTime acquisitionTimestamp;
  private final StandardUsageType usageType;
  private final double mzVsRtBalance;
  private final List<StandardCompoundReferencePoint> referencePoints;

  public StandardCompoundNormalizationFunction(@NotNull final RawDataFile referenceFile,
      @NotNull final LocalDateTime acquisitionTimestamp, @NotNull final StandardUsageType usageType,
      final double mzVsRtBalance,
      @NotNull final List<StandardCompoundReferencePoint> referencePoints) {
    this.referenceFilePlaceholder = new RawDataFilePlaceholder(referenceFile);
    this.acquisitionTimestamp = acquisitionTimestamp;
    this.usageType = usageType;
    this.mzVsRtBalance = mzVsRtBalance;
    this.referencePoints = List.copyOf(referencePoints);
  }

  @Override
  public @NotNull RawDataFilePlaceholder getReferenceFilePlaceholder() {
    return referenceFilePlaceholder;
  }

  @Override
  public @NotNull LocalDateTime getAcquisitionTimestamp() {
    return acquisitionTimestamp;
  }

  @Override
  public double getFactor(@NotNull final Double mz, @NotNull final Float rt) {
    final double standardAbundance = switch (usageType) {
      case Nearest -> getNearestStandardAbundance(mz, rt);
      case Weighted -> getWeightedStandardAbundance(mz, rt);
    };

    double legacyNormalizationFactor = standardAbundance / 100.0d;
    if (legacyNormalizationFactor == 0.0d) {
      legacyNormalizationFactor = Double.MIN_VALUE;
    }
    return 1.0d / legacyNormalizationFactor;
  }

  private double getNearestStandardAbundance(final double mz, final float rt) {
    StandardCompoundReferencePoint nearestPoint = null;
    double nearestDistance = Double.MAX_VALUE;
    for (final StandardCompoundReferencePoint point : referencePoints) {
      final double distance = calcDistance(mz, rt, point);
      if (distance <= nearestDistance) {
        nearestPoint = point;
        nearestDistance = distance;
      }
    }
    if (nearestPoint == null) {
      throw new IllegalStateException("No standard reference points available.");
    }
    return nearestPoint.abundance();
  }

  private double getWeightedStandardAbundance(final double mz, final float rt) {
    double weightedSum = 0.0d;
    double sumOfWeights = 0.0d;
    for (final StandardCompoundReferencePoint point : referencePoints) {
      final double weight = point.missingInFile() ? 0.0d : 1.0d / calcDistance(mz, rt, point);
      weightedSum += point.abundance() * weight;
      sumOfWeights += weight;
    }
    return weightedSum / sumOfWeights;
  }

  private double calcDistance(final double mz, final float rt,
      @NotNull final StandardCompoundReferencePoint point) {
    return mzVsRtBalance * Math.abs(mz - point.mz()) + Math.abs(rt - point.rt());
  }
}
