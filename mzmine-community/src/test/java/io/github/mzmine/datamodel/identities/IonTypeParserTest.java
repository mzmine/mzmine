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

package io.github.mzmine.datamodel.identities;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

class IonTypeParserTest {

  record Case(String input, String formatted, int mol, int charge) {

    Case(String input, int mol, int charge) {
      this(input, input, mol, charge);
    }
  }

  private static void testIonParser(final String input, int mol, int charge) {
    testIonParser(input, input, mol, charge);
  }

  private static void testIonParser(final String input, String formatted, int mol, int charge) {
    IonType ionType = IonTypeParser.parse(input);

    Assertions.assertNotNull(ionType, "%s could not be parsed".formatted(input));
    Assertions.assertEquals(charge, ionType.totalCharge(),
        "%s charge was wrong as %d".formatted(input, charge));
    Assertions.assertEquals(mol, ionType.molecules(),
        "%s mol was wrong as %d".formatted(input, mol));
    Assertions.assertEquals(formatted, ionType.toString());
  }

  final static String[] nh4 = new String[]{"M +NH4", "M+NH4 ] +", "[M+NH4] +", "[1M +NH4]+",
      "1M+NH4 ]1+", "(+NH4)+", "[M+NH4]", "(+NH4)"};

  @ParameterizedTest
  @FieldSource("nh4")
  void testIonTypeParsing(String s) {
    IonType type1 = IonTypes.NH4.asIonType();

    IonType ionType = IonTypeParser.parse(s);
    Assertions.assertEquals(type1.toString(), ionType.toString());
  }

  @Test
  void testIonTypeParsingMultiple() {
    final String[] string = new String[]{"[M +NH4]+", "[M-H2O+H]+", "[M -2H2O +H] +",
        "[M -H2O -H +2Na]+"};

    for (String s : string) {
      IonType ionType = IonTypeParser.parse(s);
      Assertions.assertNotNull(ionType);
      Assertions.assertEquals(s.replace(" ", ""), ionType.toString());
    }
  }

  @Test
  void testIonParserRandomString() {
    testIonParser("[M-TEST]-", "[M-TEST]-", 1, -1);
    testIonParser("[M+TEST]+", "[M+TEST]+", 1, 1);

// silent charge is only added if multiple parts are there
    IonType ion = IonTypeParser.parse("[M+TEST]+");
    Assertions.assertEquals(1, ion.parts().size());
    Assertions.assertEquals(1, ion.parts().getFirst().singleCharge());
    Assertions.assertTrue(ion.parts().stream().anyMatch(IonPart::isUnknown));

    // H is known to carry charge
    ion = IonTypeParser.parse("[M+TEST+H]+");
    Assertions.assertEquals(2, ion.parts().size());
    Assertions.assertEquals(0, ion.parts().getFirst().singleCharge());
    Assertions.assertEquals(1, ion.parts().get(1).singleCharge());
    Assertions.assertTrue(ion.parts().stream().anyMatch(IonPart::isUnknown));

    // two ion types will have silent charge added because we dont know which carries the charge
    ion = IonTypeParser.parse("[M+TEST+OTHERUNKNOWN]+");
    Assertions.assertEquals(3, ion.parts().size());
    Assertions.assertTrue(ion.parts().contains(IonParts.SILENT_CHARGE));
    Assertions.assertTrue(ion.parts().stream().anyMatch(IonPart::isUnknown));
  }

  static final List<Case> cases = List.of(
      // this is tricky to format correctly. M- does not write e- and here we want it to be written
      new Case("M-H+e", "[M-H+e]2-", 1, -2), //
      new Case("M-H-2e", "[M-H-2e]+", 1, 1), //
      new Case("2M+2H-2H2O]", "[2M-2H2O+2H]2+", 2, 2), //
      new Case("2M+", "[2M]+", 2, 1), //
      new Case("M+", "[M]+", 1, 1), //
      new Case("M-2H]2-", "[M-2H]2-", 1, -2), //
      new Case("M-H)-", "[M-H]-", 1, -1), //
      new Case("M+H", "[M+H]+", 1, 1), //
      new Case("M-H]-1", "[M-H]-", 1, -1), //
      new Case("[2M+2H+Na]3+", 2, 3), //
      new Case("M+Na+2H]+3", "[M+2H+Na]3+", 1, 3), //
      new Case("[M+2Na-H-H2O]+", "[M-H2O-H+2Na]+", 1, 1), //
      new Case("M+H+", "[M+H]+", 1, 1), //
      new Case("[M+CH3]+", "[M+CH3]+", 1, 1), //
      new Case("[M-H+CH3]+", "[M+CH3-H]+", 1, 1), //
      // Fe+3 is added first and therefore is the default charge state
      new Case("[M-H+Fe]", "[M-H+Fe]2+", 1, 2), //
      new Case("[M-H+Fe]2+", "[M-H+Fe]2+", 1, 2), //
      new Case("[M-H+Fe]+2", "[M-H+Fe]2+", 1, 2), //
      new Case("M+e", "[M+e]-", 1, -1), //
      new Case("M+2e", "[M+2e]2-", 1, -2), //
      new Case("M-e", "[M-e]+", 1, 1), //
      new Case("M-2e", "[M-2e]2+", 1, 2), //

      new Case("M+Cl", "[M+Cl]-", 1, -1), //
      new Case("M-HCl+FA", "[M-HCl+CHO2]-", 1, -1), //
      new Case("M - HCl +2 FA", "[M-HCl+2CHO2]2-", 1, -2), //
      new Case(" - HCl +2 FA", "[M-HCl+2CHO2]2-", 1, -2), //
      new Case("[ - HCl +2 FA]", "[M-HCl+2CHO2]2-", 1, -2), //

      // counter intuitve but we expect ions to have a charge and default to 1
      new Case("[M-H2O]", "[M-H2O]+", 1, 1) //
  );


  @ParameterizedTest
  @FieldSource("cases")
  void testIonParse(Case c) {
    testIonParser(c.input, c.formatted, c.mol, c.charge);
  }

  @Test
  void testWithCharge() {
    testIonParser("[M-2H2O+(H+)+(Fe+3)-2(Na+)+H2O]+2", "[M-H2O-2Na+Fe+H]2+", 1, 2);
    testIonParser("[M  -(H+)\t+ (Fe+3)-2(Na+)+H2O]+", "[M+H2O-H-2Na+Fe]+", 1, 1);
    testIonParser("[M  -(H+)\t+ (Fe+3)-2(Na+)+H2O] 2+", "[M+H2O-H-2Na+Fe]2+", 1, 2);
    testIonParser("[M  -(H+)\t+ (Fe+3) -2 (Na+) + H2O] 2+", "[M+H2O-H-2Na+Fe]2+", 1, 2);
  }

  @Test
  void parseChargeOrElse() {
    // flipped charge string needs ) or ] before 2+ charge
    Assertions.assertEquals(-2, IonTypeParser.parseChargeOrElse("Cl2]2-", null));
    Assertions.assertEquals(-2, IonTypeParser.parseChargeOrElse("2-", null));
    Assertions.assertEquals(-2, IonTypeParser.parseChargeOrElse("-2 ", null));
    Assertions.assertEquals(-2, IonTypeParser.parseChargeOrElse("Cl2)2-", null));
    // white space allowed after and trimmed
    Assertions.assertEquals(-2, IonTypeParser.parseChargeOrElse("Cl2) 2 -  \t", null));
    // default is +2 sign then number
    Assertions.assertEquals(1, IonTypeParser.parseChargeOrElse("Fe+", null));
    Assertions.assertEquals(1, IonTypeParser.parseChargeOrElse("Fe+1", null));
    Assertions.assertEquals(2, IonTypeParser.parseChargeOrElse("Fe+2", null));
    Assertions.assertEquals(-1, IonTypeParser.parseChargeOrElse("Cl-", null));
    Assertions.assertEquals(-1, IonTypeParser.parseChargeOrElse("Cl-1", null));
    Assertions.assertEquals(-2, IonTypeParser.parseChargeOrElse("Cl-2", null));
    Assertions.assertEquals(-2, IonTypeParser.parseChargeOrElse("Cl-2", null));
    Assertions.assertNull(IonTypeParser.parseChargeOrElse("Cl", null));
    Assertions.assertEquals(-1, IonTypeParser.parseChargeOrElse("Cl2-", null));

    Assertions.assertNull(IonTypeParser.parseChargeOrElse("Cl-2nothing after allowed", null));
    Assertions.assertNull(IonTypeParser.parseChargeOrElse("Cl2)2-nothing allowed after", null));
  }

  @Test
  void expectExceptionBraces() {
    assertThrows(IonPartParsingException.class, () -> IonTypeParser.parse("[M+(OH2)Ca]"));
  }

  @Test
  void expectExceptionNoCountMultiplier() {
    assertThrows(IonPartParsingException.class, () -> IonTypeParser.parse("[H]"));
  }

  @Test
  void test() {
//    IonType ionType = IonTypes.H.asIonType();
//    CompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
//
//    final MZTolerance tol = new MZTolerance(0.000001, .01);
//
//    annotation.put(SmilesStructureType.class, "C1CCN(C1)C(=O)C=CC=CC2=CC3=C(C=C2)OCO3");
//    double mzFromSmiles = CompoundDBAnnotation.calcMzForAdduct(annotation, ionType);
//
//    annotation.put(FormulaType.class, "C16H17NO3");
//    double mzFromFormula = CompoundDBAnnotation.calcMzForAdduct(annotation, ionType);
//
//    annotation.put(PrecursorMZType.class, 272.1281199);
//    annotation.put(NewIonTypeType.class, ionType);
//    double mzFromMz = CompoundDBAnnotation.calcMzForAdduct(annotation, ionType);
//
//    annotation.put(NeutralMassType.class, 271.1208434);
//    double mzFromNeutral = CompoundDBAnnotation.calcMzForAdduct(annotation, ionType);
//
//    logger.info(
//        () -> "Smiles: " + mzFromSmiles + "\tFormula: " + mzFromFormula + "\tPrecursor: " + mzFromMz
//              + "\tNeutral: " + mzFromNeutral);
//    Assertions.assertTrue(tol.checkWithinTolerance(mzFromFormula, mzFromNeutral));
//    Assertions.assertTrue(tol.checkWithinTolerance(mzFromMz, mzFromNeutral));
//    Assertions.assertTrue(tol.checkWithinTolerance(mzFromSmiles, mzFromNeutral));
  }
}