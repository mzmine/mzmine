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

package io.github.mzmine.util.scans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests how {@link IntegerMode} is written to and read from a parameter set. It is saved by the
 * NIST search and by the ADAP MGF and MSP exporters, so both the current form and the one written
 * before the enum had unique ids have to be readable.
 */
class IntegerModeTest {

  @Test
  @DisplayName("A value is saved by its unique id, not by its label")
  void savesTheUniqueId() throws ParserConfigurationException {

    for (final IntegerMode mode : IntegerMode.values()) {

      final ComboParameter<IntegerMode> parameter = newParameter();
      parameter.setValue(mode);

      final Element element = newElement();
      parameter.saveValueToXML(element);

      assertEquals(mode.getUniqueID(), element.getTextContent());
    }
  }

  @Test
  @DisplayName("A value round trips")
  void roundTrips() throws ParserConfigurationException {

    for (final IntegerMode mode : IntegerMode.values()) {

      final ComboParameter<IntegerMode> saved = newParameter();
      saved.setValue(mode);

      final Element element = newElement();
      saved.saveValueToXML(element);

      final ComboParameter<IntegerMode> loaded = newParameter();
      loaded.loadValueFromXML(element);

      assertEquals(mode, loaded.getValue());
    }
  }

  @Test
  @DisplayName("A batch saved before the unique ids still reads its value")
  void readsTheLegacyLabel() throws ParserConfigurationException {

    // what ComboParameter wrote while IntegerMode was not a UniqueIdSupplier: its toString()
    for (final IntegerMode mode : IntegerMode.values()) {

      final Element element = newElement();
      element.setTextContent(mode.toString());

      final ComboParameter<IntegerMode> loaded = newParameter();
      loaded.loadValueFromXML(element);

      assertEquals(mode, loaded.getValue(), "legacy label " + mode);
    }
  }

  private static ComboParameter<IntegerMode> newParameter() {
    // the default is deliberately not the value under test, so a failed load is visible
    return new ComboParameter<>("Integer m/z", "description", IntegerMode.values(), null);
  }

  private static Element newElement() throws ParserConfigurationException {
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .newDocument();
    return document.createElement("parameter");
  }
}
