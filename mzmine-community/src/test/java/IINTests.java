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

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IINTests {

  private static final Logger logger = Logger.getLogger(IINTests.class.getName());

  private static void testIonParser(final String input, int mol, int charge, int mod) {
    testIonParser(input, input, mol, charge, mod);
  }

  private static void testIonParser(final String input, String formatted, int mol, int charge,
      int mod) {
    IonType ionType = IonTypeParser.parse(input);

    Assertions.assertNotNull(ionType, "%s could not be parsed".formatted(input));
    Assertions.assertEquals(charge, ionType.getCharge(),
        "%s charge was wrong as %d".formatted(input, charge));
    Assertions.assertEquals(mol, ionType.getMolecules(),
        "%s mol was wrong as %d".formatted(input, mol));
    Assertions.assertEquals(mod, ionType.getModCount(),
        "%s mod was wrong as %d".formatted(input, mod));
    Assertions.assertEquals(formatted, ionType.toString(false));
  }

  @Test
  void testIonTypeParsing() {
    IonType type1 = new IonType(IonModification.NH4);
    final String[] string = new String[]{"M+NH4", "M+NH4]+", "[M+NH4]+", "[1M+NH4]+", "1M+NH4]1+"};

    for (String s : string) {
      IonType ionType = IonTypeParser.parse(s);
      Assertions.assertEquals(type1.toString(true), ionType.toString(true));
    }
  }

  @Test
  void testIonTypeParsingMultiple() {
    final String[] string = new String[]{"[M+NH4]+", "[M-H2O+H]+", "[M-2H2O+H]+", "[M-H2O-H+2Na]+"};

    for (String s : string) {
      IonType ionType = IonTypeParser.parse(s);
      Assertions.assertNotNull(ionType);
      Assertions.assertEquals(s, ionType.toString(false));
    }
  }

  @Test
  void testIonParserRandomString() {
    testIonParser("[M+TEST]+", "[M+TEST]+", 1, 1, 1);
    testIonParser("[M-TEST]-", "[M-TEST]-", 1, -1, 1);
  }

  @Test
  void testIonParse() {
    // this is tricky to format correctly. M- does not write e- and here we want it to be written
    testIonParser("M-H+e", "[M-H]-2", 1, -2, 0);
    testIonParser("M-H-2e", "[M-H]+", 1, 1, 0);
    testIonParser("2M+2H-2H2O]", "[2M-2H2O+2H]+2", 2, 2, 2);
    testIonParser("2M+", "[2M]+", 2, 1, 0);
    testIonParser("M+", "[M]+", 1, 1, 0);
    testIonParser("M-2H]2-", "[M-2H]-2", 1, -2, 0);
    testIonParser("M-H-", "[M-H]-", 1, -1, 0);
    testIonParser("M+H", "[M+H]+", 1, 1, 0);
    testIonParser("M-H]-1", "[M-H]-", 1, -1, 0);
    testIonParser("[2M+2H+Na]+3", 2, 3, 0);
    testIonParser("M+Na+2H+3", "[M+2H+Na]+3", 1, 3, 0);
    testIonParser("[M+2Na-H-H2O]+", "[M-H2O-H+2Na]+", 1, 1, 1);
    testIonParser("M+H+", "[M+H]+", 1, 1, 0);
    testIonParser("[M+CH3]+", "[M+CH3]+", 1, 1, 1);
    testIonParser("[M-H+CH3]+", "[M+CH3-H]+", 1, 1, 1);
    testIonParser("[M-H+Fe]2+", "[M+Fe-H]+2", 1, 2, 0);
    testIonParser("M+e", "[M]-", 1, -1, 0);
    testIonParser("M+2e", "[M]-2", 1, -2, 0);
    testIonParser("M-e", "[M]+", 1, 1, 0);
    testIonParser("M-2e", "[M]+2", 1, 2, 0);

    testIonParser("M+Cl", "[M+Cl]-", 1, -1, 0);
    testIonParser("M-HCl+FA", "[M-HCl+FA]-", 1, -1, 1);

    testIonParser("[M]+", "[M]+", 1, 1, 0);
    testIonParser("NA", "[M]+", 1, 1, 0);
    testIonParser("[Cat]+", "[M]+", 1, 1, 0);
    testIonParser("[Cat-C6H10O5]+", "[M-C6H10O5]+", 1, 1, 1);
    testIonParser("[Cat+C6H10O5]+", "[M+C6H10O5]+", 1, 1, 1);
    testIonParser("[M+H-C12H20O9]+", "[M-C12H20O9+H]+", 1, 1, 1);
    testIonParser(" -idontknowhatimdoing31773", "[M+-idontknowhatimdoing31773]+", 1, 1, 1);

    // counter intuitve but we expect ions to have a charge and default to 1
    testIonParser("[M-H2O]", "[M-H2O]+", 1, 1, 1);
  }

  @Test
  void test() {
    IonType ionType = new IonType(IonModification.H);
    CompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();

    final MZTolerance tol = new MZTolerance(0.000001, .01);

    annotation.put(SmilesStructureType.class, "C1CCN(C1)C(=O)C=CC=CC2=CC3=C(C=C2)OCO3");
    double mzFromSmiles = CompoundDBAnnotation.calcMzForAdduct(annotation, ionType);

    annotation.put(FormulaType.class, "C16H17NO3");
    double mzFromFormula = CompoundDBAnnotation.calcMzForAdduct(annotation, ionType);

    annotation.put(PrecursorMZType.class, 272.1281199);
    annotation.put(IonTypeType.class, ionType);
    double mzFromMz = CompoundDBAnnotation.calcMzForAdduct(annotation, ionType);

    annotation.put(NeutralMassType.class, 271.1208434);
    double mzFromNeutral = CompoundDBAnnotation.calcMzForAdduct(annotation, ionType);

    logger.info(
        () -> "Smiles: " + mzFromSmiles + "\tFormula: " + mzFromFormula + "\tPrecursor: " + mzFromMz
            + "\tNeutral: " + mzFromNeutral);
    Assertions.assertTrue(tol.checkWithinTolerance(mzFromFormula, mzFromNeutral));
    Assertions.assertTrue(tol.checkWithinTolerance(mzFromMz, mzFromNeutral));
    Assertions.assertTrue(tol.checkWithinTolerance(mzFromSmiles, mzFromNeutral));
  }
}
