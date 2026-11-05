package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

import org.jetbrains.annotations.NotNull;

/**
 * A lipid annotation the lipid annotation module would report for the tested spectrum.
 *
 * @param annotation      e.g., PE 34:1 or PE 16:0_18:1
 * @param molecularSpecies true for molecular species level, false for species level
 * @param msmsScore       MS2 score 0-1
 */
public record AnnotationTestResult(@NotNull String annotation, boolean molecularSpecies,
                                   double msmsScore) {

}
