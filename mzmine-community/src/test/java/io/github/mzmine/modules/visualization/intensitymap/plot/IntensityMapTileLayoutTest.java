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

package io.github.mzmine.modules.visualization.intensitymap.plot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import java.util.List;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class IntensityMapTileLayoutTest {

  @Test
  void mzWindowsOfOneFileShareATile() {
    final IntensityMapGrid chromatogram = new IntensityMapGrid(2, 2, "x", "y", 0, 1, 0, 1);
    final List<IntensityMapSeries> overlays = List.of(overlay("a1", "A", chromatogram),
        overlay("b1", "B", chromatogram), overlay("a2", "A", chromatogram),
        overlay("b2", "B", chromatogram));

    final List<List<IntensityMapSeries>> groups = IntensityMapTileLayout.groups(overlays);

    assertEquals(2, groups.size());
    assertEquals(List.of("a1", "a2"), groups.get(0).stream().map(IntensityMapSeries::id).toList());
    assertEquals(List.of("b1", "b2"), groups.get(1).stream().map(IntensityMapSeries::id).toList());
  }

  @Test
  void imagesKeepATileEach() {
    final IntensityMapGrid image = new IntensityMapGrid(new double[]{0, 1}, new double[]{0, 1}, "x",
        "y", true);
    final List<IntensityMapSeries> overlays = List.of(overlay("a1", "A", image),
        overlay("a2", "A", image));

    assertEquals(2, IntensityMapTileLayout.groups(overlays).size());
  }

  private static @NotNull IntensityMapSeries overlay(@NotNull final String id,
      @NotNull final String file, @NotNull final IntensityMapGrid data) {
    return new IntensityMapSeries(id, file, "m/z " + id, data, Color.BLACK);
  }
}
