package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import io.github.mzmine.util.FormulaUtils;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.MolecularFormula;

/**
 * The formula that a chain plus/minus formula fragmentation rule adds to or removes from a lipid
 * chain. Besides a plain formula like {@code C3H5O}, a signed formula like {@code +O2-H2} is
 * supported, which adds and removes atoms at the same time. This is required for fragments that
 * gain some atoms but lose others, e.g., an oxidized chain with fewer hydrogens.
 *
 * @param added   atoms added to the chain by a plus rule
 * @param removed atoms removed from the chain by a plus rule
 */
public record LipidChainFormulaModification(@NotNull IMolecularFormula added,
                                            @NotNull IMolecularFormula removed) {

  /**
   * One or more formula parts, each optionally prefixed by + or -. Trailing charge signs like
   * C2H- do not match and are handled by the plain formula parser.
   */
  private static final Pattern SIGNED_FORMULA = Pattern.compile("^[+-]?[^+-]+([+-][^+-]+)*$");
  private static final Pattern SIGNED_PART = Pattern.compile("([+-])([^+-]+)");

  /**
   * Parses a plain formula like {@code C3H5O} or a signed formula like {@code +O2-H2} or
   * {@code C2H4O-H2O}. A part without sign is added.
   *
   * @param formula the formula of the fragmentation rule
   * @return the modification or null if the formula is blank or cannot be parsed
   */
  public static @Nullable LipidChainFormulaModification parse(@Nullable String formula) {
    if (formula == null || formula.isBlank()) {
      return null;
    }
    final String normalized = formula.replaceAll("\\s+", "");
    if (!SIGNED_FORMULA.matcher(normalized).matches()) {
      // decision: keep legacy behavior for formulas with a trailing charge, e.g., C2H-
      final IMolecularFormula legacy = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
          normalized);
      return legacy == null ? null : new LipidChainFormulaModification(legacy, new MolecularFormula());
    }

    final String signed =
        normalized.startsWith("+") || normalized.startsWith("-") ? normalized : "+" + normalized;
    final IMolecularFormula added = new MolecularFormula();
    final IMolecularFormula removed = new MolecularFormula();
    final Matcher matcher = SIGNED_PART.matcher(signed);
    while (matcher.find()) {
      final IMolecularFormula part = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
          matcher.group(2));
      if (part == null) {
        return null;
      }
      FormulaUtils.addFormula("+".equals(matcher.group(1)) ? added : removed, part);
    }
    return new LipidChainFormulaModification(added, removed);
  }

  /**
   * @param formula the formula of the fragmentation rule
   * @return true if the formula can be parsed by {@link #parse(String)}
   */
  public static boolean isValid(@Nullable String formula) {
    return parse(formula) != null;
  }

  /**
   * Applies the modification as defined in the rule: base + added - removed.
   *
   * @param base the chain formula, is not changed
   * @return a new formula or empty if more atoms would be removed than are present
   */
  public @NotNull Optional<IMolecularFormula> addTo(@NotNull IMolecularFormula base) {
    return apply(base, added, removed);
  }

  /**
   * Applies the inverse modification for minus rules: base - added + removed.
   *
   * @param base the chain formula, is not changed
   * @return a new formula or empty if more atoms would be removed than are present
   */
  public @NotNull Optional<IMolecularFormula> subtractFrom(@NotNull IMolecularFormula base) {
    return apply(base, removed, added);
  }

  private static @NotNull Optional<IMolecularFormula> apply(@NotNull IMolecularFormula base,
      @NotNull IMolecularFormula add, @NotNull IMolecularFormula remove) {
    // add first so that removing atoms that were just added does not fail
    final IMolecularFormula result = FormulaUtils.addFormula(base, add, 1, true);
    return FormulaUtils.subtractFormula(result, remove, false);
  }
}
