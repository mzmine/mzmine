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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.common.math.Quantiles;
import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Canonical coordinates and lookup tolerances for a set of frames.
 */
record MzMLMobilityAxis(double[] coordinates, RangeMap<Double, Integer> indices) {

  private static final int WATERS_REFERENCE_FUNCTION = 1;

  /**
   * @param type mobility unit system of the acquired frame
   * @return inclusive radius used to recognize repeated mobility coordinates
   */
  static double binTolerance(@NotNull final MobilityType type) {
    return type == MobilityType.TIMS ? 0.000002 : 0.00002;
  }

  /**
   * @return the axis each function uses, sharing function 1's axis where it covers every bin
   */
  static @NotNull Map<Integer, MzMLMobilityAxis> forFrames(
      @NotNull final List<BuildingMobilityScanStorage> frames) {
    final var functions = frames.stream()
        .collect(Collectors.groupingBy(MzMLMobilityAxis::function));
    final var result = new HashMap<Integer, MzMLMobilityAxis>();
    final var reference = functions.containsKey(WATERS_REFERENCE_FUNCTION) ? build(
        functions.get(WATERS_REFERENCE_FUNCTION)) : null;
    for (final var entry : functions.entrySet()) {
      final var group = entry.getValue();
      if (entry.getKey() == WATERS_REFERENCE_FUNCTION || (
          entry.getKey() != MzMLFrameAssembler.NO_WATERS_FUNCTION && reference != null
              && group.stream().flatMap(f -> f.getMobilityScans().stream())
              .allMatch(scan -> reference.indices.get(scan.mobility()) != null))) {
        result.put(entry.getKey(), reference);
      } else {
        result.put(entry.getKey(), build(group));
      }
    }
    return result;
  }

  static int function(@NotNull final BuildingMobilityScanStorage frame) {
    return MzMLFrameAssembler.watersFunction(frame.getMobilityScans().getFirst().id());
  }

  private static @NotNull MzMLMobilityAxis build(
      @NotNull final List<BuildingMobilityScanStorage> frames) {
    final MobilityType type = frames.getFirst().getMobilityScans().getFirst().mobilityType();
    final boolean tims = type == MobilityType.TIMS;
    final double epsilon = binTolerance(type);
    final RangeMap<Double, Boolean> observed = TreeRangeMap.create();
    for (final var frame : frames) {
      for (final var scan : frame.getMobilityScans()) {
        final double value = scan.mobility();
        if (observed.get(value) == null) {
          observed.put(SpectraMerging.createNewNonOverlappingRange(observed,
              Range.closed(value - epsilon, value + epsilon)), true);
        }
      }
    }
    final double[] axis = observed.asMapOfRanges().keySet().stream()
        .mapToDouble(RangeUtils::rangeCenter).toArray();
    final double[] spacing = new double[axis.length - 1];
    for (int i = 0; i < spacing.length; i++) {
      spacing[i] = axis[i + 1] - axis[i];
    }
    final double tolerance =
        spacing.length == 0 ? epsilon : Quantiles.median().compute(spacing) / 10;
    final RangeMap<Double, Integer> indices = TreeRangeMap.create();
    for (int i = 0; i < axis.length; i++) {
      final double lower = i == 0 ? tolerance : Math.min(tolerance, spacing[i - 1] / 2);
      final double upper = i == spacing.length ? tolerance : Math.min(tolerance, spacing[i] / 2);
      indices.put(Range.closed(axis[i] - lower, axis[i] + upper), tims ? axis.length - 1 - i : i);
    }
    if (tims) {
      Doubles.reverse(axis);
    }
    return new MzMLMobilityAxis(axis, indices);
  }
}
