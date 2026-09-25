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

package io.github.mzmine.modules.visualization.intensitymap.render;

import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.DEPTH;
import static io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh.WIDTH;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point3D;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.jetbrains.annotations.NotNull;

/**
 * Placement of one small multiple inside the model. Tiles are scaled uniformly, so a ray keeps
 * its parameter when transformed into a tile and hits of different tiles can be compared.
 *
 * @param x     center in model coordinates
 * @param z     center in model coordinates
 * @param scale uniform scale of the tile content
 */
public record IntensityMapTile(double x, double z, double scale) {

  public static final IntensityMapTile IDENTITY = new IntensityMapTile(0, 0, 1);
  // leaves room between tiles for the axis labels of every tile
  private static final double FILL = 0.74;

  /**
   * @return a near square grid, first tile at the back left so reading order matches the default
   * view from the front
   */
  public static @NotNull List<IntensityMapTile> grid(final int count) {
    return grid(count, 0, 0);
  }

  /**
   * @param requestedColumns images per row, 0 for automatic
   * @param requestedRows    rows, 0 for automatic. Rows are added if the grid is too small.
   */
  public static @NotNull List<IntensityMapTile> grid(final int count, final int requestedColumns,
      final int requestedRows) {
    if (count <= 1) {
      return List.of(IDENTITY);
    }
    final int columns = columns(count, requestedColumns, requestedRows);
    final int rows = Math.max(requestedRows, (int) Math.ceil(count / (double) columns));
    final double unit = 1d / Math.max(columns, rows);
    final List<IntensityMapTile> tiles = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      final int column = i % columns;
      final int row = i / columns;
      tiles.add(new IntensityMapTile((column + 0.5 - columns / 2d) * WIDTH * unit,
          (rows - 1 - row + 0.5 - rows / 2d) * DEPTH * unit, unit * FILL));
    }
    return tiles;
  }

  public static int columns(final int count, final int requestedColumns, final int requestedRows) {
    if (requestedColumns > 0) {
      return Math.min(requestedColumns, count);
    }
    if (requestedRows > 0) {
      return (int) Math.ceil(count / (double) requestedRows);
    }
    return (int) Math.ceil(Math.sqrt(count));
  }

  boolean isIdentity() {
    return x == 0 && z == 0 && scale == 1;
  }

  public @NotNull List<Transform> transforms() {
    return isIdentity() ? List.of()
        : List.of(new Translate(x, 0, z), new Scale(scale, scale, scale));
  }

  public @NotNull Point3D toModel(@NotNull final Point3D local) {
    return new Point3D(local.getX() * scale + x, local.getY() * scale, local.getZ() * scale + z);
  }

  public @NotNull IntensityMapPicker.Ray toLocal(@NotNull final IntensityMapPicker.Ray ray) {
    final Point3D origin = ray.origin();
    final Point3D direction = ray.direction();
    return new IntensityMapPicker.Ray(
        new Point3D((origin.getX() - x) / scale, origin.getY() / scale,
            (origin.getZ() - z) / scale),
        new Point3D(direction.getX() / scale, direction.getY() / scale,
            direction.getZ() / scale));
  }
}
