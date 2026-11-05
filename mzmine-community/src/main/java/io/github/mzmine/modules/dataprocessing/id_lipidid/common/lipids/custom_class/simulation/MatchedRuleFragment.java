package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A peak of a measured spectrum explained by a fragmentation rule.
 *
 * @param exactMz    theoretical m/z of the fragment
 * @param observedMz m/z of the matched peak
 * @param intensity  intensity of the matched peak
 * @param ionFormula formula of the fragment ion
 * @param chain      chain of the fragment, e.g., "Acyl chain 18:1", or null
 */
public record MatchedRuleFragment(double exactMz, double observedMz, double intensity,
                                  @NotNull String ionFormula, @Nullable String chain) {

  /**
   * @return mass error in ppm
   */
  public double ppm() {
    return (observedMz - exactMz) / exactMz * 1e6;
  }
}
