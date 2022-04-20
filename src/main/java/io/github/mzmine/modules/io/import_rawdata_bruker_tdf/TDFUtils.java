/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf;

import com.google.common.collect.Range;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleImagingFrame;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.BrukerScanMode;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.TDFLibrary;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.callbacks.CentroidData;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.callbacks.ProfileData;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.FramePrecursorTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;
import io.github.mzmine.parameters.ParameterSet;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 */
public class TDFUtils {

  public static final int SCAN_PACKAGE_SIZE = 50;
  public static final int BUFFER_SIZE_INCREMENT = 100_000; // 100 kb increase each time we fail
  private static final Logger logger = Logger.getLogger(TDFUtils.class.getName());
  private static int DEFAULT_NUMTHREADS = MZmineCore.getConfiguration().getPreferences()
      .getParameter(MZminePreferences.numOfThreads).getValue();
  private final int numThreads;
  public int BUFFER_SIZE = 300000; // start with 300 kb of buffer size
  private TDFLibrary tdfLib = null;

  /**
   * the handle of the currently opened file
   **/
  private long handle = 0L;

  public TDFUtils() {
    this(DEFAULT_NUMTHREADS);
  }

  public TDFUtils(int numThreads) {
    this.numThreads = numThreads;
  }

  /**
   * Creates an array of the given size and populates it with numbers from 1 to size
   *
   * @param size The size
   * @return the array
   */
  public static int[] createPopulatedArrayFrom1(final int size) {
    return createPopulatedArray(size, 1); // scannums start at 1
  }

  public static int[] createPopulatedArray(final int size, int startOffset) {
    int[] array = new int[size];
    for (int i = 0; i < size; i++) {
      array[i] = i + startOffset;
    }
    return array;
  }

  // -----------------------------------------------------------------------------------------------
  // FILE OPENING/CLOSING
  // -----------------------------------------------------------------------------------------------

  /**
   * @param msMsType The MsMsType of the respective frame from the {@link TDFFrameTable}. The types
   *                 are also listed there.
   * @return The MS level as usually handled by MZmine
   */
  public static int getMZmineMsLevelFromBrukerMsMsType(final int msMsType) {
    return switch (msMsType) {
      case 0 -> 1;
      case 2, 9, 10, 8 -> 2;
      default -> 0;
    };
  }

  /**
   * Sets the default number of threads to use for each raw file across all {@link TDFUtils}
   * instances.
   *
   * @param numThreads
   */
  public static void setDefaultNumThreads(int numThreads) {
    numThreads = Math.max(numThreads, 1);
    final int finalNumThreads = numThreads;
    logger.finest(() -> "Setting number of threads per file to " + finalNumThreads);
    DEFAULT_NUMTHREADS = numThreads;
  }

  /**
   * Initialises the tdf library. Is called when openFile is called.
   *
   * @return true on success, false on failure.
   */
  private boolean loadLibrary() {
    if (tdfLib != null) {
      return true;
    }

    logger.finest("Initialising tdf library.");
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
      logger.log(Level.SEVERE, "Failed to load/extract timsdata library", e);
      return false;
    }

    if (timsdataLib == null) {
      logger.warning("TIMS data library could not be loaded.");
      return false;
    }

    try {
      tdfLib = Native.load(timsdataLib.getAbsolutePath(), TDFLibrary.class);
    } catch (UnsatisfiedLinkError e) {
      logger.severe("Cannot load tdf library. Is VC++ 2017 Redist installed?");
      logger.log(Level.SEVERE, e.getMessage(), e);
      MZmineCore.getDesktop()
          .displayErrorMessage("Cannot load tdf library. Is VC++ 2017 Redist installed?");
      return false;
    }
    logger.info("Native TDF library initialised " + tdfLib.toString());
    setNumThreads(numThreads);

    return true;
  }

  // -----------------------------------------------------------------------------------------------
  // HANDLING INDIVIDUAL SCANS
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
  public long openFile(final File path, final long useRecalibratedState) {
    if (handle != 0L) {
      close();
    }

    if (!loadLibrary() || tdfLib == null) {
      logger.warning(() -> "File + " + path.getAbsolutePath() + " cannot be loaded because tdf "
          + "library could not be initialised.");
      return 0L;
    }
    if (path.isFile()) {
      logger.finest(() -> "Opening tdf file " + path.getAbsolutePath());
      handle = tdfLib.tims_open(path.getParentFile().getAbsolutePath(), useRecalibratedState);
      logger.finest(() -> "File " + path.getName() + " hasReacalibratedState = "
          + tdfLib.tims_has_recalibrated_state(handle));
      return handle;
    } else {
      logger.finest(() -> "Opening tdf path " + path.getAbsolutePath());
      handle = tdfLib.tims_open(path.getAbsolutePath(), useRecalibratedState);
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
  public long openFile(final File path) {
    return openFile(path, 1);
  }

  public void close() {
    if (tdfLib != null && handle != 0L) {
      tdfLib.tims_close(handle);
    }
    handle = 0L;
  }

  /**
   * @param frameId   The id of the frame. See {@link TDFFrameTable}
   * @param scanBegin The first scan index (starting with 0)
   * @param scanEnd   The last scan index
   * @return List of double[][]. Each array represents the data points of one scan
   */
  public List<double[][]> loadDataPointsForFrame(final long frameId, final long scanBegin,
      final long scanEnd) {
    if (handle == 0L) {
      throw new IllegalStateException("No tdf data file opened yet.");
    }

    final List<double[][]> dataPoints = new ArrayList<>((int) (scanEnd - scanBegin));

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

      synchronized (tdfLib) {
        final long lastError = tdfLib.tims_read_scans_v2(handle, frameId, start, end, buffer,
            buffer.length);
        // check if the buffer size was enough
        if (printLastError(lastError)) {
          BUFFER_SIZE += BUFFER_SIZE_INCREMENT;
          final long finalStart = start;
          logger.fine(
              () -> "Could not read scans " + finalStart + "-" + end + " for frame " + frameId
                  + ". Increasing buffer size to " + BUFFER_SIZE + " and reloading.");
          buffer = new byte[BUFFER_SIZE];
          continue; // try again
        }
      }

      start = start + SCAN_PACKAGE_SIZE;

      final IntBuffer intBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
          .asIntBuffer();
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
        final double[] intensities = ConversionUtils.convertIntsToDoubles(
            Arrays.copyOfRange(scanBuffer, d, d + numPeaks));
        d += numPeaks;

        synchronized (tdfLib) {
          final double[] masses = convertIndicesToMZ(handle, frameId, indices);
          double[][] dps = new double[2][];
          dps[0] = masses;
          dps[1] = intensities;
          dataPoints.add(dps);
        }
      }
      Arrays.fill(buffer, (byte) 0);
    }
    if(dataPoints.get(0)[0].length != 0) {
      logger.finest("data");
    }
    return dataPoints;
  }

  /**
   * Loads mobility resolved scans of a specific frame. Tested with scan modes 0 and 8 (MS1 and
   * PASEF-MS/MS)
   *
   * @param frameId    The id of the frame. See {@link TDFFrameTable}
   * @param frameTable The frame table
   * @return List of scans for the given frame id. Empty scans have been filtered out.
   */
  @Nullable
  public List<BuildingMobilityScan> loadSpectraForTIMSFrame(final long frameId,
      @NotNull final TDFFrameTable frameTable) {
    return loadSpectraForTIMSFrame(frameId, frameTable, null, null);
  }

  /**
   * Loads mobility resolved scans of a specific frame. Tested with scan modes 0 and 8 (MS1 and
   * PASEF-MS/MS)
   *
   * @param frameId    The id of the frame. See {@link TDFFrameTable}
   * @param frameTable The frame table
   * @param msDetector Mass detector for the given ms level. May be null.
   * @param msParam    Mass detector parameters. May be null.
   * @return List of scans for the given frame id. Empty scans have been filtered out.
   */
  @Nullable
  public List<BuildingMobilityScan> loadSpectraForTIMSFrame(final long frameId,
      @NotNull final TDFFrameTable frameTable, @Nullable final MassDetector msDetector,
      @Nullable final ParameterSet msParam) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();
    final List<BuildingMobilityScan> spectra = new ArrayList<>(numScans);
    final List<double[][]> dataPoints = loadDataPointsForFrame(frameId, 0, numScans);

    if (numScans != dataPoints.size()) {
      logger.warning(() -> "Number of scans for frame " + frameId + " in tdf (" + numScans
          + ") does not match number of loaded scans (" + dataPoints.size() + ").");
      return null;
    }

    for (int i = 0; i < dataPoints.size(); i++) {
      if (msDetector != null && msParam != null) {
        spectra.add(new BuildingMobilityScan(i,
            msDetector.getMassValues(dataPoints.get(i)[0], dataPoints.get(i)[1], msParam)));
      } else {
        spectra.add(new BuildingMobilityScan(i, dataPoints.get(i)[0], dataPoints.get(i)[1]));
      }
    }

    return spectra;
  }

  // ---------------------------------------------------------------------------------------------
  // AVERAGE FRAMES
  // -----------------------------------------------------------------------------------------------
  private double[][] extractCentroidsForFrame(final long frameId, final int startScanNum,
      final int endScanNum) {
    if (handle == 0L) {
      throw new IllegalStateException("No tdf data file opened yet.");
    }

    final CentroidData data = new CentroidData();

    synchronized (tdfLib) {
      final long error = tdfLib.tims_extract_centroided_spectrum_for_frame(handle, frameId,
          startScanNum, endScanNum, data, Pointer.NULL);

      if (error == 0) {
        logger.warning(() -> "Could not extract centroid scan for frame " + frameId + " for scans "
            + startScanNum + " to " + endScanNum + ".");
        return new double[][]{{0}, {0}};
      }

      return data.toDoubles();
    }
  }

  /**
   * @param frameId             the frame id.
   * @param metaDataTable       {@link TDFMetaDataTable} to construct the frame.
   * @param frameTable          {@link FramePrecursorTable} to construct the frame.
   * @param maldiFrameInfoTable Nullable for LC-IMS-MS. Required in case a maldi file is loaded.
   * @return The frame.
   */
  public SimpleFrame extractCentroidScanForTimsFrame(IMSRawDataFile newFile, final long frameId,
      @NotNull final TDFMetaDataTable metaDataTable, @NotNull final TDFFrameTable frameTable,
      @NotNull final FramePrecursorTable framePrecursorTable,
      @Nullable final TDFMaldiFrameInfoTable maldiFrameInfoTable) {
    return extractCentroidScanForTimsFrame(newFile, frameId, metaDataTable, frameTable,
        framePrecursorTable, maldiFrameInfoTable, null, null, null, null);
  }

  /**
   * @param frameId             the frame id.
   * @param metaDataTable       {@link TDFMetaDataTable} to construct the frame.
   * @param frameTable          {@link FramePrecursorTable} to construct the frame.
   * @param maldiFrameInfoTable Nullable for LC-IMS-MS. Required in case a maldi file is loaded.
   * @return The frame.
   */
  public SimpleFrame extractCentroidScanForTimsFrame(IMSRawDataFile newFile, final long frameId,
      @NotNull final TDFMetaDataTable metaDataTable, @NotNull final TDFFrameTable frameTable,
      @NotNull final FramePrecursorTable framePrecursorTable,
      @Nullable final TDFMaldiFrameInfoTable maldiFrameInfoTable,
      @Nullable final MassDetector ms1Detector, @Nullable final ParameterSet ms1Param,
      @Nullable final MassDetector ms2Detector, @Nullable final ParameterSet ms2Param) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();

    final String scanDefinition =
        metaDataTable.getInstrumentType() + " - " + BrukerScanMode.fromScanMode(
            frameTable.getScanModeColumn().get(frameIndex).intValue());
    final int msLevel = getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    final PolarityType polarity = PolarityType.fromSingleChar(
        (String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));

    double[][] data = extractCentroidsForFrame(frameId, 0, numScans);

    if (msLevel == 1 && ms1Detector != null && ms1Param != null) {
      data = ms1Detector.getMassValues(data[0], data[1], ms1Param);
    } else if (msLevel == 2 && ms2Detector != null && ms2Param != null) {
      data = ms2Detector.getMassValues(data[0], data[1], ms2Param);
    }

    final double[] mobilities = convertScanNumsToOneOverK0(handle, frameId,
        createPopulatedArrayFrom1(numScans));

    Range<Double> mzRange = metaDataTable.getMzRange();

    SimpleFrame frame;
    if (maldiFrameInfoTable == null || maldiFrameInfoTable.getFrameIdColumn().isEmpty()) {
      frame = new SimpleFrame(newFile, Math.toIntExact(frameId), msLevel,
          (float) (frameTable.getTimeColumn().get(frameIndex) / 60), // to minutes
          data[0], data[1], MassSpectrumType.CENTROIDED, polarity, scanDefinition, mzRange,
          MobilityType.TIMS, null);
    } else {
      frame = new SimpleImagingFrame(newFile, Math.toIntExact(frameId), msLevel,
          (float) (frameTable.getTimeColumn().get(frameIndex) / 60), // to minutes
          data[0], data[1], MassSpectrumType.CENTROIDED, polarity, scanDefinition, mzRange,
          MobilityType.TIMS, null);
      Coordinates coords = new Coordinates(maldiFrameInfoTable.getTransformedXIndexPos(frameIndex),
          maldiFrameInfoTable.getTransformedYIndexPos(frameIndex), 0);
      ((SimpleImagingFrame) frame).setCoordinates(coords);
    }

    frame.setMobilities(mobilities);

    return frame;
  }

  @Nullable
  public int[] extractProfileForFrame(final long frameId, final long startScanNum,
      final long endScanNum) {

    final ProfileData data = new ProfileData();
    synchronized (tdfLib) {
      final long error = tdfLib.tims_extract_profile_for_frame(handle, frameId, startScanNum,
          endScanNum, data, null);

      if (error == 0) {
        logger.warning(() -> "Could not extract profile for frame " + frameId + ".");
        return null;
      }

      return data.getIntensities();
    }
  }

  /**
   * @return A pseudo profile spectrum
   */
  public SimpleFrame extractProfileScanForFrame(IMSRawDataFile newFile, final long frameId,
      @NotNull final TDFMetaDataTable metaDataTable, @NotNull final TDFFrameTable frameTable,
      @NotNull final FramePrecursorTable framePrecursorTable,
      @Nullable final TDFMaldiFrameInfoTable maldiFrameInfoTable,
      @Nullable final MassDetector ms1Detector, @Nullable final ParameterSet ms1Param,
      @Nullable final MassDetector ms2Detector, @Nullable final ParameterSet ms2Param) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();
    final String scanDefinition =
        metaDataTable.getInstrumentType() + " - " + BrukerScanMode.fromScanMode(
            frameTable.getScanModeColumn().get(frameIndex).intValue());
    final int msLevel = getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    final PolarityType polarity = PolarityType.fromSingleChar(
        (String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));
    final Range<Double> mzRange = metaDataTable.getMzRange();

    final int[] intensityData = extractProfileForFrame(frameId, 0, numScans);

    // remove all extra zeros
    final IntArrayList filteredMzIndices = new IntArrayList();
    final DoubleArrayList filteredIntensities = new DoubleArrayList();
    filteredMzIndices.add(0);
    filteredIntensities.add(intensityData[0]);
    for (int i = 1; i < intensityData.length - 1;
        i++) { // previous , this and next are zero --> do not add this data point
      if (intensityData[i - 1] != 0 || intensityData[i] != 0 || intensityData[i + 1] != 0) {
        filteredMzIndices.add(i);
        filteredIntensities.add(intensityData[i]);
      }
    }
    filteredMzIndices.add(intensityData.length - 1);
    filteredIntensities.add(intensityData[intensityData.length - 1]);

    final double[] profileMzs = convertIndicesToMZ(handle, frameId,
        filteredMzIndices.toIntArray());

    final double data[][];
    boolean massesDetected = false;
    if (msLevel == 1 && ms1Detector != null && ms1Param != null) {
      data = ms1Detector.getMassValues(profileMzs,
          filteredIntensities.toDoubleArray(), ms1Param);
      massesDetected = true;
    } else if (msLevel == 2 && ms2Detector != null && ms2Param != null) {
      data = ms2Detector.getMassValues(profileMzs,
          filteredIntensities.toDoubleArray(), ms2Param);
      massesDetected = true;
    } else {
      data = new double[2][];
      data[0] = profileMzs;
      data[1] = filteredIntensities.toDoubleArray();
    }

    SimpleFrame frame;
    if (maldiFrameInfoTable == null || maldiFrameInfoTable.getFrameIdColumn().isEmpty()) {
      frame = new SimpleFrame(newFile, Math.toIntExact(frameId), msLevel,
          (float) (frameTable.getTimeColumn().get(frameIndex) / 60), // to minutes
          data[0], data[1], massesDetected ? MassSpectrumType.CENTROIDED : MassSpectrumType.PROFILE,
          polarity, scanDefinition, mzRange, MobilityType.TIMS, null);
    } else {
      frame = new SimpleImagingFrame(newFile, Math.toIntExact(frameId), msLevel,
          (float) (frameTable.getTimeColumn().get(frameIndex) / 60), // to minutes
          data[0], data[1], massesDetected ? MassSpectrumType.CENTROIDED : MassSpectrumType.PROFILE,
          polarity, scanDefinition, mzRange, MobilityType.TIMS, null);
      Coordinates coords = new Coordinates(maldiFrameInfoTable.getTransformedXIndexPos(frameIndex),
          maldiFrameInfoTable.getTransformedYIndexPos(frameIndex), 0);
      ((SimpleImagingFrame) frame).setCoordinates(coords);
    }

    final double[] mobilities = convertScanNumsToOneOverK0(handle, frameId,
        createPopulatedArrayFrom1(numScans));
    frame.setMobilities(mobilities);

    return frame;
  }

  // ---------------------------------------------------------------------------------------------
  // PASEF MS MS FUNCTIONS
  // -----------------------------------------------------------------------------------------------
  @Deprecated
  public double[][] loadMsMsDataPointsForPrecursor_v2(final long handle, final long precursorId) {

    final CentroidData data = new CentroidData();
    final long error = tdfLib.tims_read_pasef_msms_v2(handle, new long[]{precursorId}, 1, data,
        Pointer.NULL);
    if (error == 0) {
      logger.warning(() -> "Could not extract MS/MS for precursor " + precursorId + ".");
      return new double[][]{{0}, {0}};
    }
    return data.toDoubles();
  }

  // ---------------------------------------------------------------------------------------------
  // CONVERSION FUNCTIONS
  // -----------------------------------------------------------------------------------------------
  private double[] convertIndicesToMZ(final long handle, final long frameId, final int[] indices) {

    final double[] buffer = new double[indices.length];
    final long error = tdfLib.tims_index_to_mz(handle, frameId,
        Arrays.stream(indices).asDoubleStream().toArray(), buffer, indices.length);
    if (error == 0) {
      logger.warning(() -> "Could not convert indices to mzs for frame " + frameId);
    }
    return buffer;
  }

  // ---------------------------------------------------------------------------------------------
  // SQL-RELATED FUNCTIONS
  // -----------------------------------------------------------------------------------------------

  public double[] convertScanNumsToOneOverK0(final long handle, final long frameId,
      final int[] scanNums) {
    double[] buffer = new double[scanNums.length];

    long error = tdfLib.tims_scannum_to_oneoverk0(handle, frameId,
        Arrays.stream(scanNums).asDoubleStream().toArray(), buffer, scanNums.length);
    if (error == 0) {
      logger.warning(() -> "Could not convert scan nums to 1/K0 for frame " + frameId);
    }
    return buffer;
  }

  // ---------------------------------------------------------------------------------------------
  // UTILITY FUNCTIONS
  // -----------------------------------------------------------------------------------------------

  public Float calculateCCS(double ook0, long charge, double mz) {
    if (tdfLib == null) {
      boolean loaded = loadLibrary();
      if (!loaded) {
        return null;
      }
    }
    return (float) tdfLib.tims_oneoverk0_to_ccs_for_mz(ook0, charge, mz);
  }

  public Float calculateOok0(double ccs, long charge, double mz) {
    if (tdfLib == null) {
      boolean loaded = loadLibrary();
      if (!loaded) {
        return null;
      }
    }
    return (float) tdfLib.tims_ccs_to_oneoverk0_for_mz(ccs, charge, mz);
  }

  /**
   * @param errorCode return value of tims library methods
   * @return true if an error occurred
   */
  private boolean printLastError(long errorCode) {
    if (errorCode == 0 || errorCode > BUFFER_SIZE) {
      byte[] errorBuffer = new byte[64];
      long len = tdfLib.tims_get_last_error_string(errorBuffer, errorBuffer.length);
      try {
        final String errorMessage = new String(errorBuffer, "UTF-8");
        logger.fine(() -> "Last TDF import error: " + errorMessage + " length: " + len
            + ". Required buffer size: " + errorCode + " actual size: " + BUFFER_SIZE);
        if(errorMessage.contains("CorruptFrameDataError")) {
          throw new IllegalStateException("Error reading tdf raw data. " + errorMessage);
        }
      } catch (UnsupportedEncodingException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }
      return true;
    } else {
      return false;
    }

  }

  public void setNumThreads(int numThreads) {
    if (tdfLib == null) {
      if (!loadLibrary()) {
        return;
      }
    }
    if (numThreads >= 1) {
      logger.finest(() -> "Setting number of threads per file to " + numThreads);
      tdfLib.tims_set_num_threads(numThreads);
    }
  }

}
