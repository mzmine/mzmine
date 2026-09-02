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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.XMLUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Parameter defaults, save/load round-trip, and the mapping of the simplified "Automatic" algorithm
 * onto the full carbon model setup. Also covers backward compatibility: legacy batches that still
 * contain removed parameters must load without error and fall back to the defaults.
 */
class IsotopeFinderParametersTest {

  // clone so mutations do not pollute the shared static parameter instances (as production does)
  private static IsotopeFinderParameters cloned() {
    return (IsotopeFinderParameters) new IsotopeFinderParameters().cloneParameterSet();
  }

  @Test
  void freshParametersDefaultToAutomatic() {
    final IsotopeFinderParameters params = cloned();
    assertEquals(IsotopeFinderModeOptions.AUTOMATIC, params.getValue(IsotopeFinderParameters.mode));
  }

  @Test
  void automaticMapsOntoCarbonAveragineDefaultsWithItsOwnValues() {
    final IsotopeFinderParameters params = cloned();
    final ParameterSet automatic = params.getParameter(IsotopeFinderParameters.mode)
        .setOptionGetParameters(IsotopeFinderModeOptions.AUTOMATIC);
    automatic.setParameter(AutomaticIsotopeFinderParameters.maxCharge, 3);
    automatic.setParameter(AutomaticIsotopeFinderParameters.requireC13, true);

    final CarbonAveragineAlgorithmParameters resolved = AutomaticIsotopeFinderParameters.toCarbonAveragineParameters(
        automatic);

    // exposed values are carried over
    assertEquals(3, resolved.getValue(CarbonAveragineAlgorithmParameters.maxCharge));
    assertTrue(resolved.getValue(CarbonAveragineAlgorithmParameters.requireC13));
    // everything else falls back to the documented defaults
    assertEquals(CarbonAveragineAlgorithmParameters.DEFAULT_ELEMENTS,
        resolved.getValue(CarbonAveragineAlgorithmParameters.elements));
    assertEquals(CarbonAveragineAlgorithmParameters.DEFAULT_ELEMENT_DETECTION_MODE,
        resolved.getValue(CarbonAveragineAlgorithmParameters.elementDetectionMode));
    assertFalse(resolved.getValue(CarbonAveragineAlgorithmParameters.explainableSignalsOnly));
    assertFalse(resolved.getValue(CarbonAveragineAlgorithmParameters.fwhmRefine));
  }

  @Test
  void saveLoadRoundtripPreservesAlgorithmAndItsParameters() throws Exception {
    final IsotopeFinderParameters params = cloned();
    final ParameterSet algo = params.getParameter(IsotopeFinderParameters.mode)
        .setOptionGetParameters(IsotopeFinderModeOptions.AUTOMATIC);
    algo.setParameter(AutomaticIsotopeFinderParameters.maxCharge, 4);
    algo.setParameter(AutomaticIsotopeFinderParameters.requireC13, true);

    final String xml = ParameterUtils.saveValuesToXMLString(params);
    final IsotopeFinderParameters loaded = cloned();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);

    assertEquals(IsotopeFinderModeOptions.AUTOMATIC, loaded.getValue(IsotopeFinderParameters.mode));
    final ParameterSet loadedAlgo = loaded.getParameter(IsotopeFinderParameters.mode)
        .getEmbeddedParameters();
    assertEquals(4, loadedAlgo.getValue(AutomaticIsotopeFinderParameters.maxCharge));
    assertTrue(loadedAlgo.getValue(AutomaticIsotopeFinderParameters.requireC13));
  }

  /**
   * Only the automatic option is offered, see {@link IsotopeFinderParameters#mode}, so the combo
   * carries no parameter set for the full carbon model option and cannot resolve it from a saved
   * selection. Guards the accidental re-add of an option that the GUI would then show.
   */
  @Test
  void onlyAutomaticAlgorithmIsSelectable() {
    final IsotopeFinderParameters params = cloned();
    assertNull(params.getParameter(IsotopeFinderParameters.mode)
        .getEmbeddedParameters(IsotopeFinderModeOptions.CARBON_MODEL));
  }

  @Test
  void legacyTopLevelParametersAreMappedOntoAutomatic() throws Exception {
    // simulate a legacy batch: the two parameters that moved into the algorithm, the removed
    // "Search in scans", and the old name of the algorithm parameter
    final Document doc = XMLUtils.newDocument();
    final Element root = doc.createElement("parameters");
    doc.appendChild(root);

    final Element charge = doc.createElement("parameter");
    charge.setAttribute("name", "Maximum charge of isotope m/z");
    charge.setTextContent("4");
    root.appendChild(charge);

    final Element tolerance = doc.createElement("parameter");
    tolerance.setAttribute("name", "m/z tolerance (feature-to-scan)");
    final Element absTol = doc.createElement("absolutetolerance");
    absTol.setTextContent("0.003");
    final Element ppmTol = doc.createElement("ppmtolerance");
    ppmTol.setTextContent("20.0");
    tolerance.appendChild(absTol);
    tolerance.appendChild(ppmTol);
    root.appendChild(tolerance);

    final Element legacyMode = doc.createElement("parameter");
    legacyMode.setAttribute("name", "Detection mode");
    legacyMode.setAttribute("selected_item", "signal_based");
    root.appendChild(legacyMode);

    final Element legacyScanRange = doc.createElement("parameter");
    legacyScanRange.setAttribute("name", "Search in scans");
    legacyScanRange.setTextContent("SINGLE MOST INTENSE");
    root.appendChild(legacyScanRange);

    final IsotopeFinderParameters params = cloned();
    params.loadValuesFromXML(root); // must not throw on the unknown parameters

    // the legacy top level setup maps onto the automatic algorithm, which carries the two values
    assertEquals(IsotopeFinderModeOptions.AUTOMATIC, params.getValue(IsotopeFinderParameters.mode));
    final ParameterSet automatic = params.getParameter(IsotopeFinderParameters.mode)
        .getEmbeddedParameters();
    assertEquals(4, automatic.getValue(AutomaticIsotopeFinderParameters.maxCharge));
    final MZTolerance loadedTol = automatic.getValue(
        AutomaticIsotopeFinderParameters.isotopeMzTolerance);
    assertEquals(0.003, loadedTol.getMzTolerance(), 1e-9);
    assertEquals(20.0, loadedTol.getPpmTolerance(), 1e-9);

    // a version 1 batch must inform the user that the algorithm itself changed
    assertTrue(params.getLoadingVersionMessages().contains("reworked"));
  }
}
