/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataanalysis.mcp.study;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.special.Gamma;
import org.jetbrains.annotations.NotNull;

/**
 * Numerical kernels for explicitly specified advanced study designs.
 *
 * <p>Linear and generalized least-squares calculations follow the standard Gaussian linear-model
 * likelihood. The variance moderation is the scaled inverse-chi-square empirical-Bayes model
 * described by Smyth, 2004, doi:10.2202/1544-6115.1027. It intentionally is a small, non-robust,
 * non-trended implementation; it does not claim to reproduce limma's complete fitting pipeline.
 */
public final class AdvancedStudyStatistics {

  private static final double RANK_TOLERANCE = 1e-10d;
  private static final double VARIANCE_FLOOR = 1e-300d;
  private static final NormalDistribution STANDARD_NORMAL = new NormalDistribution();

  private AdvancedStudyStatistics() {
  }

  /**
   * A fixed-effect fit; the covariance matrix is unscaled and must be multiplied by residualVariance.
   *
   * <p>{@code logLikelihood} is the maximized Gaussian maximum-likelihood value when available.
   * {@code remlCriterion} is minus twice the restricted log likelihood, including its normalizing
   * constants. Random-intercept fits optimize REML, so their maximum-likelihood value is unavailable.
   */
  public record Fit(@NotNull String status, double @NotNull [] coefficients,
                    double @NotNull [][] unscaledCovariance, double residualVariance,
                    double residualDf, int observations, int subjects, double randomInterceptVariance,
                    double logLikelihood, double remlCriterion, @NotNull List<String> warnings) {

    public Fit {
      coefficients = coefficients.clone();
      unscaledCovariance = copy(unscaledCovariance);
      warnings = List.copyOf(warnings);
    }

    /** The restricted log likelihood represented by {@link #remlCriterion()}, if it is available. */
    public double restrictedLogLikelihood() {
      return Double.isFinite(remlCriterion) ? -0.5d * remlCriterion : Double.NaN;
    }
  }

  /** A single requested linear contrast. CI is t-based for OLS and asymptotic normal for mixed models. */
  public record Contrast(@NotNull String status, double effect, double standardError, double statistic,
                         double pValue, double confidenceIntervalLower, double confidenceIntervalUpper,
                         double degreesOfFreedom, @NotNull String inferenceMethod,
                         @NotNull List<String> warnings) {
    public Contrast {
      warnings = List.copyOf(warnings);
    }
  }

  /** Empirical-Bayes variance fit and moderated contrast results in the input-feature order. */
  public record Moderation(@NotNull String status, double priorDf, double priorVariance,
                           double @NotNull [] posteriorVariances, double @NotNull [] totalDfs,
                           @NotNull List<Contrast> contrasts, @NotNull List<String> warnings) {
    public Moderation {
      posteriorVariances = posteriorVariances.clone();
      totalDfs = totalDfs.clone();
      contrasts = List.copyOf(contrasts);
      warnings = List.copyOf(warnings);
    }
  }

  /** Fits an OLS model. The supplied design must contain every desired column, including an intercept. */
  public static @NotNull Fit fitLinear(final double @NotNull [][] design, final double @NotNull [] values) {
    validateInputs(design, values);
    final int n = values.length;
    final int p = design[0].length;
    if (n <= p) return unavailable(n, 0, "More observations than fitted coefficients are required.");
    final RealMatrix x = new Array2DRowRealMatrix(design, true);
    final SingularValueDecomposition svd = new SingularValueDecomposition(x);
    if (rank(svd) < p) return unavailable(n, 0, "The supplied design is rank deficient.");
    final RealVector beta = svd.getSolver().solve(new ArrayRealVector(values, false));
    final RealVector residual = new ArrayRealVector(values, false).subtract(x.operate(beta));
    final double rss = residual.dotProduct(residual);
    final double df = n - p;
    if (!positiveResidualVariance(rss, values) || !Double.isFinite(rss)) return unavailable(n, 0,
        "Residual variance is zero or non-finite; standard errors are unavailable.");
    // decision: derive (X'X)^-1 from the already rank-checked pseudoinverse to avoid squaring conditioning.
    final RealMatrix pseudoInverse = svd.getSolver().getInverse();
    final RealMatrix inverse = pseudoInverse.multiply(pseudoInverse.transpose());
    final double sigma2 = rss / df;
    final double maximumLikelihoodSigma2 = rss / n;
    double logDetInformation = 0d;
    for (final double singular : svd.getSingularValues()) logDetInformation += 2d * Math.log(singular);
    final double normalizingConstant = Math.log(2d * Math.PI) + 1d;
    final double logLikelihood = -0.5d * n * (normalizingConstant + Math.log(maximumLikelihoodSigma2));
    final double criterion = logDetInformation + df * (normalizingConstant + Math.log(sigma2));
    return new Fit("available", beta.toArray(), inverse.getData(), sigma2, df, n, 0, 0d,
        logLikelihood, criterion, List.of());
  }

  /**
   * Fits a Gaussian random-intercept model by profiling REML over lambda=tau²/sigma².
   *
   * <p>The requested contrast uses an asymptotic Wald normal distribution because a general,
   * defensible small-sample denominator-df approximation is not supplied here.
   */
  public static @NotNull Fit fitRandomIntercept(final double @NotNull [][] design,
      final double @NotNull [] values, final @NotNull List<String> subjects,
      final @NotNull BooleanSupplier canceled) {
    validateInputs(design, values);
    if (subjects.size() != values.length) throw new IllegalArgumentException("subjects must match values");
    final int n = values.length;
    final int p = design[0].length;
    if (n <= p) return unavailable(n, 0, "More observations than fitted coefficients are required.");
    final Map<String, List<Integer>> bySubject = new LinkedHashMap<>();
    for (int i = 0; i < subjects.size(); i++) {
      final String subject = subjects.get(i);
      if (subject == null || subject.isBlank()) throw new IllegalArgumentException("subjects must be nonblank");
      bySubject.computeIfAbsent(subject, ignored -> new ArrayList<>()).add(i);
    }
    final long repeatedSubjects = bySubject.values().stream().filter(indices -> indices.size() > 1).count();
    if (bySubject.size() < 3 || repeatedSubjects < 2) return unavailable(n, bySubject.size(),
        "A random-intercept model requires at least three subjects and repeated observations for at least two subjects.");
    checkCanceled(canceled);
    final double upper = 1e8d;
    final Profile zero = profile(design, values, bySubject, 0d);
    if (!zero.available()) return unavailable(n, bySubject.size(), zero.warning());
    Profile best = zero;
    // decision: a log grid captures both near-zero and large variance ratios before local refinement.
    double left = 0d;
    double right = upper;
    for (int i = 0; i <= 80; i++) {
      checkCanceled(canceled);
      final double lambda = i == 0 ? 0d : Math.pow(10d, -8d + i * 16d / 80d);
      final Profile point = profile(design, values, bySubject, lambda);
      if (point.available() && point.criterion < best.criterion) best = point;
    }
    // Golden-section refinement on lambda (not log lambda) is deliberately skipped at the zero boundary.
    if (best.lambda > 0d && best.lambda < upper) {
      left = Math.max(0d, best.lambda / 3d);
      right = Math.min(upper, best.lambda * 3d);
      for (int i = 0; i < 60; i++) {
        checkCanceled(canceled);
        final double a = left + (right - left) * 0.3819660112501051d;
        final double b = left + (right - left) * 0.6180339887498949d;
        final Profile pa = profile(design, values, bySubject, a);
        final Profile pb = profile(design, values, bySubject, b);
        if (pa.available() && pb.available() && pa.criterion < pb.criterion) right = b;
        else left = a;
      }
      final Profile refined = profile(design, values, bySubject, (left + right) / 2d);
      if (refined.available() && refined.criterion < best.criterion) best = refined;
    }
    final List<String> warnings = new ArrayList<>();
    warnings.add("Fixed-effect contrast inference uses an asymptotic Wald normal distribution.");
    warnings.add("Maximum-likelihood log likelihood is unavailable because the random-intercept variance was optimized with REML.");
    if (best.lambda == 0d) warnings.add("The REML optimum is the no-random-intercept boundary.");
    if (best.lambda >= upper * 0.999d) warnings.add("The variance-ratio search reached its upper boundary.");
    return new Fit("available", best.beta, best.inverse, best.sigma2, n - p, n, bySubject.size(),
        best.lambda * best.sigma2, Double.NaN, best.criterion, warnings);
  }

  /** Calculates a requested contrast and its two-sided confidence interval. */
  public static @NotNull Contrast contrast(final @NotNull Fit fit, final double @NotNull [] contrast,
      final double confidenceLevel) {
    if (!"available".equals(fit.status())) return unavailableContrast(fit.status(), fit.warnings());
    if (contrast.length != fit.coefficients().length) throw new IllegalArgumentException("contrast length must match coefficients");
    validateConfidence(confidenceLevel);
    final double effect = dot(contrast, fit.coefficients());
    final double variance = quadratic(contrast, fit.unscaledCovariance()) * fit.residualVariance();
    if (!(variance > 0d) || !Double.isFinite(variance)) return unavailableContrast("contrast_unavailable",
        "Contrast variance is zero or non-finite.");
    final double se = Math.sqrt(variance);
    final double statistic = effect / se;
    final boolean mixed = fit.subjects() > 0;
    final double quantile;
    final double p;
    final String method;
    if (mixed) {
      quantile = STANDARD_NORMAL.inverseCumulativeProbability(0.5d + confidenceLevel / 2d);
      p = 2d * STANDARD_NORMAL.cumulativeProbability(-Math.abs(statistic));
      method = "asymptotic_wald_normal";
    } else {
      final TDistribution t = new TDistribution(fit.residualDf());
      quantile = t.inverseCumulativeProbability(0.5d + confidenceLevel / 2d);
      p = 2d * t.cumulativeProbability(-Math.abs(statistic));
      method = "ols_t";
    }
    return new Contrast("available", effect, se, statistic, clampProbability(p), effect - quantile * se,
        effect + quantile * se, mixed ? Double.POSITIVE_INFINITY : fit.residualDf(), method, fit.warnings());
  }

  /** Fits one shared scaled-inverse-chi-square prior and returns t-moderated contrasts. */
  public static @NotNull Moderation moderate(final @NotNull List<Fit> fits,
      final double @NotNull [] contrast, final double confidenceLevel) {
    validateConfidence(confidenceLevel);
    final List<Integer> usable = new ArrayList<>();
    for (int i = 0; i < fits.size(); i++) {
      final Fit fit = fits.get(i);
      if ("available".equals(fit.status()) && fit.subjects() == 0 && fit.residualDf() > 0d
          && fit.residualVariance() > VARIANCE_FLOOR && Double.isFinite(fit.residualVariance())
          && contrast.length == fit.coefficients().length) usable.add(i);
    }
    if (usable.size() < 3) return unavailableModeration(fits, "At least three finite OLS residual variances are required for moderation.");
    double meanAdjustedLog = 0d, meanObservationVariance = 0d, pooledNumerator = 0d,
        pooledDf = 0d;
    final double[] adjustedLogs = new double[usable.size()];
    int usableIndex = 0;
    for (final int index : usable) {
      final Fit fit = fits.get(index);
      final double adjusted = Math.log(fit.residualVariance()) - (Gamma.digamma(fit.residualDf() / 2d)
          - Math.log(fit.residualDf() / 2d));
      adjustedLogs[usableIndex++] = adjusted;
      meanAdjustedLog += adjusted;
      pooledNumerator += fit.residualDf() * fit.residualVariance();
      pooledDf += fit.residualDf();
    }
    meanAdjustedLog /= usable.size();
    for (final double adjusted : adjustedLogs) {
      final double delta = adjusted - meanAdjustedLog;
      meanObservationVariance += delta * delta;
    }
    meanObservationVariance /= usable.size() - 1d;
    double samplingVariance = 0d;
    for (final int index : usable) samplingVariance += Gamma.trigamma(fits.get(index).residualDf() / 2d);
    samplingVariance /= usable.size();
    final double priorLogVariance = meanObservationVariance - samplingVariance;
    final boolean commonVariance = !(priorLogVariance > 1e-10d) || !Double.isFinite(priorLogVariance);
    final double priorDf = commonVariance ? Double.POSITIVE_INFINITY : solvePriorDf(priorLogVariance);
    if (!commonVariance && (!Double.isFinite(priorDf) || priorDf <= 0d)) return unavailableModeration(fits,
        "Could not estimate a finite empirical-Bayes variance prior.");
    final double priorVariance = commonVariance ? pooledNumerator / pooledDf : Math.exp(meanAdjustedLog
        + Gamma.digamma(priorDf / 2d) - Math.log(priorDf / 2d));
    if (!(priorVariance > 0d) || !Double.isFinite(priorVariance)) return unavailableModeration(fits,
        "Could not estimate a finite empirical-Bayes prior variance.");
    final double[] posterior = new double[fits.size()];
    final double[] totalDf = new double[fits.size()];
    Arrays.fill(posterior, Double.NaN);
    Arrays.fill(totalDf, Double.NaN);
    final List<Contrast> output = new ArrayList<>(fits.size());
    for (final Fit fit : fits) {
      if (!"available".equals(fit.status()) || fit.subjects() != 0 || fit.residualDf() <= 0d
          || fit.residualVariance() <= VARIANCE_FLOOR || contrast.length != fit.coefficients().length) {
        output.add(unavailableContrast("moderation_unavailable", "This feature is not eligible for OLS variance moderation."));
        continue;
      }
      final int index = output.size();
      final double s2 = commonVariance ? priorVariance
          : (fit.residualDf() * fit.residualVariance() + priorDf * priorVariance)
              / (fit.residualDf() + priorDf);
      posterior[index] = s2;
      totalDf[index] = commonVariance ? pooledDf : Math.min(pooledDf, fit.residualDf() + priorDf);
      final double effect = dot(contrast, fit.coefficients());
      final double variance = quadratic(contrast, fit.unscaledCovariance()) * s2;
      if (!(variance > 0d) || !Double.isFinite(variance)) {
        output.add(unavailableContrast("moderation_unavailable", "The moderated contrast variance is unavailable."));
        continue;
      }
      final double se = Math.sqrt(variance);
      final double stat = effect / se;
      final TDistribution t = new TDistribution(totalDf[index]);
      final double q = t.inverseCumulativeProbability(0.5d + confidenceLevel / 2d);
      output.add(new Contrast("available", effect, se, stat, clampProbability(2d * t.cumulativeProbability(-Math.abs(stat))),
          effect - q * se, effect + q * se, totalDf[index], "empirical_bayes_moderated_t", List.of()));
    }
    final List<String> warnings = commonVariance ? List.of(
        "No excess variance dispersion was estimable; used the pooled common-variance limit.") : List.of(
        "Scaled inverse-chi-square prior estimated across eligible OLS feature fits.");
    return new Moderation("available", priorDf, priorVariance, posterior, totalDf, output, warnings);
  }

  private static @NotNull Profile profile(final double[][] x, final double[] y,
      final Map<String, List<Integer>> bySubject, final double lambda) {
    final int p = x[0].length;
    final double[][] information = new double[p][p];
    final double[] right = new double[p];
    double yInformationY = 0d, logDetV = 0d;
    for (final List<Integer> indices : bySubject.values()) {
      final double factor = lambda / (1d + lambda * indices.size());
      logDetV += Math.log1p(lambda * indices.size());
      final double[] sumX = new double[p];
      double sumY = 0d;
      for (final int row : indices) {
        sumY += y[row];
        for (int a = 0; a < p; a++) {
          sumX[a] += x[row][a];
          right[a] += x[row][a] * y[row];
          for (int b = 0; b < p; b++) information[a][b] += x[row][a] * x[row][b];
        }
        yInformationY += y[row] * y[row];
      }
      yInformationY -= factor * sumY * sumY;
      for (int a = 0; a < p; a++) {
        right[a] -= factor * sumX[a] * sumY;
        for (int b = 0; b < p; b++) information[a][b] -= factor * sumX[a] * sumX[b];
      }
    }
    final SingularValueDecomposition svd = new SingularValueDecomposition(new Array2DRowRealMatrix(information, false));
    if (rank(svd) < p) return Profile.unavailable("The generalized least-squares design is rank deficient.");
    final RealMatrix inverse = svd.getSolver().getInverse();
    final double[] beta = svd.getSolver().solve(new ArrayRealVector(right, false)).toArray();
    final double rss = yInformationY - dot(beta, right);
    final double df = y.length - p;
    if (!positiveResidualVariance(rss, y) || !Double.isFinite(rss)) return Profile.unavailable("REML residual variance is zero or non-finite.");
    double logDetInformation = 0d;
    for (final double singular : svd.getSingularValues()) {
      if (singular > RANK_TOLERANCE * svd.getSingularValues()[0]) logDetInformation += Math.log(singular);
    }
    final double sigma2 = rss / df;
    final double remlCriterion = logDetV + logDetInformation + df * (Math.log(2d * Math.PI) + 1d
        + Math.log(sigma2));
    return new Profile(lambda, beta, inverse.getData(), sigma2, remlCriterion, null);
  }

  private record Profile(double lambda, double[] beta, double[][] inverse, double sigma2, double criterion,
                         String warning) {
    static @NotNull Profile unavailable(final @NotNull String warning) {
      return new Profile(Double.NaN, new double[0], new double[0][0], Double.NaN, Double.NaN, warning);
    }
    boolean available() { return warning == null; }
  }

  private static double solvePriorDf(final double targetTrigamma) {
    double low = 1e-6d, high = 2d;
    while (Gamma.trigamma(high / 2d) > targetTrigamma && high < 1e8d) high *= 2d;
    if (high >= 1e8d) return Double.NaN;
    for (int i = 0; i < 100; i++) {
      final double mid = (low + high) / 2d;
      if (Gamma.trigamma(mid / 2d) > targetTrigamma) low = mid;
      else high = mid;
    }
    return (low + high) / 2d;
  }

  private static void validateInputs(final double[][] design, final double[] values) {
    if (design.length == 0 || values.length == 0 || design.length != values.length) throw new IllegalArgumentException(
        "design and values must be nonempty and have equal length");
    if (design[0].length == 0) throw new IllegalArgumentException("design must have at least one column");
    final int p = design[0].length;
    for (int i = 0; i < design.length; i++) {
      if (design[i].length != p || !Double.isFinite(values[i])) throw new IllegalArgumentException(
          "design rows and values must be finite and rectangular");
      for (final double value : design[i]) if (!Double.isFinite(value)) throw new IllegalArgumentException("design must be finite");
    }
  }

  private static void validateConfidence(final double confidenceLevel) {
    if (!(confidenceLevel > 0d && confidenceLevel < 1d) || !Double.isFinite(confidenceLevel)) {
      throw new IllegalArgumentException("confidenceLevel must be between zero and one");
    }
  }

  private static int rank(final @NotNull SingularValueDecomposition svd) {
    final double[] singular = svd.getSingularValues();
    if (singular.length == 0 || singular[0] == 0d) return 0;
    int rank = 0;
    for (final double value : singular) if (value > RANK_TOLERANCE * singular[0]) rank++;
    return rank;
  }

  private static @NotNull Fit unavailable(final int observations, final int subjects, final @NotNull String warning) {
    return new Fit("unavailable", new double[0], new double[0][0], Double.NaN, Double.NaN, observations,
        subjects, Double.NaN, Double.NaN, Double.NaN, List.of(warning));
  }

  private static @NotNull Contrast unavailableContrast(final @NotNull String status, final @NotNull String warning) {
    return new Contrast(status, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
        Double.NaN, "unavailable", List.of(warning));
  }

  private static @NotNull Contrast unavailableContrast(final @NotNull String status,
      final @NotNull List<String> warnings) {
    return new Contrast(status, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
        Double.NaN, "unavailable", warnings.isEmpty() ? List.of("Model fit is unavailable.") : warnings);
  }

  private static @NotNull Moderation unavailableModeration(final @NotNull List<Fit> fits,
      final @NotNull String warning) {
    final List<Contrast> contrasts = new ArrayList<>();
    for (int i = 0; i < fits.size(); i++) contrasts.add(unavailableContrast("moderation_unavailable", warning));
    final double[] unavailable = new double[fits.size()];
    Arrays.fill(unavailable, Double.NaN);
    return new Moderation("unavailable", Double.NaN, Double.NaN, unavailable, unavailable, contrasts, List.of(warning));
  }

  private static double dot(final double[] left, final double[] right) {
    double result = 0d;
    for (int i = 0; i < left.length; i++) result += left[i] * right[i];
    return result;
  }

  private static double quadratic(final double[] vector, final double[][] matrix) {
    double result = 0d;
    for (int i = 0; i < vector.length; i++) for (int j = 0; j < vector.length; j++) result += vector[i] * matrix[i][j] * vector[j];
    return result;
  }

  private static double clampProbability(final double value) {
    return Math.max(0d, Math.min(1d, value));
  }

  private static void checkCanceled(final @NotNull BooleanSupplier canceled) {
    if (canceled.getAsBoolean()) throw new CancellationException("Advanced study statistics calculation canceled.");
  }

  private static boolean positiveResidualVariance(final double rss, final double[] values) {
    double scale = 1d;
    for (final double value : values) scale = Math.max(scale, value * value);
    // assumption: residual sums smaller than floating-point solution noise have no inferential variance.
    return rss > 1e-24d * scale;
  }

  private static double[][] copy(final double[][] source) {
    final double[][] copy = new double[source.length][];
    for (int i = 0; i < source.length; i++) copy[i] = source[i].clone();
    return copy;
  }
}
