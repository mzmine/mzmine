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

package io.github.mzmine.modules.visualization.surface3d.plot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.Range;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DAxisKind;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DData;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DSeries;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

/**
 * Text of the plot: m/z input, ticks, number formats, and tile titles.
 */
class Surface3DPlotTextTest {

  private static final MZTolerance PPM_10 = new MZTolerance(0, 10);

  @Test
  void parsesMzValuesAndRanges() {
    final List<Range<Double>> ranges = Surface3DMzInput.parse("400, 512.5-513.5; 600.25 – 600.5",
        new MZTolerance(0, 10));
    assertEquals(3, ranges.size());
    assertEquals(400 - 0.004, ranges.get(0).lowerEndpoint(), 1e-9);
    assertEquals(400 + 0.004, ranges.get(0).upperEndpoint(), 1e-9);
    assertEquals(Range.closed(512.5, 513.5), ranges.get(1));
    assertEquals(Range.closed(600.25, 600.5), ranges.get(2));
    assertEquals(Range.closed(99.99, 100.01),
        Surface3DMzInput.parse("100", new MZTolerance(0.01, 0)).getFirst());
    assertThrows(IllegalArgumentException.class, () -> Surface3DMzInput.parse("abc", PPM_10));
    assertThrows(IllegalArgumentException.class, () -> Surface3DMzInput.parse("5-4", PPM_10));
    assertThrows(IllegalArgumentException.class, () -> Surface3DMzInput.parse(" , ", PPM_10));
  }

  @Test
  void ticksUseRoundSteps() {
    assertArrayEquals(new double[]{2, 4, 6, 8, 10, 12, 14, 16, 18},
        Surface3DTicks.ticks(1.3, 18.7, 6), 1e-9);
    assertEquals(0.02, Surface3DTicks.step(400, 400.1, 6), 1e-12);
    assertEquals("400.02", Surface3DTicks.format(400.02, 0.02));
    assertEquals("1.0E6", Surface3DTicks.intensity(1e6));
    assertArrayEquals(new double[]{0, 1, 10, 100, 1000, 10000},
        Surface3DTicks.logTicks(20000, 5), 1e-9);
  }

  @Test
  void tileTitlesOmitSharedParts() {
    final Surface3DData data = data(new double[]{1});
    final List<Surface3DSeries> sameFile = List.of(
        new Surface3DSeries("1", "a.imzML", "m/z 890.6364", data, Color.RED),
        new Surface3DSeries("2", "a.imzML", "m/z 524.2781", data, Color.BLUE));
    assertEquals(List.of("m/z 890.6364", "m/z 524.2781"), Surface3DTileTitles.of(sameFile));
    final List<Surface3DSeries> sameMz = List.of(
        new Surface3DSeries("1", "a.mzML", "all m/z", data, Color.RED),
        new Surface3DSeries("2", "b.mzML", "all m/z", data, Color.BLUE));
    // the shared file extension is left out
    assertEquals(List.of("a", "b"), Surface3DTileTitles.of(sameMz));
  }

  @Test
  void titlesKeepOnlyTheDistinctPartOfFileNames() {
    assertEquals(List.of("01", "02", "10"), Surface3DTileTitles.distinctParts(
        List.of("171103_PMA_TK_01_neg.mzML", "171103_PMA_TK_02_neg.mzML",
            "171103_PMA_TK_10_neg.mzML")));
    // cuts only at separators, numbers stay intact
    assertEquals(List.of("sample12", "sample13"),
        Surface3DTileTitles.distinctParts(List.of("run_sample12.mzML", "run_sample13.mzML")));
    assertEquals(List.of("a", "a"), Surface3DTileTitles.distinctParts(List.of("a", "a")));
  }

  @Test
  void tickDecimalsFollowTheStep() {
    assertEquals(0, Surface3DFormat.decimals(500));
    assertEquals(0, Surface3DFormat.decimals(1));
    assertEquals(1, Surface3DFormat.decimals(0.5));
    assertEquals(2, Surface3DFormat.decimals(0.02));
    final var formats = new io.github.mzmine.gui.preferences.NumberFormats(
        new java.text.DecimalFormat("0.0000"), new java.text.DecimalFormat("0.00"),
        new java.text.DecimalFormat("0.000"), new java.text.DecimalFormat("0.0"),
        new java.text.DecimalFormat("0.0E0"), new java.text.DecimalFormat("0.0"),
        new java.text.DecimalFormat("0.0"), new java.text.DecimalFormat("0.0"),
        io.github.mzmine.gui.preferences.UnitFormat.values()[0]);
    final Surface3DFormat format = new Surface3DFormat(formats);
    assertEquals("500", format.value(500, Surface3DAxisKind.MZ, 500));
    assertEquals("400.12", format.value(400.12, Surface3DAxisKind.MZ, 0.02));
    // readouts keep the full preference format
    assertEquals("400.1200", format.value(400.12, Surface3DAxisKind.MZ));
  }

  private static Surface3DData data(final double[] values) {
    final Surface3DData data = new Surface3DData(values.length, 1, "X", "Y", 0,
        values.length - 1, 0, 0);
    for (int i = 0; i < values.length; i++) {
      data.markColumn(i);
      data.addMaximum(i, 0, values[i]);
    }
    return data;
  }
}
