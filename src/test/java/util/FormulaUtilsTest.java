/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package util;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.util.FormulaUtils;
import java.util.logging.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaUtilsTest {

  private static final Logger logger = Logger.getLogger(FormulaUtilsTest.class.getName());

  @Test
  void testIonizationType() {
    final String nacetylglucosamine = "C8H15NO6";
    final double neutralMass = MolecularFormulaManipulator.getMass(
        MolecularFormulaManipulator.getMolecularFormula(nacetylglucosamine,
            SilentChemObjectBuilder.getInstance()), MolecularFormulaManipulator.MonoIsotopic);
    for (IonizationType it : IonizationType.values()) {
      if (it == IonizationType.NO_IONIZATION) {
        continue;
      }
      final IMolecularFormula glucose = MolecularFormulaManipulator.getMolecularFormula(
          nacetylglucosamine, SilentChemObjectBuilder.getInstance());

      it.ionizeFormula(glucose);

      logger.info(it + " " + MolecularFormulaManipulator.getString(glucose));
      Assert.assertEquals(Math.abs((neutralMass + it.getAddedMass()) / it.getCharge()),
          FormulaUtils.calculateMzRatio(glucose), 0.0000001d);
    }
  }

  @Test
  void testNeutralizeFormula() {
    final IMolecularFormula neutralGlucose = MolecularFormulaManipulator.getMolecularFormula(
        "C6H12O6", SilentChemObjectBuilder.getInstance());

    final IMolecularFormula n1 = FormulaUtils.neutralizeFormulaWithHydrogen("C6H12O6");
    final IMolecularFormula n2 = FormulaUtils.neutralizeFormulaWithHydrogen("C6H13O6+");
    final IMolecularFormula n3 = FormulaUtils.neutralizeFormulaWithHydrogen("C6H11O6-");

    Assertions.assertEquals(MolecularFormulaManipulator.getString(neutralGlucose),
        MolecularFormulaManipulator.getString(n1));
    Assertions.assertEquals(MolecularFormulaManipulator.getString(neutralGlucose),
        MolecularFormulaManipulator.getString(n2));
    Assertions.assertEquals(MolecularFormulaManipulator.getString(neutralGlucose),
        MolecularFormulaManipulator.getString(n3));

    final IMolecularFormula n4 = FormulaUtils.neutralizeFormulaWithHydrogen(
        FormulaUtils.getFomulaFromSmiles("OC(O1)C(O)C(O)C(O)C1CO"));
    final IMolecularFormula n5 = FormulaUtils.neutralizeFormulaWithHydrogen(
        FormulaUtils.getFomulaFromSmiles("OC(O1)C(O)C(O)C([OH2+])C1CO"));
    final IMolecularFormula n6 = FormulaUtils.neutralizeFormulaWithHydrogen(
        FormulaUtils.getFomulaFromSmiles("OC(O1)C(O)C(O)C([O-])C1CO"));

    Assertions.assertEquals(MolecularFormulaManipulator.getString(neutralGlucose),
        MolecularFormulaManipulator.getString(n4));
    Assertions.assertEquals(MolecularFormulaManipulator.getString(neutralGlucose),
        MolecularFormulaManipulator.getString(n5));
    Assertions.assertEquals(MolecularFormulaManipulator.getString(neutralGlucose),
        MolecularFormulaManipulator.getString(n6));
  }

  @Test
  void testCalcMz() {
    // M - H + 2Na
    final IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMolecularFormula(
        "[C6H11O6Na2]+", SilentChemObjectBuilder.getInstance());

    Assertions.assertEquals((float) 225.0345531,
        (float) FormulaUtils.calculateMzRatio(molecularFormula));
  }
}
