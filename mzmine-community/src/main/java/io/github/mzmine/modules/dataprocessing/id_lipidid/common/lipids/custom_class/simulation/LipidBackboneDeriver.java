package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.util.FormulaUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Derives the backbone formula of a lipid class from one known lipid of the class. The lipid
 * formula is the backbone plus what each chain contributes, so the backbone is the known formula
 * minus the chain contribution.
 */
public final class LipidBackboneDeriver {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  // decision: a large placeholder backbone so the chain bonding never runs out of atoms
  private static final String PLACEHOLDER = "C100H200N20O100P20S20";

  private LipidBackboneDeriver() {
  }

  /**
   * @param chainTypes   chains of the class in order
   * @param lipidFormula neutral formula of a known lipid of the class, e.g., C39H76NO8P for PE 34:1
   * @param carbons      total carbons in all chains of the known lipid
   * @param dbes         total double bond equivalents in all chains of the known lipid
   * @return the backbone formula or an error message
   */
  public static @NotNull BackboneResult derive(@NotNull LipidChainType[] chainTypes,
      @NotNull String lipidFormula, int carbons, int dbes) {
    if (chainTypes.length == 0) {
      return BackboneResult.error("Add the chains of the class first.");
    }
    final IMolecularFormula target = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        lipidFormula.strip());
    if (target == null) {
      return BackboneResult.error("Cannot read the formula " + lipidFormula);
    }
    final CustomLipidClass probe = new CustomLipidClass("probe", "probe",
        LipidCategories.GLYCEROLIPIDS, LipidMainClasses.OTHERGLYCEROLIPIDS, PLACEHOLDER, chainTypes,
        new LipidFragmentationRule[0]);
    final SpeciesLevelAnnotation built = LIPID_FACTORY.buildSpeciesLevelLipid(probe, carbons, dbes,
        0);
    if (built == null) {
      return BackboneResult.error(
          "Cannot build chains with %d carbons and %d double bonds.".formatted(carbons, dbes));
    }
    // backbone = target - (built - placeholder)
    final Map<String, Integer> backbone = counts(target);
    final Map<String, Integer> chainPart = counts(built.getMolecularFormula());
    counts(FormulaUtils.createMajorIsotopeMolFormulaWithCharge(PLACEHOLDER)).forEach(
        (element, n) -> chainPart.merge(element, -n, Integer::sum));
    chainPart.forEach((element, n) -> backbone.merge(element, -n, Integer::sum));

    final StringBuilder formula = new StringBuilder();
    for (final Entry<String, Integer> e : hillOrder(backbone).entrySet()) {
      if (e.getValue() < 0) {
        return BackboneResult.error(
            "The chains contain more %s than the lipid formula. Check the formula, carbons and double bonds."
                .formatted(e.getKey()));
      }
      if (e.getValue() > 0) {
        formula.append(e.getKey()).append(e.getValue() > 1 ? e.getValue() : "");
      }
    }
    if (formula.isEmpty()) {
      return BackboneResult.error("The backbone would be empty.");
    }
    return new BackboneResult(formula.toString(), null);
  }

  private static @NotNull Map<String, Integer> counts(@Nullable IMolecularFormula formula) {
    final Map<String, Integer> counts = new LinkedHashMap<>();
    if (formula != null) {
      for (final IIsotope isotope : formula.isotopes()) {
        counts.merge(isotope.getSymbol(), formula.getIsotopeCount(isotope), Integer::sum);
      }
    }
    return counts;
  }

  /**
   * C and H first, then alphabetical.
   */
  private static @NotNull Map<String, Integer> hillOrder(@NotNull Map<String, Integer> counts) {
    final Map<String, Integer> ordered = new LinkedHashMap<>();
    for (final String first : new String[]{"C", "H"}) {
      if (counts.containsKey(first)) {
        ordered.put(first, counts.get(first));
      }
    }
    counts.keySet().stream().filter(e -> !e.equals("C") && !e.equals("H")).sorted()
        .forEach(e -> ordered.put(e, counts.get(e)));
    return ordered;
  }

  /**
   * @param formula backbone formula or null on error
   * @param error   error message or null on success
   */
  public record BackboneResult(@Nullable String formula, @Nullable String error) {

    static @NotNull BackboneResult error(@NotNull String message) {
      return new BackboneResult(null, message);
    }

    public boolean isOk() {
      return formula != null;
    }
  }
}
