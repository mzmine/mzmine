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

package io.github.mzmine.modules.dataprocessing.id_gc_ei_ion_notation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import org.junit.jupiter.api.Test;

class EiFragmentIonAnnotatorTest {

  // cholesterol TMS ether, molecular ion [M]+ at 458.3938
  private static final String CHOLESTEROL_TMS = "C30H54OSi";
  private static final MZTolerance TOLERANCE = new MZTolerance(0.002, 10);

  private static EiFragmentIonAnnotator createAnnotator() {
    final EiFragmentIonAnnotator annotator = EiFragmentIonAnnotator.create(CHOLESTEROL_TMS,
        TOLERANCE, 50);
    assertNotNull(annotator);
    return annotator;
  }

  @Test
  void molecularIonIsRadicalCation() {
    final EiFragmentIonAnnotator annotator = createAnnotator();
    final double neutralMass = FormulaUtils.getMonoisotopicMass(FormulaUtils.parse(CHOLESTEROL_TMS));
    assertEquals(neutralMass, annotator.getNeutralMass(), 1e-6);
    assertEquals(neutralMass - FormulaUtils.electronMass, annotator.getMolecularIonMz(), 1e-6);

    final EiFragmentIon ion = annotator.annotate(annotator.getMolecularIonMz() + 0.001);
    assertNotNull(ion);
    assertEquals("[M-e]+", ion.ionType().toString());
    assertEquals(annotator.getMolecularIonMz(), ion.exactMz(), 1e-6);
  }

  @Test
  void fragmentIonIsMolecularIonMinusLoss() {
    final EiFragmentIonAnnotator annotator = createAnnotator();
    // C6H13OSi+ is the characteristic ion of TMS sterols, measured in the GC-TOF test data
    final EiFragmentIon ion = annotator.annotate(129.07284);
    assertNotNull(ion);
    assertEquals("C6H13OSi", FormulaUtils.getFormulaString(ion.ionFormula(), false));
    assertEquals("[M-C24H41-e]+", ion.ionType().toString());
    // the ion type calculates the fragment m/z from the neutral mass of M
    assertEquals(ion.exactMz(), ion.ionType().getMZ(annotator.getNeutralMass()), 1e-6);
  }

  @Test
  void trimethylsilylIon() {
    final EiFragmentIon ion = createAnnotator().annotate(73.0468);
    assertNotNull(ion);
    assertEquals("C3H9Si", FormulaUtils.getFormulaString(ion.ionFormula(), false));
    assertEquals("[M-C27H45O-e]+", ion.ionType().toString());
  }

  @Test
  void unexplainedMz() {
    final EiFragmentIonAnnotator annotator = createAnnotator();
    // above the molecular ion
    assertNull(annotator.annotate(500.4));
    // no sub formula within 2 mDa
    assertNull(annotator.annotate(129.2));
  }

  @Test
  void invalidFormula() {
    assertNull(EiFragmentIonAnnotator.create(null, TOLERANCE, 50));
    assertNull(EiFragmentIonAnnotator.create("", TOLERANCE, 50));
  }
}
