package io.github.mzmine.modules.dataanalysis.batchquality;

import io.github.mzmine.datamodel.FeatureStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Descriptive within-batch QC abundance trend; it does not apply corrections or acceptance rules. */
public final class RunOrderDrift {

  public enum Status {
    AVAILABLE, INSUFFICIENT_USABLE_POINTS, INVALID_ORDER, NUMERICAL_UNAVAILABLE
  }

  public record Observation(@NotNull String sampleId, double order, @Nullable Double abundance,
                            @Nullable FeatureStatus status) {
  }

  public record Exclusions(int estimated, int aggregated, int missing, int nonfinite, int negative) {
  }

  public record Result(@NotNull Status status, int usedPointCount, @NotNull Exclusions exclusions,
                       @Nullable Double slope, @Nullable Double intercept, @Nullable Double rSquared,
                       @Nullable Double mean, @Nullable Double fittedFirst, @Nullable Double fittedLast,
                       @Nullable Double changeFraction, @Nullable Double firstOrder,
                       @Nullable Double lastOrder) {
  }

  private RunOrderDrift() {
  }

  public static @NotNull Result summarize(final @NotNull List<Observation> observations) {
    final List<Observation> usable = new ArrayList<>();
    int estimated = 0;
    int aggregated = 0;
    int missing = 0;
    int nonfinite = 0;
    int negative = 0;
    for (final Observation observation : observations) {
      if (observation.status() == FeatureStatus.ESTIMATED) {
        estimated++;
      } else if (observation.status() == FeatureStatus.COMPOUND_AGGREGATED) {
        aggregated++;
      } else if (observation.status() != FeatureStatus.DETECTED && observation.status() != FeatureStatus.MANUAL) {
        missing++;
      } else if (observation.abundance() == null) {
        missing++;
      } else if (!Double.isFinite(observation.abundance())) {
        nonfinite++;
      } else if (observation.abundance() < 0d) {
        negative++;
      } else {
        usable.add(observation);
      }
    }
    final Exclusions exclusions = new Exclusions(estimated, aggregated, missing, nonfinite, negative);
    if (!validOrders(observations)) return unavailable(Status.INVALID_ORDER, exclusions);
    if (usable.size() < 3) return unavailable(Status.INSUFFICIENT_USABLE_POINTS, exclusions);
    usable.sort(Comparator.comparingDouble(Observation::order).thenComparing(Observation::sampleId));
    final Regression regression = regress(usable);
    if (regression == null) return unavailable(Status.NUMERICAL_UNAVAILABLE, exclusions);
    final double firstOrder = usable.getFirst().order();
    final double lastOrder = usable.getLast().order();
    final double fittedFirst = regression.intercept + regression.slope * firstOrder;
    final double fittedLast = regression.intercept + regression.slope * lastOrder;
    final Double changeFraction = regression.mean > 0d
        ? finite((fittedLast - fittedFirst) / regression.mean) : null;
    if (!Double.isFinite(fittedFirst) || !Double.isFinite(fittedLast)) return unavailable(Status.NUMERICAL_UNAVAILABLE, exclusions);
    return new Result(Status.AVAILABLE, usable.size(), exclusions, regression.slope, regression.intercept,
        regression.rSquared, regression.mean, fittedFirst, fittedLast, changeFraction, firstOrder, lastOrder);
  }

  private static boolean validOrders(final @NotNull List<Observation> observations) {
    final Set<Long> seen = new java.util.HashSet<>();
    for (final Observation observation : observations) {
      final double order = observation.order();
      if (!Double.isFinite(order) || !seen.add(Double.doubleToLongBits(order == 0d ? 0d : order))) return false;
    }
    return true;
  }

  private static @Nullable Regression regress(final @NotNull List<Observation> observations) {
    double meanX = 0d;
    double meanY = 0d;
    double sumXX = 0d;
    double sumXY = 0d;
    double sumYY = 0d;
    int count = 0;
    for (final Observation point : observations) {
      count++;
      final double deltaX = point.order() - meanX;
      final double deltaY = point.abundance() - meanY;
      meanX += deltaX / count;
      meanY += deltaY / count;
      sumXX += deltaX * (point.order() - meanX);
      sumXY += deltaX * (point.abundance() - meanY);
      sumYY += deltaY * (point.abundance() - meanY);
      if (!Double.isFinite(meanX) || !Double.isFinite(meanY) || !Double.isFinite(sumXX)
          || !Double.isFinite(sumXY) || !Double.isFinite(sumYY)) return null;
    }
    if (sumXX <= 0d) return null;
    final double slope = sumXY / sumXX;
    final double intercept = meanY - slope * meanX;
    if (!Double.isFinite(slope) || !Double.isFinite(intercept)) return null;
    final Double rSquared = sumYY == 0d ? null : rSquared(sumXX, sumXY, sumYY);
    return new Regression(slope, intercept, rSquared, meanY);
  }

  private static @Nullable Double finite(final double value) {
    return Double.isFinite(value) ? value : null;
  }

  private static @Nullable Double rSquared(final double sumXX, final double sumXY, final double sumYY) {
    final double correlation = sumXY / Math.sqrt(sumXX) / Math.sqrt(sumYY);
    if (!Double.isFinite(correlation)) return null;
    return Math.max(0d, Math.min(1d, correlation * correlation));
  }

  private static @NotNull Result unavailable(final @NotNull Status status, final @NotNull Exclusions exclusions) {
    return new Result(status, 0, exclusions, null, null, null, null, null, null, null, null, null);
  }

  private record Regression(double slope, double intercept, @Nullable Double rSquared, double mean) {
  }
}
