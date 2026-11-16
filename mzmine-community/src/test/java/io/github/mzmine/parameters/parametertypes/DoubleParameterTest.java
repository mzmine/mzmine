package io.github.mzmine.parameters.parametertypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DoubleParameterTest {

  @Test
  void describingDisplayPrecisionCannotChangeTheFormatOrRestrictStoredPrecision() {
    final DoubleParameter parameter = new DoubleParameter("Threshold", "",
        new DecimalFormat("0.0E0", DecimalFormatSymbols.getInstance(Locale.US)), 2345.67891);
    final DecimalFormat exposed = (DecimalFormat) parameter.getNumberFormat();
    assertEquals("2.3E3", exposed.format(parameter.getValue()));

    exposed.applyPattern("0.00000");

    assertEquals("2.3E3", parameter.getNumberFormat().format(parameter.getValue()));
    assertEquals(2345.67891, parameter.getValue());
    assertTrue(parameter.checkValue(new ArrayList<>()));
  }
}
