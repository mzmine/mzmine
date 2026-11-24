package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An ion a fragmentation rule produces for an example lipid.
 *
 * @param mz         exact m/z
 * @param ionFormula formula of the ion
 * @param chain      the chain of the fragment, e.g., "Acyl chain 18:1", or null for head group and
 *                   precursor rules
 */
public record SimulatedIon(double mz, @NotNull String ionFormula, @Nullable String chain) {

}
