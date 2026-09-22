/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util;

import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Parses molecular formula strings as formula or SMILES and creates the correct
 * {@link IMolecularFormula} object with isotopes that carry abundance and exact mass
 */
public class FormulaParser {

  private static final Logger logger = Logger.getLogger(FormulaParser.class.getName());

  /**
   * Symbols that are commonly used in formula strings although they are no elements. They are
   * translated to the isotope notation understood by CDK before parsing because CDK parses them as
   * R atoms.
   */
  private static final Map<String, String> ELEMENT_SYNONYMS = Map.of( //
      "D", "[2H]", // deuterium
      "T", "[3H]" // tritium
  );

  /**
   * Matches a synonym only if it is not the first letter of a two letter element like Dy, Db, Th,
   * or Ti, which all stay untouched.
   */
  private static final Pattern ELEMENT_SYNONYM_PATTERN = Pattern.compile("(%s)(?![a-z])".formatted(
      ELEMENT_SYNONYMS.keySet().stream().sorted().collect(Collectors.joining("|"))));

  /**
   * Content of brackets that define an isotope like [13C] or [2H]. Those brackets belong to the
   * formula and must not be mistaken for the brackets of the charge notation [CO2]-.
   */
  private static final Pattern ISOTOPE_IN_BRACKETS = Pattern.compile("\\d+[A-Z][a-z]?");

  /**
   * Creates a formula with the major isotopes (important to use this method for exact mass
   * calculation over the CDK version, which generates formulas without an exact mass)
   * <p>
   * Keeps specifically defined isotopes as is: If the formula string contains specific isotopes
   * like C5[13C] or C5[13]C then one 13C isotope will be retained and not exchanged for the major
   * isotope. The same is true for the hydrogen isotope synonyms D and T, which are read as [2H] and
   * [3H], so that D2O parses like [2H]2O.
   *
   * @return the formula or null on error
   */
  @Nullable
  public static IMolecularFormula parseFormula(@Nullable String formula) {
    if (formula == null) {
      return null;
    }
    try {
      IChemObjectBuilder builder = FormulaUtils.silentBuilder();
      // generate regular formula first
      // this method adds missing atoms as isotope with atom number 0
      // parsing TEST will result in 1 S atom and 3 atoms with number 0 -> check this to validate parsing
      formula = formula.replaceAll("\\s+", "");
      formula = replaceElementSynonyms(formula);
      formula = removeChargeBrackets(formula);

      var f = MolecularFormulaManipulator.getMolecularFormula(formula, builder);

      if (f == null) {
        return null;
      }

      // CDK parses charge only from [H]+ not from H+ so we add this behavior here
      if (FormulaUtils.isUncharged(f)) {
        final Integer charge = IonTypeParser.parseChargeOrElse(formula, null);
        f.setCharge(charge);
      }

      return FormulaUtils.replaceAllIsotopesWithoutExactMass(f);
    } catch (Exception e) {
      // usually do not log as the input is often just wrong
      // tests already cover
//      logger.log(Level.SEVERE, "Cannot create formula for: " + formula, e);
      return null;
    }
  }

  /**
   * Replaces synonyms like D and T by their isotope notation [2H] and [3H].
   */
  static @NotNull String replaceElementSynonyms(@NotNull String formula) {
    return ELEMENT_SYNONYM_PATTERN.matcher(formula)
        .replaceAll(match -> ELEMENT_SYNONYMS.get(match.group()));
  }

  /**
   * The CDK parser uses [CO2]- for charge but cannot parse [CO2] without charge, which needs to be
   * CO2. Only those brackets are removed: [2H] defines an isotope and is kept.
   */
  static @NotNull String removeChargeBrackets(@NotNull String formula) {
    if (!(formula.startsWith("[") && formula.endsWith("]"))) {
      return formula;
    }
    final String content = formula.substring(1, formula.length() - 1);
    return ISOTOPE_IN_BRACKETS.matcher(content).matches() ? formula : content;
  }

}
