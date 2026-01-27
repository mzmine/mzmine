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

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.numbers.RIType;
import io.github.mzmine.datamodel.features.types.numbers.scores.SiriusCsiScoreType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SpeciesLevelAnnotation;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnnotationSummary implements Comparable<AnnotationSummary> {

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
      @Nullable final FeatureAnnotation annotation) {
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

    final Double rowMz = row.getAverageMZ();
    final Double precursorMZ = annotation.getPrecursorMZ();

    if (rowMz != null && precursorMZ != null) {
      final double maxDiff = row.getFeatureList().getAnnotationSortConfig().mzTolerance()
          .getMzToleranceForMass(precursorMZ);
      return 1 - Math.min(Math.abs(precursorMZ - rowMz), maxDiff) / maxDiff;
    }
    return 0;
  }

  public double rtScore() {
    if (annotation == null) {
      return 0;
    }

    final Float rowRt = row.getAverageRT();
    final Float precursorRt = annotation.getRT();

    if (rowRt != null && precursorRt != null) {
      final RTTolerance rtTolerance = row.getFeatureList().getAnnotationSortConfig().rtTolerance();
      final float maxDiff = rtTolerance.getToleranceInMinutes(precursorRt);
      return 1 - Math.min(Math.abs(rowRt - precursorRt), maxDiff) / maxDiff;
    }
    return 0;
  }

  public double ccsScore() {
    if (annotation == null) {
      return 0;
    }

    final Float averageCCS = row.getAverageCCS();
    final Float ccs = annotation.getCCS();

    if (averageCCS != null && ccs != null) {
      final double maxCcsDev = row.getFeatureList().getAnnotationSortConfig().ccsTolerance();
      return getScore((averageCCS - ccs) / ccs, maxCcsDev);
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
      final double maxRiDev = row.getFeatureList().getAnnotationSortConfig().riTolerance();
      return getScore(averageRI - ri, maxRiDev);
    }
    return 0d;
  }

  public double isotopeScore() {
    if (annotation == null) {
      return 0;
    }

    final IsotopePattern bestIsotopePattern = row.getBestIsotopePattern();
    final IsotopePattern predictedIp = annotation.getIsotopePattern();
    if (bestIsotopePattern == null || predictedIp == null) {
      return 0d;
    }
    return IsotopePatternScoreCalculator.getSimilarityScore(predictedIp, bestIsotopePattern,
        new MZTolerance(0.005, 15), 0d);
  }

  public int annotationTypeScore() {
    if (annotation == null) {
      return 10;
    }

    return switch (DataTypes.get(annotation.getDataType())) {
      // lipid: if lipid has ms2 score, prefer over library match without rt or ri. otherwise same as comp db.
      case LipidMatchListType _ -> ((MatchedLipid) annotation).getMsMsScore() != null ? 2 : 4;
      case SpectralLibraryMatchesType _ -> {
        if (((SpectralDBAnnotation) annotation).getRtAbsoluteError() != null
            || ((SpectralDBAnnotation) annotation).getRiDiff() != null) {
          yield 1;
        } else {
          yield 3;
        }
      }
      case CompoundDatabaseMatchesType _ -> 4;
      case null, default -> 10;
    };
  }

  private double getScore(double actual, double predicted, double maxDiff) {
    return 1 - Math.min(Math.abs(actual - predicted), maxDiff) / maxDiff;
  }

  /**
   *
   * @param deviationFromPredicted actualValue - predictedValue or (actualValue -
   *                               predictedValue)/predictedValue
   */
  private double getScore(double deviationFromPredicted, double maxDeviation) {
    return 1 - Math.min(Math.abs(deviationFromPredicted), maxDeviation) / maxDeviation;
  }

  /**
   * https://pmc.ncbi.nlm.nih.gov/articles/PMC3772505/
   */
  @NotNull
  public MsiAnnotationLevel deriveMsiLevel() {
    if (annotation == null) {
      return MsiAnnotationLevel.LEVEL_4;
    }

    return switch (annotation) {
      case MatchedLipid l -> {
        if (l.getMsMsScore() != null && l.getMsMsScore() > 0d) {
          switch (l.getLipidAnnotation()) {
            case MolecularSpeciesLevelAnnotation _ -> {
              yield MsiAnnotationLevel.LEVEL_2;
            }
            case SpeciesLevelAnnotation _ -> {
              yield MsiAnnotationLevel.LEVEL_3;
            }
            case null, default -> {
            }
          }
        }
        yield MsiAnnotationLevel.LEVEL_4;
      }
      case SpectralDBAnnotation _ -> MsiAnnotationLevel.LEVEL_2;
      case CompoundDBAnnotation c -> {
        if (rtScore() > 0 || riScore() > 0) {
          yield MsiAnnotationLevel.LEVEL_2;
        } else if (c.get(SiriusCsiScoreType.class) != null) {
          yield MsiAnnotationLevel.LEVEL_3;
        }
        yield MsiAnnotationLevel.LEVEL_4;
      }
      default -> MsiAnnotationLevel.LEVEL_4;
    };
  }

  /**
   * https://pubs.acs.org/doi/10.1021/es5002105
   */
  @NotNull
  public SchymanskiAnnotationLevel deriveSchymanskiLevel() {
    final double ipScoreMatchThreshold = 0.75;

    if (annotation == null) {
      return SchymanskiAnnotationLevel.LEVEL_5;
    }

    return switch (annotation) {
      case MatchedLipid l -> {
        if (l.getMsMsScore() != null && l.getMsMsScore() > 0d) {
          switch (l.getLipidAnnotation()) {
            case MolecularSpeciesLevelAnnotation _ -> {
              yield SchymanskiAnnotationLevel.LEVEL_2a;
            }
            case SpeciesLevelAnnotation _ -> {
              yield SchymanskiAnnotationLevel.LEVEL_2b;
            }
            case null, default -> {
            }
          }
        }
        // lipids are hard to judge by isotope scores so default to level 5
        yield SchymanskiAnnotationLevel.LEVEL_5;
      }
      case SpectralDBAnnotation s ->
          riScore() > 0 || rtScore() > 0 ? SchymanskiAnnotationLevel.LEVEL_1
              : SchymanskiAnnotationLevel.LEVEL_2a;
      case CompoundDBAnnotation c -> {
        if (c.get(SiriusCsiScoreType.class) != null) {
          yield SchymanskiAnnotationLevel.LEVEL_3;
        } else if (isotopeScore() >= ipScoreMatchThreshold) {
          yield SchymanskiAnnotationLevel.LEVEL_4;
        } else {
          yield SchymanskiAnnotationLevel.LEVEL_5;
        }
      }
      default -> SchymanskiAnnotationLevel.LEVEL_5;
    };
  }

  @Override
  public int compareTo(@NotNull AnnotationSummary o) {
    return AnnotationSummaryOrder.MZMINE.getComparatorLowFirst().compare(this, o);
  }

  /**
   * Order in this enum also defines the order of the cells in the feature table chart.
   */
  public enum Scores {
    MS2, ISOTOPE, MZ, RT, RI, CCS;

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

  public @NotNull FeatureListRow row() {
    return row;
  }

  public @Nullable FeatureAnnotation annotation() {
    return annotation;
  }
}
