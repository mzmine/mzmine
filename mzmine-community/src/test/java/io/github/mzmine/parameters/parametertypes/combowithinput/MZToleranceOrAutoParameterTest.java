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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class MZToleranceOrAutoParameterTest {

  @NotNull
  private static MZToleranceOrAutoParameter newParameter() {
    return new MZToleranceOrAutoParameter(
        new MZToleranceParameter(ToleranceType.SCAN_TO_SCAN, 0.002, 10), AutoOrCustomOption.AUTO);
  }

  @NotNull
  private static Element newElement() throws ParserConfigurationException {
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .newDocument();
    final Element element = document.createElement("parameter");
    document.appendChild(element);
    return element;
  }

  @Test
  void defaultIsAutoWithTheEmbeddedTolerance() {
    final MZToleranceOrAuto value = newParameter().getValue();
    Assertions.assertTrue(value.isAuto());
    Assertions.assertEquals(new MZTolerance(0.002, 10), value.tolerance());
    Assertions.assertEquals("m/z tolerance (scan-to-scan)", newParameter().getName());
  }

  @Test
  void bothOptionsRoundTripThroughXml() throws ParserConfigurationException {
    for (final MZToleranceOrAuto saved : List.of(MZToleranceOrAuto.auto(new MZTolerance(0, 7.5)),
        MZToleranceOrAuto.custom(new MZTolerance(0.005, 20)))) {
      final MZToleranceOrAutoParameter parameter = newParameter();
      parameter.setValue(saved);
      final Element element = newElement();
      parameter.saveValueToXML(element);

      final MZToleranceOrAutoParameter loaded = newParameter();
      loaded.loadValueFromXML(element);
      Assertions.assertEquals(saved, loaded.getValue());
    }
  }

  @Test
  void plainToleranceOfOldBatchesLoadsAsCustom() throws ParserConfigurationException {
    // e.g., the tolerance of the ADAP chromatogram builder
    final MZToleranceParameter legacy = new MZToleranceParameter(ToleranceType.SCAN_TO_SCAN, 0.005,
        20);
    final Element element = newElement();
    legacy.saveValueToXML(element);

    final MZToleranceOrAutoParameter loaded = newParameter();
    loaded.loadValueFromXML(element);
    Assertions.assertEquals(MZToleranceOrAuto.custom(new MZTolerance(0.005, 20)),
        loaded.getValue());
  }

  @Test
  void emptyElementKeepsTheValue() throws ParserConfigurationException {
    final MZToleranceOrAutoParameter loaded = newParameter();
    loaded.loadValueFromXML(newElement());
    Assertions.assertEquals(MZToleranceOrAuto.auto(new MZTolerance(0.002, 10)), loaded.getValue());
  }

  @Test
  void cloneIsIndependent() {
    final MZToleranceOrAutoParameter parameter = newParameter();
    parameter.setValue(MZToleranceOrAuto.custom(new MZTolerance(0.001, 3)));
    final MZToleranceOrAutoParameter clone = parameter.cloneParameter();
    Assertions.assertEquals(parameter.getValue(), clone.getValue());

    clone.setValue(MZToleranceOrAuto.auto(new MZTolerance(0.001, 3)));
    Assertions.assertEquals(AutoOrCustomOption.CUSTOM, parameter.getValue().option());
  }

  @Test
  void onlyCustomNeedsTheTolerance() {
    final MZToleranceOrAutoParameter parameter = newParameter();
    final List<String> errors = new ArrayList<>();
    parameter.setValue(MZToleranceOrAuto.auto(null));
    Assertions.assertTrue(parameter.checkValue(errors), String.join(", ", errors));

    parameter.setValue(MZToleranceOrAuto.custom(new MZTolerance(0, 0)));
    Assertions.assertFalse(parameter.checkValue(errors));
  }
}
