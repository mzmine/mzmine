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

import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.DEPTH;
import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.WIDTH;

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapPicker;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapScreenGeometry;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapTile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Places the overlays by layout: all in one place, or side by side as uniformly scaled tiles in one
 * scene, so rotation and zoom stay linked and meshes are reused. Every tile has its own axes and
 * title. Also places the labels of all axes and titles on screen.
 */
final class IntensityMapTileLayout {

  /**
   * Cells per axis of the envelopes that fit the camera and outline the tiles.
   */
  static final int FIT_DIVISIONS = 12;

  private final Group model;
  private final IntensityMapAxes axes;
  // axes of the second and following side by side tiles
  private final Group extraAxesGroup = new Group();
  private final List<IntensityMapAxes> extraAxes = new ArrayList<>();
  private final Pane labelLayer;
  private final Map<String, IntensityMapSeriesState> states;
  private final Pane titleLayer = new Pane();
  private final List<Label> tileLabels = new ArrayList<>();
  private List<IntensityMapTile> tiles = List.of(IntensityMapTile.IDENTITY);
  private List<IntensityMapSeries> tiled = List.of();

  /**
   * @param axes       axes of the first tile
   * @param labelLayer layer of all axis labels and titles, so their bounds share coordinates
   * @param states     state of every overlay by id, owned by the plot
   */
  IntensityMapTileLayout(@NotNull final Group model, @NotNull final IntensityMapAxes axes,
      @NotNull final Pane labelLayer, @NotNull final Map<String, IntensityMapSeriesState> states) {
    this.model = model;
    this.axes = axes;
    this.labelLayer = labelLayer;
    this.states = states;
    extraAxesGroup.setMouseTransparent(true);
    titleLayer.setMouseTransparent(true);
    labelLayer.getChildren().setAll(axes.labels(), titleLayer);
  }

  @NotNull Group extraAxesGroup() {
    return extraAxesGroup;
  }

  /**
   * @return the tiles, one identity tile unless side by side
   */
  @NotNull List<IntensityMapTile> tiles() {
    return tiles;
  }

  /**
   * @return the overlays shown side by side, one per tile; empty unless side by side
   */
  @NotNull List<IntensityMapSeries> tiled() {
    return tiled;
  }

  /**
   * @return the tile of the overlay, the only tile unless side by side; null if the overlay has no
   * tile, e.g. when hidden
   */
  @Nullable IntensityMapTile tileOf(@NotNull final String id) {
    if (tiled.isEmpty()) {
      return tiles.getFirst();
    }
    for (int i = 0; i < tiled.size() && i < tiles.size(); i++) {
      if (tiled.get(i).id().equals(id)) {
        return tiles.get(i);
      }
    }
    return null;
  }

  /**
   * @return state of the overlay of a side by side tile, null if there is none
   */
  private @Nullable IntensityMapSeriesState tileState(final int index) {
    // tiles can briefly refer to removed overlays until the new layout is applied
    return index < tiled.size() && index < tiles.size() ? states.get(tiled.get(index).id()) : null;
  }

  /**
   * Places the overlays according to the layout. Only transforms change, meshes are reused.
   *
   * @param series     all overlays
   * @param visible    visible overlays, side by side shows only these
   * @param columns    grid columns, 0 for automatic
   * @param rows       grid rows, 0 for automatic
   * @param surfaces   group of the overlay meshes
   * @param titleStyle style of the tile titles
   * @param newAxes    sets up axes created for additional tiles
   */
  void apply(@NotNull final IntensityMapLayout layout,
      @NotNull final List<IntensityMapSeries> series,
      @NotNull final List<IntensityMapSeries> visible, final int columns, final int rows,
      @NotNull final Group surfaces, @NotNull final String titleStyle,
      @NotNull final Consumer<IntensityMapAxes> newAxes) {
    titleLayer.getChildren().clear();
    tileLabels.clear();
    for (final IntensityMapSeriesState state : states.values()) {
      state.view().getTransforms().clear();
      state.view().setTranslateY(0);
    }
    axes.geometry().getTransforms().clear();
    surfaces.getChildren().setAll(series.stream().map(s -> states.get(s.id()).view()).toList());
    switch (layout) {
      case OVERLAY -> {
        clear();
        tiles = List.of(IntensityMapTile.IDENTITY);
        tiled = List.of();
        // assumption: coincident surfaces are common for replicate samples; a small lift per
        // overlay resolves depth ties consistently instead of flickering stripes
        for (int i = 0; i < series.size(); i++) {
          states.get(series.get(i).id()).view().setTranslateY(-0.3 * i);
        }
      }
      case GRID -> {
        tiled = visible;
        tiles = IntensityMapTile.grid(tiled.size(), columns, rows);
        final List<String> titles = IntensityMapTileTitles.of(tiled);
        ensureExtraAxes(Math.max(0, tiled.size() - 1), newAxes);
        for (int i = 0; i < tiled.size(); i++) {
          final IntensityMapTile tile = tiles.get(i);
          states.get(tiled.get(i).id()).view().getTransforms().setAll(tile.transforms());
          // every tile has its own axes, calibrated like the first one
          tileAxes(i).geometry().getTransforms().setAll(tile.transforms());
          final Label label = new Label(titles.get(i));
          label.setManaged(false);
          label.setMouseTransparent(true);
          label.setVisible(false);
          label.setStyle(titleStyle);
          // multi-line titles, e.g. image annotations below the m/z
          label.setTextAlignment(TextAlignment.CENTER);
          titleLayer.getChildren().add(label);
          tileLabels.add(label);
        }
      }
    }
  }

  void setTitleStyle(@NotNull final String style) {
    for (final Label label : tileLabels) {
      label.setStyle(style);
    }
  }

  /**
   * Removes the axes of additional tiles.
   */
  void clear() {
    ensureExtraAxes(0, _ -> {
    });
  }

  @NotNull List<IntensityMapAxes> allAxes() {
    final List<IntensityMapAxes> all = new ArrayList<>(extraAxes.size() + 1);
    all.add(axes);
    all.addAll(extraAxes);
    return all;
  }

  @NotNull IntensityMapAxes tileAxes(final int index) {
    return index == 0 ? axes : extraAxes.get(index - 1);
  }

  private void ensureExtraAxes(final int count, @NotNull final Consumer<IntensityMapAxes> setup) {
    while (extraAxes.size() > count) {
      final IntensityMapAxes removed = extraAxes.removeLast();
      extraAxesGroup.getChildren().remove(removed.geometry());
      labelLayer.getChildren().remove(removed.labels());
    }
    while (extraAxes.size() < count) {
      final IntensityMapAxes added = new IntensityMapAxes();
      setup.accept(added);
      extraAxes.add(added);
      extraAxesGroup.getChildren().add(added.geometry());
      // below the title layer, which stays the last child
      labelLayer.getChildren().add(labelLayer.getChildren().size() - 1, added.labels());
    }
  }

  /**
   * @return the tile whose floor the ray hits, the first one if none
   */
  @NotNull IntensityMapTile tileAtFloor(@NotNull final IntensityMapPicker.Ray ray) {
    final int index = IntensityMapTile.floorIndex(tiles, ray);
    return tiles.get(Math.max(0, index));
  }

  /**
   * Projects all axes and places their labels and the tile titles: titles first, then axis labels
   * where they cover nothing.
   *
   * @param intensityVisible show the intensity axis, hidden in top views
   * @param showLabels       show tile titles
   * @return screen bounds of the shown titles and axis labels, which further labels must not cover
   */
  @NotNull List<Bounds> project(final boolean intensityVisible, final boolean showLabels) {
    final List<IntensityMapAxes> all = allAxes();
    for (final IntensityMapAxes value : all) {
      value.setIntensityVisible(intensityVisible);
      value.project();
    }
    if (model.getScene() == null) {
      return List.of();
    }
    final List<List<Point2D>> hulls = new ArrayList<>();
    for (int i = 0; i < tiled.size() && i < tiles.size(); i++) {
      final IntensityMapSeriesState state = tileState(i);
      hulls.add(state == null ? List.of() : tileHull(tiles.get(i), state.envelope()));
    }
    final List<Bounds> occupied = projectTileTitles(hulls, showLabels);
    for (int i = 0; i < all.size(); i++) {
      final List<List<Point2D>> others = new ArrayList<>(hulls);
      if (i < others.size()) {
        // labels of a tile may touch its own outline, e.g. ticks under tall 3D columns
        others.remove(i);
      }
      all.get(i).hideOverlaps(occupied, others,
          new BoundingBox(0, 0, labelLayer.getWidth(), labelLayer.getHeight()));
    }
    return occupied;
  }

  /**
   * Places tile titles above or below the projected outline of their tile, and only where they
   * cover no other tile, axis label, or title.
   */
  private @NotNull List<Bounds> projectTileTitles(@NotNull final List<List<Point2D>> hulls,
      final boolean showLabels) {
    final List<Bounds> obstacles = new ArrayList<>();
    if (tileLabels.isEmpty()) {
      return obstacles;
    }
    final Bounds area = new BoundingBox(0, 0, labelLayer.getWidth(), labelLayer.getHeight());
    for (int i = 0; i < tileLabels.size(); i++) {
      final Label label = tileLabels.get(i);
      if (!showLabels || i >= hulls.size() || hulls.get(i).isEmpty()) {
        label.setVisible(false);
        continue;
      }
      final Bounds tile = IntensityMapScreenGeometry.bounds(hulls.get(i));
      label.applyCss();
      final double width = Math.min(label.prefWidth(-1), Math.max(60, tile.getWidth()));
      final double height = label.prefHeight(width);
      final double left = tile.getCenterX() - width / 2;
      Bounds placed = null;
      for (final double top : new double[]{tile.getMinY() - height - 4, tile.getMaxY() + 4}) {
        final Bounds candidate = new BoundingBox(left, top, width, height);
        if (fits(candidate, hulls, obstacles, area)) {
          placed = candidate;
          break;
        }
      }
      label.setVisible(placed != null);
      if (placed != null) {
        // long titles are truncated to the tile width
        label.resizeRelocate(placed.getMinX(), placed.getMinY(), width, height);
        obstacles.add(placed);
      }
    }
    return obstacles;
  }

  private static boolean fits(@NotNull final Bounds candidate,
      @NotNull final List<List<Point2D>> hulls, @NotNull final List<Bounds> obstacles,
      @NotNull final Bounds area) {
    if (!area.contains(candidate)) {
      return false;
    }
    for (final List<Point2D> hull : hulls) {
      if (IntensityMapScreenGeometry.intersects(candidate, hull)) {
        return false;
      }
    }
    for (final Bounds obstacle : obstacles) {
      if (candidate.intersects(obstacle)) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return screen outline of the tile floor and the envelope of its signal
   */
  private @NotNull List<Point2D> tileHull(@NotNull final IntensityMapTile tile,
      final float @NotNull [] envelope) {
    final List<Point2D> points = new ArrayList<>();
    for (final Point3D corner : IntensityMapExtent.corners(tile, IntensityMapExtent.full())) {
      addScreenPoint(points, corner);
    }
    for (int cell = 0; cell < envelope.length; cell++) {
      if (envelope[cell] > -1) {
        continue;
      }
      final int cx = cell % FIT_DIVISIONS;
      final int cz = cell / FIT_DIVISIONS;
      for (int dx = 0; dx <= 1; dx++) {
        for (int dz = 0; dz <= 1; dz++) {
          addScreenPoint(points, tile.toModel(cellCorner(cx + dx, envelope[cell], cz + dz)));
        }
      }
    }
    return IntensityMapScreenGeometry.convexHull(points);
  }

  private void addScreenPoint(@NotNull final List<Point2D> points,
      @NotNull final Point3D modelPoint) {
    final Point2D point = IntensityMapScreenGeometry.project(model, modelPoint, labelLayer);
    if (point != null) {
      points.add(point);
    }
  }

  /**
   * @param x index of the corner along x, 0 to {@link #FIT_DIVISIONS}
   * @param z index of the corner along z
   * @return local position of a corner of the fitting grid
   */
  private static @NotNull Point3D cellCorner(final int x, final double y, final int z) {
    return new Point3D((x / (double) FIT_DIVISIONS - 0.5) * WIDTH, y,
        (z / (double) FIT_DIVISIONS - 0.5) * DEPTH);
  }

  /**
   * Small boxes enclosing the actual signal give a tighter fit than one large bounding box.
   *
   * @return model points of the signal of all tiles
   */
  @NotNull List<Point3D> fittingPoints(@NotNull final IntensityMapLayout layout,
      @NotNull final List<IntensityMapSeries> snapshot) {
    final List<Point3D> points = new ArrayList<>();
    switch (layout) {
      case OVERLAY -> {
        final float[] heights = new float[FIT_DIVISIONS * FIT_DIVISIONS];
        for (final IntensityMapSeries value : snapshot) {
          final float[] envelope = states.get(value.id()).envelope();
          for (int i = 0; i < envelope.length && i < heights.length; i++) {
            heights[i] = Math.min(heights[i], envelope[i]);
          }
        }
        addEnvelope(points, heights, IntensityMapTile.IDENTITY);
      }
      case GRID -> {
        for (int i = 0; i < tiled.size() && i < tiles.size(); i++) {
          final IntensityMapSeriesState state = tileState(i);
          if (state != null) {
            addEnvelope(points, state.envelope(), tiles.get(i));
          }
        }
      }
    }
    return points;
  }

  /**
   * @return model points of the signal of one side by side tile, null if there is no such tile
   */
  @Nullable List<Point3D> tileFittingPoints(final int index) {
    final IntensityMapSeriesState state = index < 0 ? null : tileState(index);
    if (state == null) {
      return null;
    }
    final List<Point3D> points = new ArrayList<>();
    addEnvelope(points, state.envelope(), tiles.get(index));
    return points;
  }

  private static void addEnvelope(@NotNull final List<Point3D> points,
      final float @NotNull [] heights, @NotNull final IntensityMapTile tile) {
    for (int z = 0; z < FIT_DIVISIONS; z++) {
      for (int x = 0; x < FIT_DIVISIONS; x++) {
        final int cell = z * FIT_DIVISIONS + x;
        final double y = cell < heights.length ? heights[cell] : 0;
        for (int dx = 0; dx <= 1; dx++) {
          for (int dz = 0; dz <= 1; dz++) {
            points.add(tile.toModel(cellCorner(x + dx, y, z + dz)));
          }
        }
      }
    }
  }
}
