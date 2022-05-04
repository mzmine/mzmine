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
    Assertions.assertEquals(Range.closed(16.020751548090537, 16.04075154809054),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4", IonizationType.POSITIVE, tol));
    Assertions.assertEquals(Range.closed(16.021848707909456, 16.04184870790946),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4", IonizationType.NEGATIVE, tol));
    Assertions.assertEquals(Range.closed(17.028576127999997, 17.048576128),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4",
            IonizationType.POSITIVE_HYDROGEN, tol));
    Assertions.assertEquals(Range.closed(15.014024127999997, 15.034024127999997),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4",
            IonizationType.NEGATIVE_HYDROGEN, tol));
    Assertions.assertEquals(Range.closed(129.00688612800002, 129.026886128),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4", IonizationType.TRIFLUORACETATE,
            tol));
    Assertions.assertEquals(Range.closed(6.998374063999999, 7.018374063999999),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4", IonizationType.NAME_1, tol));
    Assertions.assertEquals(Range.closed(13.668357909333334, 13.688357909333334),
        MzRangeFormulaCalculatorModule.getMzRangeFromFormula("CH4", IonizationType.NAME7, tol));
  }
}
