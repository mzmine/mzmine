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

import com.google.common.collect.Range;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapAxisKind;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Broken m/z axis for several separate m/z windows: every window gets a lane of the same height,
 * stacked by m/z, so narrow extraction windows stay visible next to each other instead of thin
 * lines on one continuous axis. Lane coordinates run from 0 at the bottom of the lowest lane; lane
 * k spans k to k + {@link #HEIGHT}, followed by a gap.
 *
 * @param lanes the m/z windows, sorted and disjoint
 */
record IntensityMapLanes(@NotNull List<IntensityMapLane> lanes) {

  // share of a lane unit covered by data, the rest separates the lanes
  static final double HEIGHT = 0.85;

  /**
   * @return lanes for overlays of separate m/z windows along y, null if the overlays share one
   * continuous range, e.g. a single window, overlapping windows, or images
   */
  static @Nullable IntensityMapLanes of(@NotNull final List<IntensityMapSeries> series) {
    final List<IntensityMapLane> windows = new ArrayList<>();
    for (final IntensityMapSeries value : series) {
      final IntensityMapGrid data = value.data();
      if (data.pixels() || data.yKind() != IntensityMapAxisKind.MZ) {
        return null;
      }
      windows.add(new IntensityMapLane(data.yMin(), data.yMax()));
    }
    windows.sort(Comparator.comparingDouble(IntensityMapLane::low));
    final List<IntensityMapLane> merged = new ArrayList<>();
    for (final IntensityMapLane window : windows) {
      final IntensityMapLane last = merged.isEmpty() ? null : merged.getLast();
      if (last != null && window.low() <= last.high()) {
        merged.set(merged.size() - 1,
            new IntensityMapLane(last.low(), Math.max(last.high(), window.high())));
      } else {
        merged.add(window);
      }
    }
    return merged.size() < 2 ? null : new IntensityMapLanes(List.copyOf(merged));
  }

  /**
   * @return upper end of the lane coordinates, the top of the highest lane
   */
  double top() {
    return lanes.size() - 1 + HEIGHT;
  }

  /**
   * @return the lane containing the m/z, -1 if it lies between lanes
   */
  int laneOf(final double mz) {
    for (int i = 0; i < lanes.size(); i++) {
      if (lanes.get(i).contains(mz)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * @return lane coordinate of the m/z, NaN if it lies between lanes
   */
  double toLane(final double mz) {
    final int index = laneOf(mz);
    return index < 0 ? Double.NaN : index + lanes.get(index).share(mz) * HEIGHT;
  }

  /**
   * @return m/z of a lane coordinate, NaN in the gaps between lanes
   */
  double toMz(final double lane) {
    final int index = (int) Math.floor(lane);
    if (index < 0 || index >= lanes.size() || lane - index > HEIGHT) {
      return Double.NaN;
    }
    return lanes.get(index).at((lane - index) / HEIGHT);
  }

  /**
   * @return m/z of the lane coordinate, in a gap the closest lane edge
   */
  double nearestMz(final double lane) {
    final int index = Math.clamp((long) Math.floor(lane), 0, lanes.size() - 1);
    return lanes.get(index).at(Math.clamp((lane - index) / HEIGHT, 0, 1));
  }

  /**
   * @return the overlay data in lane coordinates; data of one window lie in one lane
   */
  @NotNull IntensityMapGrid toLanes(@NotNull final IntensityMapGrid data) {
    final int index = Math.max(0, laneOf((data.yMin() + data.yMax()) / 2));
    final IntensityMapLane lane = lanes.get(index);
    final double scale = HEIGHT / lane.width();
    return data.withLinearY(scale, index - lane.low() * scale);
  }

  /**
   * @param view  visible part in lane coordinates
   * @param laned data of the overlay in lane coordinates
   * @return the visible part of the lane of the overlay in m/z, null if its lane is out of view
   */
  @Nullable IntensityMapOverlayView toMz(@NotNull final IntensityMapOverlayView view,
      @NotNull final IntensityMapGrid laned) {
    final Range<Double> visible = view.region().y();
    if (visible == null) {
      return view;
    }
    final int index = (int) Math.floor((laned.yMin() + laned.yMax()) / 2);
    final double low = Math.max(visible.lowerEndpoint(), index);
    final double high = Math.min(visible.upperEndpoint(), index + HEIGHT);
    if (high <= low) {
      return null;
    }
    final double span = visible.upperEndpoint() - visible.lowerEndpoint();
    // the lane only gets its share of the screen height
    final double height = span > 0 ? view.height() * (high - low) / span : view.height();
    return new IntensityMapOverlayView(
        new IntensityMapRegion(view.region().x(), Range.closed(nearestMz(low), nearestMz(high))),
        view.all(), view.width(), height);
  }
}
