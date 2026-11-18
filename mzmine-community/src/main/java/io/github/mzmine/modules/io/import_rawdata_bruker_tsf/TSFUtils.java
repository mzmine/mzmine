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

package io.github.mzmine.modules.io.import_rawdata_bruker_tsf;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleImagingScan;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.builders.SimpleBuildingScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.BrukerScanMode;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFFrameMsMsInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tsf.datamodel.TSFLib;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class to load Bruker TSF spectra (Maldi acquired on timsTOF FleX, but without tims
 * dimension.).
 */
public class TSFUtils implements AutoCloseable {

  public static final int BUFFER_SIZE_INCREMENT = 50_000;
  private static final Logger logger = Logger.getLogger(TSFUtils.class.getName());
  private final Arena offHeap = Arena.ofAuto();

  private int bufferSize = 50_000;
  private long handle;

  // Shared native scratch buffers. A TSF handle must not be accessed concurrently.
  private MemorySegment indexBuffer = offHeap.allocate(TSFLib.C_DOUBLE, bufferSize);
  private MemorySegment mzBuffer = offHeap.allocate(TSFLib.C_DOUBLE, bufferSize);
  private MemorySegment centroidIntensityBuffer = offHeap.allocate(TSFLib.C_FLOAT, bufferSize);
  private MemorySegment profileIntensityBuffer = offHeap.allocate(TSFLib.C_INT, bufferSize);
  private MemorySegment errorBuffer = offHeap.allocate(TSFLib.C_CHAR, 256);

  // Scaled intensities of the profile points kept by the zero-filtering.
  private double[] profileIntensities = new double[bufferSize];

  public TSFUtils() {
    setNumThreads(1);
  }

  /**
   * @param frameId The id of the frame. See {@link TDFFrameTable}
   * @return List of double[][]. Each array represents the data points of one scan
   */
  @NotNull
  public double[][] loadCentroidSpectrum(final long frameId) {
    ensureOpen();

    int numDataPoints;
    while (true) {
      numDataPoints = TSFLib.tsf_read_line_spectrum_v2(handle, frameId, indexBuffer,
          centroidIntensityBuffer, bufferSize);

      if (numDataPoints < 0) {
        throw new IllegalStateException(getLastErrorMessage());
      }
      if (numDataPoints > bufferSize) {
        growBuffers(numDataPoints, frameId);
        continue;
      }
      break;
    }

    if (numDataPoints == 0) {
      return new double[][]{new double[0], new double[0]};
    }

    final int conversionResult = TSFLib.tsf_index_to_mz(handle, frameId, indexBuffer, mzBuffer,
        numDataPoints);
    if (conversionResult == 0) {
      throw new IllegalStateException(getLastErrorMessage());
    }

    final double[] mzs = mzBuffer.asSlice(0, (long) numDataPoints * TSFLib.C_DOUBLE.byteSize())
        .toArray(TSFLib.C_DOUBLE);
    final double[] intensities = new double[numDataPoints];
    for (int i = 0; i < numDataPoints; i++) {
      intensities[i] = centroidIntensityBuffer.getAtIndex(TSFLib.C_FLOAT, i);
    }
    return new double[][]{mzs, intensities};
  }

  @NotNull
  public double[][] loadProfileSpectrum(final long frameId) {
    ensureOpen();

    int numDataPoints;
    while (true) {
      numDataPoints = TSFLib.tsf_read_profile_spectrum_v2(handle, frameId, profileIntensityBuffer,
          bufferSize);

      if (numDataPoints < 0) {
        throw new IllegalStateException(getLastErrorMessage());
      }
      if (numDataPoints > bufferSize) {
        growBuffers(numDataPoints, frameId);
        continue;
      }
      break;
    }

    if (numDataPoints == 0) {
      return new double[][]{new double[0], new double[0]};
    }

    // decision: read the raw uint32 profile and scale it ourselves. Benchmarked faster than
    // tsf_read_scaled_profile_spectrum_v2, which converts to float inside the native library.
    final double scalingFactor = TSFLib.tsf_read_scaling_factor(handle, frameId);
    // filter directly on the native buffers, kept indices are written straight to indexBuffer
    final int convertedSize = filterZeroIntensities(profileIntensityBuffer, numDataPoints,
        scalingFactor, indexBuffer, profileIntensities);
    if (convertedSize == 0) {
      return new double[][]{new double[0], new double[0]};
    }

    final int conversionResult = TSFLib.tsf_index_to_mz(handle, frameId, indexBuffer, mzBuffer,
        convertedSize);
    if (conversionResult == 0) {
      throw new IllegalStateException(getLastErrorMessage());
    }

    // The last zero is only a conversion boundary and is not part of the returned spectrum.
    final int resultSize = convertedSize - 1;
    final double[] mzs = mzBuffer.asSlice(0, (long) resultSize * TSFLib.C_DOUBLE.byteSize())
        .toArray(TSFLib.C_DOUBLE);
    return new double[][]{mzs, Arrays.copyOf(profileIntensities, resultSize)};
  }

  /**
   * @param file
   * @param frameId
   * @param metaDataTable
   * @param frameTable
   * @param maldiTable
   * @param spectrumType  The spectrum type to load.
   * @return
   */
  @NotNull
  public ImagingScan loadMaldiScan(@NotNull final RawDataFile file, final long frameId,
      @NotNull final TDFMetaDataTable metaDataTable, @NotNull final TSFFrameTable frameTable,
      @NotNull final TDFMaldiFrameInfoTable maldiTable,
      @NotNull final MassSpectrumType spectrumType) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final String scanDefinition =
        metaDataTable.getInstrumentType() + " - " + BrukerScanMode.fromScanMode(
            frameTable.getScanModeColumn().get(frameIndex).intValue());
    final int msLevel = TDFUtils.getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    final PolarityType polarity = PolarityType.fromSingleChar(
        (String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));
    final Range<Double> mzRange = metaDataTable.getMzRange();

    final Coordinates coords = new Coordinates(maldiTable.getTransformedXIndexPos(frameIndex),
        maldiTable.getTransformedYIndexPos(frameIndex), 0);

    final double[][] mzIntensities = switch (spectrumType) {
      case PROFILE -> loadProfileSpectrum(frameId);
      case CENTROIDED, THRESHOLDED, MIXED, ANY -> loadCentroidSpectrum(frameId);
    };

    return new SimpleImagingScan(file, Math.toIntExact(frameId), msLevel,
        (float) (frameTable.getTimeColumn().get(frameIndex) / 60), 0, 0, mzIntensities[0],
        mzIntensities[1], spectrumType, polarity, scanDefinition, mzRange, coords);
  }

  @Nullable
  public Scan loadScan(@NotNull final RawDataFile file, final long frameId,
      @NotNull final TDFMetaDataTable metaDataTable, @NotNull final TSFFrameTable frameTable,
      @NotNull final TDFFrameMsMsInfoTable msMsInfoTable,
      @Nullable final TDFMaldiFrameInfoTable maldiTable,
      @NotNull final MassSpectrumType spectrumType,
      @NotNull final ScanImportProcessorConfig config) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final String scanDefinition =
        metaDataTable.getInstrumentType() + " - " + BrukerScanMode.fromScanMode(
            frameTable.getScanModeColumn().get(frameIndex).intValue());
    final int msLevel = TDFUtils.getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    final float rt = frameTable.getTimeColumn().get(frameIndex).floatValue() / 60f;
    final PolarityType polarity = PolarityType.fromSingleChar(
        (String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));
    final Range<Double> mzRange = metaDataTable.getMzRange();

    final SimpleBuildingScan metadata = new SimpleBuildingScan((int) frameId, msLevel, polarity,
        spectrumType, rt, 0d, 0);
    if (!config.scanFilter().matches(metadata)) {
      return null;
    }

    final double[][] mzIntensities =
        spectrumType == MassSpectrumType.CENTROIDED ? loadCentroidSpectrum(frameId)
            : loadProfileSpectrum(frameId);

    final SimpleSpectralArrays arrays = config.processor()
        .processScan(metadata, new SimpleSpectralArrays(mzIntensities[0], mzIntensities[1]));

    final MassSpectrumType spectrumTypeAfterProcessing =
        config.isMassDetectActive(msLevel) || spectrumType == MassSpectrumType.CENTROIDED
            ? MassSpectrumType.CENTROIDED : spectrumType;

    /*if (msLevel > 1) {
      ce = (double) Objects.requireNonNullElse(
          msMsInfoTable.getColumn(TDFFrameMsMsInfoTable.COLLISION_ENERGY).get(frameIndex), 0d);
      precursor = (double) Objects.requireNonNullElse(
          msMsInfoTable.getColumn(TDFFrameMsMsInfoTable.TRIGGER_MASS).get(frameIndex), 0d);
      precursorCharge = (int)(long) Objects.requireNonNullElse(
          msMsInfoTable.getColumn(TDFFrameMsMsInfoTable.PRECURSOR_CHARGE).get(frameIndex), 0);
    }*/

    if (maldiTable == null || maldiTable.getFrameIdColumn().isEmpty()) {
      final SimpleScan scan = new SimpleScan(file, (int) frameId, msLevel, rt, null, arrays.mzs(),
          arrays.intensities(), spectrumTypeAfterProcessing, polarity, scanDefinition, mzRange);
      if (config.isMassDetectActive(msLevel)) {
        scan.addMassList(new ScanPointerMassList(scan));
      }
      return scan;
    } else {
      final Coordinates coords = new Coordinates(maldiTable.getTransformedXIndexPos(frameIndex),
          maldiTable.getTransformedYIndexPos(frameIndex), 0);

      final SimpleImagingScan scan = new SimpleImagingScan(file, Math.toIntExact(frameId),
          msLevel, (float) (frameTable.getTimeColumn().get(frameIndex) / 60), 0, 0, arrays.mzs(),
          arrays.intensities(), spectrumTypeAfterProcessing, polarity, scanDefinition, mzRange,
          coords);
      if (config.isMassDetectActive(msLevel)) {
        scan.addMassList(new ScanPointerMassList(scan));
      }
      return scan;
    }
  }

  /**
   * Opens the tsf_bin file.
   * <p>
   * Note: Separate Threads may not concurrently use the same handle!
   *
   * @param path                 The path
   * @param useRecalibratedState 0 or 1
   * @return 0 on error, the handle otherwise.
   */
  public long openFile(@NotNull final File path, final int useRecalibratedState) {
    if (handle != 0L) {
      close();
    }

    final String directory =
        path.isFile() ? path.getParentFile().getAbsolutePath() : path.getAbsolutePath();
    logger.finest(() -> "Opening tsf path " + directory);
    handle = TSFLib.tsf_open(offHeap.allocateFrom(directory), useRecalibratedState);

    if (handle == 0L) {
      logger.severe(() -> "Could not open TSF file: " + getLastErrorMessage());
      return handle;
    }
    logger.finest(() -> "File " + path.getName() + " hasReacalibratedState = "
        + TSFLib.tsf_has_recalibrated_state(handle));
    return handle;
  }


  /**
   * Opens the tdf_bin file.
   * <p>
   * Note: Separate Threads may not concurrently use the same handle! Note: Uses the recalibrated
   * state by default, if there is any.
   *
   * @param path The path
   * @return 0 on error, the handle otherwise.
   */
  public long openFile(@NotNull final File path) {
    return openFile(path, 1);
  }

  // ---------------------------------------------------------------------------------------------
  // UTILITY FUNCTIONS
  // -----------------------------------------------------------------------------------------------

  @Override
  public void close() {
    if (handle != 0L) {
      TSFLib.tsf_close(handle);
    }
    handle = 0L;
  }

  @NotNull
  private String getLastErrorMessage() {
    errorBuffer.fill((byte) 0);
    // tsf_get_last_error_string returns the full message length (including the trailing zero byte).
    // If that exceeds the buffer the message was truncated, so grow the buffer and query again.
    final int required = TSFLib.tsf_get_last_error_string(errorBuffer,
        (int) errorBuffer.byteSize());
    if (required > errorBuffer.byteSize()) {
      errorBuffer = offHeap.allocate(TSFLib.C_CHAR, required);
      TSFLib.tsf_get_last_error_string(errorBuffer, required);
    }
    return errorBuffer.getString(0, StandardCharsets.UTF_8);
  }

  /**
   * Removes long runs of zero intensities from a profile spectrum while keeping every non-zero
   * point together with its immediate zero neighbours (the shoulders of each peak). If at least one
   * point is kept, a single trailing zero is appended as a conversion boundary (it is stripped
   * again by the caller after the index-to-mz conversion). The first point (index 0) is never
   * emitted.
   *
   * @param intensities    raw profile intensities as uint32 ({@link TSFLib#C_INT}), the position
   *                       is the index; read for indices {@code [0, numDataPoints - 1]}
   * @param numDataPoints  number of valid entries to consider
   * @param scalingFactor  factor to multiply the raw intensities with, see
   *                       {@link TSFLib#tsf_read_scaling_factor(long, long)}
   * @param outIndices     output segment for the kept indices ({@link TSFLib#C_DOUBLE}), must hold
   *                       at least {@code numDataPoints} values
   * @param outIntensities output buffer for the kept, scaled intensities, must hold at least
   *                       {@code numDataPoints} values
   * @return the number of values written to the output buffers (0 if nothing was kept)
   */
  public static int filterZeroIntensities(@NotNull final MemorySegment intensities,
      final int numDataPoints, final double scalingFactor, @NotNull final MemorySegment outIndices,
      @NotNull final double[] outIntensities) {
    if (numDataPoints < 2) {
      return 0;
    }

    int numValues = 0;
    // values are uint32 read into a signed int, so values >= 2^31 are negative in Java. Only ever
    // compare against != 0, a > 0 check would treat these large intensities as zero.
    int previous = intensities.getAtIndex(TSFLib.C_INT, 0);
    int current = intensities.getAtIndex(TSFLib.C_INT, 1);

    // decision: iterate up to the last real point (index numDataPoints - 1) so it is not dropped.
    // The last point has no successor within the data, so its next value counts as 0. This also
    // avoids reading intensities[numDataPoints], which may hold stale data from a previous scan.
    for (int i = 1; i < numDataPoints; i++) {
      final int next = (i + 1 < numDataPoints) ? intensities.getAtIndex(TSFLib.C_INT, i + 1) : 0;
      if (current != 0 || next != 0 || previous != 0) {
        outIndices.setAtIndex(TSFLib.C_DOUBLE, numValues, i);
        // convert as unsigned, otherwise intensities >= 2^31 would become negative
        outIntensities[numValues] = Integer.toUnsignedLong(current) * scalingFactor;
        numValues++;
      }
      previous = current;
      current = next;
    }

    if (numValues > 0) {
      // add a last 0 as a conversion boundary
      outIndices.setAtIndex(TSFLib.C_DOUBLE, numValues, numDataPoints - 1);
      outIntensities[numValues] = 0d;
      numValues++;
    }

    return numValues;
  }

  private void growBuffers(final int requiredCapacity, final long frameId) {
    final int increments = Math.ceilDiv(requiredCapacity, BUFFER_SIZE_INCREMENT);
    bufferSize = Math.multiplyExact(increments, BUFFER_SIZE_INCREMENT);
    logger.fine(() -> "Could not read scan " + frameId + ". Increasing buffer size to " + bufferSize
        + " and reloading.");

    indexBuffer = offHeap.allocate(TSFLib.C_DOUBLE, bufferSize);
    mzBuffer = offHeap.allocate(TSFLib.C_DOUBLE, bufferSize);
    centroidIntensityBuffer = offHeap.allocate(TSFLib.C_FLOAT, bufferSize);
    profileIntensityBuffer = offHeap.allocate(TSFLib.C_INT, bufferSize);
    profileIntensities = new double[bufferSize];
  }

  private void ensureOpen() {
    if (handle == 0L) {
      throw new IllegalStateException("No TSF data file opened yet.");
    }
  }

  private static void setNumThreads(final int numThreads) {
    if (numThreads >= 1) {
      logger.finest(() -> "Setting number of threads per file to " + numThreads);
      TSFLib.tsf_set_num_threads(numThreads);
    }
  }
}
