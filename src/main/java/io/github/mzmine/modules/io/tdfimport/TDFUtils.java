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

package io.github.mzmine.modules.io.tdfimport;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.tdfimport.datamodel.callbacks.CentroidData;
import io.github.mzmine.modules.io.tdfimport.datamodel.TDFLibrary;
import io.github.mzmine.modules.io.tdfimport.datamodel.callbacks.ProfileData;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.FramePrecursorTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.FramePrecursorTable.FramePrecursorInfo;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFMetaDataTable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class TDFUtils {

  private static final Logger logger = Logger.getLogger(TDFUtils.class.getName());
  private static TDFLibrary tdfLib = null;

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
        libraryFileName = "libtimstdata.so";
      } else if (com.sun.jna.Platform.isMac()) {
        logger.info("MacOS is not supported by Bruker Daltonics. Please contact Bruker Daltonics.");
        MZmineCore.getDesktop().displayMessage(
            "MacOS is not supported by Bruker Daltonics. Please contact Bruker Daltonics.");
        return false;
      } else {
        return false;
      }
      timsdataLib = Native
          .extractFromResourcePath("vendorlib/bruker/" + libraryFileName,
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
//                                    FILE OPENING/CLOSING
// -----------------------------------------------------------------------------------------------

  /**
   * Opens the tdf_bin file.
   * <p>
   * Note: Separate Threads may not concurrently use the same handle!
   *
   * @param path                 The path
   * @param useRecalibratedState 0 or 1
   * @return 0 on error, the handle otherwise.
   */
  public static long openFile(File path, long useRecalibratedState) {
    if (tdfLib == null) {
      if (!loadLibrary()) {
        return 0L;
      }
    }
    if (path.isFile()) {
      return tdfLib.tims_open(path.getParentFile().getAbsolutePath(), useRecalibratedState);
    } else {
      return tdfLib.tims_open(path.getAbsolutePath(), useRecalibratedState);
    }
  }

  /**
   * Opens the tdf_bin file.
   * <p>
   * Note: Separate Threads may not concurrently use the same handle!
   *
   * @param path The path
   * @return 0 on error, the handle otherwise.
   */
  public static long openFile(File path) {
    return openFile(path, 0);
  }

  public static long close(long handle) {
    return tdfLib.tims_close(handle);
  }

// -----------------------------------------------------------------------------------------------
//                                HANDLING INDIVIDUAL SCANS
// -----------------------------------------------------------------------------------------------

  /**
   * @param handle    {@link TDFUtils#openFile(File)}
   * @param frameId   The id of the frame. See {@link TDFFrameTable}
   * @param scanBegin The first scan number
   * @param scanEnd   The last scan number
   * @return List of DataPoint[]. Each array represents the data points of one scan
   */
  public static List<DataPoint[]> loadDataPointsForFrame(long handle, long frameId, long scanBegin,
      long scanEnd) {

    final List<DataPoint[]> dataPoints = new ArrayList<>((int) (scanEnd - scanBegin));

    // load scans in packs of 50 to not cause a buffer overflow
    long start = scanBegin;
    while (start < scanEnd) {
      // start is inclusive, end is exclusive
      final long end = (start + 50) < scanEnd ? start + 50 : scanEnd;
      final int numScans = (int) (end - start);

      final byte[] buffer = new byte[200000];
      final long buf_len = tdfLib
          .tims_read_scans_v2(handle, frameId, start, end, buffer, buffer.length);
      // we increment here in case we failed to read
      start = start + 50;

      final IntBuffer intBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();

      if (buf_len == 0) {
        logger.warning("Cannot read scans " + start + "-" + end + " for frame " + frameId);
        continue;
      }
      final int[] scanBuffer = new int[intBuffer.remaining()];
      intBuffer.get(scanBuffer);
      // check out the layout of scanBuffer:
      // - the first numScan integers specify the number of peaks for each scan
      // - the next integers are pairs of (x,y) values for the scans. The x values are not masses but index values
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
    }
    return dataPoints;
  }

  /**
   * Loads mobility resolved scans of a specific pasef frame. (ScanMode 8 in the Frames Table)
   *
   * @param handle              {@link TDFUtils#openFile(File)}
   * @param frameId             The id of the frame. See {@link TDFFrameTable}
   * @param firstScanNum        The first scan num
   * @param frameTable          The frame table
   * @param metaDataTable       The metadata table
   * @param framePrecursorTable The FramePrecursorTable
   * @return List of scans for the given frame id. Empty scans have been filtered out.
   */
  @Nullable
  public static List<Scan> loadScansForPASEFFrame(final long handle, final long frameId, final int firstScanNum,
      final TDFFrameTable frameTable, final TDFMetaDataTable metaDataTable,
      final FramePrecursorTable framePrecursorTable) {

    // idk if frames are ordered consecutively and there are no skipped numbers, but let's play it safe
    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);

    final long numScans = (long) frameTable.getColumn(TDFFrameTable.NUM_SCANS).get(frameIndex);
    final List<Scan> scans = new ArrayList<>((int) numScans);
    final List<DataPoint[]> dataPoints = loadDataPointsForFrame(handle, frameId, 0, numScans);

    if (numScans != dataPoints.size()) {
      logger.info(
          "Number of scans for frame " + frameId + " in tdf (" + numScans
              + ") does not match number of loaded scans (" + dataPoints.size() + ").");
      return null;
    }

    final double[] mobilities = convertScanNumsToOneOverK0(handle, frameId,
        createPopulatedArray((int) numScans));

    final String scanDefinition =
        metaDataTable.getInstrumentType() + " - " + getDescriptionFromBrukerScanMode(
            frameTable.getScanModeColumn().get(frameIndex).intValue());
    final int msLevel = getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    final PolarityType polarity = PolarityType.fromSingleChar(
        (String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));

    // TODO: fragment scans

    for (int i = 0; i < dataPoints.size(); i++) {
      if (dataPoints.get(i).length == 0) {
        continue;
      }
      double precursorMz = 0.d;
      int precursorCharge = 0;
      if (msLevel == 2) {
        FramePrecursorInfo fpi = framePrecursorTable.getPrecursorInfoAtScanNum(frameId, i);
        precursorMz = fpi.getLargestPeakMz();
        precursorCharge = fpi.getCharge();
      }
      Scan scan = new SimpleScan(null,
          firstScanNum + i, // scans are numbered consecutively
          msLevel,
          frameTable.getTimeColumn().get(frameIndex) / 60, // to minutes
          precursorMz,
          precursorCharge,
          null,
          dataPoints.get(i),
          MassSpectrumType.CENTROIDED,
          polarity,
          scanDefinition,
          metaDataTable.getMzRange(),
          mobilities[i],
          MobilityType.TIMS);
      scans.add(scan);
    }
    return scans;
  }

  // ---------------------------------------------------------------------------------------------
//                                      AVERAGE FRAMES
// -----------------------------------------------------------------------------------------------
  private static DataPoint[] extractCentroidsForFrame(long handle, long frameId,
      int startScanNum, int endScanNum) {

    final CentroidData data = new CentroidData();
    final long error = tdfLib
        .tims_extract_centroided_spectrum_for_frame(handle, frameId, startScanNum, endScanNum, data,
            Pointer.NULL);

    if (error == 0) {
      logger.info(
          "Could not extract centroid scan for frame " + frameId + " for scans " + startScanNum
              + " to " + endScanNum + ".");
      return new DataPoint[0];
    }

    return data.toDataPoints();
  }

  public static SimpleFrame exctractCentroidScanForFrame(long handle, long frameId, int scanNum,
      TDFMetaDataTable metaDataTable, TDFFrameTable frameTable) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();
    final DataPoint[] dps = extractCentroidsForFrame(handle, frameId, 0, numScans);

    final String scanDefinition =
        metaDataTable.getInstrumentType() + " - " + getDescriptionFromBrukerScanMode(
            frameTable.getScanModeColumn().get(frameIndex).intValue());
    final int msLevel = getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    final PolarityType polarity = PolarityType.fromSingleChar(
        (String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));

    SimpleFrame frame = new SimpleFrame(null,
        scanNum,
        msLevel,
        (Double) frameTable.getColumn(TDFFrameTable.TIME).get(frameIndex) / 60, // to minutes
        0.d,
        0,
        null,
        dps,
        MassSpectrumType.CENTROIDED,
        polarity,
        scanDefinition,
        metaDataTable.getMzRange(),
        (int) frameId,
        MobilityType.TIMS);
    return frame;
  }

  @Nullable
  public static ProfileData extractProfileForFrame(long handle, long frameId, long startScanNum,
      long endScanNum) {

    final ProfileData data = new ProfileData();
    final long error = tdfLib
        .tims_extract_profile_for_frame(handle, frameId, startScanNum, endScanNum, data, null);

    if (error == 0) {
      logger.info("Could not extract profile for frame " + frameId + ".");
      return null;
    }

    return data;
  }

  @Deprecated // not ready yet, yields to wrong m/z values. How does bruker distribute them?
  public static Scan extractProfileScanForFrame(long handle, long frameId, int scanNum,
      TDFMetaDataTable metaDataTable, TDFFrameTable frameTable) {
    int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();
    ProfileData data = extractProfileForFrame(handle, frameId, 0, numScans);
    String scanDefinition =
        metaDataTable.getInstrumentType() + " - " + getDescriptionFromBrukerScanMode(
            frameTable.getScanModeColumn().get(frameIndex).intValue());
    int msLevel = getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    PolarityType polarity = PolarityType.fromSingleChar(
        (String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));

    DataPoint[] dps = data.toDataPoints(metaDataTable.getMzRange().lowerEndpoint(),
        metaDataTable.getMzRange().upperEndpoint());

    Scan scan = new SimpleScan(null,
        scanNum,
        msLevel,
        (Double) frameTable.getColumn(TDFFrameTable.TIME).get(frameIndex) / 60, // to minutes
        0.d,
        0,
        null,
        dps,
        MassSpectrumType.CENTROIDED,
        polarity,
        scanDefinition,
        metaDataTable.getMzRange());
    return scan;
  }

  // ---------------------------------------------------------------------------------------------
//                                    PASEF MS MS FUNCTIONS
// -----------------------------------------------------------------------------------------------
  public static DataPoint[] loadMsMsDataPointsForPrecursor_v2(long handle, long precursorId) {

    final CentroidData data = new CentroidData();
    final long error = tdfLib
        .tims_read_pasef_msms_v2(handle, new long[]{precursorId}, 1, data, Pointer.NULL);
    if (error == 0) {
      logger.info("Could not extract msms for precursor " + precursorId + ".");
      return new DataPoint[0];
    }
    return data.toDataPoints();
  }

  // ---------------------------------------------------------------------------------------------
//                                    CONVERSION FUNCTIONS
// -----------------------------------------------------------------------------------------------
  private static double[] convertIndicesToMZ(long handle, long frameId, int[] indices) {

    final double[] buffer = new double[indices.length];
    final long error = tdfLib
        .tims_index_to_mz(handle, frameId, Arrays.stream(indices).asDoubleStream().toArray(),
            buffer, indices.length);
    if (error == 0) {
      logger.info("Could not convert indices to mzs for frame " + frameId);
    }
    return buffer;
  }

  public static double[] convertScanNumsToOneOverK0(long handle, long frameId, int[] scanNums) {
    double[] buffer = new double[scanNums.length];

    long error = tdfLib
        .tims_scannum_to_oneoverk0(handle, frameId,
            Arrays.stream(scanNums).asDoubleStream().toArray(),
            buffer, scanNums.length);
    if (error == 0) {
      logger.info("Could not convert scan nums to 1/K0 for frame " + frameId);
    }
    return buffer;
  }

  /**
   * Creates an array of the given size and populates it with numbers from 0 to size-1
   *
   * @param size The size
   * @return the array
   */
  public static int[] createPopulatedArray(int size) {
    int[] array = new int[size];
    for (int i = 0; i < size; i++) {
      array[i] = i;
    }
    return array;
  }

// ---------------------------------------------------------------------------------------------
//                                    SQL-RELATED FUNCTIONS
// -----------------------------------------------------------------------------------------------

  /**
   * @param msMsType The MsMsType of the respective frame from the {@link TDFFrameTable}. The types
   *                 are also listed there.
   * @return The MS level as usually handled by MZmine
   */
  public static int getMZmineMsLevelFromBrukerMsMsType(int msMsType) {
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

  /**
   * @param scanMode The ScanMode of the respective frame from the {@link TDFFrameTable}. The types
   *                 are also listed there.
   * @return Descriptive string for the scan description.
   */
  public static String getDescriptionFromBrukerScanMode(int scanMode) {
    switch (scanMode) {
      case 0:
        return "MS^1";
      case 1:
        return "Auto MS/MS";
      case 2:
        return "MRM";
      case 3:
        return "in-source CID";
      case 4:
        return "broadband CID";
      case 8:
        return "PASEF";
      case 9:
        return "DIA";
      case 10:
        return "PRM";
      case 20:
        return "MALDI";
      default:
        return "Unknown scan mode";

    }
  }

  // ---------------------------------------------------------------------------------------------
//                                    UTILITY FUNCTIONS
// -----------------------------------------------------------------------------------------------

}
