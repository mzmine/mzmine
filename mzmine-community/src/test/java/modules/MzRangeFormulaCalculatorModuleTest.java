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

package modules;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.tools.mzrangecalculator.MzRangeFormulaCalculatorModule;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MzRangeFormulaCalculatorModuleTest {

  @Test
  void getMzRangeFromFormula() {

    final MZTolerance tol = new MZTolerance(0.01, 5);
    Assertions.assertEquals(Range.closed(16.02075154809093, 16.040751548090935),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4", IonizationType.POSITIVE, tol));
    Assertions.assertEquals(Range.closed(16.02184870790906, 16.041848707909065),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4", IonizationType.NEGATIVE, tol));
    Assertions.assertEquals(Range.closed(17.02857658009093, 17.048576580090934),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4",
            IonizationType.POSITIVE_HYDROGEN, tol));
    Assertions.assertEquals(Range.closed(15.014023675909066, 15.034023675909065),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4",
            IonizationType.NEGATIVE_HYDROGEN, tol));
    Assertions.assertEquals(Range.closed(129.00688760790908, 129.02688760790906),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4", IonizationType.TRIFLUORACETATE,
            tol));
    Assertions.assertEquals(Range.closed(6.998373611909065, 7.018373611909064),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4", IonizationType.NAME_1, tol));
    Assertions.assertEquals(Range.closed(13.668357910757603, 13.688357910757603),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4", IonizationType.NAME7, tol));
  }
}
