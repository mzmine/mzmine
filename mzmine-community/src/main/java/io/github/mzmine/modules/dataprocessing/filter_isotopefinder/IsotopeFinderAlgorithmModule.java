package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * An isotope finder algorithm: the full setup of the detection run, selected by
 * {@link IsotopeFinderModeOptions}. Every algorithm currently resolves to the carbon-averagine
 * algorithm - a simplified option only exposes fewer parameters and fills the rest with sensible
 * defaults. Implementations must provide a public no-args constructor (they are instantiated by
 * reflection in {@link io.github.mzmine.main.MZmineCore#getModuleInstance}).
 */
public interface IsotopeFinderAlgorithmModule extends MZmineModule {

  /**
   * @param params the embedded parameters of this algorithm option.
   * @return a self-contained carbon-averagine parameter set that the engine is built from. Never the
   * instance passed in, so callers may not rely on identity.
   */
  @NotNull CarbonAveragineAlgorithmParameters resolve(@NotNull ParameterSet params);
}
