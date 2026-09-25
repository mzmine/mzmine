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

package io.github.mzmine.modules.visualization.surface3d.render;

import io.github.mzmine.modules.visualization.surface3d.data.Surface3DBounds;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DData;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Geometry is calculated off the FX thread and uploaded in bulk as
 * {@link javafx.scene.shape.VertexFormat#POINT_NORMAL_TEXCOORD}. Precomputed normals avoid the
 * expensive smoothing group evaluation JavaFX would otherwise run on the render thread. Only
 * vertices referenced by a face are kept.
 */
public record Surface3DMesh(float @NotNull [] points, float @NotNull [] normals,
                            float @NotNull [] texture, int @NotNull [] faces) {

  public static final double WIDTH = 480;
  public static final double DEPTH = 360;
  public static final double HEIGHT = 220;
  private static final BooleanSupplier NEVER = () -> false;
  // pixel column normals: top, -x, +x, -z, +z
  private static final float[] PIXEL_NORMALS = {0, -1, 0, -1, 0, 0, 1, 0, 0, 0, 0, -1, 0, 0, 1};
  private static final int WALL_MINUS_X = 1;
  private static final int WALL_PLUS_X = 2;
  private static final int WALL_MINUS_Z = 4;
  private static final int WALL_PLUS_Z = 8;

  public int triangles() {
    return faces.length / 9;
  }

  public static @NotNull Surface3DMesh build(@NotNull final Surface3DData data,
      @NotNull final Surface3DBounds bounds, final boolean logarithmic) {
    return build(data, new Surface3DScale(bounds, logarithmic, false), NEVER);
  }

  public static @NotNull Surface3DMesh build(@NotNull final Surface3DData data,
      @NotNull final Surface3DScale scale, @NotNull final BooleanSupplier canceled) {
    return build(data, scale, canceled, null);
  }

  /**
   * @param owners optional overlay colors of a composite. Faces then use one flat overlay color
   *               instead of the intensity paint scale, because interpolating between overlay
   *               indices would blend through unrelated colors.
   */
  public static @NotNull Surface3DMesh build(@NotNull final Surface3DData data,
      @NotNull final Surface3DScale scale, @NotNull final BooleanSupplier canceled,
      @Nullable final Owners owners) {
    return build(data, scale, canceled, owners, null);
  }

  /**
   * @param mix blend fractions of two channels per cell (third is the remainder), used as 2D
   *            texture coordinates into a mixing triangle. Barycentric weights interpolate
   *            linearly, so colors between vertices are exact mixes. Takes precedence over owners.
   */
  public static @NotNull Surface3DMesh build(@NotNull final Surface3DData data,
      @NotNull final Surface3DScale scale, @NotNull final BooleanSupplier canceled,
      @Nullable final Owners owners, final float @Nullable [] mix) {
    final Owners flat = mix == null ? owners : null;
    return data.pixels() ? pixels(data, scale, canceled, flat, mix)
        : surface(data, scale, canceled, flat, mix);
  }

  /**
   * Texture coordinate of a blend fraction, at the texel centers of a MIX_SIZE texture.
   */
  public static float mixCoordinate(final float fraction) {
    return (float) ((0.5 + Math.clamp(fraction, 0, 1) * (MIX_SIZE - 1)) / MIX_SIZE);
  }

  /**
   * Size of the square mixing texture, see {@link #mixCoordinate(float)}
   */
  public static final int MIX_SIZE = 64;

  /**
   * @param cells overlay index per data cell (row * width + column)
   * @param count number of overlay colors in the texture
   */
  public record Owners(int @NotNull [] cells, int count) {

    float @NotNull [] texture() {
      final float[] texture = new float[count * 2];
      for (int i = 0; i < count; i++) {
        texture[i * 2] = (float) ((i + 0.5) / count);
        texture[i * 2 + 1] = 0.5f;
      }
      return texture;
    }

    int at(final int cell) {
      return Math.clamp(cells[cell], 0, count - 1);
    }
  }

  /**
   * Local x coordinate of a data coordinate.
   */
  public static double localX(@NotNull final Surface3DBounds bounds, final double value) {
    return (bounds.normalizeX(value) - 0.5) * WIDTH;
  }

  public static double localZ(@NotNull final Surface3DBounds bounds, final double value) {
    return (bounds.normalizeY(value) - 0.5) * DEPTH;
  }

  public static float @NotNull [] envelope(@NotNull final Surface3DData data,
      @NotNull final Surface3DScale scale, final int divisions,
      @NotNull final BooleanSupplier canceled) {
    final float[] heights = new float[divisions * divisions];
    final int[] columns = new int[data.width()];
    for (int x = 0; x < data.width(); x++) {
      columns[x] = Math.clamp(
          (int) ((localX(scale.bounds(), data.xValue(x)) / WIDTH + 0.5) * divisions), 0,
          divisions - 1);
    }
    for (int y = 0; y < data.height(); y++) {
      checkCanceled(canceled);
      final int row = Math.clamp(
          (int) ((localZ(scale.bounds(), data.yValue(y)) / DEPTH + 0.5) * divisions), 0,
          divisions - 1);
      for (int x = 0; x < data.width(); x++) {
        final float value = data.intensity(x, y);
        if (value > 0 && !scale.belowNoise(data, value)) {
          final int cell = row * divisions + columns[x];
          heights[cell] = Math.min(heights[cell], (float) (-scale.height(data, value) * HEIGHT));
        }
      }
    }
    return heights;
  }

  private static @NotNull Surface3DMesh surface(@NotNull final Surface3DData data,
      @NotNull final Surface3DScale scale, @NotNull final BooleanSupplier canceled,
      @Nullable final Owners owners, final float @Nullable [] mix) {
    final Surface3DBounds bounds = scale.bounds();
    // a single row or column is widened into a thin ribbon so it remains visible
    final int width = Math.max(2, data.width());
    final int height = Math.max(2, data.height());
    final float[] xs = new float[width];
    final float[] zs = new float[height];
    for (int x = 0; x < width; x++) {
      xs[x] = (float) (localX(bounds, data.xValue(Math.min(x, data.width() - 1))) + (
          data.width() == 1 ? (x - 0.5) * 1.5 : 0));
    }
    for (int y = 0; y < height; y++) {
      zs[y] = (float) (localZ(bounds, data.yValue(Math.min(y, data.height() - 1))) + (
          data.height() == 1 ? (y - 0.5) * 1.5 : 0));
    }

    // pass 1: quads with measured corners and signal above the noise floor
    final boolean[] quads = new boolean[(width - 1) * (height - 1)];
    final int[] remap = new int[width * height];
    int quadCount = 0;
    for (int y = 0; y < height - 1; y++) {
      checkCanceled(canceled);
      final int sy = Math.min(y, data.height() - 1);
      final int ny = Math.min(y + 1, data.height() - 1);
      for (int x = 0; x < width - 1; x++) {
        final int sx = Math.min(x, data.width() - 1);
        final int nx = Math.min(x + 1, data.width() - 1);
        if (!data.isPresent(sx, sy) || !data.isPresent(nx, sy) || !data.isPresent(sx, ny)
            || !data.isPresent(nx, ny)) {
          continue;
        }
        // Empty baseline faces obscure overlays and contribute no signal.
        if (quiet(data, scale, sx, sy) && quiet(data, scale, nx, sy) && quiet(data, scale, sx, ny)
            && quiet(data, scale, nx, ny)) {
          continue;
        }
        quads[y * (width - 1) + x] = true;
        quadCount++;
        final int a = y * width + x;
        remap[a] = remap[a + 1] = remap[a + width] = remap[a + width + 1] = 1;
      }
    }
    int vertices = 0;
    for (int i = 0; i < remap.length; i++) {
      remap[i] = remap[i] == 0 ? -1 : vertices++;
    }

    // pass 2: referenced vertices with analytic normals of the height field
    final float[] points = new float[vertices * 3];
    final float[] normals = new float[vertices * 3];
    final float[] texture = owners != null ? owners.texture() : new float[vertices * 2];
    for (int y = 0; y < height; y++) {
      checkCanceled(canceled);
      for (int x = 0; x < width; x++) {
        final int target = remap[y * width + x];
        if (target < 0) {
          continue;
        }
        final double value = data.intensity(Math.min(x, data.width() - 1),
            Math.min(y, data.height() - 1));
        points[target * 3] = xs[x];
        points[target * 3 + 1] = height(data, scale, x, y);
        points[target * 3 + 2] = zs[y];
        final int x0 = Math.max(0, x - 1);
        final int x1 = Math.min(width - 1, x + 1);
        final int y0 = Math.max(0, y - 1);
        final int y1 = Math.min(height - 1, y + 1);
        final double dx = xs[x1] - xs[x0];
        final double dz = zs[y1] - zs[y0];
        final double fx =
            dx == 0 ? 0 : (height(data, scale, x1, y) - height(data, scale, x0, y)) / dx;
        final double fz =
            dz == 0 ? 0 : (height(data, scale, x, y1) - height(data, scale, x, y0)) / dz;
        // surface y = f(x, z) with up = -y has the normal (fx, -1, fz)
        final double length = Math.sqrt(fx * fx + 1 + fz * fz);
        normals[target * 3] = (float) (fx / length);
        normals[target * 3 + 1] = (float) (-1 / length);
        normals[target * 3 + 2] = (float) (fz / length);
        if (mix != null) {
          final int cell = Math.min(y, data.height() - 1) * data.width() + Math.min(x,
              data.width() - 1);
          texture[target * 2] = mixCoordinate(mix[cell * 2]);
          texture[target * 2 + 1] = mixCoordinate(mix[cell * 2 + 1]);
        } else if (owners == null) {
          texture[target * 2] = textureU(scale.color(data, value));
          texture[target * 2 + 1] = 0.5f;
        }
      }
    }

    // pass 3: two triangles per quad, point and normal share one index
    final int[] faces = new int[quadCount * 18];
    int next = 0;
    for (int y = 0; y < height - 1; y++) {
      checkCanceled(canceled);
      for (int x = 0; x < width - 1; x++) {
        if (!quads[y * (width - 1) + x]) {
          continue;
        }
        final int a = remap[y * width + x];
        final int b = remap[y * width + x + 1];
        final int c = remap[(y + 1) * width + x];
        final int d = remap[(y + 1) * width + x + 1];
        if (owners == null) {
          next = triangle(faces, next, a, b, c, a, b, c);
          next = triangle(faces, next, b, d, c, b, d, c);
        } else {
          final int owner = quadOwner(data, owners, x, y);
          next = triangle(faces, next, a, b, c, owner, owner, owner);
          next = triangle(faces, next, b, d, c, owner, owner, owner);
        }
      }
    }
    return new Surface3DMesh(points, normals, texture, faces);
  }

  private static boolean quiet(@NotNull final Surface3DData data,
      @NotNull final Surface3DScale scale, final int x, final int y) {
    final float value = data.intensity(x, y);
    return value == 0 || scale.belowNoise(data, value);
  }

  /**
   * @return owner of the most intense corner, which is the visually dominant part of the quad
   */
  private static int quadOwner(@NotNull final Surface3DData data, @NotNull final Owners owners,
      final int x, final int y) {
    int best = -1;
    float maximum = -1;
    for (int dy = 0; dy <= 1; dy++) {
      for (int dx = 0; dx <= 1; dx++) {
        final int cx = Math.min(x + dx, data.width() - 1);
        final int cy = Math.min(y + dy, data.height() - 1);
        final float value = data.intensity(cx, cy);
        if (value > maximum) {
          maximum = value;
          best = cy * data.width() + cx;
        }
      }
    }
    return owners.at(best);
  }

  private static float height(@NotNull final Surface3DData data,
      @NotNull final Surface3DScale scale, final int x, final int y) {
    final float value = data.intensity(Math.min(x, data.width() - 1),
        Math.min(y, data.height() - 1));
    // values below the noise floor are drawn at the baseline where neighbors need them
    return scale.belowNoise(data, value) ? 0 : (float) (-scale.height(data, value) * HEIGHT);
  }

  private static float textureU(final double color) {
    return (float) ((0.5 + Math.clamp(color, 0, 1) * 1023) / 1024);
  }

  private static @NotNull Surface3DMesh pixels(@NotNull final Surface3DData data,
      @NotNull final Surface3DScale scale, @NotNull final BooleanSupplier canceled,
      @Nullable final Owners owners, final float @Nullable [] mix) {
    final Surface3DBounds bounds = scale.bounds();
    final int width = data.width();
    final int height = data.height();
    // pass 1: a wall is only visible where the neighbor is missing, separated, or lower
    final byte[] walls = new byte[width * height];
    int cells = 0;
    int wallVertices = 0;
    int triangles = 0;
    for (int y = 0; y < height; y++) {
      checkCanceled(canceled);
      for (int x = 0; x < width; x++) {
        if (!visible(data, scale, x, y)) {
          continue;
        }
        cells++;
        triangles += 2;
        final double value = data.intensity(x, y);
        if (value <= 0 || scale.height(data, value) <= 0) {
          continue;
        }
        int mask = 0;
        if (exposed(data, scale, x, y, x - 1, y, value)) {
          mask |= WALL_MINUS_X;
        }
        if (exposed(data, scale, x, y, x + 1, y, value)) {
          mask |= WALL_PLUS_X;
        }
        if (exposed(data, scale, x, y, x, y - 1, value)) {
          mask |= WALL_MINUS_Z;
        }
        if (exposed(data, scale, x, y, x, y + 1, value)) {
          mask |= WALL_PLUS_Z;
        }
        walls[y * width + x] = (byte) mask;
        if (mask != 0) {
          wallVertices += 4;
          triangles += 2 * Integer.bitCount(mask);
        }
      }
    }
    final float[] points = new float[(cells * 4 + wallVertices) * 3];
    final float[] texture = owners != null ? owners.texture() : new float[cells * 2];
    final int[] faces = new int[triangles * 9];
    int vertex = 0;
    int cell = 0;
    int next = 0;
    for (int y = 0; y < height; y++) {
      checkCanceled(canceled);
      for (int x = 0; x < width; x++) {
        if (!visible(data, scale, x, y)) {
          continue;
        }
        final double value = data.intensity(x, y);
        final float top = (float) (-scale.height(data, value) * HEIGHT);
        final float x0 = (float) localX(bounds, data.xValue(x) - data.pixelWidth() / 2);
        final float x1 = (float) localX(bounds, data.xValue(x) + data.pixelWidth() / 2);
        final float z0 = (float) localZ(bounds, data.yValue(y) - data.pixelHeight() / 2);
        final float z1 = (float) localZ(bounds, data.yValue(y) + data.pixelHeight() / 2);
        final int t;
        if (mix != null) {
          texture[cell * 2] = mixCoordinate(mix[(y * width + x) * 2]);
          texture[cell * 2 + 1] = mixCoordinate(mix[(y * width + x) * 2 + 1]);
          t = cell;
        } else if (owners == null) {
          texture[cell * 2] = textureU(scale.color(data, value));
          texture[cell * 2 + 1] = 0.5f;
          t = cell;
        } else {
          t = owners.at(y * width + x);
        }
        final int t0 = vertex;
        vertex = point(points, vertex, x0, top, z0);
        vertex = point(points, vertex, x1, top, z0);
        vertex = point(points, vertex, x0, top, z1);
        vertex = point(points, vertex, x1, top, z1);
        next = pixelTriangle(faces, next, t0, t0 + 1, t0 + 2, 0, t);
        next = pixelTriangle(faces, next, t0 + 1, t0 + 3, t0 + 2, 0, t);
        final int mask = walls[y * width + x];
        if (mask != 0) {
          final int b0 = vertex;
          vertex = point(points, vertex, x0, 0, z0);
          vertex = point(points, vertex, x1, 0, z0);
          vertex = point(points, vertex, x0, 0, z1);
          vertex = point(points, vertex, x1, 0, z1);
          if ((mask & WALL_MINUS_X) != 0) {
            next = pixelTriangle(faces, next, t0, b0, t0 + 2, 1, t);
            next = pixelTriangle(faces, next, t0 + 2, b0, b0 + 2, 1, t);
          }
          if ((mask & WALL_PLUS_X) != 0) {
            next = pixelTriangle(faces, next, t0 + 1, t0 + 3, b0 + 1, 2, t);
            next = pixelTriangle(faces, next, t0 + 3, b0 + 3, b0 + 1, 2, t);
          }
          if ((mask & WALL_MINUS_Z) != 0) {
            next = pixelTriangle(faces, next, t0, t0 + 1, b0, 3, t);
            next = pixelTriangle(faces, next, t0 + 1, b0 + 1, b0, 3, t);
          }
          if ((mask & WALL_PLUS_Z) != 0) {
            next = pixelTriangle(faces, next, t0 + 2, b0 + 2, t0 + 3, 4, t);
            next = pixelTriangle(faces, next, t0 + 3, b0 + 2, b0 + 3, 4, t);
          }
        }
        cell++;
      }
    }
    return new Surface3DMesh(points, PIXEL_NORMALS.clone(), texture, faces);
  }

  /**
   * @return true for measured pixels above the noise floor. Measured zeros are kept unless a
   * noise floor is set.
   */
  private static boolean visible(@NotNull final Surface3DData data,
      @NotNull final Surface3DScale scale, final int x, final int y) {
    return data.isPresent(x, y) && !scale.belowNoise(data, data.intensity(x, y));
  }

  private static boolean exposed(@NotNull final Surface3DData data,
      @NotNull final Surface3DScale scale, final int x, final int y, final int nx, final int ny,
      final double value) {
    if (nx < 0 || ny < 0 || nx >= data.width() || ny >= data.height() || !visible(data, scale, nx,
        ny)) {
      return true;
    }
    // non-adjacent coordinates leave a visible gap between the columns
    if (nx != x && Math.abs(data.xValue(nx) - data.xValue(x)) > data.pixelWidth() * 1.01) {
      return true;
    }
    if (ny != y && Math.abs(data.yValue(ny) - data.yValue(y)) > data.pixelHeight() * 1.01) {
      return true;
    }
    return data.intensity(nx, ny) < value;
  }

  private static int point(final float @NotNull [] points, final int vertex, final float x,
      final float y, final float z) {
    points[vertex * 3] = x;
    points[vertex * 3 + 1] = y;
    points[vertex * 3 + 2] = z;
    return vertex + 1;
  }

  /**
   * Height field triangle: point and normal indices are shared, texture indices are separate.
   */
  private static int triangle(final int @NotNull [] faces, final int next, final int a,
      final int b, final int c, final int ta, final int tb, final int tc) {
    faces[next] = a;
    faces[next + 1] = a;
    faces[next + 2] = ta;
    faces[next + 3] = b;
    faces[next + 4] = b;
    faces[next + 5] = tb;
    faces[next + 6] = c;
    faces[next + 7] = c;
    faces[next + 8] = tc;
    return next + 9;
  }

  private static int pixelTriangle(final int @NotNull [] faces, final int next, final int a,
      final int b, final int c, final int normal, final int texture) {
    faces[next] = a;
    faces[next + 1] = normal;
    faces[next + 2] = texture;
    faces[next + 3] = b;
    faces[next + 4] = normal;
    faces[next + 5] = texture;
    faces[next + 6] = c;
    faces[next + 7] = normal;
    faces[next + 8] = texture;
    return next + 9;
  }

  private static void checkCanceled(@NotNull final BooleanSupplier canceled) {
    if (canceled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
      throw new CancellationException();
    }
  }
}
