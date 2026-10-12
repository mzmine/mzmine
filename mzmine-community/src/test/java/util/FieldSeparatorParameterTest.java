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

package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.parameters.parametertypes.combowithinput.FieldSeparator;
import io.github.mzmine.parameters.parametertypes.combowithinput.FieldSeparatorOption;
import io.github.mzmine.parameters.parametertypes.combowithinput.FieldSeparatorParameter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The separator parameters were plain string parameters before, their values must still load.
 */
public class FieldSeparatorParameterTest {

  private static Element parameterElement(String textContent) throws ParserConfigurationException {
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .newDocument();
    final Element element = document.createElement("parameter");
    element.setAttribute("name", FieldSeparatorParameter.DEFAULT_NAME);
    element.setTextContent(textContent);
    document.appendChild(element);
    return element;
  }

  private static FieldSeparator loadLegacy(FieldSeparatorParameter parameter, String storedValue)
      throws ParserConfigurationException {
    parameter.loadValueFromXML(parameterElement(storedValue));
    return parameter.getValue();
  }

  @Test
  void testDefaults() {
    final FieldSeparatorParameter read = FieldSeparatorParameter.forReading("description");
    assertEquals(FieldSeparator.AUTO, read.getValue());
    assertTrue(read.getValue().isAuto());

    final FieldSeparatorParameter write = FieldSeparatorParameter.forWriting("description",
        FieldSeparator.TAB);
    assertEquals(FieldSeparator.TAB, write.getValue());
    // auto cannot be used to write a file
    assertFalse(List.of(FieldSeparatorOption.WRITE_OPTIONS).contains(FieldSeparatorOption.AUTO));
  }

  @Test
  void testSeparatorStrings() {
    assertEquals(",", FieldSeparator.COMMA.separator());
    assertEquals(";", FieldSeparator.SEMICOLON.separator());
    assertEquals("\t", FieldSeparator.TAB.separator());
    assertEquals(" ", new FieldSeparator(FieldSeparatorOption.SPACE).separator());
    assertEquals("auto", FieldSeparator.AUTO.separator());
    assertEquals('\t', FieldSeparator.TAB.separatorChar());

    final FieldSeparator custom = new FieldSeparator(FieldSeparatorOption.CUSTOM, "|");
    assertEquals("|", custom.separator());
    assertEquals('|', custom.separatorChar());
    // the escaped tab that users had to type into the old text field
    assertEquals("\t", new FieldSeparator(FieldSeparatorOption.CUSTOM, "\t").separator());
  }

  @Test
  void testParseLegacyValues() {
    assertEquals(FieldSeparator.COMMA, FieldSeparator.parse(","));
    assertEquals(FieldSeparator.SEMICOLON, FieldSeparator.parse(";"));
    assertEquals(FieldSeparator.TAB, FieldSeparator.parse("\t"));
    assertEquals(FieldSeparator.TAB, FieldSeparator.parse("\t"));
    assertEquals(new FieldSeparator(FieldSeparatorOption.SPACE), FieldSeparator.parse(" "));
    assertEquals(FieldSeparator.AUTO, FieldSeparator.parse("auto"));
    assertEquals(FieldSeparator.AUTO, FieldSeparator.parse(""));
    assertEquals(new FieldSeparator(FieldSeparatorOption.CUSTOM, "|"), FieldSeparator.parse("|"));
  }

  @Test
  void testLoadOldStringParameterValues() throws ParserConfigurationException {
    // batches out there store <parameter name="Field separator">,</parameter>
    assertEquals(FieldSeparator.COMMA,
        loadLegacy(FieldSeparatorParameter.forReading("description"), ","));
    assertEquals(FieldSeparator.TAB,
        loadLegacy(FieldSeparatorParameter.forReading("description"), "\t"));
    assertEquals(new FieldSeparator(FieldSeparatorOption.CUSTOM, "|"),
        loadLegacy(FieldSeparatorParameter.forReading("description"), "|"));

    // an empty legacy value maps to auto, but an export has no auto option and keeps its default
    assertEquals(FieldSeparator.AUTO,
        loadLegacy(FieldSeparatorParameter.forReading("description"), ""));
    assertEquals(FieldSeparator.COMMA,
        loadLegacy(FieldSeparatorParameter.forWriting("description", FieldSeparator.COMMA), ""));
  }

  @Test
  void testSaveAndLoad() throws ParserConfigurationException {
    for (FieldSeparator value : List.of(FieldSeparator.AUTO, FieldSeparator.COMMA,
        FieldSeparator.SEMICOLON, FieldSeparator.TAB,
        new FieldSeparator(FieldSeparatorOption.SPACE),
        new FieldSeparator(FieldSeparatorOption.CUSTOM, "|"))) {
      final FieldSeparatorParameter parameter = FieldSeparatorParameter.forReading("description");
      parameter.setValue(value);

      final Element element = parameterElement("");
      parameter.saveValueToXML(element);

      final FieldSeparatorParameter loaded = FieldSeparatorParameter.forReading("description");
      loaded.loadValueFromXML(element);
      assertEquals(value.separator(), loaded.getValue().separator(), value.toString());
      assertEquals(value.option(), loaded.getValue().option(), value.toString());
    }
  }

  @Test
  void testCheckValue() {
    final List<String> errors = new ArrayList<>();
    final FieldSeparatorParameter parameter = FieldSeparatorParameter.forReading("description");
    assertTrue(parameter.checkValue(errors));

    parameter.setValue(new FieldSeparator(FieldSeparatorOption.CUSTOM, ""));
    assertFalse(parameter.checkValue(errors));
    assertEquals(1, errors.size());

    parameter.setValue(new FieldSeparator(FieldSeparatorOption.CUSTOM, "|"));
    assertTrue(parameter.checkValue(errors));
  }
}
