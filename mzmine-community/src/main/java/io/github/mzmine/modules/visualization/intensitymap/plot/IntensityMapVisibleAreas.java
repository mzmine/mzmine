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

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapBounds;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapTile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.SubScene;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the part of every overlay that is on screen, e.g. to read only the visible data in detail.
 */
final class IntensityMapVisibleAreas {

  private final IntensityMapCamera camera;
  private final IntensityMapPlotArea plotArea;
  private final IntensityMapTileLayout tileLayout;
  private final Group model;
  private final SubScene scene;

  IntensityMapVisibleAreas(@NotNull final IntensityMapCamera camera,
      @NotNull final IntensityMapPlotArea plotArea,
      @NotNull final IntensityMapTileLayout tileLayout, @NotNull final Group model,
      @NotNull final SubScene scene) {
    this.camera = camera;
    this.plotArea = plotArea;
    this.tileLayout = tileLayout;
    this.model = model;
    this.scene = scene;
  }

  /**
   * @param visible  overlays in view
   * @param plotTile tile with a fixed plot area in 2D, null for none
   * @param cropped  the bounds are a part of the data, so a view of all tiles is not zoomed out
   * @return the visible part of every overlay in view. Side by side, only tiles on screen count, so
   * zooming into one tile does not need the data of the others.
   */
  @NotNull IntensityMapVisibleArea of(@NotNull final IntensityMapBounds bounds,
      @NotNull final List<IntensityMapSeries> visible, @Nullable final IntensityMapTile plotTile,
      final boolean cropped) {
    final Map<String, IntensityMapOverlayView> overlays = new LinkedHashMap<>();
    final List<IntensityMapTile> tiles = tileLayout.tiles();
    final List<List<IntensityMapSeries>> tiled = tileLayout.tiled();
    if (plotTile != null) {
      // the 2D view clips everything outside its fixed plot area
      final Rectangle2D area = plotArea.area(plotTile);
      final IntensityMapOverlayView view = overlayView(bounds, plotArea.floor(plotTile),
          area == null ? scene.getWidth() : area.getWidth(),
          area == null ? scene.getHeight() : area.getHeight(), cropped);
      final int index = tiles.indexOf(plotTile);
      if (tiled.isEmpty()) {
        visible.forEach(value -> overlays.put(value.id(), view));
      } else if (index >= 0 && index < tiled.size()) {
        tiled.get(index).forEach(value -> overlays.put(value.id(), view));
      }
    } else if (tiled.isEmpty()) {
      final IntensityMapOverlayView view = overlayView(bounds, camera.visibleFloor(tiles),
          scene.getWidth(), scene.getHeight(), cropped);
      visible.forEach(value -> overlays.put(value.id(), view));
    } else {
      // decision: every tile has its own visible part; with parts of neighboring tiles on screen,
      // e.g. the right edge of one sample and the left edge of the next, their union would span
      // nearly all data and count as zoomed out
      for (int i = 0; i < tiles.size() && i < tiled.size(); i++) {
        final IntensityMapTile tile = tiles.get(i);
        final double[] floor = camera.visibleFloor(List.of(tile));
        final double[] screen = floor == null ? null : screenExtent(tile, floor);
        if (screen != null) {
          final IntensityMapOverlayView view = overlayView(bounds, floor, screen[1] - screen[0],
              screen[3] - screen[2], cropped);
          tiled.get(i).forEach(value -> overlays.put(value.id(), view));
        }
      }
    }
    return new IntensityMapVisibleArea(overlays);
  }

  /**
   * @param floor visible floor extent in local coordinates, null if unknown
   * @return the visible part, all data if the extent is unknown
   */
  private static @NotNull IntensityMapOverlayView overlayView(
      @NotNull final IntensityMapBounds bounds, final double @Nullable [] floor, final double width,
      final double height, final boolean cropped) {
    if (floor == null || IntensityMapExtent.isEmpty(floor)) {
      return cropped ? new IntensityMapOverlayView(
          IntensityMapExtent.toData(bounds, IntensityMapExtent.full()), false, width, height)
          : new IntensityMapOverlayView(IntensityMapRegion.FULL, true, width, height);
    }
    return new IntensityMapOverlayView(IntensityMapExtent.toData(bounds, floor),
        !cropped && IntensityMapExtent.nearlyAll(floor), width, height);
  }

  /**
   * @return screen extent {x0, x1, y0, y1} of a floor extent of the tile inside the viewport, null
   * if it cannot be projected
   */
  private double @Nullable [] screenExtent(@NotNull final IntensityMapTile tile,
      final double @NotNull [] floor) {
    final double[] screen = IntensityMapExtent.screen(model,
        IntensityMapExtent.corners(tile, floor), scene);
    if (screen == null) {
      return null;
    }
    return new double[]{Math.clamp(screen[0], 0, scene.getWidth()),
        Math.clamp(screen[1], 0, scene.getWidth()), Math.clamp(screen[2], 0, scene.getHeight()),
        Math.clamp(screen[3], 0, scene.getHeight())};
  }
}
