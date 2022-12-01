/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IINTests {

  private static final Logger logger = Logger.getLogger(IINTests.class.getName());

  @Test
  void testIonTypeParsing() {
    IonType type1 = new IonType(IonModification.NH4);
    final String[] string = new String[] {"M+NH4", "M+NH4]+", "[M+NH4]+", "[1M+NH4]+", "1M+NH4]1+"};

    for (String s : string) {
      IonType ionType = IonType.parseFromString(s);
      Assertions.assertEquals(type1, ionType);
    }
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

    annotation.put(PrecursorMZType.class,272.1281199);
    annotation.put(IonTypeType.class, ionType);
    double mzFromMz = CompoundDBAnnotation.calcMzForAdduct(annotation, ionType);

    annotation.put(NeutralMassType.class, 271.1208434);
    double mzFromNeutral = CompoundDBAnnotation.calcMzForAdduct(annotation, ionType);

    logger.info(() -> "Smiles: " + mzFromSmiles + "\tFormula: " + mzFromFormula + "\tPrecursor: " + mzFromMz + "\tNeutral: " + mzFromNeutral);
    Assertions.assertTrue(tol.checkWithinTolerance(mzFromFormula, mzFromNeutral));
    Assertions.assertTrue(tol.checkWithinTolerance(mzFromMz, mzFromNeutral));
    Assertions.assertTrue(tol.checkWithinTolerance(mzFromSmiles, mzFromNeutral));
  }
}
