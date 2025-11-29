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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.datamodel.structures.StructureInputType;
import io.github.mzmine.datamodel.structures.StructureParser;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Parses molecular formula strings as formula or SMILES and creates the correct
 * {@link IMolecularFormula} object with isotopes that carry abundance and exact mass
 */
public class FormulaParser {

  private static final Logger logger = Logger.getLogger(FormulaParser.class.getName());

  /**
   * Creates a formula with the major isotopes (important to use this method for exact mass
   * calculation over the CDK version, which generates formulas without an exact mass)
   * <p>
   * Keeps specifically defined isotopes as is: If the formula string contains specific isotopes
   * like C5[13C] then one 13C isotope will be retained and not exchanged for the major isotope.
   * <p>
   * May also parse a SMILES if formula fails
   *
   * @param formulaOrSmiles formula or smiles
   * @return the formula or null on error
   */
  @Nullable
  public static IMolecularFormula parseFormulaOrSMILES(String formulaOrSmiles) {
    IMolecularFormula f = parseFormula(formulaOrSmiles);
    if (f == null) {
      // try to parse as smiles
      // useful for formulas given as CH3-OH
      final MolecularStructure structure = StructureParser.silent()
          .parseStructure(formulaOrSmiles, StructureInputType.SMILES);
      if (structure != null) {
        f = structure.formula();
        logger.finer(() -> "Formula %s was parsed as SMILES %s".formatted(formulaOrSmiles,
            structure.formulaString()));
      }
    }
    return f;
  }

  /**
   * Creates a formula with the major isotopes (important to use this method for exact mass
   * calculation over the CDK version, which generates formulas without an exact mass)
   * <p>
   * Keeps specifically defined isotopes as is: If the formula string contains specific isotopes
   * like C5[13C] then one 13C isotope will be retained and not exchanged for the major isotope.
   *
   * @return the formula or null on error
   */
  @Nullable
  public static IMolecularFormula parseFormula(String formula) {
    try {
      IChemObjectBuilder builder = FormulaUtils.silentBuilder();
      // generate regular formula first
      // this method adds missing atoms as isotope with atom number 0
      // parsing TEST will result in 1 S atom and 3 atoms with number 0 -> check this to validate parsing
      var f = MolecularFormulaManipulator.getMolecularFormula(formula.replace(" ", ""), builder);

      if (f == null) {
        return null;
      }

      for (IIsotope iso : f.isotopes()) {
        if ((iso.getAtomicNumber() == null) || (iso.getAtomicNumber() == 0)) {
          logger.warning("Cannot parse formula %s as there are unknown atoms".formatted(formula));
          return null;
        }

        final IIsotope exactIso = FormulaUtils.getExactIsotope(iso);
        if (exactIso != null) {
          // replace isotopes
          // needed, as MolecularFormulaManipulator method returns isotopes
          // without exact mass info
          iso.setMassNumber(exactIso.getMassNumber());
          iso.setExactMass(exactIso.getExactMass());
          iso.setNaturalAbundance(exactIso.getNaturalAbundance());
        }
      }
      return f;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Cannot create formula for: " + formula, e);
      return null;
    }
  }

}
