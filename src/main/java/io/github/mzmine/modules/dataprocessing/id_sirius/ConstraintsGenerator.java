/*
 * (C) Copyright 2015-2018 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.mzmine.modules.dataprocessing.id_sirius;

import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

/**
 * <p>
 * Class ConstraintsGenerator.
 * </p>
 * This class allows to construct a Sirius object FormulaConstraints using MolecularFormulaRange
 * object
 */
public class ConstraintsGenerator {

  private static final PeriodicTable periodicTable = PeriodicTable.getInstance();

  private ConstraintsGenerator() {}

  /**
   * <p>
   * Method for generating FormulaConstraints from user-defined search space
   * </p>
   * Parses isotopes from input parameter and transforms it into Element objects and sets their
   * range value
   * 
   * @param range - User defined search space of possible elements
   * @return new Constraint to be used in Sirius
   */
  public static FormulaConstraints generateConstraint(MolecularFormulaRange range) {

    Element elements[] = new Element[range.getIsotopeCount()];
    int k = 0;

    // Add items from `range` into array
    for (IIsotope isotope : range.isotopes()) {
      int atomicNumber = isotope.getAtomicNumber();
      final Element element = periodicTable.get(atomicNumber);
      elements[k++] = element;
    }

    // Generate initial constraint w/o concrete Element range
    FormulaConstraints constraints = new FormulaConstraints(new ChemicalAlphabet(elements));

    synchronized (periodicTable) {
      // Specify each Element range
      for (IIsotope isotope : range.isotopes()) {
        Integer atomicNumber = isotope.getAtomicNumber();
        final Element element = periodicTable.get(atomicNumber);
        int min = range.getIsotopeCountMin(isotope);
        int max = range.getIsotopeCountMax(isotope);

        constraints.setLowerbound(element, min);
        constraints.setUpperbound(element, max);
      }
    }

    return constraints;
  }
}
