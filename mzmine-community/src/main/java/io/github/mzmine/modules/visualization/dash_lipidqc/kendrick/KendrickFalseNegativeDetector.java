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

package io.github.mzmine.modules.visualization.dash_lipidqc.kendrick;

import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.computeCombinedAnnotationScore;
import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnalysisType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation.LipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipidStatus;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.FattyAcylSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.ISpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SphingolipidSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SterolSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detects potentially missed lipid annotations in the Kendrick plot space.
 */
/**
 * Detects candidate false-negative lipid annotations by predicting the expected Kendrick mass
 * defect trend and DBE pattern for the surrounding lipid class, then flagging rows whose measured
 * values deviate from those predictions.
 */
public final class KendrickFalseNegativeDetector {

  private static final double CH2_EXACT_MASS = FormulaUtils.calculateExactMass("CH2");
  private static final double KENDRICK_FACTOR_CH2 = Math.round(CH2_EXACT_MASS) / CH2_EXACT_MASS;
  private static final double MZ_PPM_TOLERANCE = 18d;
  private static final double CH2_KMD_TOLERANCE = 0.018d;
  private static final double DBE_KMD_TOLERANCE = 0.024d;
  private static final int MIN_POINTS_PER_MODEL = 2;
  private static final @NotNull MZTolerance MZ_TOLERANCE_MS2 = new MZTolerance(0.005, 10d);

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  private final @NotNull ModularFeatureList featureList;
  private final @NotNull LipidAnalysisType lipidAnalysisType;
  private final @NotNull List<Ch2Model> ch2Models;
  private final @NotNull List<DbeModel> dbeModels;

  public KendrickFalseNegativeDetector(final @NotNull ModularFeatureList featureList) {
    this.featureList = featureList;
    lipidAnalysisType = requireNonNullElse(LipidQcScoringUtils.detectLipidAnalysisType(featureList),
        LipidAnalysisType.LC_REVERSED_PHASE);
    final List<AnnotatedPoint> annotatedPoints = collectAnnotatedPoints(featureList);
    ch2Models = buildCh2Models(annotatedPoints);
    dbeModels = buildDbeModels(annotatedPoints);
  }

  public @Nullable KendrickFalseNegativeCandidate detectCandidate(
      final @NotNull FeatureListRow row) {
    if (!row.getLipidMatches().isEmpty()) {
      return null;
    }
    final double mz = row.getAverageMZ();
    if (!Double.isFinite(mz) || mz <= 0d) {
      return null;
    }
    final double kmd = kendrickMassDefectForCh2(mz);
    final PolarityType rowPolarity = row.getRepresentativePolarity();
    final Map<CandidateKey, AggregateCandidate> candidates = new LinkedHashMap<>();

    for (final Ch2Model model : ch2Models) {
      if (!isPolarityCompatible(rowPolarity, model.ionizationType())) {
        continue;
      }
      final @Nullable Hypothesis hypothesis = model.evaluate(mz, kmd);
      if (hypothesis == null) {
        continue;
      }
      mergeHypothesis(candidates, hypothesis);
    }

    for (final DbeModel model : dbeModels) {
      if (!isPolarityCompatible(rowPolarity, model.ionizationType())) {
        continue;
      }
      final @Nullable Hypothesis hypothesis = model.evaluate(mz, kmd);
      if (hypothesis == null) {
        continue;
      }
      mergeHypothesis(candidates, hypothesis);
    }

    final @Nullable AggregateCandidate bestCandidate = candidates.values().stream()
        .sorted(Comparator.comparingInt(AggregateCandidate::supportCount).reversed()
            .thenComparingDouble(AggregateCandidate::combinedError))
        .findFirst().orElse(null);
    if (bestCandidate == null || bestCandidate.supportCount() <= 0) {
      return null;
    }

    final @Nullable SpeciesLevelAnnotation annotation = LIPID_FACTORY.buildSpeciesLevelLipid(
        bestCandidate.lipidClass(), bestCandidate.carbons(), bestCandidate.dbe(), 0);
    if (annotation == null) {
      return null;
    }

    final MatchedLipid initialMatch = new MatchedLipid(annotation, row.getAverageMZ(),
        bestCandidate.ionizationType(), Set.of(), 0d, MatchedLipidStatus.UNCONFIRMED);
    initialMatch.setComment("Potentially missed annotation inferred from Kendrick trend models.");
    final @Nullable MatchedLipid ms2MatchedSuggestion = tryMatchMs2(row, initialMatch);
    final MatchedLipid suggestion = ms2MatchedSuggestion != null ? ms2MatchedSuggestion : initialMatch;
    final double overallScore = computeCombinedAnnotationScore(featureList, row, suggestion, true,
        lipidAnalysisType.hasRetentionTimePattern(), lipidAnalysisType);
    final String detail = "Predicted from %s; %s".formatted(bestCandidate.supportLabel(),
        String.join("; ", bestCandidate.details())) + (ms2MatchedSuggestion != null
        ? "; MS2 support found (" + ms2MatchedSuggestion.getMatchedFragments().size()
          + " matched fragments)" : "; no MS2 support found");
    return new KendrickFalseNegativeCandidate(suggestion, overallScore, detail,
        bestCandidate.className(), bestCandidate.ch2TrendKmd(), bestCandidate.dbeTrendSlope(),
        bestCandidate.dbeTrendIntercept());
  }

  private static @Nullable MatchedLipid tryMatchMs2(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid candidateMatch) {
    final List<Scan> scans = row.getAllFragmentScans();
    if (scans.isEmpty()) {
      return null;
    }
    final LipidFragmentationRule[] rules = candidateMatch.getLipidAnnotation().getLipidClass()
        .getFragmentationRules();
    if (rules == null || rules.length == 0) {
      return null;
    }
    final ISpeciesLevelMatchedLipidFactory speciesFactory = getSpeciesLevelFactory(
        candidateMatch.getLipidAnnotation().getLipidClass().getCoreClass());
    final LipidAnnotationChainParameters chainParameters = new LipidAnnotationChainParameters();
    MatchedLipid bestMatch = null;
    for (final Scan scan : scans) {
      if (scan.getMassList() == null) {
        continue;
      }
      final LipidFragmentFactory fragmentFactory = new LipidFragmentFactory(MZ_TOLERANCE_MS2,
          candidateMatch.getLipidAnnotation(), candidateMatch.getIonizationType(), rules, scan,
          chainParameters);
      final Set<LipidFragment> fragments = new HashSet<>(fragmentFactory.findLipidFragments());
      if (fragments.isEmpty()) {
        continue;
      }
      final MatchedLipid matched = speciesFactory.validateSpeciesLevelAnnotation(row.getAverageMZ(),
          candidateMatch.getLipidAnnotation(), fragments, scan.getMassList().getDataPoints(), 0d,
          MZ_TOLERANCE_MS2, candidateMatch.getIonizationType());
      if (matched == null) {
        continue;
      }
      if (bestMatch == null || safeMs2Score(matched) > safeMs2Score(bestMatch)) {
        bestMatch = matched;
      }
    }
    return bestMatch;
  }

  private static double safeMs2Score(final @NotNull MatchedLipid match) {
    final Double score = match.getMsMsScore();
    return score == null ? 0d : score;
  }

  private static @NotNull ISpeciesLevelMatchedLipidFactory getSpeciesLevelFactory(
      final @NotNull LipidCategories lipidCategory) {
    return switch (lipidCategory) {
      case FATTYACYLS -> new FattyAcylSpeciesLevelMatchedLipidFactory();
      case GLYCEROLIPIDS, GLYCEROPHOSPHOLIPIDS ->
          new GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory();
      case SPHINGOLIPIDS -> new SphingolipidSpeciesLevelMatchedLipidFactory();
      case STEROLLIPIDS -> new SterolSpeciesLevelMatchedLipidFactory();
      case PRENOLLIPIDS, SACCHAROLIPIDS, POLYKETIDES ->
          new GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory();
    };
  }

  private static boolean isPolarityCompatible(final @Nullable PolarityType rowPolarity,
      final @NotNull IonizationType ionizationType) {
    if (rowPolarity == null || rowPolarity == PolarityType.UNKNOWN
        || rowPolarity == PolarityType.ANY) {
      return true;
    }
    return rowPolarity == ionizationType.getPolarity();
  }

  private static @NotNull List<AnnotatedPoint> collectAnnotatedPoints(
      final @NotNull ModularFeatureList featureList) {
    final List<AnnotatedPoint> points = new ArrayList<>();
    for (final FeatureListRow candidateRow : featureList.getRows()) {
      final @Nullable MatchedLipid match = LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(
          candidateRow);
      if (match == null) {
        continue;
      }
      final double mz = candidateRow.getAverageMZ();
      if (!Double.isFinite(mz) || mz <= 0d) {
        continue;
      }
      final @Nullable CarbonDbe carbonDbe = extractCarbonDbe(match.getLipidAnnotation());
      if (carbonDbe == null || carbonDbe.carbons() <= 0 || carbonDbe.dbe() < 0) {
        continue;
      }
      points.add(new AnnotatedPoint(match.getLipidAnnotation().getLipidClass(),
          match.getLipidAnnotation().getLipidClass().getName(), carbonDbe.carbons(),
          carbonDbe.dbe(), mz, kendrickMassDefectForCh2(mz), match.getIonizationType()));
    }
    return points;
  }

  private static @Nullable CarbonDbe extractCarbonDbe(final @NotNull ILipidAnnotation annotation) {
    final int carbons = annotation.getChainsCarbonCount();
    final int dbe = annotation.getChainsDoubleBondCount();
    if (carbons <= 0 || dbe < 0) {
      return null;
    }
    return new CarbonDbe(carbons, dbe);
  }

  private static @NotNull List<Ch2Model> buildCh2Models(
      final @NotNull List<AnnotatedPoint> annotatedPoints) {
    final Map<ClassDbeIonKey, List<AnnotatedPoint>> grouped = new HashMap<>();
    for (final AnnotatedPoint point : annotatedPoints) {
      grouped.computeIfAbsent(
              new ClassDbeIonKey(point.className(), point.dbe(), point.ionizationType()),
              _ -> new ArrayList<>())
          .add(point);
    }
    final List<Ch2Model> models = new ArrayList<>();
    for (final List<AnnotatedPoint> groupPoints : grouped.values()) {
      final @Nullable Ch2Model model = buildCh2Model(groupPoints);
      if (model != null) {
        models.add(model);
      }
    }
    return models;
  }

  private static @NotNull List<DbeModel> buildDbeModels(
      final @NotNull List<AnnotatedPoint> annotatedPoints) {
    final Map<ClassCarbonIonKey, List<AnnotatedPoint>> grouped = new HashMap<>();
    for (final AnnotatedPoint point : annotatedPoints) {
      grouped.computeIfAbsent(
              new ClassCarbonIonKey(point.className(), point.carbons(), point.ionizationType()),
              _ -> new ArrayList<>())
          .add(point);
    }
    final List<DbeModel> models = new ArrayList<>();
    for (final List<AnnotatedPoint> groupPoints : grouped.values()) {
      final @Nullable DbeModel model = buildDbeModel(groupPoints);
      if (model != null) {
        models.add(model);
      }
    }
    return models;
  }

  private static @Nullable Ch2Model buildCh2Model(final @NotNull List<AnnotatedPoint> points) {
    if (points.size() < MIN_POINTS_PER_MODEL
        || points.stream().mapToInt(AnnotatedPoint::carbons).distinct().count() < 2) {
      return null;
    }
    final @Nullable LinearModel mzModel = fitLinear(points.stream().map(AnnotatedPoint::carbons).toList(),
        points.stream().map(AnnotatedPoint::mz).toList());
    if (mzModel == null || mzModel.slope() <= 0.8d) {
      return null;
    }
    final double medianKmd = median(points.stream().map(AnnotatedPoint::kmd).toList());
    final AnnotatedPoint representative = points.getFirst();
    return new Ch2Model(representative.lipidClass(), representative.className(),
        representative.dbe(), representative.ionizationType(), mzModel.slope(),
        mzModel.intercept(), medianKmd, points.size());
  }

  private static @Nullable DbeModel buildDbeModel(final @NotNull List<AnnotatedPoint> points) {
    if (points.size() < MIN_POINTS_PER_MODEL
        || points.stream().mapToInt(AnnotatedPoint::dbe).distinct().count() < 2) {
      return null;
    }
    final @Nullable LinearModel mzModel = fitLinear(points.stream().map(AnnotatedPoint::dbe).toList(),
        points.stream().map(AnnotatedPoint::mz).toList());
    if (mzModel == null || mzModel.slope() >= -0.15d) {
      return null;
    }
    final @Nullable LinearModel kmdModel = fitLinear(points.stream().map(AnnotatedPoint::mz).toList(),
        points.stream().map(AnnotatedPoint::kmd).toList());
    if (kmdModel == null) {
      return null;
    }
    final AnnotatedPoint representative = points.getFirst();
    return new DbeModel(representative.lipidClass(), representative.className(),
        representative.carbons(), representative.ionizationType(), mzModel.slope(),
        mzModel.intercept(), kmdModel.slope(), kmdModel.intercept(), points.size());
  }

  private static void mergeHypothesis(
      final @NotNull Map<CandidateKey, AggregateCandidate> candidates,
      final @NotNull Hypothesis hypothesis) {
    final CandidateKey key = new CandidateKey(hypothesis.className(), hypothesis.carbons(),
        hypothesis.dbe(), hypothesis.ionizationType());
    final AggregateCandidate existing = candidates.get(key);
    if (existing == null) {
      candidates.put(key, new AggregateCandidate(hypothesis));
      return;
    }
    existing.merge(hypothesis);
  }

  private static double median(final @NotNull List<Double> values) {
    if (values.isEmpty()) {
      return 0d;
    }
    final List<Double> sorted = values.stream().filter(Double::isFinite).sorted().toList();
    if (sorted.isEmpty()) {
      return 0d;
    }
    final int middle = sorted.size() / 2;
    if (sorted.size() % 2 == 0) {
      return (sorted.get(middle - 1) + sorted.get(middle)) / 2d;
    }
    return sorted.get(middle);
  }

  private static @Nullable LinearModel fitLinear(final @NotNull List<? extends Number> xValues,
      final @NotNull List<? extends Number> yValues) {
    if (xValues.size() != yValues.size() || xValues.size() < 2) {
      return null;
    }
    double sumX = 0d;
    double sumY = 0d;
    double sumXX = 0d;
    double sumXY = 0d;
    int n = 0;
    for (int i = 0; i < xValues.size(); i++) {
      final double x = xValues.get(i).doubleValue();
      final double y = yValues.get(i).doubleValue();
      if (!Double.isFinite(x) || !Double.isFinite(y)) {
        continue;
      }
      sumX += x;
      sumY += y;
      sumXX += x * x;
      sumXY += x * y;
      n++;
    }
    if (n < 2) {
      return null;
    }
    final double denominator = n * sumXX - sumX * sumX;
    if (Math.abs(denominator) < 1e-9d) {
      return null;
    }
    final double slope = (n * sumXY - sumX * sumY) / denominator;
    final double intercept = (sumY - slope * sumX) / n;
    return new LinearModel(slope, intercept);
  }

  private static double kendrickMassDefectForCh2(final double mz) {
    final double kendrickMass = mz * KENDRICK_FACTOR_CH2;
    return Math.round(kendrickMass) - kendrickMass;
  }

  private static double ppmError(final double observedMz, final double expectedMz) {
    if (!Double.isFinite(observedMz) || !Double.isFinite(expectedMz) || expectedMz == 0d) {
      return Double.POSITIVE_INFINITY;
    }
    return Math.abs((observedMz - expectedMz) / expectedMz * 1e6d);
  }

  private record CarbonDbe(int carbons, int dbe) {

  }

  private record LinearModel(double slope, double intercept) {

  }

  private record ClassDbeIonKey(@NotNull String className, int dbe,
                                @NotNull IonizationType ionizationType) {

  }

  private record ClassCarbonIonKey(@NotNull String className, int carbons,
                                   @NotNull IonizationType ionizationType) {

  }

  private record CandidateKey(@NotNull String className, int carbons, int dbe,
                              @NotNull IonizationType ionizationType) {

  }

  private record AnnotatedPoint(@NotNull ILipidClass lipidClass, @NotNull String className,
                                int carbons, int dbe, double mz, double kmd,
                                @NotNull IonizationType ionizationType) {

  }

  private record Hypothesis(@NotNull ILipidClass lipidClass, @NotNull String className, int carbons,
                            int dbe, @NotNull IonizationType ionizationType, boolean ch2Support,
                            boolean dbeSupport, double normalizedError, @NotNull String detail,
                            @Nullable Double ch2TrendKmd, @Nullable Double dbeTrendSlope,
                            @Nullable Double dbeTrendIntercept) {

  }

  private static final class AggregateCandidate {

    private final @NotNull ILipidClass lipidClass;
    private final @NotNull String className;
    private final int carbons;
    private final int dbe;
    private final @NotNull IonizationType ionizationType;
    private boolean ch2Support;
    private boolean dbeSupport;
    private double combinedError;
    private @Nullable Double ch2TrendKmd;
    private @Nullable Double dbeTrendSlope;
    private @Nullable Double dbeTrendIntercept;
    private final @NotNull List<String> details = new ArrayList<>();

    private AggregateCandidate(final @NotNull Hypothesis hypothesis) {
      lipidClass = hypothesis.lipidClass();
      className = hypothesis.className();
      carbons = hypothesis.carbons();
      dbe = hypothesis.dbe();
      ionizationType = hypothesis.ionizationType();
      ch2Support = hypothesis.ch2Support();
      dbeSupport = hypothesis.dbeSupport();
      combinedError = hypothesis.normalizedError();
      ch2TrendKmd = hypothesis.ch2TrendKmd();
      dbeTrendSlope = hypothesis.dbeTrendSlope();
      dbeTrendIntercept = hypothesis.dbeTrendIntercept();
      details.add(hypothesis.detail());
    }

    private void merge(final @NotNull Hypothesis hypothesis) {
      ch2Support = ch2Support || hypothesis.ch2Support();
      dbeSupport = dbeSupport || hypothesis.dbeSupport();
      combinedError = Math.min(combinedError, hypothesis.normalizedError());
      if (ch2TrendKmd == null && hypothesis.ch2TrendKmd() != null) {
        ch2TrendKmd = hypothesis.ch2TrendKmd();
      }
      if (dbeTrendSlope == null && hypothesis.dbeTrendSlope() != null) {
        dbeTrendSlope = hypothesis.dbeTrendSlope();
      }
      if (dbeTrendIntercept == null && hypothesis.dbeTrendIntercept() != null) {
        dbeTrendIntercept = hypothesis.dbeTrendIntercept();
      }
      if (!details.contains(hypothesis.detail())) {
        details.add(hypothesis.detail());
      }
    }

    private @NotNull ILipidClass lipidClass() {
      return lipidClass;
    }

    private @NotNull String className() {
      return className;
    }

    private int carbons() {
      return carbons;
    }

    private int dbe() {
      return dbe;
    }

    private @NotNull IonizationType ionizationType() {
      return ionizationType;
    }

    private int supportCount() {
      int support = 0;
      if (ch2Support) {
        support++;
      }
      if (dbeSupport) {
        support++;
      }
      return support;
    }

    private double combinedError() {
      return combinedError;
    }

    private @NotNull List<String> details() {
      return List.copyOf(details);
    }

    private @NotNull String supportLabel() {
      return switch (supportCount()) {
        case 2 -> "CH2 and DBE trend support";
        case 1 -> ch2Support ? "CH2 trend support" : "DBE trend support";
        default -> "Kendrick trend support";
      };
    }

    private @Nullable Double ch2TrendKmd() {
      return ch2TrendKmd;
    }

    private @Nullable Double dbeTrendSlope() {
      return dbeTrendSlope;
    }

    private @Nullable Double dbeTrendIntercept() {
      return dbeTrendIntercept;
    }
  }

  private record Ch2Model(@NotNull ILipidClass lipidClass, @NotNull String className, int dbe,
                          @NotNull IonizationType ionizationType, double slopeMzPerCarbon,
                          double interceptMz, double medianKmd, int pointsCount) {

    private @Nullable Hypothesis evaluate(final double mz, final double kmd) {
      if (!Double.isFinite(mz) || !Double.isFinite(kmd)) {
        return null;
      }
      final double carbonEstimate = (mz - interceptMz) / slopeMzPerCarbon;
      if (!Double.isFinite(carbonEstimate)) {
        return null;
      }
      final int predictedCarbons = (int) Math.round(carbonEstimate);
      if (predictedCarbons <= 0) {
        return null;
      }
      final double expectedMz = interceptMz + slopeMzPerCarbon * predictedCarbons;
      final double ppm = ppmError(mz, expectedMz);
      if (ppm > MZ_PPM_TOLERANCE) {
        return null;
      }
      final double kmdError = Math.abs(kmd - medianKmd);
      if (kmdError > CH2_KMD_TOLERANCE) {
        return null;
      }
      final double normalizedError = ppm / MZ_PPM_TOLERANCE + kmdError / CH2_KMD_TOLERANCE;
      final String detail =
          "%s Δppm=%.1f, ΔKMD=%.4f (n=%d)".formatted(className, ppm, kmdError, pointsCount);
      return new Hypothesis(lipidClass, className, predictedCarbons, dbe, ionizationType, true,
          false, normalizedError, detail, medianKmd, null, null);
    }
  }

  private record DbeModel(@NotNull ILipidClass lipidClass, @NotNull String className, int carbons,
                          @NotNull IonizationType ionizationType, double slopeMzPerDbe,
                          double interceptMz, double kmdSlopeMz, double kmdIntercept,
                          int pointsCount) {

    private @Nullable Hypothesis evaluate(final double mz, final double kmd) {
      if (!Double.isFinite(mz) || !Double.isFinite(kmd)) {
        return null;
      }
      final double dbeEstimate = (mz - interceptMz) / slopeMzPerDbe;
      if (!Double.isFinite(dbeEstimate)) {
        return null;
      }
      final int predictedDbe = (int) Math.round(dbeEstimate);
      if (predictedDbe < 0) {
        return null;
      }
      final double expectedMz = interceptMz + slopeMzPerDbe * predictedDbe;
      final double ppm = ppmError(mz, expectedMz);
      if (ppm > MZ_PPM_TOLERANCE) {
        return null;
      }
      final double expectedKmd = kmdIntercept + kmdSlopeMz * mz;
      final double kmdError = Math.abs(kmd - expectedKmd);
      if (kmdError > DBE_KMD_TOLERANCE) {
        return null;
      }
      final double normalizedError = ppm / MZ_PPM_TOLERANCE + kmdError / DBE_KMD_TOLERANCE;
      final String detail =
          "%s Δppm=%.1f, ΔKMD=%.4f (n=%d)".formatted(className, ppm, kmdError, pointsCount);
      return new Hypothesis(lipidClass, className, carbons, predictedDbe, ionizationType, false,
          true, normalizedError, detail, null, kmdSlopeMz, kmdIntercept);
    }
  }
}
