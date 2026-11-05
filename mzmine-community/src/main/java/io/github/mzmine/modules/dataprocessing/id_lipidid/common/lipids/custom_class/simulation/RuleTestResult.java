package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The peaks a fragmentation rule explains in a measured spectrum.
 *
 * @param ruleIndex index of the rule in the lipid class
 * @param rule      the rule
 * @param matches   matched peaks, empty if the expected fragment was not found
 */
public record RuleTestResult(int ruleIndex, @NotNull LipidFragmentationRule rule,
                             @NotNull List<MatchedRuleFragment> matches) {

  public boolean isMatched() {
    return !matches.isEmpty();
  }
}
