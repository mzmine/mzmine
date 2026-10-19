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

import static io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory.GC_CI;
import static io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory.GC_EI;
import static io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory.HILIC;
import static io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory.HPLC;
//import static io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory.LC_WAVELET;
import static io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory.UHPLC;

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
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMsPolarity;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.DoubleSearchDomain;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.SearchScale;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * Definitions connect typed estimators directly to wizard parameters and batch overrides. Each
 * definition declares its {@link OptimizationRole} and the wizard presets it applies to; all lists
 * are derived from these.
 */
public final class OptimizationParameterRegistry {

  /**
   * Presets whose batch uses the local minimum resolver.
   */
  private static final Set<WizardParameterFactory> LOCAL_MINIMUM_RESOLVER = presets(HPLC, UHPLC,
      HILIC, GC_CI);
  private static final Set<WizardParameterFactory> ALL_IMS = presets(
      IonMobilityWizardParameterFactory.valuesExceptNoIms());

  // ----------------- HPLC wizard
  static final WizardParameterDefinition<RTTolerance> FWHM = new WizardParameterDefinition<>("FWHM",
      WizardPart.ION_INTERFACE, IonInterfaceHplcWizardParameters.approximateChromatographicFWHM,
      OptimizationRole.SELECTED_BY_DEFAULT, presets(HPLC, UHPLC, HILIC, GC_CI, GC_EI),
      ParameterEstimators::chromatographicFwhm);
  static final WizardParameterDefinition<Integer> MINIMUM_CONSECUTIVE_SCANS = new WizardParameterDefinition<>(
      "Min consecutive", WizardPart.ION_INTERFACE,
      IonInterfaceHplcWizardParameters.minNumberOfDataPoints, OptimizationRole.SELECTED_BY_DEFAULT,
      presets(HPLC, UHPLC, HILIC, GC_CI, /*LC_WAVELET,*/ GC_EI),
      ParameterEstimators::minimumConsecutiveScans);
  static final WizardParameterDefinition<RTTolerance> INTER_SAMPLE_RT = new WizardParameterDefinition<>(
      "Inter sample RT tolerance", WizardPart.ION_INTERFACE,
      IonInterfaceHplcWizardParameters.interSampleRTTolerance, OptimizationRole.SELECTED_BY_DEFAULT,
      presets(HPLC, UHPLC, HILIC, GC_CI, /*LC_WAVELET,*/ GC_EI),
      ParameterEstimators::interSampleRt);
  static final WizardParameterDefinition<Boolean> RT_CORRECTION = new WizardParameterDefinition<>(
      "RT correction", WizardPart.ION_INTERFACE, IonInterfaceHplcWizardParameters.scanRtCorrection,
      OptimizationRole.SELECTED_BY_DEFAULT, presets(HPLC, UHPLC, HILIC, GC_CI/*, LC_WAVELET*/),
      ParameterEstimators::rtCorrection);
  /**
   * decision: estimate only, prepared and applied for the sequence like every other estimate.
   * assumption: the run phase detection targets LC (solvent gradient, salts), not GC
   */
  static final WizardParameterDefinition<Range<Double>> CROP_RT = new WizardParameterDefinition<>(
      "Crop retention time", WizardPart.ION_INTERFACE, IonInterfaceHplcWizardParameters.cropRtRange,
      OptimizationRole.ESTIMATE_ONLY, presets(HPLC, UHPLC, HILIC/*, LC_WAVELET*/),
      ParameterEstimators::cropRtRange);

  // ---------------- MS
  private static final Set<WizardParameterFactory> ALL_MS = presets(
      MassSpectrometerWizardParameterFactory.values());
  static final WizardParameterDefinition<Double> MINIMUM_FEATURE_HEIGHT = new WizardParameterDefinition<>(
      "Min height", WizardPart.MS, MassSpectrometerWizardParameters.minimumFeatureHeight,
      OptimizationRole.SELECTED_BY_DEFAULT, ALL_MS, ParameterEstimators::minimumFeatureHeight);
  static final WizardParameterDefinition<WizardMassDetectorNoiseLevels> MS1_NOISE = new WizardParameterDefinition<>(
      "MS1 noise level", WizardPart.MS, MassSpectrometerWizardParameters.massDetectorOption,
      OptimizationRole.SELECTED_BY_DEFAULT, ALL_MS, ParameterEstimators::ms1Noise);
  static final WizardParameterDefinition<MZTolerance> MZ_TOLERANCE = new WizardParameterDefinition<>(
      "MZ tolerance option", WizardPart.MS, MassSpectrometerWizardParameters.scanToScanMzTolerance,
      OptimizationRole.SELECTED_BY_DEFAULT, ALL_MS, ParameterEstimators::mzTolerance);
  /**
   * decision: estimate only. Derived from the alignment of the representative files, so every
   * candidate and the wizard use the same cross-file tolerance.
   */
  static final WizardParameterDefinition<MZTolerance> SAMPLE_TO_SAMPLE_MZ_TOLERANCE = new WizardParameterDefinition<>(
      "Sample-to-sample m/z tolerance", WizardPart.MS,
      MassSpectrometerWizardParameters.sampleToSampleMzTolerance, OptimizationRole.ESTIMATE_ONLY,
      ALL_MS, ParameterEstimators::sampleToSampleMzTolerance);
  /**
   * decision: estimate only. The polarity is fixed by the pre-classification before the statistics
   * are computed, so every candidate and the wizard use it.
   */
  static final WizardParameterDefinition<WizardMsPolarity> POLARITY = new WizardParameterDefinition<>(
      "Ion mode", WizardPart.MS, MassSpectrometerWizardParameters.polarity,
      OptimizationRole.ESTIMATE_ONLY, ALL_MS, ParameterEstimators::polarity);
  static final WizardParameterDefinition<Double> MOBILITY_FWHM = new WizardParameterDefinition<>(
      "FWHM (mobility)", WizardPart.IMS, IonMobilityWizardParameters.approximateImsFWHM,
      OptimizationRole.SELECTED_BY_DEFAULT, ALL_IMS, ParameterEstimators::mobilityFwhm);

  // --------------- batch parameters - Local min resolver
  static final BatchParameterDefinition<Double> TOP_TO_EDGE = new BatchParameterDefinition<>(
      "Top-to-edge ratio", MinimumSearchFeatureResolverModule.class,
      MinimumSearchFeatureResolverParameters.MIN_RATIO, ApplicationScope.FIRST,
      OptimizationRole.SELECTED_BY_DEFAULT, LOCAL_MINIMUM_RESOLVER,
      _ -> new ParameterEstimate<>(1.7d, ValueOrigin.HEURISTIC,
          new DoubleSearchDomain(1.5d, 3d, SearchScale.LINEAR), ParameterEstimators.FIXED_DEFAULT));
  static final BatchParameterDefinition<Double> CHROMATOGRAPHIC_THRESHOLD = new BatchParameterDefinition<>(
      "Chrom. Threshold", MinimumSearchFeatureResolverModule.class,
      MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL,
      ApplicationScope.FIRST, OptimizationRole.SELECTED_BY_DEFAULT, LOCAL_MINIMUM_RESOLVER,
      _ -> new ParameterEstimate<>(0.85d, ValueOrigin.HEURISTIC,
          new DoubleSearchDomain(0.5d, 0.97d, SearchScale.LINEAR),
          ParameterEstimators.FIXED_DEFAULT));

  private static final List<ParameterDefinition<?>> ALL = sorted(Stream.concat(
      Stream.of(MINIMUM_FEATURE_HEIGHT, MS1_NOISE, MZ_TOLERANCE, SAMPLE_TO_SAMPLE_MZ_TOLERANCE,
          POLARITY, FWHM, MINIMUM_CONSECUTIVE_SCANS, INTER_SAMPLE_RT, RT_CORRECTION, CROP_RT,
          MOBILITY_FWHM, TOP_TO_EDGE, CHROMATOGRAPHIC_THRESHOLD),
      WaveletParameterDefinitions.definitions().stream()).toList());

  private OptimizationParameterRegistry() {
  }

  /**
   * @return all definitions that can be selected for optimization
   */
  public static @NotNull List<ParameterDefinition<?>> allSolutions() {
    return ALL.stream().filter(d -> d.role() != OptimizationRole.ESTIMATE_ONLY).toList();
  }

  /**
   * @return the definitions selected for optimization in new configurations
   */
  public static @NotNull List<ParameterDefinition<?>> defaultSolutions() {
    return ALL.stream().filter(d -> d.role() == OptimizationRole.SELECTED_BY_DEFAULT).toList();
  }

  /**
   * @return all definitions that apply to the presets of the sequence, including estimate-only
   * definitions
   */
  public static @NotNull List<ParameterDefinition<?>> forSequence(
      @NotNull WizardSequence sequence) {
    return ALL.stream().filter(d -> d.appliesTo(sequence)).toList();
  }

  static @NotNull Set<WizardParameterFactory> presets(
      @NotNull WizardParameterFactory... factories) {
    return Set.of(factories);
  }

  private static @NotNull List<ParameterDefinition<?>> sorted(
      @NotNull List<ParameterDefinition<?>> definitions) {
    return definitions.stream().sorted(Comparator.comparing(ParameterDefinition::name)).toList();
  }
}
