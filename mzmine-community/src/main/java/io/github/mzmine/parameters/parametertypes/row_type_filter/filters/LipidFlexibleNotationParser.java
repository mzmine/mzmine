/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.row_type_filter.filters;

import static io.github.mzmine.util.maths.MathOperator.EQUAL;
import static io.github.mzmine.util.maths.MathOperator.GREATER_EQ;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.parameters.parametertypes.row_type_filter.QueryFormatException;
import io.github.mzmine.util.maths.CountOperator;
import io.github.mzmine.util.maths.MathOperator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

class LipidFlexibleNotationParser {

  public static LipidFlexibleChain toFlexibleChain(ILipidChain chain) {
    return LipidFlexibleChain.createExact(chain.getNumberOfCarbons(), chain.getNumberOfDBEs(),
        chain.getNumberOfOxygens());
  }

  public static LipidFlexibleNotation toFlexible(MolecularSpeciesLevelAnnotation lipid) {
    final List<LipidFlexibleChain> chains = lipid.getLipidChains().stream()
        .map(LipidFlexibleNotationParser::toFlexibleChain).toList();
    final String lipidClassAbbr = lipid.getLipidClass().getAbbr();
    return new LipidFlexibleNotation(new LipidClass(lipidClassAbbr), chains);
  }

  record LipidClass(String name) {

    public boolean isUndefined() {
      return name.equalsIgnoreCase("c");
    }

    /**
     * Better to check isUndefined before
     */
    public boolean matches(String candidate) {
      return name.equalsIgnoreCase(candidate);
    }

    public boolean matchesLipidClass(List<String> classesCandidates) {
      if (isUndefined()) {
        return true;
      }
      for (String candidate : classesCandidates) {
        if (matches(candidate)) {
          return true;
        }
      }
      return false;
    }
  }

  record LipidFlexibleChain(CountOperator carbons, CountOperator doubleBonds,
                            CountOperator oxygens) {

    public static LipidFlexibleChain createExact(int numC, int numDB, int numOxy) {
      return new LipidFlexibleChain(new CountOperator(numC, EQUAL), new CountOperator(numDB, EQUAL),
          new CountOperator(numOxy, EQUAL));
    }

    public boolean matches(int numC, int numDB, int numOxy) {
      return carbons.matches(numC) && doubleBonds.matches(numDB) && oxygens.matches(numOxy);
    }

    /**
     * Only applies the operators from this chain
     *
     * @param other does not use the operators from other
     * @return true if matches
     */
    public boolean matches(LipidFlexibleChain other) {
      return matches(other.carbons.value(), other.doubleBonds.value(), other.oxygens.value());
    }

    @Override
    public @NotNull String toString() {
      return "%s:%s;%s".formatted(carbons, doubleBonds, oxygens);
    }

    @NotNull
    public LipidFlexibleChain withOperator(MathOperator operator) {
      // if zero or more then it was undefined
      final CountOperator nc = carbons.isGreaterEqZero() ? carbons : carbons.withOperator(operator);
      final CountOperator ndb =
          doubleBonds.isGreaterEqZero() ? doubleBonds : doubleBonds.withOperator(operator);
      final CountOperator no = oxygens.isGreaterEqZero() ? oxygens : oxygens.withOperator(operator);
      return new LipidFlexibleChain(nc, ndb, no);
    }

  }

  record LipidFlexibleNotation(LipidFlexibleNotationParser.LipidClass lipidClass,
                               List<LipidFlexibleChain> chains, LipidFlexibleChain totalCount) {

    LipidFlexibleNotation(LipidClass lipidClass, List<LipidFlexibleChain> chains) {
      int totalCarbons = 0;
      int totalDB = 0;
      int totalOxy = 0;
      // default is greater eq also if there is no chain definition
      MathOperator cOp = GREATER_EQ;
      MathOperator dbOp = GREATER_EQ;
      MathOperator oxyOp = GREATER_EQ;
      for (LipidFlexibleChain chain : chains) {
        totalCarbons += chain.carbons().value();
        cOp = chain.carbons.operator();
        totalDB += chain.doubleBonds().value();
        dbOp = chain.doubleBonds().operator();
        totalOxy += chain.oxygens().value();
        oxyOp = chain.oxygens().operator();
      }
      // the totals like PC18:2_18:0 are PC36:2
      var totalCount = new LipidFlexibleChain(new CountOperator(totalCarbons, cOp),
          new CountOperator(totalDB, dbOp), new CountOperator(totalOxy, oxyOp));

      this(lipidClass, chains, totalCount);
    }

    /**
     * No chain definition just sum: PC20:2
     */
    public boolean isSpeciesLevel() {
      return chains.size() == 1;
    }

    /**
     * Chain definition like: PC20:2_18:1
     */
    public boolean isMolecularSpeciesLevel() {
      return chains.size() > 1;
    }

    public boolean isClassOnly() {
      return chains.isEmpty();
    }

    public boolean matchesSpeciesLevel(int numC, int numDB, int numOxy) {
      if (isClassOnly()) {
        return true;
      }
      if (!isSpeciesLevel()) {
        return false;
      }

      return totalCount.matches(numC, numDB, numOxy);
    }

    /**
     * Only applies operators of this.chains not from other so other is not flexible but exact
     */
    public boolean matchesClass(LipidFlexibleNotation other) {
      return lipidClass.isUndefined() || lipidClass.equals(other.lipidClass);
    }

    /**
     * Only applies operators of this.chains not from other so other is not flexible but exact. Does
     * not check class.
     */
    public boolean matchesChains(LipidFlexibleNotation other) {
      if (isClassOnly()) {
        return true;
      }
      if (isSpeciesLevel()) {
        // only sum needs to match
        return matchesSpeciesLevel(other.totalCount.carbons.value(),
            other.totalCount.doubleBonds.value(), other.totalCount.oxygens.value());
      }
      // molecular species level all chains need matching
      if (chains.size() != other.chains.size()) {
        return false;
      }

      for (int i = 0; i < chains.size(); i++) {
        if (!chains.get(i).matches(other.chains.get(i))) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      final String chainsString = chains.stream().map(Objects::toString)
          .collect(Collectors.joining("_"));
      return lipidClass.name() + chainsString;
    }

    public @NotNull LipidFlexibleNotation withOperator(MathOperator operator) {
      final List<LipidFlexibleChain> chainsCopy = chains.stream()
          .map(chain -> chain.withOperator(operator)).toList();
      return new LipidFlexibleNotation(lipidClass, chainsCopy);
    }
  }


  /**
   * Options for input are many:
   * <pre>
   *   - PC only define class
   *   - C for any lipid class
   *   - C20 for 20 C and any number of double bonds and oxygen
   *   - C20:2 for 20 C and 2 double bonds
   *   - PC20:2 also specifying the lipid class as PC
   *   - C>20:2 for more than 20 C and exactly 2 double bonds
   *   - C>20:>2 also more than 2 double bonds
   *   - PC18:2_18:0 defining chains
   *   - PC>18:>2_>18:0 defining ranges as well
   *   - Ranges PC20:2 - PC40:6
   * </pre>
   *
   * @param notation                    the name
   * @param requireMoreThanClass        true to force more information than just lipid class. Chains
   *                                    need to be present which means either one sum definition or
   *                                    multiple chains, false to also allow just the lipid class
   *                                    definition without chains. This is tricky when parsing
   *                                    random compound names, this case should use strict mode
   * @param requireDoubleBondDefinition require oxigen definitions. This can be great when parsing
   *                                    random for compound names as a strict mode.
   * @return a flexible notation for matching of lipids
   * @throws QueryFormatException in case input notation is not valid
   */
  @NotNull
  public static LipidFlexibleNotation parseLipidNotation(String notation,
      boolean requireMoreThanClass, boolean requireDoubleBondDefinition)
      throws QueryFormatException {
    // Extract lipid class (letters before numbers)
    int classEnd = 0;
    while (classEnd < notation.length() && !Character.isDigit(notation.charAt(classEnd))
        && notation.charAt(classEnd) != '>' && notation.charAt(classEnd) != '<') {
      classEnd++;
    }
    String lipidClass = notation.substring(0, classEnd).trim();
    if (lipidClass.isEmpty()) {
      throw new QueryFormatException("No lipid class found");
    }

    // Parse chains
    List<LipidFlexibleChain> chains = new ArrayList<>();

    String[] chainStrings = notation.substring(classEnd).split("[_/]");
    for (String chain : chainStrings) {
      if (!chain.isBlank()) {
        chains.add(parseChain(chain.trim(), requireDoubleBondDefinition));
      }
    }

    if (requireMoreThanClass && chains.isEmpty()) {
      throw new QueryFormatException("No chains found");
    }

    return new LipidFlexibleNotation(new LipidClass(lipidClass), chains);
  }

  /**
   * format is Carbons:double bonds; Oxygens as in 18:2;O2
   */
  @NotNull
  private static LipidFlexibleNotationParser.LipidFlexibleChain parseChain(String chain,
      boolean requireDoubleBondDefinition) throws QueryFormatException {
    if (chain.isBlank()) {
      throw new IllegalStateException("Should stop before passing blank chain");
    }

    String[] parts = chain.split("[:;]");

    if (requireDoubleBondDefinition && parts.length < 2) {
      throw new QueryFormatException(
          "Requires double bond definition, e.g. 18:2 but none was provided");
    }

    CountOperator carbons = parseNumber(parts[0]);
    // if not defined than allow any
    CountOperator doubleBonds =
        parts.length > 1 ? parseNumber(parts[1]) : new CountOperator(0, GREATER_EQ);
    CountOperator oxygens =
        parts.length > 2 ? parseNumber(parts[2], "o") : new CountOperator(0, GREATER_EQ);

    return new LipidFlexibleChain(carbons, doubleBonds, oxygens);
  }

  @NotNull
  private static CountOperator parseNumber(@NotNull String number) throws QueryFormatException {
    return parseNumber(number, "");
  }

  @NotNull
  private static CountOperator parseNumber(@NotNull String number, @NotNull String potentialSuffix)
      throws QueryFormatException {
    number = number.trim();
    // remove suffix
    if (!potentialSuffix.isEmpty() && number.toLowerCase()
        .endsWith(potentialSuffix.toLowerCase())) {
      number = number.substring(0, number.length() - potentialSuffix.length()).trim();
    }

    MathOperator operator = EQUAL;
    if (number.startsWith("<=") || number.startsWith("=<")) {
      operator = MathOperator.LESS_EQ;
      number = number.substring(2);
    } else if (number.startsWith(">=") || number.startsWith("=>")) {
      operator = MathOperator.GREATER_EQ;
      number = number.substring(2);
    } else if (number.startsWith(">")) {
      operator = MathOperator.GREATER;
      number = number.substring(1);
    } else if (number.startsWith("<")) {
      operator = MathOperator.LESS;
      number = number.substring(1);
    } else if (number.startsWith("≥")) {
      operator = MathOperator.GREATER_EQ;
      number = number.substring(1);
    } else if (number.startsWith("≤")) {
      operator = MathOperator.LESS_EQ;
      number = number.substring(1);
    }

    try {
      return new CountOperator(Integer.parseInt(number.trim()), operator);
    } catch (NumberFormatException e) {
      throw new QueryFormatException("Invalid number format");
    }
  }

}
