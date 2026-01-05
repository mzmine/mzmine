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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.compoundannotations;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.RIType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnnotationSummary {

  private static final double maxPpmDiff = 15;
  private static final double maxRtDiff = 0.3;
  private static final double maxCcsDev = 0.10;
  private static final double maxRiDev = 0.10;

  @NotNull
  private final FeatureListRow row;
  @Nullable
  private final FeatureAnnotation annotation;

  private AnnotationSummary(@NotNull final FeatureListRow row,
      @Nullable final FeatureAnnotation annotation) {
    this.row = row;
    this.annotation = annotation;
  }

  public static @NotNull AnnotationSummary of(FeatureListRow row) {
    return new AnnotationSummary(row,
        CompoundAnnotationUtils.getBestFeatureAnnotation(row).orElse(null));
  }

  public static AnnotationSummary of(@NotNull final FeatureListRow row,
      @NotNull final FeatureAnnotation annotation) {
    return new AnnotationSummary(row, annotation);
  }

  public double score(@NotNull Scores type) {
    return switch (type) {
      case MZ -> mzScore();
      case RT -> rtScore();
      case RI -> riScore();
      case CCS -> ccsScore();
      case ISOTOPE -> isotopeScore();
      case MS2 -> ms2Score();
    };
  }

  public double mzScore() {
    if (annotation == null) {
      return 0;
    }

    Double rowMz = row.getAverageMZ();
    Double precursorMZ = annotation.getPrecursorMZ();

    if (rowMz != null && precursorMZ != null) {
      return 1
          - Math.min(Math.abs(MathUtils.getPpmDiff(precursorMZ, rowMz)), maxPpmDiff) / maxPpmDiff;
    }
    return 0;
  }

  public double rtScore() {
    if (annotation == null) {
      return 0;
    }

    Float rowRt = row.getAverageRT();
    Float precursorRt = annotation.getRT();

    if (rowRt != null && precursorRt != null) {
      return 1 - Math.min(Math.abs(rowRt - precursorRt), maxRtDiff) / maxRtDiff;
    }
    return 0;
  }

  public double ccsScore() {
    if (annotation == null) {
      return 0;
    }

    Float averageCCS = row.getAverageCCS();
    Float ccs = annotation.getCCS();

    if (averageCCS != null && ccs != null) {
      return getScore(averageCCS / ccs, maxCcsDev);
    }
    return 0;
  }

  public double ms2Score() {
    return switch (annotation) {
      case SpectralDBAnnotation s -> s.getScore();
      case MatchedLipid l -> l.getMsMsScore();
      case null, default -> 0;
    };
  }

  public double riScore() {
    if (annotation == null) {
      return 0;
    }

    Float averageRI = row.getAverageRI();
    Float ri = CompoundAnnotationUtils.getTypeValue(annotation, RIType.class);

    if (averageRI != null && ri != null) {
      return getScore(averageRI / ri, maxRiDev);
    }
    return 0d;
  }

  public double isotopeScore() {
    if (annotation == null) {
      return 0;
    }

    IsotopePattern bestIsotopePattern = row.getBestIsotopePattern();
    IsotopePattern predictedIp = annotation.calculateIsotopePattern();
    if (bestIsotopePattern == null || predictedIp == null) {
      return 0d;
    }
    return IsotopePatternScoreCalculator.getSimilarityScore(predictedIp, bestIsotopePattern,
        new MZTolerance(0.005, 15), 0d);
  }

  private double getScore(double actual, double predicted, double maxDiff) {
    return 1 - Math.min(Math.abs(actual - predicted), maxDiff) / maxDiff;
  }

  /**
   *
   * @param deviationFromPredicted actualValue - predictedValue or actualValue/predictedValue
   */
  private double getScore(double deviationFromPredicted, double maxDeviation) {
    return 1 - Math.min(Math.abs(deviationFromPredicted), maxDeviation) / maxDeviation;
  }

  public enum Scores {
    MZ, RT, RI, CCS, ISOTOPE, MS2;

    public String label() {
      return switch (this) {
        case MZ -> "m/z";
        case RT -> "RT";
        case RI -> "RI";
        case CCS -> "CCS";
        case ISOTOPE -> "IP";
        case MS2 -> "MS2";
      };
    }
  }
}
