/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.io.import_bruker_tsf;

import com.google.common.collect.Range;
import com.sun.jna.Native;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleImagingScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_bruker_tdf.TDFUtils;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.BrukerScanMode;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.import_imzml.Coordinates;
import io.github.mzmine.modules.io.import_mzml_msdk.ConversionUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class TSFUtils {

  public static final int BUFFER_SIZE_INCREMENT = 50_000;
  private static Logger logger = Logger.getLogger(TSFUtils.class.getName());
  private TSFData tsfdata = null;
  private int BUFFER_SIZE = 50_000;

  // centroid
  private double[] centroidIndexArray = new double[BUFFER_SIZE]; // a double array -> twice the size
  private float[] centroidIntensityArray = new float[BUFFER_SIZE]; // a float array
  private double[] centroidMzArray = new double[BUFFER_SIZE];

  // profile
  private long[] profileIntensityArray = new long[BUFFER_SIZE];
  private double[] profileIndexArray = createPopulatedArray(BUFFER_SIZE);
  private double[] profileMzArray = new double[BUFFER_SIZE];

  TSFUtils() throws IOException, UnsupportedOperationException {
    try {
      loadLibrary();
    } catch (IOException | UnsupportedOperationException e) {
      e.printStackTrace();
      throw e;
    }
  }

  public static double[] createPopulatedArray(final int size) {
    double[] array = new double[size];
    for (int i = 0; i < size; i++) {
      array[i] = i;
    }
    return array;
  }

  /**
   * @param handle  {@link TSFUtils#openFile(File)}
   * @param frameId The id of the frame. See {@link TDFFrameTable}
   * @return List of double[][]. Each array represents the data points of one scan
   */
  public double[][] loadCentroidSpectrum(final long handle, final long frameId) {
    long numDataPoints = 0;
    do {
      numDataPoints = tsfdata
          .tsf_read_line_spectrum(handle, frameId, centroidIndexArray, centroidIntensityArray,
              centroidIndexArray.length);

      // check if the buffer size was enough
      if (printLastError(numDataPoints) || numDataPoints > centroidIndexArray.length) {
        BUFFER_SIZE += BUFFER_SIZE_INCREMENT;
        logger.fine(() -> "Could not read scan " + frameId
            + ". Increasing buffer size to " + BUFFER_SIZE + " and reloading.");
        centroidIntensityArray = new float[BUFFER_SIZE];
        centroidIndexArray = new double[BUFFER_SIZE];
        centroidMzArray = new double[BUFFER_SIZE]; // double array -> twice the size
      }
    } while (numDataPoints == 0);

    tsfdata.tsf_index_to_mz(handle, frameId, centroidIndexArray, centroidMzArray, numDataPoints);

    double[][] mzIntensities = new double[2][];
    mzIntensities[0] = Arrays.copyOfRange(centroidMzArray, 0, (int) numDataPoints);
    mzIntensities[1] = ConversionUtils
        .convertFloatsToDoubles(centroidIntensityArray, (int) numDataPoints);
    return mzIntensities;
  }

  public double[][] loadProfileSpectrum(long handle, final long frameId) {
    long numDataPoints = 0;
    do {
      numDataPoints = tsfdata
          .tsf_read_profile_spectrum(handle, frameId, profileIntensityArray,
              profileIntensityArray.length);

      // check if the buffer size was enough
      if (printLastError(numDataPoints) || numDataPoints > profileIndexArray.length) {
        BUFFER_SIZE += BUFFER_SIZE_INCREMENT;
        logger.fine(() -> "Could not read scan " + frameId
            + ". Increasing buffer size to " + BUFFER_SIZE + " and reloading.");
        profileIntensityArray = new long[BUFFER_SIZE];
        profileIndexArray = createPopulatedArray(BUFFER_SIZE);
        profileMzArray = new double[BUFFER_SIZE];
      }
    } while (numDataPoints == 0);

    tsfdata.tsf_index_to_mz(handle, frameId, profileIndexArray, profileMzArray, numDataPoints);

    double[][] mzIntensities = new double[2][];
    mzIntensities[0] = Arrays.copyOfRange(profileMzArray, 0, (int) numDataPoints);
    mzIntensities[1] = ConversionUtils
        .convertLongsToDoubles(profileIntensityArray, (int) numDataPoints);
    return mzIntensities;
  }

  public ImagingScan loadMaldiScan(RawDataFile file, final long handle, final long frameId, @Nonnull
      TDFMetaDataTable metaDataTable, @Nonnull TSFFrameTable frameTable, @Nonnull
      TDFMaldiFrameInfoTable maldiTable) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final String scanDefinition = metaDataTable.getInstrumentType() + " - "
        + BrukerScanMode.fromScanMode(frameTable.getScanModeColumn().get(frameIndex).intValue());
    final int msLevel = TDFUtils.getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    final PolarityType polarity = PolarityType
        .fromSingleChar((String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));
    final Range<Double> mzRange = metaDataTable.getMzRange();

    final Coordinates coords = new Coordinates(
        maldiTable.getTransformedXIndexPos((int) (frameIndex)),
        maldiTable.getTransformedYIndexPos((int) (frameIndex)), 0);

    double[][] mzIntensities = loadCentroidSpectrum(handle, frameId);

    return new SimpleImagingScan(file, Math.toIntExact(frameId), msLevel,
        (float) (frameTable.getTimeColumn().get(frameIndex) / 60), 0, 0, mzIntensities[0],
        mzIntensities[1], MassSpectrumType.CENTROIDED, polarity, scanDefinition, mzRange, coords);
  }

  private boolean loadLibrary()
      throws IOException, UnsupportedOperationException {
    logger.finest("Initialising tdf library.");
    File timsdataLib = null;
    String libraryFileName;
    if (com.sun.jna.Platform.isWindows() && com.sun.jna.Platform.is64Bit()) {
      libraryFileName = "timsdata_x64.dll";
    } else if (com.sun.jna.Platform.isWindows() && !com.sun.jna.Platform.is64Bit()) {
      libraryFileName = "timsdata_x32.dll";
    } else if (com.sun.jna.Platform.isLinux()) {
      libraryFileName = "libtimsdata.so";
    } else if (com.sun.jna.Platform.isMac()) {
      logger.info("MacOS is not supported by Bruker Daltonics. Please contact Bruker Daltonics.");
      MZmineCore.getDesktop().displayMessage(
          "MacOS is not supported by Bruker Daltonics. Please contact Bruker Daltonics.");
      throw new UnsupportedOperationException(
          "MacOS is not supported by Bruker Daltonics. Please contact Bruker Daltonics.");
    } else {
      throw new UnsupportedOperationException(
          "Unsupported OS. Cannot initialise timsdata library.");
    }
    timsdataLib = Native.extractFromResourcePath("/vendorlib/bruker/" + libraryFileName,
        TDFUtils.class.getClassLoader());
    if (timsdataLib == null) {
      throw new FileNotFoundException(
          "File " + libraryFileName + " not found. Cannot initialise timsdata library.");
    }
    tsfdata = Native.load(timsdataLib.getAbsolutePath(), TSFData.class);
    return tsfdata != null;
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
  public long openFile(final File path, final long useRecalibratedState) {
    if (tsfdata == null) {
      logger
          .warning(() -> "File + " + path.getAbsolutePath() + " cannot be loaded because timsdata "
              + "library could not be initialised.");
      return 0L;
    }
    if (path.isFile()) {
      logger.finest(() -> "Opening tsf file " + path.getAbsolutePath());
      final long handle =
          tsfdata.tsf_open(path.getParentFile().getAbsolutePath(), useRecalibratedState);
      logger.finest(() -> "File " + path.getName() + " hasReacalibratedState = "
          + tsfdata.tsf_has_recalibrated_state(handle));
      return handle;
    } else {
      logger.finest(() -> "Opening tsf path " + path.getAbsolutePath());
      final long handle = tsfdata.tsf_open(path.getAbsolutePath(), useRecalibratedState);
      logger.finest(() -> "File " + path.getName() + " hasReacalibratedState = "
          + tsfdata.tsf_has_recalibrated_state(handle));
      return handle;
    }
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
  public long openFile(final File path) {
    return openFile(path, 1);
  }

  // ---------------------------------------------------------------------------------------------
  // UTILITY FUNCTIONS
  // -----------------------------------------------------------------------------------------------

  public void close(final long handle) {
    tsfdata.tsf_close(handle);
  }

  /**
   * @param errorCode return value of tims library methods
   * @return true if an error occurred
   */
  private boolean printLastError(long errorCode) {
    if (errorCode == 0 || errorCode > BUFFER_SIZE) {
      byte[] errorBuffer = new byte[64];
      long len = tsfdata.tsf_get_last_error_string(errorBuffer, errorBuffer.length);
      final String errorMessage = new String(errorBuffer, StandardCharsets.UTF_8);
      logger.fine(() -> "Last TDF import error: " + errorMessage + " length: " + len
          + ". Required buffer size: " + errorCode + " actual size: " + BUFFER_SIZE);
      return true;
    } else {
      return false;
    }
  }
}
