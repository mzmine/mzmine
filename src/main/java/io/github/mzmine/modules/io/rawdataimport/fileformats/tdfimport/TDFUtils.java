/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport;

import com.google.common.collect.Range;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleMobilityScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.BrukerScanMode;
import io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.TDFLibrary;
import io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.callbacks.CentroidData;
import io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.callbacks.ProfileData;
import io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.sql.FramePrecursorTable;
import io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.sql.TDFMetaDataTable;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author https://github.com/SteffenHeu
 */
public class TDFUtils {

  public static final int SCAN_PACKAGE_SIZE = 50;

  public static final int BUFFER_SIZE_INCREMENT = 100000; // 100 kb increase each time we fail
  private static final Logger logger = Logger.getLogger(TDFUtils.class.getName());
  public static int BUFFER_SIZE = 300000; // start with 300 kb of buffer size
  private static TDFLibrary tdfLib = null;

  private TDFUtils() {}

  /**
   * Initialises the tdf library. Is called when openFile is called.
   *
   * @return true on success, false on failure.
   */
  private static boolean loadLibrary() {
    File timsdataLib = null;
    String libraryFileName;
    try {
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
        return false;
      } else {
        return false;
      }
      timsdataLib = Native.extractFromResourcePath("/vendorlib/bruker/" + libraryFileName,
          TDFUtils.class.getClassLoader());
    } catch (IOException e) {
      e.printStackTrace();
      logger.warning("Failed to load/extract timsdata library");
      return false;
    }

    if (timsdataLib == null) {
      logger.warning("TIMS data library could not be loaded.");
      return false;
    }

    tdfLib = Native.load(timsdataLib.getAbsolutePath(), TDFLibrary.class);

    logger.info("Native TDF library initialised " + tdfLib.toString());
    return true;
  }

  // -----------------------------------------------------------------------------------------------
  // FILE OPENING/CLOSING
  // -----------------------------------------------------------------------------------------------

  /**
   * Opens the tdf_bin file.
   * <p>
   * Note: Separate Threads may not concurrently use the same handle!
   *
   * @param path The path
   * @param useRecalibratedState 0 or 1
   * @return 0 on error, the handle otherwise.
   */
  public static long openFile(final File path, final long useRecalibratedState) {
    if (!loadLibrary() || tdfLib == null) {
      return 0L;
    }
    if (path.isFile()) {
      final long handle =
          tdfLib.tims_open(path.getParentFile().getAbsolutePath(), useRecalibratedState);
      logger.finest(() -> "File " + path.getName() + " hasReacalibratedState = "
          + tdfLib.tims_has_recalibrated_state(handle));
      return handle;
    } else {
      final long handle = tdfLib.tims_open(path.getAbsolutePath(), useRecalibratedState);
      logger.finest(() -> "File " + path.getName() + " hasReacalibratedState = "
          + tdfLib.tims_has_recalibrated_state(handle));
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
  public static long openFile(final File path) {
    return openFile(path, 0);
  }

  public static long close(final long handle) {
    return tdfLib.tims_close(handle);
  }

  // -----------------------------------------------------------------------------------------------
  // HANDLING INDIVIDUAL SCANS
  // -----------------------------------------------------------------------------------------------

  /**
   * @param handle {@link TDFUtils#openFile(File)}
   * @param frameId The id of the frame. See {@link TDFFrameTable}
   * @param scanBegin The first scan number
   * @param scanEnd The last scan number
   * @return List of DataPoint[]. Each array represents the data points of one scan
   */
  public static List<DataPoint[]> loadDataPointsForFrame(final long handle, final long frameId,
      final long scanBegin, final long scanEnd) {

    final List<DataPoint[]> dataPoints = new ArrayList<>((int) (scanEnd - scanBegin));

    // buffer to store our scans. allocation takes time, so we want to reuse it
    // cannot be final, since we might have to increase the buffer size on the run
    // we don't just take a huge buffer, because clearing it takes time, too
    byte[] buffer = new byte[BUFFER_SIZE];

    // load scans in packs of SCAN_PACKAGE_SIZE to not cause a buffer overflow
    long start = scanBegin;
    while (start < scanEnd) {
      // start is inclusive, end is exclusive
      final long end = Math.min((start + SCAN_PACKAGE_SIZE), scanEnd);
      final int numScans = (int) (end - start);

      final long lastError =
          tdfLib.tims_read_scans_v2(handle, frameId, start, end, buffer, buffer.length);

      // check if the buffer size was enough
      if (printLastError(lastError)) {
        BUFFER_SIZE += BUFFER_SIZE_INCREMENT;
        final long finalStart = start;
        logger.fine(() -> "Could not read scans " + finalStart + "-" + end + " for frame " + frameId
            + ". Increasing buffer size to " + BUFFER_SIZE + " and reloading.");
        buffer = new byte[BUFFER_SIZE];
        continue; // try again
      }

      start = start + SCAN_PACKAGE_SIZE;

      final IntBuffer intBuffer =
          ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
      final int[] scanBuffer = new int[intBuffer.remaining()];
      intBuffer.get(scanBuffer);
      // check out the layout of scanBuffer:
      // - the first numScan integers specify the number of peaks for each scan
      // - the next integers are pairs of (x,y) values for the scans. The x values are not masses
      // but index values
      int d = numScans;
      for (int i = 0; i < numScans; i++) {
        final int numPeaks = scanBuffer[i];
        final int[] indices = Arrays.copyOfRange(scanBuffer, d, d + numPeaks);
        d += numPeaks;
        final int[] intensities = Arrays.copyOfRange(scanBuffer, d, d + numPeaks);
        d += numPeaks;
        final double[] masses = convertIndicesToMZ(handle, frameId, indices);

        final DataPoint[] dps = new DataPoint[numPeaks];
        for (int j = 0; j < numPeaks; j++) {
          dps[j] = new SimpleDataPoint(masses[j], intensities[j]);
        }
        dataPoints.add(dps);
      }
      Arrays.fill(buffer, (byte) 0);
    }
    return dataPoints;
  }

  /**
   * Loads mobility resolved scans of a specific frame. Tested with scan modes 0 and 8 (MS1 and
   * PASEF-MS/MS)
   *
   * @param handle {@link TDFUtils#openFile(File)}
   * @param frameId The id of the frame. See {@link TDFFrameTable}
   * @param frameTable The frame table // * @param metaDataTable The metadata table // *
   * @return List of scans for the given frame id. Empty scans have been filtered out.
   */
  @Nullable
  public static Set<MobilityScan> loadSpectraForTIMSFrame(RawDataFile newFile, final long handle,
      final long frameId, final Frame frame, @Nonnull final TDFFrameTable frameTable) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();
    final LinkedHashSet<MobilityScan> spectra = new LinkedHashSet<>(numScans);
    final List<DataPoint[]> dataPoints = loadDataPointsForFrame(handle, frameId, 0, numScans);

    if (numScans != dataPoints.size()) {
      logger.warning(() -> "Number of scans for frame " + frameId + " in tdf (" + numScans
          + ") does not match number of loaded scans (" + dataPoints.size() + ").");
      return null;
    }

    for (int i = 0; i < dataPoints.size(); i++) {
      /*
       * if (dataPoints.get(i).length == 0) { continue; }
       */

      spectra.add(new SimpleMobilityScan(newFile, i, frame, dataPoints.get(i)));
    }

    return spectra;
  }

  @Nullable
  public static List<Scan> loadScansForMaldiTimsFrame(final long handle, final long frameId,
      final TDFFrameTable frameTable, final TDFMetaDataTable metaDataTable,
      final TDFMaldiFrameInfoTable maldiTable) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();
    final long firstScanNum = frameTable.getFirstScanNumForFrame(frameId);

    List<Scan> scans = new ArrayList<>(numScans);
    final List<DataPoint[]> dataPoints = loadDataPointsForFrame(handle, frameId, 0, numScans);
    if (numScans != dataPoints.size()) {
      logger.warning(() -> "Number of scans for frame " + frameId + " in tdf (" + numScans
          + ") does not match number of loaded scans (" + dataPoints.size() + ").");
      return null;
    }

    final double[] mobilities =
        convertScanNumsToOneOverK0(handle, frameId, createPopulatedArray(numScans));

    // final String scanDefinition =
    // metaDataTable.getInstrumentType() + " - " + BrukerScanMode.fromScanMode(
    // frameTable.getScanModeColumn().get(frameIndex).intValue());
    final String scanDefinition = "x: " + maldiTable.getxIndexPosColumn().get(frameIndex) + " y: "
        + maldiTable.getyIndexPosColumn().get(frameIndex);
    final int msLevel = 1;
    final PolarityType polarity = PolarityType
        .fromSingleChar((String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));

    for (int i = 0; i < dataPoints.size(); i++) {
      if (dataPoints.get(i).length == 0) {
        continue;
      }
      final double precursorMz = 0.d;
      final int precursorCharge = 0;
      /*
       * Scan scan = new SimpleScan(null, Math.toIntExact(firstScanNum + i), msLevel, (float)
       * (frameTable.getTimeColumn().get(frameIndex) / 60), // to minutes precursorMz,
       * precursorCharge, dataPoints.get(i), MassSpectrumType.CENTROIDED, polarity, scanDefinition,
       * metaDataTable.getMzRange(), mobilities[i], MobilityType.TIMS); scans.add(scan);
       */
    }
    return scans;
  }

  // ---------------------------------------------------------------------------------------------
  // AVERAGE FRAMES
  // -----------------------------------------------------------------------------------------------
  private static DataPoint[] extractCentroidsForFrame(final long handle, final long frameId,
      final int startScanNum, final int endScanNum) {

    final CentroidData data = new CentroidData();
    final long error = tdfLib.tims_extract_centroided_spectrum_for_frame(handle, frameId,
        startScanNum, endScanNum, data, Pointer.NULL);

    if (error == 0) {
      logger.warning(() -> "Could not extract centroid scan for frame " + frameId + " for scans "
          + startScanNum + " to " + endScanNum + ".");
      return new DataPoint[0];
    }

    return data.toDataPoints();
  }

  /**
   * @param handle
   * @param frameId
   * @param metaDataTable
   * @param frameTable
   * @return
   */
  public static SimpleFrame exctractCentroidScanForTimsFrame(IMSRawDataFile newFile,
      final long handle, final long frameId, @Nonnull final TDFMetaDataTable metaDataTable,
      @Nonnull final TDFFrameTable frameTable,
      @Nonnull final FramePrecursorTable framePrecursorTable) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();
    final DataPoint[] dps = extractCentroidsForFrame(handle, frameId, 0, numScans);

    final String scanDefinition = metaDataTable.getInstrumentType() + " - "
        + BrukerScanMode.fromScanMode(frameTable.getScanModeColumn().get(frameIndex).intValue());
    final int msLevel = getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    final PolarityType polarity = PolarityType
        .fromSingleChar((String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));

    final double[] mobilities =
        convertScanNumsToOneOverK0(handle, frameId, createPopulatedArray(numScans));
    Map<Integer, Double> mobilitiesMap = new LinkedHashMap<>();

    for (int i = 0; i < mobilities.length; i++) {
      mobilitiesMap.put(i, mobilities[i]);
    }

    Range<Double> mzRange = metaDataTable.getMzRange();

    SimpleFrame frame = new SimpleFrame(newFile, Math.toIntExact(frameId), msLevel,
        (float) (frameTable.getTimeColumn().get(frameIndex) / 60), // to minutes
        0.d, 0, dps, MassSpectrumType.CENTROIDED, polarity, scanDefinition, mzRange,
        MobilityType.TIMS, numScans, mobilitiesMap,
        framePrecursorTable.getMsMsInfoForFrame(Math.toIntExact(frameId)));

    frame.setMobilities(mobilitiesMap.values().stream().mapToDouble(Double::doubleValue).toArray());

    return frame;
  }

  @Nullable
  public static ProfileData extractProfileForFrame(final long handle, final long frameId,
      final long startScanNum, final long endScanNum) {

    final ProfileData data = new ProfileData();
    final long error = tdfLib.tims_extract_profile_for_frame(handle, frameId, startScanNum,
        endScanNum, data, null);

    if (error == 0) {
      logger.warning(() -> "Could not extract profile for frame " + frameId + ".");
      return null;
    }

    return data;
  }

  /**
   * @param handle
   * @param frameId
   * @param scanNum
   * @param metaDataTable
   * @param frameTable
   * @return
   * @deprecated not ready yet, yields to wrong m/z values. How does bruker distribute them?
   */
  /*
   * @Deprecated public static Scan extractProfileScanForFrame(final long handle, final long
   * frameId, final int scanNum, final TDFMetaDataTable metaDataTable, final TDFFrameTable
   * frameTable) {
   *
   * final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId); final int numScans =
   * frameTable.getNumScansColumn().get(frameIndex).intValue(); final ProfileData data =
   * extractProfileForFrame(handle, frameId, 0, numScans); final String scanDefinition =
   * metaDataTable.getInstrumentType() + " - " +
   * BrukerScanMode.fromScanMode(frameTable.getScanModeColumn().get(frameIndex).intValue()); final
   * int msLevel = getMZmineMsLevelFromBrukerMsMsType(
   * frameTable.getMsMsTypeColumn().get(frameIndex).intValue()); final PolarityType polarity =
   * PolarityType .fromSingleChar((String)
   * frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));
   *
   * final DataPoint[] dps = data.toDataPoints(metaDataTable.getMzRange().lowerEndpoint(),
   * metaDataTable.getMzRange().upperEndpoint());
   *
   * return new SimpleScan(null, scanNum, msLevel, (float)
   * (frameTable.getTimeColumn().get(frameIndex) / 60), // to minutes 0.d, 0, dps,
   * MassSpectrumType.CENTROIDED, polarity, scanDefinition, metaDataTable.getMzRange()); }
   */

  // ---------------------------------------------------------------------------------------------
  // PASEF MS MS FUNCTIONS
  // -----------------------------------------------------------------------------------------------
  @Deprecated
  public static DataPoint[] loadMsMsDataPointsForPrecursor_v2(final long handle,
      final long precursorId) {

    final CentroidData data = new CentroidData();
    final long error =
        tdfLib.tims_read_pasef_msms_v2(handle, new long[] {precursorId}, 1, data, Pointer.NULL);
    if (error == 0) {
      logger.warning(() -> "Could not extract MS/MS for precursor " + precursorId + ".");
      return new DataPoint[0];
    }
    return data.toDataPoints();
  }

  // ---------------------------------------------------------------------------------------------
  // CONVERSION FUNCTIONS
  // -----------------------------------------------------------------------------------------------
  private static double[] convertIndicesToMZ(final long handle, final long frameId,
      final int[] indices) {

    final double[] buffer = new double[indices.length];
    final long error = tdfLib.tims_index_to_mz(handle, frameId,
        Arrays.stream(indices).asDoubleStream().toArray(), buffer, indices.length);
    if (error == 0) {
      logger.warning(() -> "Could not convert indices to mzs for frame " + frameId);
    }
    return buffer;
  }

  public static double[] convertScanNumsToOneOverK0(final long handle, final long frameId,
      final int[] scanNums) {
    double[] buffer = new double[scanNums.length];

    long error = tdfLib.tims_scannum_to_oneoverk0(handle, frameId,
        Arrays.stream(scanNums).asDoubleStream().toArray(), buffer, scanNums.length);
    if (error == 0) {
      logger.warning(() -> "Could not convert scan nums to 1/K0 for frame " + frameId);
    }
    return buffer;
  }

  /**
   * Creates an array of the given size and populates it with numbers from 0 to size-1
   *
   * @param size The size
   * @return the array
   */
  private static int[] createPopulatedArray(final int size) {
    int[] array = new int[size];
    for (int i = 0; i < size; i++) {
      array[i] = i;
    }
    return array;
  }

  // ---------------------------------------------------------------------------------------------
  // SQL-RELATED FUNCTIONS
  // -----------------------------------------------------------------------------------------------

  /**
   * @param msMsType The MsMsType of the respective frame from the {@link TDFFrameTable}. The types
   *        are also listed there.
   * @return The MS level as usually handled by MZmine
   */
  public static int getMZmineMsLevelFromBrukerMsMsType(final int msMsType) {
    switch (msMsType) {
      case 0:
        return 1;
      case 2:
        return 2;
      case 9:
        return 2;
      case 10:
        return 2;
      case 8:
        return 2;
      default:
        return 0;
    }
  }

  // ---------------------------------------------------------------------------------------------
  // UTILITY FUNCTIONS
  // -----------------------------------------------------------------------------------------------

  /**
   * @param errorCode return value of tims library methods
   * @return true if an error occurred
   */
  private static boolean printLastError(long errorCode) {
    if (errorCode == 0 || errorCode > BUFFER_SIZE) {
      byte[] errorBuffer = new byte[64];
      long len = tdfLib.tims_get_last_error_string(errorBuffer, errorBuffer.length);
      try {
        final String errorMessage = new String(errorBuffer, "UTF-8");
        logger.fine(() -> "Last TDF import error: " + errorMessage + " length: " + len
            + ". Required buffer size: " + errorCode + " actual size: " + BUFFER_SIZE);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      return true;
    } else {
      return false;
    }
  }

}
