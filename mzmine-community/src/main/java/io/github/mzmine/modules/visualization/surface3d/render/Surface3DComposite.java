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
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DSeries;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One surface with the maximum of all overlays, colored by the overlay that dominates each cell.
 * Coincident surfaces of similar samples otherwise flicker between colors (depth fighting), and
 * transparency cannot resolve which overlay is higher.
 *
 * @param data   maximum per cell, relative to each overlay maximum if normalized
 * @param owners index into the source list per cell, -1 if unmeasured
 * @param mix    for blending: intensity fractions of the first two sources per cell (the third
 *               is the remainder), null if not requested
 */
public record Surface3DComposite(@NotNull Surface3DData data, @NotNull Surface3DMesh.Owners owners,
                                 float @Nullable [] mix) {

  /**
   * @param sources visible overlays in display order
   * @param scale   shared scale of the overlays; normalized scales compare relative intensities
   */
  public static @NotNull Surface3DComposite build(@NotNull final List<Surface3DSeries> sources,
      @NotNull final Surface3DScale scale, @NotNull final BooleanSupplier canceled) {
    return build(sources, scale, canceled, false);
  }

  /**
   * @param mix compute blend fractions of up to {@link Surface3DBlend#MAX_CHANNELS} sources
   */
  public static @NotNull Surface3DComposite build(@NotNull final List<Surface3DSeries> sources,
      @NotNull final Surface3DScale scale, @NotNull final BooleanSupplier canceled,
      final boolean mix) {
    if (sources.isEmpty()) {
      throw new IllegalArgumentException("No overlays");
    }
    final Surface3DData first = sources.getFirst().data();
    final boolean pixels = sources.stream().allMatch(s -> s.data().pixels());
    // union of measured coordinates, limited to the densest overlay so the budget holds
    final int width = sources.stream().mapToInt(s -> s.data().width()).max().orElseThrow();
    final int height = sources.stream().mapToInt(s -> s.data().height()).max().orElseThrow();
    final double[] x = Surface3DData.reduceCoordinates(
        union(sources.stream().map(s -> xs(s.data())).toList()), width);
    final double[] y = Surface3DData.reduceCoordinates(
        union(sources.stream().map(s -> ys(s.data())).toList()), height);
    final Surface3DData data = new Surface3DData(x, y, first.xLabel(), first.yLabel(), pixels);
    data.setAxisKinds(first.xKind(), first.yKind());
    if (pixels) {
      data.setPixelSize(sources.stream().mapToDouble(s -> s.data().pixelWidth()).min().orElse(1),
          sources.stream().mapToDouble(s -> s.data().pixelHeight()).min().orElse(1));
    }
    final int[] owners = new int[x.length * y.length];
    Arrays.fill(owners, -1);
    final int channels = Math.min(sources.size(), Surface3DBlend.MAX_CHANNELS);
    final float[] weights = mix ? new float[owners.length * Surface3DBlend.MAX_CHANNELS] : null;
    for (int i = 0; i < sources.size(); i++) {
      final Surface3DData source = sources.get(i).data();
      final double maximum = scale.normalized() ? source.maximum() : 1;
      for (int row = 0; row < source.height(); row++) {
        if (canceled.getAsBoolean()) {
          throw new CancellationException();
        }
        final int targetRow = data.binY(source.yValue(row));
        for (int column = 0; column < source.width(); column++) {
          if (!source.isPresent(column, row)) {
            continue;
          }
          final int targetColumn = data.binX(source.xValue(column));
          if (targetColumn < 0 || targetRow < 0) {
            continue;
          }
          final int cell = targetRow * data.width() + targetColumn;
          final double value =
              maximum > 0 ? source.intensity(column, row) / maximum : source.intensity(column, row);
          data.markPresent(targetColumn, targetRow);
          if (weights != null && i < channels) {
            final int w = cell * Surface3DBlend.MAX_CHANNELS + i;
            weights[w] = (float) Math.max(weights[w], value);
          }
          if (owners[cell] < 0 || value > data.intensity(targetColumn, targetRow)) {
            data.addMaximum(targetColumn, targetRow, value);
            owners[cell] = i;
          }
        }
      }
    }
    return new Surface3DComposite(data, new Surface3DMesh.Owners(owners, sources.size()),
        weights == null ? null : fractions(weights, channels));
  }

  /**
   * @return fractions of channel 1 and 2 per cell, cells without signal are split evenly
   */
  private static float @NotNull [] fractions(final float @NotNull [] weights,
      final int channels) {
    final int cells = weights.length / Surface3DBlend.MAX_CHANNELS;
    final float[] fractions = new float[cells * 2];
    for (int cell = 0; cell < cells; cell++) {
      double sum = 0;
      for (int c = 0; c < channels; c++) {
        sum += weights[cell * Surface3DBlend.MAX_CHANNELS + c];
      }
      for (int c = 0; c < 2; c++) {
        final double share = c >= channels ? 0 : sum > 0
            ? weights[cell * Surface3DBlend.MAX_CHANNELS + c] / sum : 1d / channels;
        fractions[cell * 2 + c] = (float) share;
      }
    }
    return fractions;
  }

  /**
   * @return a scale for the composite values, which are relative when the overlays are normalized
   */
  public @NotNull Surface3DScale scale(@NotNull final Surface3DScale overlays) {
    final Surface3DBounds b = overlays.bounds();
    final double maximum = overlays.normalized() ? data.maximum() : b.maximum();
    return new Surface3DScale(new Surface3DBounds(b.xMin(), b.xMax(), b.yMin(), b.yMax(), maximum),
        overlays.transform(), false, overlays.noiseFloor(), overlays.baseline());
  }

  private static double @NotNull [] xs(@NotNull final Surface3DData data) {
    final double[] values = new double[data.width()];
    for (int i = 0; i < values.length; i++) {
      values[i] = data.xValue(i);
    }
    return values;
  }

  private static double @NotNull [] ys(@NotNull final Surface3DData data) {
    final double[] values = new double[data.height()];
    for (int i = 0; i < values.length; i++) {
      values[i] = data.yValue(i);
    }
    return values;
  }

  private static double @NotNull [] union(@NotNull final List<double[]> axes) {
    return axes.stream().flatMapToDouble(Arrays::stream).distinct().sorted().toArray();
  }
}
