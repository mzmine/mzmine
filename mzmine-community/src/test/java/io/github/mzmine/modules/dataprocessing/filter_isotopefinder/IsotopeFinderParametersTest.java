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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.util.XMLUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Parameter defaults, save/load round-trip, and backward compatibility (legacy batches that still
 * contain the removed "Search in scans" parameter must load without error and fall back to the
 * signal-based, single-scan defaults).
 */
class IsotopeFinderParametersTest {

  // clone so mutations do not pollute the shared static parameter instances (as production does)
  private static IsotopeFinderParameters cloned() {
    return (IsotopeFinderParameters) new IsotopeFinderParameters().cloneParameterSet();
  }

  @Test
  void freshParametersDefaultToSignalBasedSingleScan() {
    final IsotopeFinderParameters params = cloned();
    assertEquals(IsotopeFinderModeOptions.SIGNAL_BASED,
        params.getValue(IsotopeFinderParameters.mode));
    assertFalse(params.getValue(IsotopeFinderParameters.fwhmRefine));
  }

  @Test
  void saveLoadRoundtripPreservesModeChargeAndRefine() throws Exception {
    final IsotopeFinderParameters params = cloned();
    params.setParameter(IsotopeFinderParameters.maxCharge, 4);
    params.getParameter(IsotopeFinderParameters.mode)
        .setValue(IsotopeFinderModeOptions.FORMULA_PREDICTION);
    params.getParameter(IsotopeFinderParameters.fwhmRefine).setValue(true);

    final String xml = ParameterUtils.saveValuesToXMLString(params);
    final IsotopeFinderParameters loaded = cloned();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);

    assertEquals(4, loaded.getValue(IsotopeFinderParameters.maxCharge));
    assertEquals(IsotopeFinderModeOptions.FORMULA_PREDICTION,
        loaded.getValue(IsotopeFinderParameters.mode));
    assertTrue(loaded.getValue(IsotopeFinderParameters.fwhmRefine));
  }

  @Test
  void legacyScanRangeParameterIsIgnoredAndDefaultsApply() throws Exception {
    // simulate a legacy batch: only the (kept) maxCharge plus the removed "Search in scans"
    final Document doc = XMLUtils.newDocument();
    final Element root = doc.createElement("parameters");
    doc.appendChild(root);

    final Element charge = doc.createElement("parameter");
    charge.setAttribute("name", IsotopeFinderParameters.maxCharge.getName());
    charge.setTextContent("3");
    root.appendChild(charge);

    final Element legacyScanRange = doc.createElement("parameter");
    legacyScanRange.setAttribute("name", "Search in scans");
    legacyScanRange.setTextContent("SINGLE MOST INTENSE");
    root.appendChild(legacyScanRange);

    final IsotopeFinderParameters params = cloned();
    params.loadValuesFromXML(root); // must not throw on the unknown parameter

    assertEquals(3, params.getValue(IsotopeFinderParameters.maxCharge));
    assertEquals(IsotopeFinderModeOptions.SIGNAL_BASED,
        params.getValue(IsotopeFinderParameters.mode));
    assertFalse(params.getValue(IsotopeFinderParameters.fwhmRefine));
  }
}
