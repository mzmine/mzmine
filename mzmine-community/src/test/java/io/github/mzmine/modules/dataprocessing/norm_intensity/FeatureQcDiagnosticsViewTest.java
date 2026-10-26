/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FeatureQcDiagnosticsViewTest {

  @Test
  void pageBoundsRowsWithoutCopyingTheCompleteDiagnostics() {
    final List<Integer> rows = java.util.stream.IntStream.range(0, 205).boxed().toList();

    assertEquals(List.of(0, 1), FeatureQcDiagnosticsView.page(rows, 0, 2));
    assertEquals(List.of(200, 201, 202, 203, 204), FeatureQcDiagnosticsView.page(rows, 2, 100));
    assertTrue(FeatureQcDiagnosticsView.page(rows, 3, 100).isEmpty());
    assertTrue(FeatureQcDiagnosticsView.page(rows, 0, 0).isEmpty());
  }
}
