/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_chemstation;

import static io.github.mzmine.util.RawDataFileTypeDetector.CHEMSTATION_MS_FILE;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Streaming reader for the legacy Agilent ChemStation {@code DATA.MS} format.
 *
 * <p>The binary layout and intensity decoding follow the independently validated Atmos/Rainbow
 * implementation. ChemStation stores m/z as an unsigned big-endian word in twentieths of a mass
 * unit and intensity as a 14-bit mantissa plus a two-bit base-8 exponent.</p>
 */
public final class ChemStationMsParser implements AutoCloseable {

  private static final int FULL_FILE_HEADER = 0x01320000;
  private static final long DATA_START_OFFSET = 0x10A;
  private static final long PARTIAL_DATA_START = 0x2F2;
  private static final int MIN_SCAN_RECORD_BYTES = 28;

  private final RandomAccessFile input;
  private final long fileLength;
  private final int totalScans;
  private int readScans;

  public ChemStationMsParser(@NotNull File chemStationSource) throws IOException {
    final File dataFile = chemStationSource.isFile() ? chemStationSource
        : findDataMsFile(chemStationSource);
    if (dataFile == null) {
      throw new IOException("No ChemStation .MS file found in " + chemStationSource);
    }
    input = new RandomAccessFile(dataFile, "r");
    fileLength = input.length();
    try {
      totalScans = initialize();
    } catch (Throwable error) {
      input.close();
      throw error;
    }
  }

  private int initialize() throws IOException {
    if (fileLength < DATA_START_OFFSET + Short.BYTES) {
      throw new IOException("ChemStation DATA.MS header is truncated");
    }

    input.seek(0);
    final int header = input.readInt();
    if (header == FULL_FILE_HEADER) {
      final String type = readString(0x4, 1);
      final int count;
      if ("MSD Spectral File".equals(type)) {
        input.seek(0x116);
        final long unsignedCount = Integer.toUnsignedLong(input.readInt());
        if (unsignedCount > Integer.MAX_VALUE) {
          throw new IOException("Invalid ChemStation scan count: " + unsignedCount);
        }
        count = (int) unsignedCount;
      } else {
        input.seek(0x142);
        count = readUnsignedShortLittleEndian();
      }

      input.seek(DATA_START_OFFSET);
      final long dataStart = input.readUnsignedShort() * 2L - 2L;
      if (dataStart < 0 || dataStart >= fileLength) {
        throw new IOException("Invalid ChemStation data offset: " + dataStart);
      }
      input.seek(dataStart);
      return count;
    }

    // Partial LC-MS files omit the normal header pointer. Rainbow/Atmos use the stable 0x2F2
    // record offset after verifying that the pointer field is zero.
    input.seek(DATA_START_OFFSET);
    if (input.readUnsignedShort() != 0 || PARTIAL_DATA_START >= fileLength) {
      throw new IOException("Unsupported or corrupt ChemStation DATA.MS file");
    }
    input.seek(PARTIAL_DATA_START);
    return -1;
  }

  public int getTotalScans() {
    return totalScans;
  }

  public int getReadScans() {
    return readScans;
  }

  public double getFinishedPercentage() {
    if (totalScans > 0) {
      return Math.min(1d, (double) readScans / totalScans);
    }
    try {
      return fileLength == 0 ? 0d : Math.min(1d, (double) input.getFilePointer() / fileLength);
    } catch (IOException ignored) {
      return 0d;
    }
  }

  /** Reads the next scan, or returns {@code null} at the end of the scan records. */
  public @Nullable ChemStationScan readNextScan() throws IOException {
    if (totalScans >= 0 && readScans >= totalScans) {
      return null;
    }
    if (fileLength - input.getFilePointer() < MIN_SCAN_RECORD_BYTES) {
      return endOrTruncated();
    }

    try {
      input.skipBytes(2);
      final float retentionTime = (float) (Integer.toUnsignedLong(input.readInt()) / 60000d);
      input.skipBytes(6);
      final int pairCount = input.readUnsignedShort();
      input.skipBytes(4);

      final long bytesNeeded = pairCount * 4L + 10L;
      if (bytesNeeded > fileLength - input.getFilePointer()) {
        return endOrTruncated();
      }

      // Rounding to nominal mass matches Rainbow's default ChemStation precision. A TreeMap also
      // combines the occasional duplicate nominal masses and guarantees MZmine's sorted-m/z
      // invariant without materializing a dense zero-filled mass matrix.
      final TreeMap<Double, Double> spectrum = new TreeMap<>();
      for (int pair = 0; pair < pairCount; pair++) {
        final double mz = Math.rint(input.readUnsignedShort() / 20d);
        final int encodedIntensity = input.readUnsignedShort();
        final int exponent = encodedIntensity >>> 14;
        final int mantissa = encodedIntensity & 0x3FFF;
        final double intensity = mantissa * (double) (1 << (3 * exponent));
        spectrum.merge(mz, intensity, Double::sum);
      }
      input.skipBytes(10);
      readScans++;

      final double[] mzValues = new double[spectrum.size()];
      final double[] intensityValues = new double[spectrum.size()];
      int index = 0;
      for (Map.Entry<Double, Double> point : spectrum.entrySet()) {
        mzValues[index] = point.getKey();
        intensityValues[index] = point.getValue();
        index++;
      }
      return new ChemStationScan(retentionTime, mzValues, intensityValues);
    } catch (EOFException eof) {
      return endOrTruncated();
    }
  }

  private @Nullable ChemStationScan endOrTruncated() throws IOException {
    if (totalScans >= 0 && readScans < totalScans) {
      throw new EOFException(
          "ChemStation DATA.MS ended after %d of %d scans".formatted(readScans, totalScans));
    }
    return null;
  }

  private int readUnsignedShortLittleEndian() throws IOException {
    final int low = input.readUnsignedByte();
    final int high = input.readUnsignedByte();
    return low | high << 8;
  }

  private String readString(long offset, int gap) throws IOException {
    input.seek(offset);
    final int byteLength = input.readUnsignedByte() * gap;
    final byte[] raw = new byte[byteLength];
    input.readFully(raw);
    final StringBuilder value = new StringBuilder(byteLength / gap);
    for (int index = 0; index < raw.length; index += gap) {
      value.append((char) Byte.toUnsignedInt(raw[index]));
    }
    return value.toString().strip();
  }

  public static @Nullable File findDataMsFile(@NotNull File folder) {
    final File[] files = folder.listFiles();
    if (files == null) {
      return null;
    }
    for (File file : files) {
      if (file.isFile() && file.getName().equalsIgnoreCase(CHEMSTATION_MS_FILE)) {
        return file;
      }
    }
    for (File file : files) {
      if (io.github.mzmine.util.RawDataFileTypeDetector.isChemStationMsFile(file)) {
        return file;
      }
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    input.close();
  }

  public record ChemStationScan(float retentionTime, double[] mzValues,
                                double[] intensityValues) {

  }
}
