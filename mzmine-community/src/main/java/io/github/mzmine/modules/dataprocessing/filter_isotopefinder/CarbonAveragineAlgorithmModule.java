package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The full carbon-averagine isotope finder algorithm: estimates the carbon count from the searched
 * m/z to model the 13C envelope, with heavy-isotope-aware upper bounds. Requires no formula
 * prediction and exposes every parameter of the detection run.
 */
public class CarbonAveragineAlgorithmModule implements IsotopeFinderAlgorithmModule {

  @Override
  public @NotNull String getName() {
    return "Carbon-averagine";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return CarbonAveragineAlgorithmParameters.class;
  }

  @Override
  public @NotNull CarbonAveragineAlgorithmParameters resolve(@NotNull final ParameterSet params) {
    // clone so the engine never reads (or the caller never mutates) the live GUI/config instance
    return (CarbonAveragineAlgorithmParameters) params.cloneParameterSet();
  }
}
