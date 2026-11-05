package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Result of testing a lipid class against a measured MS2 spectrum for one lipid species that
 * matches the precursor m/z.
 *
 * @param species        species level annotation, e.g., PE 34:1
 * @param precursorMz    theoretical precursor m/z of the species
 * @param precursorPpm   mass error of the measured precursor m/z in ppm
 * @param rules          result for each rule of the tested adduct
 * @param annotations    annotations the lipid annotation module would report, best first
 * @param explainedPeaks number of measured peaks explained by at least one rule
 * @param totalPeaks     number of measured peaks
 */
public record SpectrumTestResult(@NotNull String species, double precursorMz, double precursorPpm,
                                 @NotNull List<RuleTestResult> rules,
                                 @NotNull List<AnnotationTestResult> annotations,
                                 int explainedPeaks, int totalPeaks) {

  public long matchedRules() {
    return rules.stream().filter(RuleTestResult::isMatched).count();
  }
}
