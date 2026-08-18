/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_hapsite;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reader for INFICON HAPSITE {@code .hps} scan spectra.
 *
 * <p>The container has several observed GPIR layouts. This reader supports the native HapsScan
 * section, full-scan GPIR records, compact GPIR records, and the older 1014-byte GPIR layout used
 * by the Atmos importer.</p>
 */
public final class HapsiteHpsParser {

  private static final byte[] FILE_MAGIC = {'S', 'P', 'A', 'H'};
  private static final byte[] GPIR_MARKER = "HapsGPIR".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] SCAN_MARKER = "HapsScan".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] INFO_MARKER = "HapsInfo".getBytes(StandardCharsets.US_ASCII);

  private static final int GPIR_RECORD_STRIDE = 1014;
  private static final int GPIR_RECORD_FLOAT_OFFSET = 2;
  private static final int GPIR_RECORD_FLOAT_COUNT = 253;
  private static final int GPIR_FULLSCAN_RECORD_STRIDE = 1040;
  private static final int GPIR_FULLSCAN_FLOAT_COUNT = 260;
  private static final int GPIR_COMPACT_RECORD_STRIDE = 910;
  private static final int GPIR_COMPACT_FLOAT_COUNT = 212;
  private static final int HAPSSCAN_HEADER_BYTES = 84;
  private static final double GPIR_SIGNAL_LIMIT = 10_000_000d;
  private static final double HAPSSCAN_SIGNAL_LIMIT = 1_000_000_000d;
  private static final int MIN_TIME_FIT_POINTS = 2;

  private static final Pattern REPORT_PEAK = Pattern.compile(
      "(?m)^\\s*(\\d+)\\s+(\\d+)m(\\d+)s\\s+(\\d+)\\s+([0-9.]+)\\s+([0-9.]+)\\s*$");

  private final byte[] bytes;
  private final Layout layout;
  private final double timeSlopeSeconds;
  private final double timeInterceptSeconds;
  private int nextRecord;

  public HapsiteHpsParser(@NotNull File file) throws IOException {
    if (!file.isFile()) {
      throw new IOException("HAPSITE source is not a file: " + file);
    }
    bytes = Files.readAllBytes(file.toPath());
    if (!hasMagic(bytes)) {
      throw new IOException("Not an INFICON HAPSITE .hps file: " + file.getName());
    }

    final int gpirOffset = indexOf(bytes, GPIR_MARKER, 0, bytes.length);
    final int infoOffset = indexOf(bytes, INFO_MARKER, Math.max(0, gpirOffset + 1), bytes.length);
    if (gpirOffset < 0 || infoOffset < 0 || gpirOffset + 256 >= infoOffset) {
      throw new IOException("HAPSITE file has no readable GPIR scan section: " + file.getName());
    }

    final long declaredLength = readLittleEndianUnsignedInt(bytes, gpirOffset + 176);
    final int dataOffset = gpirOffset + 256;
    final int availableLength = (int) Math.max(0,
        Math.min(declaredLength, (long) infoOffset - dataOffset));
    layout = determineLayout(gpirOffset, infoOffset, dataOffset, availableLength);

    final TimeFit reportFit = fitReportTimes(decodeReport(bytes));
    // fitReportTimes only produces a calibrated slope from two or more report peaks. With fewer it
    // returns the neutral 1 s/scan fit, which must not win over a layout's known calibration.
    if (reportFit.pointCount() < MIN_TIME_FIT_POINTS && layout.fallbackTimeFit() != null) {
      timeSlopeSeconds = layout.fallbackTimeFit().slopeSeconds();
      timeInterceptSeconds = layout.fallbackTimeFit().interceptSeconds();
    } else {
      timeSlopeSeconds = reportFit.slopeSeconds();
      timeInterceptSeconds = reportFit.interceptSeconds();
    }
  }

  public int getTotalScans() {
    return layout.recordCount();
  }

  public double getFinishedPercentage() {
    return layout.recordCount() == 0 ? 0d : (double) nextRecord / layout.recordCount();
  }

  public @Nullable HapsiteScan readNextScan() {
    if (nextRecord >= layout.recordCount()) {
      return null;
    }

    final int recordIndex = nextRecord++;
    final DecodedSpectrum spectrum = layout.adaptiveCompact()
        ? decodeAdaptiveCompact(recordIndex) : decodeFixed(recordIndex);
    final int reportScan = recordIndex + layout.reportScanOffset();
    final float retentionTime = (float) Math.max(0d,
        (timeSlopeSeconds * reportScan + timeInterceptSeconds) / 60d);
    return new HapsiteScan(recordIndex + 1, retentionTime, spectrum.mzValues(),
        spectrum.intensityValues());
  }

  private Layout determineLayout(int gpirOffset, int infoOffset, int dataOffset,
      int availableLength) throws IOException {
    final int hapsiteScanOffset = indexOf(bytes, SCAN_MARKER, gpirOffset, infoOffset);
    final boolean hasHapsiteScan = hapsiteScanOffset >= 0;
    if (hasHapsiteScan) {
      final int scanDataOffset = hapsiteScanOffset + HAPSSCAN_HEADER_BYTES;
      final int scanBytes = infoOffset - scanDataOffset;
      if (scanDataOffset > 4 && scanBytes >= GPIR_FULLSCAN_RECORD_STRIDE * 10
          && scanBytes % GPIR_FULLSCAN_RECORD_STRIDE == 0) {
        return new Layout(scanDataOffset - 4, GPIR_FULLSCAN_RECORD_STRIDE, 0, 45d,
            GPIR_FULLSCAN_FLOAT_COUNT, scanBytes / GPIR_FULLSCAN_RECORD_STRIDE,
            HAPSSCAN_SIGNAL_LIMIT, 1, false, null);
      }
    }

    final int defaultRecordCount = availableLength / GPIR_RECORD_STRIDE;
    if (!hasHapsiteScan && availableLength >= GPIR_COMPACT_RECORD_STRIDE * 10) {
      final int compactCount = availableLength / GPIR_COMPACT_RECORD_STRIDE;
      if (availableLength % GPIR_COMPACT_RECORD_STRIDE == 0
          && compactCount > defaultRecordCount) {
        return new Layout(dataOffset, GPIR_COMPACT_RECORD_STRIDE, 0, 0d, 0, compactCount,
            GPIR_SIGNAL_LIMIT, 1, true, new TimeFit(5d / 6d, 0d, 0));
      }
      if (availableLength % GPIR_FULLSCAN_RECORD_STRIDE == 0) {
        return new Layout(dataOffset, GPIR_FULLSCAN_RECORD_STRIDE, 0, 42d,
            GPIR_FULLSCAN_FLOAT_COUNT, availableLength / GPIR_FULLSCAN_RECORD_STRIDE,
            GPIR_SIGNAL_LIMIT, 1, false, new TimeFit(31d / 30d, 148.3666666667d, 0));
      }
    }

    if (defaultRecordCount >= 10) {
      // This family labels the first raw records two counts ahead of the embedded report.
      return new Layout(dataOffset, GPIR_RECORD_STRIDE, GPIR_RECORD_FLOAT_OFFSET, 23d,
          GPIR_RECORD_FLOAT_COUNT, defaultRecordCount, GPIR_SIGNAL_LIMIT, -1, false, null);
    }
    throw new IOException("Unsupported HAPSITE GPIR record layout");
  }

  private DecodedSpectrum decodeFixed(int recordIndex) {
    return decodeWindow(layout.dataOffset() + recordIndex * layout.recordStride()
        + layout.floatOffset(), layout.mzStart(), layout.floatCount(), layout.signalLimit());
  }

  private DecodedSpectrum decodeAdaptiveCompact(int recordIndex) {
    final int recordOffset = layout.dataOffset() + recordIndex * layout.recordStride();
    final Window[] windows = {new Window(64, 58d, GPIR_COMPACT_FLOAT_COUNT),
        new Window(56, 56d, GPIR_COMPACT_FLOAT_COUNT),
        new Window(2, 25d, (GPIR_COMPACT_RECORD_STRIDE - 2) / Float.BYTES)};
    DecodedSpectrum best = new DecodedSpectrum(new double[0], new double[0], -1d);
    for (Window window : windows) {
      if (recordOffset + window.floatOffset() + window.floatCount() * Float.BYTES > bytes.length) {
        continue;
      }
      final DecodedSpectrum candidate = decodeWindow(recordOffset + window.floatOffset(),
          window.mzStart(), window.floatCount(), GPIR_SIGNAL_LIMIT);
      if (candidate.totalIntensity() > best.totalIntensity()) {
        best = candidate;
      }
    }
    return best;
  }

  private DecodedSpectrum decodeWindow(int offset, double mzStart, int count, double signalLimit) {
    final double[] temporaryMz = new double[count];
    final double[] temporaryIntensity = new double[count];
    int retained = 0;
    double total = 0d;
    for (int index = 0; index < count; index++) {
      final int valueOffset = offset + index * Float.BYTES;
      if (valueOffset < 0 || valueOffset + Float.BYTES > bytes.length) {
        break;
      }
      final float value = readLittleEndianFloat(bytes, valueOffset);
      if (!Float.isFinite(value) || value <= 0f || value >= signalLimit) {
        continue;
      }
      temporaryMz[retained] = mzStart + index;
      temporaryIntensity[retained] = value;
      total += value;
      retained++;
    }

    final double[] mzValues = new double[retained];
    final double[] intensityValues = new double[retained];
    System.arraycopy(temporaryMz, 0, mzValues, 0, retained);
    System.arraycopy(temporaryIntensity, 0, intensityValues, 0, retained);
    return new DecodedSpectrum(mzValues, intensityValues, total);
  }

  private static TimeFit fitReportTimes(String report) {
    final Matcher matcher = REPORT_PEAK.matcher(report);
    final List<double[]> points = new ArrayList<>();
    while (matcher.find()) {
      final double scan = Double.parseDouble(matcher.group(1));
      final double seconds = Integer.parseInt(matcher.group(2)) * 60d
          + Integer.parseInt(matcher.group(3));
      if (scan > 0) {
        points.add(new double[]{scan, seconds});
      }
    }
    // Every uncalibrated return below reports zero usable points so the caller consistently falls
    // back to the layout calibration instead of silently accepting the neutral 1 s/scan fit.
    if (points.size() < MIN_TIME_FIT_POINTS) {
      return new TimeFit(1d, 0d, 0);
    }

    final double count = points.size();
    double sumScan = 0d;
    double sumTime = 0d;
    double sumScanSquared = 0d;
    double sumScanTime = 0d;
    for (double[] point : points) {
      sumScan += point[0];
      sumTime += point[1];
      sumScanSquared += point[0] * point[0];
      sumScanTime += point[0] * point[1];
    }
    final double denominator = count * sumScanSquared - sumScan * sumScan;
    if (Math.abs(denominator) < 1e-9) {
      return new TimeFit(1d, 0d, 0);
    }
    final double slope = (count * sumScanTime - sumScan * sumTime) / denominator;
    final double intercept = (sumTime - slope * sumScan) / count;
    return Double.isFinite(slope) && slope > 0d && Double.isFinite(intercept)
        ? new TimeFit(slope, intercept, points.size()) : new TimeFit(1d, 0d, 0);
  }

  private static String decodeReport(byte[] input) {
    final String text = new String(input, StandardCharsets.ISO_8859_1).replace('\0', '\n');
    final String marker = "Unknown Identification Report";
    final int firstReport = text.indexOf(marker);
    if (firstReport < 0) {
      return text;
    }
    final int detailed = text.indexOf("Number of Identifications:", firstReport);
    if (detailed >= 0) {
      final int start = text.lastIndexOf(marker, detailed);
      return text.substring(start >= 0 ? start : detailed);
    }
    return text.substring(text.lastIndexOf(marker));
  }

  private static boolean hasMagic(byte[] input) {
    if (input.length < 8) {
      return false;
    }
    for (int index = 0; index < FILE_MAGIC.length; index++) {
      if (input[index + 4] != FILE_MAGIC[index]) {
        return false;
      }
    }
    return true;
  }

  private static int indexOf(byte[] input, byte[] pattern, int from, int to) {
    final int end = Math.min(input.length, to) - pattern.length;
    for (int offset = Math.max(0, from); offset <= end; offset++) {
      int index = 0;
      while (index < pattern.length && input[offset + index] == pattern[index]) {
        index++;
      }
      if (index == pattern.length) {
        return offset;
      }
    }
    return -1;
  }

  private static long readLittleEndianUnsignedInt(byte[] input, int offset) throws IOException {
    if (offset < 0 || offset + Integer.BYTES > input.length) {
      throw new IOException("Truncated HAPSITE GPIR header");
    }
    return Integer.toUnsignedLong(ByteBuffer.wrap(input, offset, Integer.BYTES)
        .order(ByteOrder.LITTLE_ENDIAN).getInt());
  }

  private static float readLittleEndianFloat(byte[] input, int offset) {
    return ByteBuffer.wrap(input, offset, Float.BYTES).order(ByteOrder.LITTLE_ENDIAN).getFloat();
  }

  public record HapsiteScan(int scanNumber, float retentionTime, double[] mzValues,
                           double[] intensityValues) {

  }

  private record Layout(int dataOffset, int recordStride, int floatOffset, double mzStart,
                        int floatCount, int recordCount, double signalLimit, int reportScanOffset,
                        boolean adaptiveCompact, @Nullable TimeFit fallbackTimeFit) {

  }

  private record TimeFit(double slopeSeconds, double interceptSeconds, int pointCount) {

  }

  private record Window(int floatOffset, double mzStart, int floatCount) {

  }

  private record DecodedSpectrum(double[] mzValues, double[] intensityValues,
                                 double totalIntensity) {

  }
}
