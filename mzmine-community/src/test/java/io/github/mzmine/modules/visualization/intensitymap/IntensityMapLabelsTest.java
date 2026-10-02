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

package io.github.mzmine.modules.visualization.intensitymap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapLayer;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IntensityMapLabelsTest {

  @Test
  void textJoinsNameAndIon() {
    assertEquals("Caffeine [M+H]+", IntensityMapLabels.text("Caffeine", "[M+H]+", "195.0877"));
  }

  @Test
  void rangesFallBackToTheApex() {
    assertEquals(Range.closed(4.5, 4.75), IntensityMapLabels.range(Range.closed(4.5f, 4.75f), 4.6));
    assertEquals(Range.singleton(4.6), IntensityMapLabels.range(null, 4.6));
  }

  @Test
  void imageTitlesNameTheFirstIonsAndCountTheRest() {
    assertEquals("PC 34:1 [M+H]+", IntensityMapLabels.joinNames(List.of("PC 34:1 [M+H]+")));
    assertEquals("PC 34:1 [M+H]+, PE 36:2 [M+Na]+ +2", IntensityMapLabels.joinNames(
        List.of("PC 34:1 [M+H]+", "PE 36:2 [M+Na]+", "SM 34:1", "TG 52:2")));
  }

  @Test
  void gcDeconvolutionIonsAreReadFromThePseudoSpectrum() {
    final RawDataFile file = Mockito.mock(RawDataFile.class);
    final Feature feature = Mockito.mock(Feature.class);
    Mockito.when(feature.getMZ()).thenReturn(85.1);
    // created outside the stubbing call, because the constructor reads the storage of the mocked
    // file
    final Scan spectrum = new SimplePseudoSpectrum(file, 1, 4.2f, null,
        new double[]{57, 71, 85.1, 200}, new double[]{10, 20, 30, 40}, PolarityType.POSITIVE, null,
        PseudoSpectrumType.GC_EI);
    Mockito.when(feature.getAllMS2FragmentScans()).thenReturn(List.of(spectrum));
    final IntensityMapLayer layer = new IntensityMapLayer("a", file, Range.closed(50d, 150d),
        Color.BLACK, true);
    // the feature itself and ions outside the overlay are left out
    final List<double[]> ions = IntensityMapLabels.deconvoluted(feature, layer);
    assertEquals(2, ions.size());
    assertEquals(57, ions.get(0)[0], 1e-9);
    assertEquals(10, ions.get(0)[1], 1e-9);
    assertEquals(71, ions.get(1)[0], 1e-9);
  }

  @Test
  void otherPseudoSpectraAreNotDeconvolutionIons() {
    final RawDataFile file = Mockito.mock(RawDataFile.class);
    final Feature feature = Mockito.mock(Feature.class);
    Mockito.when(feature.getMZ()).thenReturn(85.1);
    // fragments of LC-DIA are not signals of the MS1 map
    final Scan spectrum = new SimplePseudoSpectrum(file, 2, 4.2f, null, new double[]{57, 71},
        new double[]{10, 20}, PolarityType.POSITIVE, null, PseudoSpectrumType.LC_DIA);
    Mockito.when(feature.getAllMS2FragmentScans()).thenReturn(List.of(spectrum));
    final IntensityMapLayer layer = new IntensityMapLayer("a", file, Range.closed(50d, 150d),
        Color.BLACK, true);
    assertTrue(IntensityMapLabels.deconvoluted(feature, layer).isEmpty());
  }

  @Test
  void textFallsBackToMz() {
    assertEquals("m/z 195.0877 [M+Na]+", IntensityMapLabels.text(null, "[M+Na]+", "195.0877"));
    assertEquals("m/z 195.0877", IntensityMapLabels.text(" ", null, "195.0877"));
  }
}
