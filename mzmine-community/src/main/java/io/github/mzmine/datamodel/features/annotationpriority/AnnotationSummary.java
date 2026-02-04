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
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.RIType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.scores.SiriusCsiScoreType;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.dataprocessing.filter_sortannotations.CombinedScoreWeights;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.OptionalDouble;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnnotationSummary implements Comparable<AnnotationSummary> {

  @NotNull
  private final FeatureListRow row;
  @Nullable
  private final FeatureAnnotation annotation;

  // the last version that was used to precompute the combinedScore, start with Integer.MAX to compute lazy
  private int lastSortConfigVersion = Integer.MAX_VALUE;
  // precomputed combinedScore with config of version lastSortConfigVersion, lazy computation on access
  private double combinedScore;

  // lazy precomputed on access
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private OptionalDouble isotopeScore;

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

  public double score(@NotNull Scores type, double defaultValue) {
    return score(type).orElse(defaultValue);
  }

  /**
   * Scores might be missing due to missing information in the annotation source like a spectral
   * library entry without retention time or because the feature list has no CCS or other values.
   *
   * @return an optional double which is empty if the score is missing
   */
  public OptionalDouble score(@NotNull Scores type) {
    return switch (type) {
      case COMBINED -> OptionalDouble.of(combinedScore());
      case MZ -> mzScore();
      case RT -> rtScore();
      case RI -> riScore();
      case CCS -> ccsScore();
      case ISOTOPE -> isotopeScore();
      case MS2 -> ms2Score();
    };
  }

  /**
   * Check config version and if needed recalculate cached scores. The scores are calculated lazily
   * once a score has been requested
   */
  private void checkRecalcCachedScores() {
    final int currentAnnotationSortConfigVersion = getCurrentAnnotationSortConfigVersion();
    if (currentAnnotationSortConfigVersion == lastSortConfigVersion) {
      return;
    }

    // set new version first so that combined score calculation does not trigger isotope score calc
    lastSortConfigVersion = currentAnnotationSortConfigVersion;

    // isotope is the most expensive currently
    isotopeScore = calcIsotopeScore();
    // combined score last as it uses all other scores
    combinedScore = calcCombinedScore();
  }

  /**
   * Fetch the precomputed combined score in case config is still the same, otherwise compute value
   * again.
   */
  public double combinedScore() {
    checkRecalcCachedScores();
    return combinedScore;
  }

  private double calcCombinedScore() {
    final AnnotationSummarySortConfig config = getAnnotationSortConfig();
    final CombinedScoreWeights weights = config.combinedScoreWeights();
    double score = 0;
    double totalWeight = 0;

    for (Scores type : Scores.values()) {
      // check if the whole table has CCS, RI, RT to skip this for all annotations
      // cannot just skip RT score for one annotation that has no RT while all others with RT add it
      // this may give a penalty to annotations with better match with additional properties
      if (type == Scores.COMBINED || !isActiveScore(type)) {
        continue;
      }

      final double weight = weights.get(type);
      score += score(type).orElse(0d) * weight;
      totalWeight += weight;
    }

    return totalWeight > 0 ? score / totalWeight : 0d;
  }

  /**
   * A score is active if the underlying feature list has all properties needed to calculate it.
   * Like CCSType as a row type for CCS score.
   *
   * @return true if score is active
   */
  public boolean isActiveScore(@NotNull Scores type) {
    final FeatureList featureList = row.getFeatureList();
    return switch (type) {
      case CCS -> featureList.hasRowType(CCSType.class);
      case RI -> featureList.hasRowType(RIType.class);
      case RT ->
          featureList.hasRowType(RTType.class) && !FeatureListUtils.hasAllImagingData(featureList);
      case MZ, MS2, ISOTOPE, COMBINED -> true;
    };
  }

  public OptionalDouble mzScore() {
    if (annotation == null) {
      return OptionalDouble.empty();
    }

    final Double rowMz = row.getAverageMZ();
    final Double precursorMZ = annotation.getPrecursorMZ();

    if (rowMz != null && precursorMZ != null) {
      final double maxDiff = getAnnotationSortConfig().mzTolerance()
          .getMzToleranceForMass(precursorMZ);
      return OptionalDouble.of(getScore(rowMz, precursorMZ, maxDiff));
    }
    return OptionalDouble.empty();
  }

  private @NotNull AnnotationSummarySortConfig getAnnotationSortConfig() {
    return row.getFeatureList().getAnnotationSortConfig();
  }

  private int getCurrentAnnotationSortConfigVersion() {
    return row.getFeatureList().getAnnotationSortConfigVersion();
  }

  public OptionalDouble rtScore() {
    if (annotation == null) {
      return OptionalDouble.empty();
    }

    final Float rowRt = row.getAverageRT();
    final Float precursorRt = annotation.getRT();

    if (rowRt != null && precursorRt != null) {
      final RTTolerance rtTolerance = getAnnotationSortConfig().rtTolerance();
      final float maxDiff = rtTolerance.getToleranceInMinutes(precursorRt);
      return OptionalDouble.of(getScore(rowRt, precursorRt, maxDiff));
    }
    return OptionalDouble.empty();
  }

  public OptionalDouble ccsScore() {
    if (annotation == null) {
      return OptionalDouble.empty();
    }

    final Float averageCCS = row.getAverageCCS();
    final Float ccs = annotation.getCCS();

    if (averageCCS != null && ccs != null) {
      final double maxCcsDev = getAnnotationSortConfig().ccsTolerance();
      return OptionalDouble.of(getScore((averageCCS - ccs) / ccs, maxCcsDev));
    }
    return OptionalDouble.empty();
  }

  public OptionalDouble ms2Score() {
    return switch (annotation) {
      // use similarity.getScore to get double score instead of Float from FeatureAnnotation.getScore
      case SpectralDBAnnotation s -> OptionalDouble.of(s.getSimilarity().getScore());
      case MatchedLipid l -> OptionalDouble.of(l.getMsMsScore());
      case CompoundDBAnnotation db -> {
        final Float score = db.get(SiriusCsiScoreType.class);
        if (score != null) {
          yield OptionalDouble.of(score);
        }
        yield OptionalDouble.empty();
      }
      case null, default -> OptionalDouble.empty();
    };
  }

  public OptionalDouble riScore() {
    if (annotation == null) {
      return OptionalDouble.empty();
    }

    Float averageRI = row.getAverageRI();
    Float ri = CompoundAnnotationUtils.getTypeValue(annotation, RIType.class);

    if (averageRI != null && ri != null) {
      final double maxRiDev = getAnnotationSortConfig().riTolerance();
      return OptionalDouble.of(getScore(averageRI, ri, maxRiDev));
    }
    return OptionalDouble.empty();
  }

  public OptionalDouble isotopeScore() {
    checkRecalcCachedScores();
    return isotopeScore;
  }

  public OptionalDouble calcIsotopeScore() {
    if (annotation == null) {
      return OptionalDouble.empty();
    }

    final IsotopePattern bestIsotopePattern = row.getBestIsotopePattern();
    final IsotopePattern predictedIp = annotation.getIsotopePattern();
    if (bestIsotopePattern == null || predictedIp == null) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(
        IsotopePatternScoreCalculator.getSimilarityScore(predictedIp, bestIsotopePattern,
            new MZTolerance(0.005, 15), 0d));
  }

  /// [FeatureAnnotation] types are ranked from best to worst:
  /// - Spectral library match with RT or retention index match are highest.
  /// - MatchedLipid with MS2: Usually better for this class of compounds than spectral matches
  /// - Spectral matches (without RT or RI)
  /// - MatchedLipid
  /// - CompoundDB: Contains different types of annotations from local DB, SIRIUS but they all for
  /// now fall under the same rank and are sorted internally.
  ///
  /// @return a rank mzmine [FeatureAnnotation] types the lower the better.
  public int mzmineAnnotationTypeRank() {
    if (annotation == null) {
      return 10;
    }

    return switch (DataTypes.get(annotation.getDataType())) {
      // lipid: if lipid has ms2 score, prefer over library match without rt or ri. otherwise same as comp db.
      case LipidMatchListType _ -> {
        final MatchedLipid lipid = (MatchedLipid) annotation;
        // MSMSscore is null if no MS2 scan available and 0 if keep unconfirmed is active
        // better to check matched fragments. If one is matched then rank this higher
        final Set<LipidFragment> matchedFragments = lipid.getMatchedFragments();
        if (matchedFragments.isEmpty()) {
          yield 4; // worse than spectral match rating
        } else {
          yield 2; // better than spectral match
        }
      }
      case SpectralLibraryMatchesType _ -> {
        // RT is often provided in libraries but may completely mismatch the actual RT
        // therefore check if the score is at least within range and use low minimum score
        if (score(Scores.RT, 0d) > 0.01 || score(Scores.RI, 0d) > 0.01) {
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
    if (Double.isNaN(actual) || Double.isNaN(predicted) || Double.isNaN(maxDiff)
        || Double.compare(maxDiff, 0) == 0) {
//      make sure we dont return NaN
      return 0;
    }
    return 1 - Math.min(Math.abs(actual - predicted), maxDiff) / maxDiff;
  }

  /**
   *
   * @param deviationFromPredicted actualValue - predictedValue or (actualValue -
   *                               predictedValue)/predictedValue
   */
  private double getScore(double deviationFromPredicted, double maxDeviation) {
    if (Double.isNaN(deviationFromPredicted) || Double.isNaN(maxDeviation)) {
      //      make sure we dont return NaN
      return 0;
    }
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
        if (rtScore().orElse(0d) > 0.01 || riScore().orElse(0d) > 0.01) {
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
          riScore().orElse(0d) > 0 || rtScore().orElse(0d) > 0 ? SchymanskiAnnotationLevel.LEVEL_1
              : SchymanskiAnnotationLevel.LEVEL_2a;
      case CompoundDBAnnotation c -> {
        if (c.get(SiriusCsiScoreType.class) != null) {
          yield SchymanskiAnnotationLevel.LEVEL_3;
        } else if (isotopeScore().orElse(0) >= ipScoreMatchThreshold) {
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

  @NotNull
  public String scoreLabel(@NotNull Scores type) {
    if (!isActiveScore(type)) {
      return "(unavailable for feature list)";
    }

    final OptionalDouble score = score(type);

    if (score.isPresent()) {
      return "%.3f".formatted(score.getAsDouble());
    }

    return "(unavailable for annotation)";
  }

  /**
   * Order in this enum also defines the order of the cells in the feature table chart.
   */
  public enum Scores implements UniqueIdSupplier {
    COMBINED, MS2, ISOTOPE, MZ, RT, RI, CCS;

    @NotNull
    public String label() {
      return switch (this) {
        case COMBINED -> "ALL"; // used in chart cell so needs to be short
        case MZ -> "m/z";
        case RT -> "RT";
        case RI -> "RI";
        case CCS -> "CCS";
        case ISOTOPE -> "IP";
        case MS2 -> "MS2";
      };
    }

    @NotNull
    public String fullName() {
      return switch (this) {
        case MZ, RT, RI, CCS, MS2 -> label(); // same short and long
        case COMBINED -> "Combined (" + label() + ")";
        case ISOTOPE -> "Isotope pattern (" + label() + ")";
      };
    }

    @Override
    @NotNull
    public String getUniqueID() {
      return switch (this) {
        case COMBINED -> "combined_score";
        case MZ -> "mz";
        case RT -> "rt";
        case RI -> "ri";
        case CCS -> "ccs";
        case ISOTOPE -> "isotope_pattern";
        case MS2 -> "ms2";
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
