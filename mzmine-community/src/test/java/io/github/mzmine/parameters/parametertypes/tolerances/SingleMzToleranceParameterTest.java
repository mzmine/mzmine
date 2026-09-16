/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.tolerances;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests the single unit m/z tolerance, whose whole point is that only one of the two terms of an
 * {@link MZTolerance} is ever set - the tools it is made for take one value, not the maximum of
 * two.
 */
class SingleMzToleranceParameterTest {

  private static SingleMzToleranceParameter newParameter(final MzToleranceUnit unit) {
    return new SingleMzToleranceParameter("tolerance", "description", unit, 0.005, 20);
  }

  @Test
  @DisplayName("The starting value has only the term of its unit set")
  void startsWithOneTerm() {

    assertEquals(new MZTolerance(0, 20), newParameter(MzToleranceUnit.PPM).getValue());
    assertEquals(new MZTolerance(0.005, 0), newParameter(MzToleranceUnit.DA).getValue());
  }

  @Test
  @DisplayName("A single term tolerance is the term itself at every m/z")
  void isTheTermItself() {

    // the maximum MZTolerance computes is the one term that is set, so the other never interferes
    assertEquals(20 * 100 / 1e6, MzToleranceUnit.PPM.toTolerance(20).getMzToleranceForMass(100),
        1e-12);
    assertEquals(0.005, MzToleranceUnit.DA.toTolerance(0.005).getMzToleranceForMass(1000), 1e-12);
  }

  @Test
  @DisplayName("The unit survives a save and load")
  void roundTripsThroughXml() throws ParserConfigurationException {

    for (final MzToleranceUnit unit : MzToleranceUnit.values()) {

      final SingleMzToleranceParameter saved = newParameter(unit);
      final Element element = newElement();
      saved.saveValueToXML(element);

      final SingleMzToleranceParameter loaded = newParameter(
          unit == MzToleranceUnit.DA ? MzToleranceUnit.PPM : MzToleranceUnit.DA);
      loaded.loadValueFromXML(element);

      assertEquals(saved.getValue(), loaded.getValue(), "round trip of " + unit);
      assertEquals(unit, MzToleranceUnit.of(loaded.getValue()));
    }
  }

  @Test
  @DisplayName("A tolerance of zero is rejected")
  void rejectsZero() {

    final SingleMzToleranceParameter parameter = newParameter(MzToleranceUnit.PPM);
    final List<String> errors = new ArrayList<>();

    assertTrue(parameter.checkValue(errors));

    parameter.setValue(MzToleranceUnit.PPM.toTolerance(0));
    assertFalse(parameter.checkValue(errors));

    parameter.setValue(null);
    assertFalse(parameter.checkValue(errors));
  }

  @Test
  @DisplayName("A clone keeps the value and stays independent")
  void clonesIndependently() {

    final SingleMzToleranceParameter parameter = newParameter(MzToleranceUnit.PPM);
    final SingleMzToleranceParameter clone = parameter.cloneParameter();

    assertEquals(parameter.getValue(), clone.getValue());

    clone.setValue(MzToleranceUnit.DA.toTolerance(0.02));
    assertEquals(new MZTolerance(0, 20), parameter.getValue());
  }

  private static Element newElement() throws ParserConfigurationException {
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .newDocument();
    return document.createElement("parameter");
  }
}
