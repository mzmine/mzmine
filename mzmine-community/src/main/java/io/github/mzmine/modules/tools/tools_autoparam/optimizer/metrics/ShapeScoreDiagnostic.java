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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics;

import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.FeatureDataType;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.annotations.shapeclassification.RtQualitySummaryType;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.AsymmetricGaussianPeak;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.FitQuality;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.GaussianDoublePeak;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.GaussianPeak;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakFitterUtils;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakModelFunction;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakQualitySummary;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakShapeClassification;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Measures how many features of a result list a strict chromatographic shape check would reject.
 * Diagnostic only — never an optimization objective, though the value does feed the optional shape
 * rejection constraint.
 * <p>
 * Three independent criteria are applied, and a feature is rejected when it fails any of them:
 * <ul>
 *   <li><b>shape fit</b> — the best of a Gaussian, an asymmetric Gaussian and a double Gaussian must
 *       reach an R² threshold, which is what the wizard's "Apply strict shape filtering" step
 *       demands</li>
 *   <li><b>double peak</b> — a double Gaussian winning the fit means two coeluting peaks the
 *       resolver failed to separate, however well that model happens to fit</li>
 *   <li><b>sign changes</b> — a chromatogram that reverses direction more than once every
 *       {@value #SIGN_CHANGE_POINTS} points is noise rather than a peak. Cheap, and it catches
 *       jagged traces that a lenient fit still accepts</li>
 * </ul>
 * They are deliberately independent: noise can occasionally reach a respectable R² against a broad
 * Gaussian, but it rarely does so while also being smooth, and an unsplit peak pair fits its own
 * model almost perfectly.
 * <p>
 * Reading the result depends on whether the shape filter was part of the queue. With it enabled every
 * surviving feature already passed the R² bar and only the stored score is available, so neither the
 * fit nor the double peak criterion can reject anything and the sign changes carry the result. With
 * it disabled nothing has been removed and the numbers describe the real amount of junk the
 * parameters produced — that is the informative case.
 */
public final class ShapeScoreDiagnostic {

  /**
   * Attribute key under which the rejected share is stored on a solution. Shared so the constraint
   * can read back what the diagnostic wrote.
   */
  public static final String ATTR_REMOVE_PERCENT = "Shape filter would remove / %";

  /**
   * Attribute key under which the share rejected as an unsplit peak pair is stored. Reported on its
   * own because it is the one rejection reason that blames the resolver rather than the noise
   * settings, so it calls for a different fix than the rest.
   * <p>
   * decision: named after {@link PeakShapeClassification#DOUBLE_GAUSSIAN}'s own label, so the
   * column matches the wording the rest of mzmine uses for the same classification.
   */
  public static final String ATTR_DOUBLE_PEAK_PERCENT = "Double peak / %";

  /**
   * Number of points within which a single slope reversal is tolerated. Two is strict enough to
   * remove jagged noise without rejecting real but sparsely sampled peaks.
   */
  private static final double SIGN_CHANGE_POINTS = 2d;

  /**
   * A ratio at or above this fails the sign change criterion.
   * {@link QualityParameters#getSignChanges} already subtracts the one reversal a clean single peak
   * necessarily has, so a well shaped chromatogram scores near zero.
   */
  private static final double MAX_SIGN_CHANGE_RATIO = 1d;

  /**
   * Maximum number of features inspected per evaluation. Fitting is the expensive part and the
   * result is only ever read as a proportion, so a uniform subsample is enough: at 2000 samples the
   * standard error of a rate near 20 % is below one percentage point, while a result list of 100k
   * features would otherwise cost 100k fits on every single candidate.
   */
  private static final int MAX_FITS = 10_000;

  // assumption: the same three models the feature filter fits, so the scores are comparable
  private static final List<PeakModelFunction> PEAK_MODELS = List.of(new GaussianPeak(),
      new AsymmetricGaussianPeak(), new GaussianDoublePeak());

  private ShapeScoreDiagnostic() {
  }

  /**
   * Counts the features a strict shape check would reject.
   *
   * @param minShapeScore the R² threshold, normally {@link io.github.mzmine.modules.dataprocessing.filter_featurefilter.FeatureFilterParameters#DEFAULT_SHAPE_SCORE}
   */
  public static @NotNull Result evaluate(@NotNull FeatureList featureList, double minShapeScore) {
    // decision: only the detected data points, so leading and trailing zeros neither flatten the fit
    // nor add spurious slope reversals
    final FeatureDataAccess access = EfficientDataAccess.of(featureList,
        FeatureDataType.ONLY_DETECTED);
    final int total = access.getNumOfFeatures();
    final int stride = Math.max(1, total / MAX_FITS);

    // reused across features, bounded by the widest feature in the list
    final double[] intensities = new double[access.getMaxNumberOfValues()];
    final double[] rts = new double[access.getMaxNumberOfValues()];

    int seen = 0;
    int inspected = 0;
    int rejected = 0;
    int poorShape = 0;
    int doublePeak = 0;
    int unfittable = 0;
    int signChanges = 0;

    while (access.hasNextFeature()) {
      access.nextFeature();
      if (seen++ % stride != 0) {
        continue;
      }
      inspected++;

      final int numValues = access.getNumberOfValues();
      for (int i = 0; i < numValues; i++) {
        intensities[i] = access.getIntensity(i);
        rts[i] = access.getRetentionTime(i);
      }

      boolean reject = false;

      // cheap first: a smooth peak barely changes direction
      if (QualityParameters.signChangesPerNPoints(0, numValues, intensities, SIGN_CHANGE_POINTS)
          >= MAX_SIGN_CHANGE_RATIO) {
        signChanges++;
        reject = true;
      }

      final Float stored = storedShapeScore(access.getFeature());
      if (stored != null) {
        // reuse the score the filter already stored, so an enabled filter costs no extra fitting
        if (stored < minShapeScore) {
          poorShape++;
          reject = true;
        }
      } else {
        // exact length copies, because the shared buffers keep stale values past numValues
        final FitQuality fit = PeakFitterUtils.fitPeakModels(Arrays.copyOf(rts, numValues),
            Arrays.copyOf(intensities, numValues), PEAK_MODELS);
        if (fit == null) {
          // the filter also drops what it cannot fit, e.g. fewer than five data points
          unfittable++;
          reject = true;
        } else {
          if (fit.rSquared() < minShapeScore) {
            poorShape++;
            reject = true;
          }
          // decision: counted separately instead of folded into the poor shape count. A double
          // Gaussian fits an unsplit peak pair well, so the R² criterion does not catch it, and the
          // two reasons call for different fixes - resolver settings versus noise settings.
          if (fit.peakShapeClassification() == PeakShapeClassification.DOUBLE_GAUSSIAN) {
            doublePeak++;
            reject = true;
          }
        }
      }

      if (reject) {
        rejected++;
      }
    }

    return new Result(total, inspected, rejected, poorShape, doublePeak, unfittable, signChanges);
  }

  private static @Nullable Float storedShapeScore(@NotNull Feature feature) {
    if (!(feature instanceof ModularFeature modular)) {
      return null;
    }
    final PeakQualitySummary summary = modular.get(RtQualitySummaryType.class);
    return summary != null ? summary.shapeClassificationScore() : null;
  }

  /**
   * @param totalFeatures features in the list
   * @param inspected     features actually checked, capped at {@link #MAX_FITS}
   * @param rejected      inspected features failing at least one criterion
   * @param poorShape     inspected features whose best fit was below the threshold
   * @param doublePeak    inspected features best described by a double Gaussian, i.e. two coeluting
   *                      peaks the resolver left merged
   * @param unfittable    inspected features that could not be fitted at all — usually too few data
   *                      points, which points at the minimum consecutive data points parameter
   *                      rather than at noise
   * @param signChanges   inspected features reversing direction too often to be a peak. All four
   *                      reasons overlap, because a feature can fail several criteria — only
   *                      {@code rejected} counts every feature once
   */
  public record Result(int totalFeatures, int inspected, int rejected, int poorShape,
                       int doublePeak, int unfittable, int signChanges) {

    /**
     * Rejected share of the inspected features, in percent, rounded to one decimal.
     */
    public double wouldRemovePercent() {
      return percentOf(rejected);
    }

    public double doublePeakPercent() {
      return percentOf(doublePeak);
    }

    public double unfittablePercent() {
      return percentOf(unfittable);
    }

    public double signChangesPercent() {
      return percentOf(signChanges);
    }

    /**
     * The rejected share extrapolated to the full list.
     */
    public long wouldRemove() {
      return Math.round(totalFeatures * rejected / (double) Math.max(1, inspected));
    }

    private double percentOf(int count) {
      if (inspected == 0) {
        return 0d;
      }
      return Math.round(1000d * count / inspected) / 10d;
    }

    @Override
    public @NotNull String toString() {
      return "%.1f %% rejected (%.1f %% poor shape, %.1f %% double peak, %.1f %% unfittable, %.1f %% sign changes), from %d of %d features".formatted(
          wouldRemovePercent(), percentOf(poorShape), doublePeakPercent(), unfittablePercent(),
          signChangesPercent(), inspected, totalFeatures);
    }
  }
}
