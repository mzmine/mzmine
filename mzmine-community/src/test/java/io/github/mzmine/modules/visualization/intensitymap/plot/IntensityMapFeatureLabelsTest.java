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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapLabel;

import java.util.List;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;

class IntensityMapFeatureLabelsTest {

  private static final Bounds AREA = new BoundingBox(0, 0, 400, 300);

  @Test
  void labelsPreferTheSpaceAboveRight() {
    final Bounds bounds = IntensityMapFeatureLabels.free(new Point2D(100, 100), 50, 14, List.of(),
        AREA);
    assertNotNull(bounds);
    assertEquals(103, bounds.getMinX(), 1e-9);
    assertEquals(83, bounds.getMinY(), 1e-9);
  }

  @Test
  void takenSpaceMovesTheLabelToTheLeft() {
    final Bounds taken = new BoundingBox(103, 83, 50, 14);
    final Bounds bounds = IntensityMapFeatureLabels.free(new Point2D(100, 100), 50, 14,
        List.of(taken), AREA);
    assertNotNull(bounds);
    assertEquals(47, bounds.getMinX(), 1e-9);
    assertEquals(83, bounds.getMinY(), 1e-9);
  }

  @Test
  void labelsStayInsideTheArea() {
    // above is outside, so only below right remains
    final Bounds bounds = IntensityMapFeatureLabels.free(new Point2D(100, 5), 50, 14, List.of(),
        AREA);
    assertNotNull(bounds);
    assertEquals(8, bounds.getMinY(), 1e-9);
  }

  @Test
  void searchMatchesNamesAndDescriptionsIgnoringCase() {
    final IntensityMapLabel caffeine = label("Caffeine", "[M+Na]+", "Compound 3: Caffeine");
    final IntensityMapLabel unknown = label(null, "m/z 301.1410", "m/z 301.1410 · RT 4.1 min");
    final IntensityMapFeatureLabels labels = new IntensityMapFeatureLabels();
    labels.setLabels(List.of(caffeine, unknown));
    assertEquals(List.of("Caffeine"), labels.names());

    labels.setFilter(" caff ");
    assertTrue(labels.matches(caffeine));
    assertFalse(labels.matches(unknown));
    labels.setFilter("301.14");
    assertFalse(labels.matches(caffeine));
    assertTrue(labels.matches(unknown));
    labels.setFilter("");
    assertTrue(labels.matches(caffeine));
    assertTrue(labels.matches(unknown));
  }

  private static IntensityMapLabel label(final String name, final String text,
      final String description) {
    return new IntensityMapLabel("s", 1, 100, Range.singleton(1d), Range.singleton(100d), 10, name,
        text, description, name != null, false, null, List.of());
  }

  @Test
  void labelsWithoutSpaceAreHidden() {
    assertNull(IntensityMapFeatureLabels.free(new Point2D(100, 100), 50, 14,
        List.of(new BoundingBox(0, 0, 400, 300)), AREA));
  }
}
