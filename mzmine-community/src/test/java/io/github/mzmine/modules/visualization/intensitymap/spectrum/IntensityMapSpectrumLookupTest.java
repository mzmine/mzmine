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

package io.github.mzmine.modules.visualization.intensitymap.spectrum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.modules.visualization.intensitymap.IntensityMapDimensions;
import io.github.mzmine.modules.visualization.intensitymap.IntensityMapParameters;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapSliceMode;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

public class IntensityMapSpectrumLookupTest {

  @Test
  void lcMsUsesTheClosestRetentionTime() {
    final RawDataFile file = mock(RawDataFile.class);
    final Scan a = mock(Scan.class);
    final Scan b = mock(Scan.class);
    when(a.getRetentionTime()).thenReturn(1f);
    when(b.getRetentionTime()).thenReturn(2f);
    final var source = new IntensityMapSpectrumLookup(parameters(file, new Scan[]{b, a}),
        IntensityMapDimensions.LC_MS);
    assertEquals(IntensityMapSliceMode.X, source.sliceMode());
    assertSame(a, source.scanAt(file, 1.4, 500));
    assertSame(b, source.scanAt(file, 1.6, 500));
    assertSame(b, source.scanAt(file, 99, 500));
  }

  @Test
  void mobilityFramesHaveNoSpectrumSource() {
    // frames are picked from a base peak chromatogram instead
    final RawDataFile file = mock(IMSRawDataFile.class);
    assertThrows(IllegalArgumentException.class,
        () -> new IntensityMapSpectrumLookup(parameters(file, new Scan[0]),
            IntensityMapDimensions.MOBILITY_FRAME));
  }

  @Test
  void imagingUsesThePixelUnderThePosition() {
    final ImagingRawDataFile file = mock(ImagingRawDataFile.class);
    when(file.getImagingParam()).thenReturn(new ImagingParameters(30, 30, 10, 10, 3, 3));
    final ImagingScan pixel = mock(ImagingScan.class);
    when(pixel.getCoordinates()).thenReturn(new Coordinates(2, 1, 0));
    final var source = new IntensityMapSpectrumLookup(parameters(file, new Scan[]{pixel}),
        IntensityMapDimensions.IMAGING);
    assertEquals(IntensityMapSliceMode.POINT, source.sliceMode());
    assertSame(pixel, source.scanAt(file, 21, 9));
    assertNull(source.scanAt(file, 10, 10));
  }

  @Test
  void apexSnapsToTheMostIntensePointInTheWindow() {
    final Scan scan = mock(Scan.class);
    final double[] mz = {99.9, 100.0, 100.01, 100.02, 100.3};
    final double[] intensity = {50, 10, 30, 20, 99};
    when(scan.getNumberOfDataPoints()).thenReturn(mz.length);
    when(scan.binarySearch(anyDouble(), any(DefaultTo.class))).thenReturn(1);
    for (int i = 0; i < mz.length; i++) {
      when(scan.getMzValue(i)).thenReturn(mz[i]);
      when(scan.getIntensityValue(i)).thenReturn(intensity[i]);
    }
    assertEquals(100.01, IntensityMapSpectrumLookup.apex(scan, Range.closed(99.95, 100.05), 100.0),
        1e-12);
  }

  private static @NotNull ParameterSet parameters(@NotNull final RawDataFile file,
      final Scan @NotNull [] scans) {
    final ParameterSet parameters = mock(ParameterSet.class);
    final ScanSelection selection = mock(ScanSelection.class);
    when(selection.getMatchingScans(file)).thenReturn(scans);
    when(parameters.getValue(IntensityMapParameters.scanSelection)).thenReturn(selection);
    return parameters;
  }
}
