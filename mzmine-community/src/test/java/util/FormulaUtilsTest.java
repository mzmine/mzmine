/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package util;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.FormulaWithExactMz;
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
        FormulaUtils.getFormulaFromSmiles("OC(O1)C(O)C(O)C(O)C1CO"));
    final IMolecularFormula n5 = FormulaUtils.neutralizeFormulaWithHydrogen(
        FormulaUtils.getFormulaFromSmiles("OC(O1)C(O)C(O)C([OH2+])C1CO"));
    final IMolecularFormula n6 = FormulaUtils.neutralizeFormulaWithHydrogen(
        FormulaUtils.getFormulaFromSmiles("OC(O1)C(O)C(O)C([O-])C1CO"));

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

  @Test
  void testGetAllSubformulas() {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula("C6H6O2N+");
    FormulaWithExactMz[] all = FormulaUtils.getAllFormulas(formula);
    assert all.length == 293;
  }

  @Test
  void testGetAllSubformulasGreater50() {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula("C6H6O2N+");
    FormulaWithExactMz[] all = FormulaUtils.getAllFormulas(formula, 50);
    assert all.length == 191;
  }

  @Test
  void testGetAllSubformulasDoubleCharge() {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula("C6H6O2N+2");
    FormulaWithExactMz[] all = FormulaUtils.getAllFormulas(formula, 1, 10);
    assert all.length == 287;
  }

  @Test
  void testGetAllSubformulasGreater200() {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula("C3H3O+");
    FormulaWithExactMz[] all = FormulaUtils.getAllFormulas(formula, 200);
    assert all.length == 0;
  }

  @Test
  void testFindMzInFormula() {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula("C3H4O2N+");
    FormulaWithExactMz[] all = FormulaUtils.getAllFormulas(formula, 40);
    assert FormulaUtils.getClosestIndexOfFormula(25, all) == 0;
    assert FormulaUtils.getClosestIndexOfFormula(55, all) == 32;
    assert FormulaUtils.getClosestIndexOfFormula(250, all) == all.length - 1;
  }

  @Test
  void ionizeFormulaTest() {
    var adduct = new IonType(IonModification.M_PLUS);
    var annotation = new SimpleCompoundDBAnnotation("C");
    var annotationPlus = new SimpleCompoundDBAnnotation("CH+");
    var ion1 = annotation.ionize(adduct);
    // will remove one H+ to neutralize
    var ion2 = annotationPlus.ionize(adduct);

    Assertions.assertEquals(ion1.getPrecursorMZ(), ion2.getPrecursorMZ());
  }
}
