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

package io.github.mzmine.modules.visualization.intensitymap.sampling;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.intensitymap.IntensityMapDimensions;
import io.github.mzmine.modules.visualization.intensitymap.IntensityMapParameters;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapAxisKind;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapDetail;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Samples raw spectra at the detail needed for the current view, preserving bin maxima. All m/z
 * overlays of one file are extracted in a single pass over its spectra.
 */
public final class IntensityMapSampler {

  public static final String MZ_LABEL = "m/z";
  // assumption: centroid m/z scatters by a few ppm between scans. Finer bins only produce
  // jagged ridges instead of revealing more signal.
  private static final double MIN_CENTROID_BIN_PPM = 5;
  private static final int BIN_WIDTH_PROBES = 24;

  private IntensityMapSampler() {
  }

  public static @NotNull IntensityMapGrid sample(@NotNull final RawDataFile file,
      @NotNull final ParameterSet parameters, @NotNull final Progress progress) {
    return sample(file, parameters, IntensityMapDetail.DEFAULT, progress);
  }

  public static @NotNull IntensityMapGrid sample(@NotNull final RawDataFile file,
      @NotNull final ParameterSet parameters, @NotNull final IntensityMapDetail detail,
      @NotNull final Progress progress) {
    return sample(file, parameters, defaultMzRange(file, parameters), detail, progress);
  }

  /**
   * Uses an overlay's extraction range without modifying the shared scan/frame settings.
   */
  public static @NotNull IntensityMapGrid sample(@NotNull final RawDataFile file,
      @NotNull final ParameterSet parameters, @NotNull final Range<Double> mzRange,
      @NotNull final IntensityMapDetail detail, @NotNull final Progress progress) {
    final IntensityMapGrid data = sample(file, parameters, List.of(mzRange),
        IntensityMapRegion.FULL, detail, progress)[0];
    if (data == null) {
      throw new IllegalArgumentException("No data in the selected m/z range");
    }
    return data;
  }

  /**
   * @param mzRanges one overlay per range, read in one pass over the spectra
   * @param region   window in displayed coordinates
   * @return one entry per range, null if the range lies outside the region
   */
  public static @Nullable IntensityMapGrid @NotNull [] sample(@NotNull final RawDataFile file,
      @NotNull final ParameterSet parameters, @NotNull final List<Range<Double>> mzRanges,
      @NotNull final IntensityMapRegion region, @NotNull final IntensityMapDetail detail,
      @NotNull final Progress progress) {
    return sample(file, parameters, mzRanges, region, ImageNormalization.NO_NORMALIZATION,
        new IntensityMapFrameCache(), detail, progress);
  }

  /**
   * @param normalization the imaging intensity normalization, applied per scan or pixel. The
   *                      reference is the complete scan selection, so regions keep their values.
   * @param frames        the mobility frame to show
   */
  public static @Nullable IntensityMapGrid @NotNull [] sample(@NotNull final RawDataFile file,
      @NotNull final ParameterSet parameters, @NotNull final List<Range<Double>> mzRanges,
      @NotNull final IntensityMapRegion region, @NotNull final ImageNormalization normalization,
      @NotNull final IntensityMapFrameCache frames, @NotNull final IntensityMapDetail detail,
      @NotNull final Progress progress) {
    final IntensityMapDimensions mode = resolveMode(file,
        parameters.getValue(IntensityMapParameters.mode));
    final ScanSelection selection = scanSelection(parameters, mode);
    final Scan[] scans = selection.getMatchingScans(file);
    if (scans.length == 0) {
      throw new IllegalArgumentException("No scans match the scan selection " + selection);
    }
    return switch (mode) {
      case AUTOMATIC -> throw new IllegalStateException("Automatic mode must be resolved");
      case LC_MS ->
          spectra(lowestMsLevel(scans), Scan::getRetentionTime, axisLabel("Retention time", "min"),
              IntensityMapAxisKind.RETENTION_TIME, false, mzRanges, region, normalization, detail,
              progress);
      case MOBILITY_FRAME -> {
        if (!(file instanceof IMSRawDataFile)) {
          throw new IllegalArgumentException("The selected file has no ion mobility frames");
        }
        final Frame frame = frames.frame(scans);
        final String unit =
            frame.getMobilityType() == null ? "" : frame.getMobilityType().getUnit();
        // decision: m/z horizontal and mobility in depth, like the usual IMS heatmaps
        yield spectra(frame.getSortedMobilityScans(), scan -> ((MobilityScan) scan).getMobility(),
            axisLabel("Mobility", unit), IntensityMapAxisKind.MOBILITY, true, mzRanges, region,
            normalization, detail, progress);
      }
      case IMAGING -> {
        if (!(file instanceof ImagingRawDataFile imaging)) {
          throw new IllegalArgumentException("The selected file is not imaging data");
        }
        yield image(imaging, scans, mzRanges, region, normalization, detail, progress);
      }
    };
  }

  /**
   * @return the scan selection of the parameters. For imaging the retention time filter is removed:
   * pixels have no meaningful retention time (imzML stores 0), so a range left from LC-MS data
   * would exclude every pixel.
   */
  public static @NotNull ScanSelection scanSelection(@NotNull final ParameterSet parameters,
      @NotNull final IntensityMapDimensions mode) {
    final ScanSelection selection = parameters.getValue(IntensityMapParameters.scanSelection);
    return mode == IntensityMapDimensions.IMAGING && selection.getScanRTRange() != null
        ? selection.cloneWithNewRtRange(null) : selection;
  }

  /**
   * @return the dimensions to show. Imaging files always show images unless a mobility frame is
   * requested: an LC-MS view of pixels, e.g. from settings stored for LC-MS data, matches no scans
   * because pixels have no meaningful retention time.
   */
  public static @NotNull IntensityMapDimensions resolveMode(@NotNull final RawDataFile file,
      @NotNull final IntensityMapDimensions requested) {
    if (file instanceof ImagingRawDataFile) {
      return requested == IntensityMapDimensions.MOBILITY_FRAME && file instanceof IMSRawDataFile
          ? IntensityMapDimensions.MOBILITY_FRAME : IntensityMapDimensions.IMAGING;
    }
    return
        requested == IntensityMapDimensions.AUTOMATIC || requested == IntensityMapDimensions.IMAGING
            ? IntensityMapDimensions.LC_MS : requested;
  }

  /**
   * @return true if m/z is one of the displayed coordinate axes
   */
  public static boolean hasMzAxis(@NotNull final IntensityMapDimensions mode) {
    return switch (mode) {
      case AUTOMATIC, LC_MS, MOBILITY_FRAME -> true;
      case IMAGING -> false;
    };
  }

  /**
   * @return the label with its unit in the style of the preferences, e.g. "Retention time / min"
   */
  public static @NotNull String axisLabel(@NotNull final String label,
      @Nullable final String unit) {
    return ConfigService.getConfiguration().getUnitFormat().format(label, unit);
  }

  /**
   * @return the scans of the lowest MS level in the selection, usually MS1
   */
  public static @NotNull List<Scan> lowestMsLevel(final Scan @NotNull [] scans) {
    // decision (user request): scans of another MS level are not zeros between the scans of a
    // trace, e.g. MS2 scans of DDA data between MS1 scans. As columns they would break every
    // trace along retention time.
    final int level = Arrays.stream(scans).mapToInt(Scan::getMSLevel).min().orElse(1);
    return Arrays.stream(scans).filter(scan -> scan.getMSLevel() == level).toList();
  }

  public static @NotNull Range<Double> defaultMzRange(@NotNull final RawDataFile file,
      @NotNull final ParameterSet parameters) {
    final Range<Double> selected = parameters.getValue(IntensityMapParameters.mzRange);
    return selected == null ? file.getDataMZRange() : selected;
  }

  /**
   * @param transposed show m/z on x and the scan coordinate on y
   */
  private static @Nullable IntensityMapGrid @NotNull [] spectra(
      @NotNull final List<? extends Scan> allScans,
      @NotNull final ToDoubleFunction<Scan> coordinate, @NotNull final String coordinateLabel,
      @NotNull final IntensityMapAxisKind coordinateKind, final boolean transposed,
      @NotNull final List<Range<Double>> mzRanges, @NotNull final IntensityMapRegion region,
      @NotNull final ImageNormalization normalization, @NotNull final IntensityMapDetail detail,
      @NotNull final Progress progress) {
    final ToDoubleFunction<Scan> factors = normalization.scanFactors(allScans);
    final Range<Double> coordinateWindow = transposed ? region.y() : region.x();
    final Range<Double> mzWindow = transposed ? region.x() : region.y();
    final List<Scan> scans = new ArrayList<>(allScans.size());
    for (final Scan scan : allScans) {
      final double value = coordinate.applyAsDouble(scan);
      if (Double.isFinite(value) && IntensityMapRegion.contains(coordinateWindow, value)) {
        scans.add(scan);
      }
    }
    final IntensityMapGrid[] result = new IntensityMapGrid[mzRanges.size()];
    if (scans.isEmpty()) {
      if (region.isFull()) {
        throw new IllegalArgumentException("No spectra with valid coordinates");
      }
      return result;
    }
    final double[] nativeX = scans.stream().mapToDouble(coordinate).distinct().sorted().toArray();
    final double[] lower = new double[mzRanges.size()];
    final double[] upper = new double[mzRanges.size()];
    final SpectrumBuffers buffers = new SpectrumBuffers();
    for (int i = 0; i < mzRanges.size(); i++) {
      final Range<Double> visible = IntensityMapRegion.intersect(mzRanges.get(i), mzWindow);
      if (visible == null) {
        continue;
      }
      lower[i] = visible.lowerEndpoint();
      upper[i] = visible.upperEndpoint();
      final double width = upper[i] - lower[i];
      final int mzBins = width <= 0 ? 1 : (int) Math.min(Integer.MAX_VALUE - 1,
          Math.ceil(width / minimumMzBin(scans, lower[i], upper[i], buffers)) + 1);
      final var size = detail.grid(nativeX.length, mzBins, false);
      result[i] = new IntensityMapGrid(IntensityMapGrid.reduceCoordinates(nativeX, size.x()),
          IntensityMapGrid.coordinates(size.y(), visible.lowerEndpoint(), visible.upperEndpoint()),
          coordinateLabel, MZ_LABEL, false);
      result[i].setAxisKinds(coordinateKind, IntensityMapAxisKind.MZ);
    }
    for (int s = 0; s < scans.size(); s++) {
      checkCanceled(progress);
      final Scan scan = scans.get(s);
      final double position = coordinate.applyAsDouble(scan);
      final double factor = factors.applyAsDouble(scan);
      boolean read = false;
      for (int i = 0; i < result.length; i++) {
        final IntensityMapGrid data = result[i];
        if (data == null) {
          continue;
        }
        final int x = data.binX(position);
        if (x < 0) {
          continue;
        }
        data.markColumn(x);
        if (!read) {
          buffers.read(scan);
          read = true;
        }
        final double max = upper[i];
        // spectra are sorted by m/z, so only the extraction window is visited
        for (int p = lowerBound(buffers.mz, buffers.count, lower[i]);
            p < buffers.count && buffers.mz[p] <= max; p++) {
          data.addMaximum(x, data.binY(buffers.mz[p]), buffers.intensity[p] * factor);
        }
      }
      progress.advance(s + 1, scans.size());
    }
    if (transposed) {
      for (int i = 0; i < result.length; i++) {
        if (result[i] != null) {
          result[i] = result[i].transpose();
        }
      }
    }
    return result;
  }

  /**
   * Profile spectra need bins wider than the point spacing, otherwise empty bins between profile
   * points produce comb artifacts in narrow m/z windows.
   */
  private static double minimumMzBin(@NotNull final List<Scan> scans, final double lower,
      final double upper, @NotNull final SpectrumBuffers buffers) {
    double minimum = Math.max(upper, 1) * MIN_CENTROID_BIN_PPM * 1e-6;
    final int probes = Math.min(scans.size(), BIN_WIDTH_PROBES);
    final List<Double> spacings = new ArrayList<>();
    for (int k = 0; k < probes; k++) {
      final Scan scan = scans.get(probes == 1 ? 0 : k * (scans.size() - 1) / (probes - 1));
      if (scan.getSpectrumType() != MassSpectrumType.PROFILE) {
        continue;
      }
      buffers.read(scan);
      final int start = lowerBound(buffers.mz, buffers.count, lower);
      final List<Double> local = new ArrayList<>();
      for (int p = start + 1; p < buffers.count && buffers.mz[p] <= upper; p++) {
        local.add(buffers.mz[p] - buffers.mz[p - 1]);
      }
      if (local.size() >= 2) {
        local.sort(Double::compare);
        spacings.add(local.get(local.size() / 2));
      }
    }
    if (!spacings.isEmpty()) {
      spacings.sort(Double::compare);
      // 1.5 x spacing guarantees at least one profile point per bin
      minimum = Math.max(minimum, 1.5 * spacings.get(spacings.size() / 2));
    }
    return minimum;
  }

  private static @Nullable IntensityMapGrid @NotNull [] image(
      @NotNull final ImagingRawDataFile file, final Scan @NotNull [] scans,
      @NotNull final List<Range<Double>> mzRanges, @NotNull final IntensityMapRegion region,
      @NotNull final ImageNormalization normalization, @NotNull final IntensityMapDetail detail,
      @NotNull final Progress progress) {
    final ToDoubleFunction<Scan> factors = normalization.scanFactors(
        Arrays.stream(scans).filter(scan -> scan instanceof ImagingScan).toList());
    final boolean physical = hasPhysicalPixelSize(file);
    final double[] step = imagingStep(file);
    final double stepX = step[0];
    final double stepY = step[1];
    // Keep imported coordinates verbatim: never guess zero/one based origins from a sparse image.
    final List<ImagingScan> pixels = Arrays.stream(scans)
        .filter(scan -> scan instanceof ImagingScan).map(scan -> (ImagingScan) scan)
        .filter(scan -> scan.getCoordinates() != null).filter(
            scan -> IntensityMapRegion.contains(region.x(), scan.getCoordinates().getX() * stepX)
                && IntensityMapRegion.contains(region.y(), scan.getCoordinates().getY() * stepY))
        .toList();
    final IntensityMapGrid[] result = new IntensityMapGrid[mzRanges.size()];
    if (pixels.isEmpty()) {
      if (region.isFull()) {
        throw new IllegalArgumentException("No scans with imaging coordinates");
      }
      return result;
    }
    final double[] nativeX = pixels.stream()
        .mapToDouble(scan -> scan.getCoordinates().getX() * stepX).distinct().sorted().toArray();
    final double[] nativeY = pixels.stream()
        .mapToDouble(scan -> scan.getCoordinates().getY() * stepY).distinct().sorted().toArray();
    final var size = detail.grid(nativeX.length, nativeY.length, true);
    // merged pixels are whole blocks on the acquisition grid, so they tile without gaps
    final int factorX = IntensityMapGrid.blockFactor(nativeX, size.x(), stepX);
    final int factorY = IntensityMapGrid.blockFactor(nativeY, size.y(), stepY);
    final double[] x = IntensityMapGrid.blockCoordinates(nativeX, factorX, stepX);
    final double[] y = IntensityMapGrid.blockCoordinates(nativeY, factorY, stepY);
    for (int i = 0; i < result.length; i++) {
      final String unit = physical ? "µm" : "pixel";
      result[i] = new IntensityMapGrid(x, y, axisLabel("X", unit), axisLabel("Y", unit), true);
      result[i].setPixelSize(stepX * factorX, stepY * factorY);
    }
    final SpectrumBuffers buffers = new SpectrumBuffers();
    for (int s = 0; s < pixels.size(); s++) {
      checkCanceled(progress);
      final ImagingScan scan = pixels.get(s);
      buffers.read(scan);
      final int column = result[0].binX(scan.getCoordinates().getX() * stepX);
      final int row = result[0].binY(scan.getCoordinates().getY() * stepY);
      for (int i = 0; i < result.length; i++) {
        final double max = mzRanges.get(i).upperEndpoint();
        double intensity = 0;
        for (int p = lowerBound(buffers.mz, buffers.count, mzRanges.get(i).lowerEndpoint());
            p < buffers.count && buffers.mz[p] <= max; p++) {
          if (Double.isFinite(buffers.intensity[p]) && buffers.intensity[p] > 0) {
            intensity += buffers.intensity[p];
          }
        }
        // maximum across spatial bins keeps intensities independent of the level of detail
        result[i].addMaximum(column, row, intensity * factors.applyAsDouble(scan));
      }
      progress.advance(s + 1, pixels.size());
    }
    return result;
  }

  static boolean hasPhysicalPixelSize(@NotNull final ImagingRawDataFile file) {
    final var imaging = file.getImagingParam();
    return imaging != null && imaging.getLateralWidth() > 0 && imaging.getLateralHeight() > 0
        && imaging.getMaxNumberOfPixelX() > 0 && imaging.getMaxNumberOfPixelY() > 0;
  }

  /**
   * @return displayed x and y distance between neighboring pixels, µm or 1 without metadata
   */
  public static double @NotNull [] imagingStep(@NotNull final ImagingRawDataFile file) {
    if (!hasPhysicalPixelSize(file)) {
      return new double[]{1, 1};
    }
    final var imaging = file.getImagingParam();
    return new double[]{imaging.getLateralWidth() / imaging.getMaxNumberOfPixelX(),
        imaging.getLateralHeight() / imaging.getMaxNumberOfPixelY()};
  }

  /**
   * @return first index with a value >= target
   */
  static int lowerBound(final double @NotNull [] values, final int count, final double target) {
    final int index = BinarySearch.binarySearch(values, target, DefaultTo.GREATER_EQUALS, 0, count);
    // count ends the scan loops when no value is large enough
    return index < 0 ? count : index;
  }

  private static final class SpectrumBuffers {

    private double[] mzBuffer = new double[0];
    private double[] intensityBuffer = new double[0];
    private double[] mz = new double[0];
    private double[] intensity = new double[0];
    private int count;

    private void read(@NotNull final MassSpectrum spectrum) {
      count = spectrum.getNumberOfDataPoints();
      if (mzBuffer.length < count) {
        mzBuffer = new double[count];
        intensityBuffer = new double[count];
      }
      // Some implementations return their own arrays. Never pass those to a subsequent spectrum.
      mz = spectrum.getMzValues(mzBuffer);
      intensity = spectrum.getIntensityValues(intensityBuffer);
    }
  }

  private static void checkCanceled(@NotNull final Progress progress) {
    if (progress.canceled()) {
      throw new CancellationException();
    }
  }

  public interface Progress {

    boolean canceled();

    void advance(int completed, int total);
  }
}
