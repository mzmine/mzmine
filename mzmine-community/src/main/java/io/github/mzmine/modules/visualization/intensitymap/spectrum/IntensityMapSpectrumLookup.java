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

package io.github.mzmine.modules.visualization.intensitymap.spectrum;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.intensitymap.IntensityMapDimensions;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapSliceMode;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapSampler;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the spectrum behind a position of the 3D view: the scan closest in retention time for
 * LC-MS or the pixel for imaging. Lookups are cached per file, so they are cheap enough for
 * interactive use. Mobility frames show a base peak chromatogram to pick the frame instead.
 */
public final class IntensityMapSpectrumLookup {

  private final ParameterSet parameters;
  private final IntensityMapDimensions mode;
  private final Map<RawDataFile, Lookup> lookups = new HashMap<>();

  public IntensityMapSpectrumLookup(@NotNull final ParameterSet parameters,
      @NotNull final IntensityMapDimensions mode) {
    if (mode == IntensityMapDimensions.MOBILITY_FRAME) {
      throw new IllegalArgumentException("Mobility frames show a chromatogram, not spectra");
    }
    this.parameters = parameters;
    this.mode = mode;
  }

  public @NotNull IntensityMapSliceMode sliceMode() {
    return switch (mode) {
      case AUTOMATIC, LC_MS -> IntensityMapSliceMode.X;
      case IMAGING -> IntensityMapSliceMode.POINT;
      case MOBILITY_FRAME -> throw unsupported();
    };
  }

  /**
   * @param x displayed x coordinate
   * @param y displayed y coordinate
   */
  public @Nullable Scan scanAt(@NotNull final RawDataFile file, final double x, final double y) {
    try {
      return lookups.computeIfAbsent(file, this::createLookup).find(x, y);
    } catch (final IllegalArgumentException ex) {
      // no matching scans or frame in this file
      return null;
    }
  }

  /**
   * @return a short description of the position a spectrum represents
   */
  public @NotNull String describe(@NotNull final Scan scan) {
    final NumberFormats formats = ConfigService.getGuiFormats();
    return switch (mode) {
      case AUTOMATIC, LC_MS ->
          "RT " + formats.rt(scan.getRetentionTime()) + " min · scan #" + scan.getScanNumber();
      case MOBILITY_FRAME -> throw unsupported();
      case IMAGING -> scan instanceof ImagingScan pixel && pixel.getCoordinates() != null
          ? "Pixel " + pixel.getCoordinates().getX() + ", " + pixel.getCoordinates().getY()
          : "Scan #" + scan.getScanNumber();
    };
  }

  /**
   * @return the m/z of the most intense point within the window, or the center if the window is
   * empty. Snaps clicks on profile spectra to the peak apex.
   */
  public static double apex(@NotNull final Scan scan, @NotNull final Range<Double> window,
      final double center) {
    final DataPoint apex = ScanUtils.findBasePeak(scan, window);
    return apex == null ? center : apex.getMZ();
  }

  private @NotNull Lookup createLookup(@NotNull final RawDataFile file) {
    final Scan[] scans = IntensityMapSampler.scanSelection(parameters, mode).getMatchingScans(file);
    return switch (mode) {
      case AUTOMATIC, LC_MS -> {
        // the same scans as the 3D view
        final Scan[] sorted = IntensityMapSampler.lowestMsLevel(scans).stream()
            .filter(scan -> Float.isFinite(scan.getRetentionTime()))
            .sorted(Comparator.comparingDouble(Scan::getRetentionTime)).toArray(Scan[]::new);
        final double[] times = Arrays.stream(sorted).mapToDouble(Scan::getRetentionTime).toArray();
        yield (x, _) -> nearest(sorted, times, x);
      }
      case MOBILITY_FRAME -> throw unsupported();
      case IMAGING -> {
        if (!(file instanceof ImagingRawDataFile imaging)) {
          yield (_, _) -> null;
        }
        final double[] step = IntensityMapSampler.imagingStep(imaging);
        final Map<Long, Scan> pixels = new HashMap<>();
        for (final Scan scan : scans) {
          if (scan instanceof ImagingScan pixel && pixel.getCoordinates() != null) {
            pixels.putIfAbsent(key(pixel.getCoordinates().getX(), pixel.getCoordinates().getY()),
                pixel);
          }
        }
        yield (x, y) -> pixels.get(
            key((int) Math.round(x / step[0]), (int) Math.round(y / step[1])));
      }
    };
  }

  private static @NotNull IllegalStateException unsupported() {
    return new IllegalStateException("Mobility frames have no spectrum source");
  }

  private static long key(final int x, final int y) {
    return ((long) x << 32) | (y & 0xffffffffL);
  }

  private static @Nullable Scan nearest(final Scan @NotNull [] scans,
      final double @NotNull [] values, final double value) {
    final int index = BinarySearch.binarySearch(values, value, DefaultTo.CLOSEST_VALUE);
    return index < 0 ? null : scans[index];
  }

  @FunctionalInterface
  private interface Lookup {

    @Nullable Scan find(double x, double y);
  }
}
