/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
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
