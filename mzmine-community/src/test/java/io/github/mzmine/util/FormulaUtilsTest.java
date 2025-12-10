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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

class FormulaUtilsTest {

  private static final Logger logger = Logger.getLogger(FormulaUtilsTest.class.getName());

  record Case(String input, @NotNull String formatted, @NotNull String formattedIsotopic,
              Integer charge) {

    Case(String input, @NotNull String formatted, Integer charge) {
      this(input, formatted, formatted, charge);
    }
  }

  final static List<Case> cases = List.of( //
      new Case("[NH4]+", "[H4N]+", 1) //
      , new Case("NH4", "H4N", null) //
      , new Case("NH4+", "[H4N]+", 1) //
      , new Case("COOH", "CHO2", null) //
      , new Case("(NH4)+", "[H4N]+", 1) //
      , new Case("(NH4)-2", "[H4N]2-", -2) //
      // flipped charge only works with []2- not with ()2- as ()2- would mean 2 times what is in ()
      , new Case("NH4-2", "[H4N]2-", -2) //
      , new Case("[NH4]2-", "[H4N]2-", -2) //
      , new Case("[NH4]-2", "[H4N]2-", -2) //
      , new Case("[13C]C5H12+", "[C6H12]+", "[C5[13]CH12]+", 1) //
      , new Case("[[13C]C5H12]+", "[C6H12]+", "[C5[13]CH12]+", 1) //
  );

  @ParameterizedTest
  @FieldSource("cases")
  void testParsingCases(Case c) {
    final IMolecularFormula isotopic = FormulaUtils.parse(c.input);
    assertNotNull(isotopic);

    final IMolecularFormula major = FormulaUtils.replaceAllToMajorIsotopes(isotopic);

    assertNotNull(major);
    assertEquals(c.charge, major.getCharge());
    assertEquals(c.charge, isotopic.getCharge());
    assertEquals(c.formatted,
        FormulaUtils.getFormulaString(major, FormulaStringFlavor.DEFAULT_CHARGED));
    assertEquals(c.formattedIsotopic,
        FormulaUtils.getFormulaString(isotopic, FormulaStringFlavor.DEFAULT_CHARGED));
  }

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
    final String gluStr = "C6H12O6";
    final IMolecularFormula neutralGlucose = MolecularFormulaManipulator.getMolecularFormula(gluStr,
        SilentChemObjectBuilder.getInstance());

    final IMolecularFormula n1 = FormulaUtils.neutralizeFormulaWithHydrogen(gluStr);
    final IMolecularFormula n2 = FormulaUtils.neutralizeFormulaWithHydrogen("C6H13O6+");
    final IMolecularFormula n3 = FormulaUtils.neutralizeFormulaWithHydrogen("C6H11O6-");

    assertEquals(gluStr, FormulaUtils.getFormulaString(n1));
    assertEquals(gluStr, FormulaUtils.getFormulaString(n2));
    assertEquals(gluStr, FormulaUtils.getFormulaString(n3));

    final IMolecularFormula n4 = FormulaUtils.neutralizeFormulaWithHydrogen(
        FormulaUtils.getFormulaFromSmiles("OC(O1)C(O)C(O)C(O)C1CO"));
    final IMolecularFormula n5 = FormulaUtils.neutralizeFormulaWithHydrogen(
        FormulaUtils.getFormulaFromSmiles("OC(O1)C(O)C(O)C([OH2+])C1CO"));
    final IMolecularFormula n6 = FormulaUtils.neutralizeFormulaWithHydrogen(
        FormulaUtils.getFormulaFromSmiles("OC(O1)C(O)C(O)C([O-])C1CO"));

    assertEquals(gluStr, FormulaUtils.getFormulaString(n4));
    assertEquals(gluStr, FormulaUtils.getFormulaString(n5));
    assertEquals(gluStr, FormulaUtils.getFormulaString(n6));
  }

  @Test
  void testCalcMz() {
    // M - H + 2Na
    final IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMolecularFormula(
        "[C6H11O6Na2]+", SilentChemObjectBuilder.getInstance());

    assertEquals((float) 225.0345531, (float) FormulaUtils.calculateMzRatio(molecularFormula));
  }

  @Test
  void testGetAllSubformulas() {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge("C6H6O2N+");
    FormulaWithExactMz[] all = FormulaUtils.getAllFormulas(formula);
    assert all.length == 293;
  }

  @Test
  void testGetAllSubformulasGreater50() {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge("C6H6O2N+");
    FormulaWithExactMz[] all = FormulaUtils.getAllFormulas(formula, 50);
    assert all.length == 191;
  }

  @Test
  void testGetAllSubformulasDoubleCharge() {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge("C6H6O2N+2");
    FormulaWithExactMz[] all = FormulaUtils.getAllFormulas(formula, 1, 10);
    assert all.length == 287;
  }

  @Test
  void testGetAllSubformulasGreater200() {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge("C3H3O+");
    FormulaWithExactMz[] all = FormulaUtils.getAllFormulas(formula, 200);
    assert all.length == 0;
  }

  @Test
  void testFindMzInFormula() {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge("C3H4O2N+");
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

    assertEquals(ion1.getPrecursorMZ(), ion2.getPrecursorMZ());
  }

  @Test
  void getMonoisotopicMass() {
    assertEquals(4.028203556, FormulaUtils.getMonoisotopicMass(formula("[2]H2")), 0.000001);
    assertEquals(4.028203556, FormulaUtils.getMonoisotopicMass(formula("[2H]2")), 0.000001);
    assertEquals(86.00670968, FormulaUtils.getMonoisotopicMass(formula("C5[13]C2")), 0.000001);
    assertEquals(86.00561252018146, FormulaUtils.getMonoisotopicMass(formula("[C5[13C]2]+2")),
        0.000001);
    assertEquals(13.00335484, FormulaUtils.getMonoisotopicMass(formula("[13]C")), 0.000001);
    assertEquals(13.00335484 * 2, FormulaUtils.getMonoisotopicMass(formula("[13]C2")), 0.000001);
    assertEquals(11.99945142009073, FormulaUtils.getMonoisotopicMass(formula("C+")), 0.000001);
    assertEquals(12, FormulaUtils.getMonoisotopicMass(formula("C")), 0.000001);
    assertEquals(93.99077174390926, FormulaUtils.getMonoisotopicMass(formula("CH2O5-")), 0.000001);
  }

  private static @Nullable IMolecularFormula formula(final String formula) {
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(formula);
  }

  @Test
  void getFormulaString() {
    // without charge
    assertEquals("C", FormulaUtils.getFormulaString(formula("C"), false));
    assertEquals("CHO2", FormulaUtils.getFormulaString(formula("HCO2+"), false));
    assertEquals("C", FormulaUtils.getFormulaString(formula("C-2"), false));
    // with charge
    assertEquals("C", FormulaUtils.getFormulaString(formula("C"), true));
    assertEquals("[CHO2]+", FormulaUtils.getFormulaString(formula("HCO2+"), true));
    assertEquals("[C]2-", FormulaUtils.getFormulaString(formula("C-2"), true));
    assertEquals("[C]2-", FormulaUtils.getFormulaString(formula("[C]-2"), true));
    assertEquals("[C]2-", FormulaUtils.getFormulaString(formula("[C]2-"), true));

    // with isotopes
    assertEquals("[[13]C]2-", FormulaUtils.getFormulaString(formula("[[13C]]2-"), true));
    assertEquals("[13]C", FormulaUtils.getFormulaString(formula("[[13C]]2-"), false));
    assertEquals("C2[13]C[79]BrCl[158]Gd",
        FormulaUtils.getFormulaString(formula("[[13]CC2BrClGd]2-"), false));
    assertEquals("[C2[13]C[79]BrCl[158]Gd]2-",
        FormulaUtils.getFormulaString(formula("[[13]CC2BrClGd]2-"), true));
  }

  @Test
  void parseFormulaString() {
    parseFormula("D2O", true);
    parseFormula("TEST", true); //
    parseFormula("GGG", true); //
    parseFormula("H2o", true);
    parseFormula("H2O", false);
  }

  private static IMolecularFormula parseFormula(final String formula, boolean resultNull) {
    var f = FormulaUtils.parse(formula);
    if (resultNull) {
      assertNull(f, () -> "Parsed formula for %s is %s".formatted(formula,
          FormulaUtils.getFormulaString(f, false)));
    } else {
      assertNotNull(f, "Formula for %s is null".formatted(formula));
    }
    return f;
  }
}