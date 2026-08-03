package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The simplified isotope finder algorithm: exposes only m/z tolerance, the 13C requirement, and the
 * maximum charge, and runs the {@link CarbonAveragineAlgorithmModule} with defaults for everything
 * else.
 */
public class AutomaticIsotopeFinderModule implements IsotopeFinderAlgorithmModule {

  @Override
  public @NotNull String getName() {
    return "Automatic";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return AutomaticIsotopeFinderParameters.class;
  }

  @Override
  public @NotNull CarbonAveragineAlgorithmParameters resolve(@NotNull final ParameterSet params) {
    final MZTolerance tolerance = params.getValue(
        AutomaticIsotopeFinderParameters.isotopeMzTolerance);
    final boolean requireC13 = params.getValue(AutomaticIsotopeFinderParameters.requireC13);
    final int maxCharge = params.getValue(AutomaticIsotopeFinderParameters.maxCharge);

    final CarbonAveragineAlgorithmParameters full = CarbonAveragineAlgorithmParameters.createDefault();
    full.setAll(CarbonAveragineAlgorithmParameters.DEFAULT_ELEMENTS,
        CarbonAveragineAlgorithmParameters.DEFAULT_ELEMENT_DETECTION_MODE, tolerance, maxCharge,
        requireC13, CarbonAveragineAlgorithmParameters.DEFAULT_EXPLAINABLE_SIGNALS_ONLY,
        CarbonAveragineAlgorithmParameters.DEFAULT_FWHM_REFINE,
        CarbonAveragineEnvelopeParameters.createDefault());
    return full;
  }
}
