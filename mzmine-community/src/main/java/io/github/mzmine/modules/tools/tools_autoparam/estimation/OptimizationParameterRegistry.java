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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import com.google.common.collect.Range;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonMobilityWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMassDetectorNoiseLevels;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.DoubleSearchDomain;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.SearchScale;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Definitions connect typed estimators directly to wizard parameters and batch overrides.
 */
public final class OptimizationParameterRegistry {

  static final WizardParameterDefinition<Double> MINIMUM_FEATURE_HEIGHT = new WizardParameterDefinition<>(
      "Min height", WizardPart.MS, MassSpectrometerWizardParameters.minimumFeatureHeight,
      ParameterEstimators::minimumFeatureHeight);
  static final WizardParameterDefinition<WizardMassDetectorNoiseLevels> MS1_NOISE = new WizardParameterDefinition<>(
      "MS1 noise level", WizardPart.MS, MassSpectrometerWizardParameters.massDetectorOption,
      ParameterEstimators::ms1Noise);
  static final WizardParameterDefinition<MZTolerance> MZ_TOLERANCE = new WizardParameterDefinition<>(
      "MZ tolerance option", WizardPart.MS, MassSpectrometerWizardParameters.scanToScanMzTolerance,
      ParameterEstimators::mzTolerance);

  static final WizardParameterDefinition<RTTolerance> FWHM = new WizardParameterDefinition<>("FWHM",
      WizardPart.ION_INTERFACE, IonInterfaceHplcWizardParameters.approximateChromatographicFWHM,
      ParameterEstimators::chromatographicFwhm);
  static final WizardParameterDefinition<Integer> MINIMUM_CONSECUTIVE_SCANS = new WizardParameterDefinition<>(
      "Min consecutive", WizardPart.ION_INTERFACE,
      IonInterfaceHplcWizardParameters.minNumberOfDataPoints,
      ParameterEstimators::minimumConsecutiveScans);
  static final WizardParameterDefinition<RTTolerance> INTER_SAMPLE_RT = new WizardParameterDefinition<>(
      "Inter sample RT tolerance", WizardPart.ION_INTERFACE,
      IonInterfaceHplcWizardParameters.interSampleRTTolerance, ParameterEstimators::interSampleRt);
  static final WizardParameterDefinition<Boolean> RT_CORRECTION = new WizardParameterDefinition<>(
      "RT correction", WizardPart.ION_INTERFACE, IonInterfaceHplcWizardParameters.scanRtCorrection,
      ParameterEstimators::rtCorrection);

  /**
   * decision: estimate only. Not part of {@link #allSolutions()}, so the optimizer never offers it,
   * but prepared and applied for the sequence like every other estimate.
   */
  static final WizardParameterDefinition<Range<Double>> CROP_RT = new WizardParameterDefinition<>(
      "Crop retention time", WizardPart.ION_INTERFACE, IonInterfaceHplcWizardParameters.cropRtRange,
      ParameterEstimators::cropRtRange);

  static final WizardParameterDefinition<Double> MOBILITY_FWHM = new WizardParameterDefinition<>(
      "FWHM (mobility)", WizardPart.IMS, IonMobilityWizardParameters.approximateImsFWHM,
      ParameterEstimators::mobilityFwhm);

  static final BatchParameterDefinition<Double> TOP_TO_EDGE = new BatchParameterDefinition<>(
      "Top-to-edge ratio", MinimumSearchFeatureResolverModule.class,
      MinimumSearchFeatureResolverParameters.MIN_RATIO, ApplicationScope.FIRST,
      _ -> new ParameterEstimate<>(1.7d, ValueOrigin.HEURISTIC,
          new DoubleSearchDomain(1.5d, 3d, SearchScale.LINEAR)));
  static final BatchParameterDefinition<Double> CHROMATOGRAPHIC_THRESHOLD = new BatchParameterDefinition<>(
      "Chrom. Threshold", MinimumSearchFeatureResolverModule.class,
      MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL,
      ApplicationScope.FIRST, _ -> new ParameterEstimate<>(0.85d, ValueOrigin.HEURISTIC,
      new DoubleSearchDomain(0.5d, 0.97d, SearchScale.LINEAR)));

  private static final List<ParameterDefinition<?>> WAVELET = WaveletParameterDefinitions.definitions();
  private static final List<ParameterDefinition<?>> DEFAULTS = sorted(
      List.of(MINIMUM_FEATURE_HEIGHT, MS1_NOISE, MZ_TOLERANCE, FWHM, MINIMUM_CONSECUTIVE_SCANS,
          INTER_SAMPLE_RT, RT_CORRECTION, MOBILITY_FWHM, TOP_TO_EDGE, CHROMATOGRAPHIC_THRESHOLD));

  private OptimizationParameterRegistry() {
  }

  public static @NotNull List<ParameterDefinition<?>> allSolutions() {
    return sorted(java.util.stream.Stream.concat(DEFAULTS.stream(), WAVELET.stream()).toList());
  }

  public static @NotNull List<ParameterDefinition<?>> defaultSolutions() {
    return DEFAULTS;
  }

  public static @NotNull List<ParameterDefinition<?>> forSequence(
      @NotNull WizardSequence sequence) {
    return sorted(
        sequence.stream().flatMap(step -> forFactory(step.getFactory()).stream()).distinct()
            .toList());
  }

  private static @NotNull List<ParameterDefinition<?>> forFactory(
      @NotNull WizardParameterFactory factory) {
    if (factory instanceof MassSpectrometerWizardParameterFactory) {
      return List.of(MS1_NOISE, MZ_TOLERANCE, MINIMUM_FEATURE_HEIGHT);
    }
    if (factory instanceof IonMobilityWizardParameterFactory mobility) {
      return switch (mobility) {
        case NO_IMS -> List.of();
        case TIMS, IMS, DTIMS, TWIMS, SLIM -> List.of(MOBILITY_FWHM);
      };
    }
    if (factory instanceof IonInterfaceWizardParameterFactory ionInterface) {
      return switch (ionInterface) {
        case HPLC, UHPLC, HILIC ->
            List.of(FWHM, MINIMUM_CONSECUTIVE_SCANS, INTER_SAMPLE_RT, RT_CORRECTION, TOP_TO_EDGE,
                CHROMATOGRAPHIC_THRESHOLD, CROP_RT);
        // assumption: the run phase detection targets LC (solvent gradient, salts), not GC
        case GC_CI ->
            List.of(FWHM, MINIMUM_CONSECUTIVE_SCANS, INTER_SAMPLE_RT, RT_CORRECTION, TOP_TO_EDGE,
                CHROMATOGRAPHIC_THRESHOLD);
        case LC_WAVELET -> java.util.stream.Stream.concat(
                List.of(MINIMUM_CONSECUTIVE_SCANS, INTER_SAMPLE_RT, RT_CORRECTION, CROP_RT).stream(),
                WAVELET.stream())
            .toList();
        case GC_EI -> List.of(FWHM, MINIMUM_CONSECUTIVE_SCANS, INTER_SAMPLE_RT);
        case MALDI, LDI, DESI, SIMS, DIRECT_INFUSION, FLOW_INJECT -> List.of();
      };
    }
    return List.of();
  }

  private static @NotNull List<ParameterDefinition<?>> sorted(
      @NotNull List<ParameterDefinition<?>> definitions) {
    return definitions.stream().sorted(Comparator.comparing(ParameterDefinition::name)).toList();
  }
}
