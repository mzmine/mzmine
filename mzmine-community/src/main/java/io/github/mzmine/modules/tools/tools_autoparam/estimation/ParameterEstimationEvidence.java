/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import io.github.mzmine.modules.tools.tools_autoparam.StatisticsPlotType;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMassDetectorNoiseLevels;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.DataFileStatistics;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Bounded aggregate evidence for a prepared parameter estimate.
 *
 * <p>The record deliberately contains no raw-file identities, spectra, traces, or metadata.</p>
 */
public record ParameterEstimationEvidence(@NotNull String basis, @NotNull String explanation,
                                          int observationCount,
                                          @NotNull String observationUnit,
                                          @NotNull Map<String, Double> measurements,
                                          @NotNull List<String> limitations) {

  public ParameterEstimationEvidence {
    if (observationCount < 0) {
      throw new IllegalArgumentException("Observation count must not be negative");
    }
    measurements = Map.copyOf(measurements.entrySet().stream()
        .filter(entry -> Double.isFinite(entry.getValue()))
        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
            (_, _) -> { throw new IllegalArgumentException("Duplicate measurement key"); },
            LinkedHashMap::new)));
    limitations = List.copyOf(limitations);
  }

  public static @NotNull ParameterEstimationEvidence describe(
      final @NotNull ParameterEstimationContext context,
      final @NotNull PreparedParameter<?> parameter) {
    final ParameterDefinition<?> definition = parameter.definition();
    if (definition.equals(OptimizationParameterRegistry.RT_CORRECTION)) {
      return rtCorrection(context.analysis());
    }

    final RawDataAnalysis analysis = context.analysis();
    if (definition.equals(OptimizationParameterRegistry.FWHM)) {
      return measured(parameter, analysis.fwhms().length, "isotope-trace widths",
          Map.of("median_peak_width_minutes", median(analysis.fwhms())),
          "Median measured isotope-trace width.");
    }
    if (definition.equals(OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT)) {
      return measured(parameter, analysis.heights().length, "lowest-isotope heights",
          Map.of("median_lowest_isotope_height", median(analysis.heights())),
          "Median lowest-isotope height, corrected to each file's reference injection time when available; raw heights are used otherwise.");
    }
    if (definition.equals(OptimizationParameterRegistry.MINIMUM_CONSECUTIVE_SCANS)) {
      return measured(parameter, analysis.consecutiveScans().length, "isotope-trace scan counts",
          Map.of("median_scan_count", median(analysis.consecutiveScans())),
          "Half the median measured isotope-trace scan count, rounded by the estimator.");
    }
    if (definition.equals(OptimizationParameterRegistry.MS1_NOISE)) {
      return noiseEvidence(parameter, analysis);
    }
    if (definition.equals(OptimizationParameterRegistry.MZ_TOLERANCE)) {
      return mzToleranceEvidence(parameter, analysis);
    }
    if (definition.equals(OptimizationParameterRegistry.SAMPLE_TO_SAMPLE_MZ_TOLERANCE)) {
      return measured(parameter, analysis.sampleMzToleranceCounts().values().stream()
              .mapToInt(Integer::intValue).sum(), "aligned rows with m/z deviations",
          Map.of("target_coverage_fraction", 0.8d),
          "Smallest predefined tolerance covering at least 80% of the aligned-row m/z deviations; fixed during optimization.");
    }
    if (definition.equals(OptimizationParameterRegistry.CROP_RT)) {
      final long effectiveRanges = analysis.files().stream()
          .filter(file -> file.effectiveRtRange() != null).count();
      final boolean detected = effectiveRanges > 0 && effectiveRanges >= 0.5 * analysis.files().size();
      return measured(parameter, analysis.files().size(), "analyzed files",
          Map.of("files_with_detected_separation_range", (double) effectiveRanges),
          detected ? "Median start and end of detected separation ranges; fixed during optimization."
              : "Union of recorded MS1 retention-time ranges because fewer than half the files have a detected separation range; fixed during optimization.");
    }
    if (definition.equals(OptimizationParameterRegistry.POLARITY)) {
      return measured(parameter, analysis.files().size(), "analyzed files",
          Map.of(), "Polarity fixed before statistics were computed, from the wizard choice checked against the files or from unambiguous raw-file polarity; fixed during optimization.");
    }
    if (definition.equals(OptimizationParameterRegistry.INTER_SAMPLE_RT)) {
      return measured(parameter, analysis.rtDeviations().length, "aligned feature RT deviations",
          Map.of("p98_rt_deviation_minutes", quantile(analysis.rtDeviations(), 0.98)),
          "98th percentile of aligned feature retention-time deviations.");
    }
    if (definition.equals(OptimizationParameterRegistry.MOBILITY_FWHM)) {
      return preset(parameter, "No mobility-width measurement is currently derived by this estimator.");
    }
    if (definition.equals(OptimizationParameterRegistry.TOP_TO_EDGE)) {
      return heuristic(parameter,
          "Fixed starting ratio for the local minimum resolver. It requires a peak's top intensity to rise above its edge intensities by this ratio, helping reject small fluctuations. "
              + "Keep it, edit it, or include it in separate optimization.");
    }
    if (definition.equals(OptimizationParameterRegistry.CHROMATOGRAPHIC_THRESHOLD)) {
      return heuristic(parameter,
          "Fixed starting percentile for the local minimum resolver. It excludes chromatogram points below the selected intensity percentile to suppress low-intensity background. "
              + "Keep it, edit it, or include it in separate optimization.");
    }
    return originOnly(parameter);
  }

  /**
   * Native plot types that directly support a parameter estimate. This keeps bounded review
   * evidence tied to the same registry definitions as the estimators instead of display names.
   */
  public static @NotNull List<StatisticsPlotType> plotTypes(
      final @NotNull ParameterDefinition<?> definition) {
    if (definition.equals(OptimizationParameterRegistry.FWHM)
        || definition.equals(OptimizationParameterRegistry.RT_CORRECTION)) {
      return List.of(StatisticsPlotType.FWHM);
    }
    if (definition.equals(OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT)) {
      return List.of(StatisticsPlotType.LOWEST_ISOTOPE_HEIGHT);
    }
    if (definition.equals(OptimizationParameterRegistry.MINIMUM_CONSECUTIVE_SCANS)) {
      return List.of(StatisticsPlotType.ISOTOPE_DATA_POINTS);
    }
    if (definition.equals(OptimizationParameterRegistry.MS1_NOISE)) {
      return List.of(StatisticsPlotType.EDGE_INTENSITY);
    }
    if (definition.equals(OptimizationParameterRegistry.MZ_TOLERANCE)) {
      return List.of(StatisticsPlotType.BEST_TOLERANCE_FREQUENCY);
    }
    return List.of();
  }

  public static @NotNull ParameterEstimationEvidence rtCorrection(
      final @NotNull RawDataAnalysis analysis) {
    final double[] medians = analysis.fileMedianRtDeviations();
    final double[] widths = analysis.fwhms();
    final Map<String, Double> measurements = new LinkedHashMap<>();
    add(measurements, "eligible_file_count", medians.length);
    add(measurements, "median_peak_width_minutes", median(widths));
    add(measurements, "median_file_deviation_minutes", median(medians));
    add(measurements, "largest_file_deviation_minutes", maximum(medians));
    add(measurements, "decision_threshold_minutes",
        ParameterEstimators.rtCorrectionThreshold(medians, widths));
    add(measurements, "minimum_eligible_file_count", 3d);
    add(measurements, "minimum_shared_observations_per_file", 5d);
    add(measurements, "minimum_row_detection_fraction", 0.8d);
    add(measurements, "minimum_row_detection_floor", 3d);

    final List<String> limitations = new ArrayList<>();
    limitations.add("Only aligned rows detected in at least max(3, ceil(80% of analyzed files)) contribute; each eligible file needs at least five shared observations.");
    limitations.add("Shared-row count is unavailable; the analysis retains only per-file aggregate deviations.");
    limitations.add("The aggregate deviations are absolute, so they do not show drift direction or chromatographic region.");
    if (medians.length < 3 || widths.length == 0) {
      limitations.add("There are fewer than three eligible files or no peak-width measurements, so this estimate has insufficient evidence.");
      return new ParameterEstimationEvidence("preset_default",
          "The RT-correction estimator has insufficient measured evidence.", medians.length,
          "eligible files", measurements, limitations);
    }
    limitations.add("Correction feasibility and benefit are not assessed here.");
    return new ParameterEstimationEvidence("measured_derived_heuristic",
        "Correction is indicated only when the largest eligible file deviation is strictly greater than max(3 × median file deviation, 0.5 × median peak width).",
        medians.length, "eligible files", measurements, limitations);
  }

  private static @NotNull ParameterEstimationEvidence noiseEvidence(
      final @NotNull PreparedParameter<?> parameter, final @NotNull RawDataAnalysis analysis) {
    if (parameter.origin() == ValueOrigin.PRESET_DEFAULT) {
      return preset(parameter, "No chromatogram-edge intensity measurements are available.");
    }
    if (parameter.origin() == ValueOrigin.HEURISTIC) {
      return heuristic(parameter,
          "The factor-of-lowest-signal detector starts at the fixed factor 5; MS2 noise is derived as MS1 divided by 2.5 and is not independently measured.");
    }
    final Map<String, Double> measurements = new LinkedHashMap<>();
    add(measurements, "edge_intensity_p07", quantile(analysis.edgeIntensities(), 0.07));
    if (parameter.initialValue() instanceof WizardMassDetectorNoiseLevels levels) {
      add(measurements, "ms1_noise_level", levels.getMs1NoiseLevel());
      add(measurements, "ms2_noise_level", levels.getMsnNoiseLevel());
      add(measurements, "ms2_to_ms1_divisor", 2.5d);
    }
    return new ParameterEstimationEvidence("measured_derived_heuristic",
        "MS1 noise is the seventh percentile of chromatogram-edge intensities; MS2 noise is MS1 divided by 2.5.",
        analysis.edgeIntensities().length, "chromatogram-edge intensities", measurements,
        List.of("MS2 noise is derived from MS1 rather than independently measured."));
  }

  private static @NotNull ParameterEstimationEvidence mzToleranceEvidence(
      final @NotNull PreparedParameter<?> parameter, final @NotNull RawDataAnalysis analysis) {
    if (parameter.origin() != ValueOrigin.RAW_DATA) {
      return parameter.origin() == ValueOrigin.PRESET_DEFAULT ? preset(parameter,
          "No observed isotope-tolerance counts are available.") : heuristic(parameter,
          "A preset tolerance option is used because no file statistics are available.");
    }
    final int count = analysis.files().stream().map(DataFileStatistics::extractToleranceCounts)
        .flatMap(counts -> counts.values().stream()).mapToInt(Integer::intValue).sum();
    if (count == 0) {
      return new ParameterEstimationEvidence("native_fallback",
          "No within-file isotope-tolerance observations are available; the native estimator uses its predefined fallback tolerance option.",
          0, "within-file isotope-tolerance observations", Map.of(),
          List.of("The prepared parameter retains RAW_DATA origin because files were present, but this value is not derived from observed isotope tolerances."));
    }
    return new ParameterEstimationEvidence("measured_derived_rule",
        "The most frequent sufficient isotope tolerance selects the next predefined tolerance option.",
        count, "within-file isotope-tolerance observations", Map.of(),
        List.of("The selected option is constrained by the wizard detector and resolution settings."));
  }

  private static @NotNull ParameterEstimationEvidence measured(
      final @NotNull PreparedParameter<?> parameter, final int observationCount,
      final @NotNull String observationUnit, final @NotNull Map<String, Double> measurements,
      final @NotNull String explanation) {
    if (parameter.origin() == ValueOrigin.PRESET_DEFAULT) {
      return preset(parameter, "Insufficient measurements for this estimator.");
    }
    return new ParameterEstimationEvidence("measured_derived_rule", explanation, observationCount,
        observationUnit, finiteMeasurements(measurements), List.of());
  }

  private static @NotNull ParameterEstimationEvidence heuristic(
      final @NotNull PreparedParameter<?> parameter, final @NotNull String explanation) {
    return new ParameterEstimationEvidence("heuristic", explanation, 0, "observations", Map.of(),
        List.of());
  }

  private static @NotNull ParameterEstimationEvidence preset(
      final @NotNull PreparedParameter<?> parameter, final @NotNull String explanation) {
    return new ParameterEstimationEvidence("preset_default", explanation, 0, "observations", Map.of(),
        List.of("This is a preset-derived value; no data estimate is available."));
  }

  private static @NotNull ParameterEstimationEvidence originOnly(
      final @NotNull PreparedParameter<?> parameter) {
    return switch (parameter.origin()) {
      case HEURISTIC -> heuristic(parameter,
          "This parameter has a heuristic initial value; no parameter-specific evidence description is registered.");
      case PRESET_DEFAULT -> preset(parameter,
          "This parameter has a preset-derived initial value; no parameter-specific evidence description is registered.");
      case RAW_DATA -> new ParameterEstimationEvidence("measured_derived_rule",
          "This parameter is derived from measured data, but no parameter-specific aggregate evidence description is registered.",
          0, "observations", Map.of(), List.of("The estimator does not expose a bounded observation count for this parameter."));
    };
  }

  private static @NotNull Map<String, Double> finiteMeasurements(
      final @NotNull Map<String, Double> measurements) {
    final Map<String, Double> finite = new LinkedHashMap<>();
    measurements.forEach((key, value) -> add(finite, key, value));
    return finite;
  }

  private static void add(final @NotNull Map<String, Double> measurements,
      final @NotNull String key, final double value) {
    if (Double.isFinite(value)) {
      measurements.put(key, value);
    }
  }

  private static double median(final double @NotNull [] values) {
    return quantile(values, 0.5);
  }

  private static double quantile(final double @NotNull [] values, final double quantile) {
    return values.length == 0 ? Double.NaN : io.github.mzmine.util.MathUtils.calcQuantileSorted(values,
        quantile);
  }

  private static double maximum(final double @NotNull [] values) {
    return values.length == 0 ? Double.NaN : values[values.length - 1];
  }
}
