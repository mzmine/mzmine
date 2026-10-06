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

package io.github.mzmine.modules.tools.tools_autoparam.statistics;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MathUtils;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared raw-statistic rules used by the dashboard and single-pass optimizer estimate.
 */
public final class RawDataParameterEstimation {

  /**
   * Quantile of chromatogram edge intensities used as the absolute noise level for instruments
   * without an injection time. The optimum was near the seventh percentile across the measured
   * reference datasets.
   * <p>
   * assumption: the factor-of-lowest-signal detector has no matching distribution because edge
   * intensities are absolute counts, not factors.
   */
  private static final double ABSOLUTE_NOISE_QUANTILE = 0.07;

  /**
   * Share of the median isotope trace width used as the minimum number of consecutive scans. A peak
   * has to be resolvable from clearly fewer scans than a typical one spans, or the weaker half of
   * the isotopes is discarded, and clearly more than a couple, or noise passes as a peak.
   */
  private static final double MIN_CONSECUTIVE_SCAN_FRACTION = 0.5;

  private RawDataParameterEstimation() {
  }

  public static @Nullable Double estimateFwhm(double @NotNull [] fwhms) {
    return estimateQuantile(fwhms, 0.5);
  }

  /**
   * @param heights lowest isotope heights, converted to the reference injection time for
   *                injection-time data, see
   *                {@link DataFileStatistics#getInjectionTimeCorrectedLowestIsotopeHeights()}
   */
  public static @Nullable Double estimateMinHeight(double @NotNull [] heights) {
    return estimateQuantile(heights, 0.5);
  }

  /**
   * Injection time of a typical MS1 scan, used to convert heights measured in scans with other
   * injection times. Orbitrap intensities are normalized to the injection time, so the noise floor
   * of a scan scales with 1/IT, and the intense seed features have their apex in short-IT scans.
   * <p>
   * decision: the median scan. Higher quantiles lower the min height further, which the optimized
   * reference datasets did not consistently support.
   *
   * @param ms1Scans         the MS1 scans the statistics were computed from
   * @param effectiveRtRange the separation without dead volume and wash, or null to use all scans
   * @return the median injection time in ms, or null if the file is not injection-time normalized
   */
  public static @Nullable Double estimateReferenceInjectionTime(@NotNull RawDataFile file,
      @NotNull List<? extends Scan> ms1Scans, @Nullable SimpleFloatRange effectiveRtRange) {
    // decision: timsTOF reports injection times, but we don't scale the noise level with it.
    if (file instanceof IMSRawDataFile) {
      return null;
    }
    final double[] injectionTimes = ms1Scans.stream().filter(
            scan -> effectiveRtRange == null || effectiveRtRange.contains(scan.getRetentionTime()))
        .filter(Scan::hasInjectionTime).mapToDouble(Scan::getInjectionTime).filter(it -> it > 0)
        .toArray();
    return estimateQuantile(injectionTimes, 0.5);
  }

  /**
   * Returns the integer minimum-consecutive-scans value that is applied to the wizard.
   */
  public static @Nullable Double estimateMinConsecutiveScans(
      double @NotNull [] isotopeTraceWidths) {
    final Double median = estimateQuantile(isotopeTraceWidths, 0.5);
    return median == null ? null : (double) Math.round(MIN_CONSECUTIVE_SCAN_FRACTION * median);
  }

  /**
   * Selects one predefined tolerance step above the tolerance that most frequently covered the
   * observed isotope signals.
   */
  public static @NotNull MZTolerance estimateMzTolerance(
      @NotNull List<@NotNull DataFileStatistics> statistics) {
    final MZTolerance mostFrequentTolerance = statistics.stream()
        .map(DataFileStatistics::extractToleranceCounts)
        .flatMap(counts -> counts.entrySet().stream())
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, Integer::sum)).entrySet().stream()
        .max(Entry.comparingByValue()).map(Entry::getKey)
        .orElse(MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA);
    final List<MZTolerance> options = MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS;
    final int estimatedIndex = Math.clamp(options.indexOf(mostFrequentTolerance) + 1, 0,
        options.size() - 1);
    return options.get(estimatedIndex);
  }

  /**
   * This absolute-intensity value is not applicable to a factor-of-lowest-signal detector.
   */
  public static @Nullable Double estimateAbsoluteNoiseLevel(double @NotNull [] edgeIntensities) {
    return estimateQuantile(edgeIntensities, ABSOLUTE_NOISE_QUANTILE);
  }

  /**
   * Infers the mass detector convention when no wizard choice is available. Injection-time MS1 data
   * use a factor of the lowest signal; TOF-style absolute-intensity data do not.
   */
  public static @NotNull MassDetectorWizardOptions inferMassDetectorType(
      @NotNull List<@NotNull DataFileStatistics> statistics) {
    final boolean hasMs1InjectionTime = statistics.stream().map(DataFileStatistics::file).anyMatch(
        file -> !(file instanceof IMSRawDataFile) && file.getScans().stream()
            .anyMatch(scan -> scan.getMSLevel() == 1 && scan.hasInjectionTime()));
    return hasMs1InjectionTime ? MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL
        : MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL;
  }

  private static @Nullable Double estimateQuantile(double @NotNull [] values, double quantile) {
    return values.length == 0 ? null : MathUtils.calcQuantile(values, quantile);
  }
}
