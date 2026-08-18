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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import com.google.common.collect.Range;
import com.sun.jna.Native;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleImagingFrame;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.BrukerScanMode;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.TDFLib;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.TdfPressureCompensation;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.msms_profile_spectrum_function;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.msms_spectrum_function;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.msms_spectrum_function.Function;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.FramePrecursorTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;
import io.github.mzmine.util.StringUtils;
import io.mzio.general.Result;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfByte;
import java.lang.foreign.ValueLayout.OfInt;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.Alert.AlertType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.TDFLibrary;

/**
 * private static SymbolLookup getSymbolLookup() { try { return
 * SymbolLookup.libraryLookup(FileAndPathUtil.resolveInExternalToolsDir(
 * "bruker_tdf/%s".formatted(System.mapLibraryName("timsdata"))).toPath(), LIBRARY_ARENA)
 * .or(SymbolLookup.loaderLookup()).or(Linker.nativeLinker().defaultLookup()); } catch
 * (IllegalArgumentException e) { if(Platform.isMac()) { throw new RuntimeException("TDF is not
 * supported on MacOS.", e); } throw new RuntimeException("Failed to load TDF library.", e); } }
 */

/**
 * @author https://github.com/SteffenHeu
 */
public class TDFUtils implements AutoCloseable {

  public static final int SCAN_PACKAGE_SIZE = 5_000;
  public static final int BUFFER_SIZE_INCREMENT = 100_000; // 100 kb increase each time we fail
  private static final Logger logger = Logger.getLogger(TDFUtils.class.getName());
  private final NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private final Int2DoubleMap indexToMzBuffer = new Int2DoubleOpenHashMap();
  private final Int2IntMap indicesToIndexMap = new Int2IntOpenHashMap();
  private final Arena offHeap = Arena.ofAuto();
  public int BUFFER_SIZE = 300000; // start with 300 kb of buffer size
  private File file;
  private MemorySegment mzIndexDoubleBuffer = offHeap.allocate(0);
  private MemorySegment mobScanMzDoubleBuffer = offHeap.allocate(0);

  /**
   * the handle of the currently opened file
   **/
  private long handle = 0L;
  private OfInt INT_LITTLE_ENDIAN = TDFLib.C_INT.withOrder(ByteOrder.LITTLE_ENDIAN);
  private TdfPressureCompensation applyPressureComp = TdfPressureCompensation.NONE;

  public TDFUtils() {
  }

  public TDFUtils(TdfPressureCompensation pressureCompensation) {
    this();
    this.applyPressureComp = pressureCompensation;
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
  public long openFile(final File path, final int useRecalibratedState) {
    if (handle != 0L) {
      close();
    }

    file = path;

//    if (!loadLibrary() || tdfLib == null) {
//      logger.warning(() -> "File + " + path.getAbsolutePath() + " cannot be loaded because tdf "
//          + "library could not be initialised.");
//      return 0L;
//    }

    logger.finest(() -> "Opening tdf file " + path.getAbsolutePath());
    final String dirToOpen =
        path.isFile() ? path.getParentFile().getAbsolutePath() : path.getAbsolutePath() + '\0';

    // UTF8 required to load files from paths with special chars like ü
    // todo: check this is 0 terminated
    final byte[] fileBytes = Native.toByteArray(dirToOpen, StandardCharsets.UTF_8);
    handle = TDFLib.tims_open_v2(offHeap.allocateFrom(OfByte.JAVA_BYTE, fileBytes),
        useRecalibratedState, applyPressureComp.mode());

    if (handle == 0) {
      Result result = printLastError(0);
      if (result.message().contains("The minimum version (14.44.xx")) {
        DialogLoggerUtil.showDialog(AlertType.ERROR, "Outdated MSVC Runtime",
            "The MSVC runtime does not match the minimum required version 14.44.xx for the tdf import.\n%s".formatted(
                result.message()));
        result.throwOnError();
      } else {
        result.throwOnError();
      }
    }
    logger.finest(() -> "File " + path.getName() + " hasReacalibratedState = "
        + TDFLib.tims_has_recalibrated_state(handle));
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
  public long openFile(final File path) {
    return openFile(path, 1);
  }

  @Override
  public void close() {
    if (handle != 0L) {
      TDFLib.tims_close(handle);
    }
    handle = 0L;
    file = null;
  }

  /**
   * use {@link #loadDataPointsForFrame_v2(long, int, int)}
   *
   * @param frameId   The id of the frame. See {@link TDFFrameTable}
   * @param scanBegin The first scan index (starting with 0)
   * @param scanEnd   The last scan index
   * @return List of {@link SimpleSpectralArrays}, each represents the data points of one scan
   */
  @Deprecated
  public List<SimpleSpectralArrays> loadDataPointsForFrame(final long frameId, final int scanBegin,
      final int scanEnd) {
    if (handle == 0L) {
      throw new IllegalStateException("No tdf data file opened yet.");
    }

    final List<SimpleSpectralArrays> dataPoints = new ArrayList<>((int) (scanEnd - scanBegin));

    // buffer to store our scans. allocation takes time, so we want to reuse it
    // cannot be final, since we might have to increase the buffer size on the run
    // we don't just take a huge buffer, because clearing it takes time, too
    MemorySegment buffer = offHeap.allocate(BUFFER_SIZE, INT_LITTLE_ENDIAN.byteAlignment());

    // load scans in packs of SCAN_PACKAGE_SIZE to not cause a buffer overflow
    int start = scanBegin;
    while (start < scanEnd) {
      // start is inclusive, end is exclusive
      final int end = Math.min((start + SCAN_PACKAGE_SIZE), scanEnd);
      final int numScans = (int) (end - start);

//      synchronized (tdfLib) {
      final long lastError = TDFLib.tims_read_scans_v2(handle, frameId, start, end, buffer,
          (int) buffer.byteSize());
      if (lastError > BUFFER_SIZE) {
        BUFFER_SIZE = ((int) (lastError / BUFFER_SIZE_INCREMENT + 1)) * BUFFER_SIZE_INCREMENT;
        buffer = offHeap.allocate(BUFFER_SIZE, INT_LITTLE_ENDIAN.byteAlignment());
        continue;
      } else if (lastError == 0) {
        printLastError(lastError).throwOnError();
      }
//      }

      start = start + SCAN_PACKAGE_SIZE;

      // check out the layout of scanBuffer:
      // - the first numScan integers specify the number of peaks for each scan
      // - the next integers are pairs of (x,y) values for the scans. The x values are not masses
      // but index values
      int d = numScans;
      for (int i = 0; i < numScans; i++) {
        final int numPeaks = buffer.getAtIndex(INT_LITTLE_ENDIAN, i);
        final int[] indices = buffer.asSlice(d * 4L, 4L * numPeaks).toArray(INT_LITTLE_ENDIAN);
        d += numPeaks;
        final double[] intensities = ConversionUtils.convertIntsToDoubles(
            buffer.asSlice(d * 4L, 4L * numPeaks).toArray(INT_LITTLE_ENDIAN));
        d += numPeaks;

//        synchronized (tdfLib) {
        // todo - we should be able to pass the memory segment slice here
        final double[] masses = convertIndicesToMZ(handle, frameId, indices);
        dataPoints.add(new SimpleSpectralArrays(masses, intensities));
//        }
      }
      buffer.fill((byte) 0);
    }
    return dataPoints;
  }

  /**
   * Extracts mobility scans for the given range of scan numbers. Uses a caching functionality to be
   * faster.
   *
   * @param frameId   The id of the frame. See {@link TDFFrameTable}
   * @param scanBegin The first scan index (starting with 0)
   * @param scanEnd   The last scan index
   * @return List of {@link SimpleSpectralArrays}, each represents the data points of one scan
   */
  public List<SimpleSpectralArrays> loadDataPointsForFrame_v2(final long frameId,
      final int scanBegin, final int scanEnd) {
    if (handle == 0L) {
      throw new IllegalStateException("No tdf data file opened yet.");
    }
    // the buffer is only valid for one frame,
    // otherwise the index -> mz mapping may change due to temperature compensation
    indexToMzBuffer.clear();

    final List<SimpleSpectralArrays> dataPoints = new ArrayList<>((int) (scanEnd - scanBegin));

    // buffer to store our scans. allocation takes time, so we want to reuse it
    // cannot be final, since we might have to increase the buffer size on the run
    // we don't just take a huge buffer, because clearing it takes time, too
    MemorySegment buffer = offHeap.allocate(BUFFER_SIZE, INT_LITTLE_ENDIAN.byteAlignment());

    // load scans in packs of SCAN_PACKAGE_SIZE to not cause a buffer overflow
    int start = scanBegin;
    while (start < scanEnd) {
      // start is inclusive, end is exclusive
      final int end = Math.min((start + SCAN_PACKAGE_SIZE), scanEnd);
      final int numScans = (int) (end - start);

//      synchronized (tdfLib) {
      final long lastError = TDFLib.tims_read_scans_v2(handle, frameId, start, end, buffer,
          (int) buffer.byteSize());
      if (lastError > BUFFER_SIZE) {
        BUFFER_SIZE = ((int) (lastError / BUFFER_SIZE_INCREMENT + 1)) * BUFFER_SIZE_INCREMENT;
        buffer = offHeap.allocate(BUFFER_SIZE, INT_LITTLE_ENDIAN.byteAlignment());
        continue;
      } else if (lastError == 0) {
        printLastError(lastError).throwOnError();
      }
//      }

      start = start + SCAN_PACKAGE_SIZE;

      // check out the layout of scanBuffer:
      // - the first numScan integers specify the number of peaks for each scan
      // - the next integers are pairs of (x,y) values for the scans. The x values are not masses
      // but index values
      int d = numScans;
      for (int i = 0; i < numScans; i++) {
        final int numPeaks = buffer.getAtIndex(INT_LITTLE_ENDIAN, i);
//        final int[] indices = buffer.asSlice(d * 4L, 4L * numPeaks).toArray(INT_LITTLE_ENDIAN);
        final MemorySegment indices = buffer.asSlice(d * 4L, 4L * numPeaks);
        d += numPeaks;
        final double[] intensities = ConversionUtils.convertIntsToDoubles(
            buffer.asSlice(d * 4L, 4L * numPeaks).toArray(INT_LITTLE_ENDIAN));
        d += numPeaks;

//        synchronized (tdfLib) {
        // todo we should be able to pass the memory segment slice directly
        final double[] masses = convertIndicesToMZ_v3(handle, frameId, indices);
        dataPoints.add(new SimpleSpectralArrays(masses, intensities));
//        }
      }
      buffer.fill((byte) 0);
    }
    return dataPoints;
  }


  /**
   * Loads mobility resolved scans of a specific frame. Tested with scan modes 0 and 8 (MS1 and
   * PASEF-MS/MS)
   *
   * @param frame           The id of the frame. See {@link TDFFrameTable}
   * @param frameTable      The frame table
   * @param processorConfig import scan processor config
   * @return List of scans for the given frame id. Empty scans have been filtered out.
   */
  @NotNull
  public List<BuildingMobilityScan> loadSpectraForTIMSFrame(final SimpleFrame frame,
      @NotNull final TDFFrameTable frameTable,
      @NotNull final ScanImportProcessorConfig processorConfig) {
    final long frameId = frame.getFrameId();
    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();
    final List<BuildingMobilityScan> spectra = new ArrayList<>(numScans);

    final List<SimpleSpectralArrays> dataPoints = loadDataPointsForFrame_v2(frameId, 0, numScans);

    if (numScans != dataPoints.size()) {
      logger.warning(() -> "TDF file " + file.getName() + ": Number of scans for frame " + frameId
          + " in tdf (" + numScans + ") does not match number of loaded scans (" + dataPoints.size()
          + ").");
      return spectra;
    }

    for (int i = 0; i < dataPoints.size(); i++) {
      SimpleSpectralArrays data = dataPoints.get(i);
      if (processorConfig.hasProcessors()) {
        data = processorConfig.processor().processScan(frame, data);
        spectra.add(new BuildingMobilityScan(i, data.mzs(), data.intensities(),
            MassSpectrumType.CENTROIDED)); // tdf ims is always centroided
      }
    }

    return spectra;
  }

  // ---------------------------------------------------------------------------------------------
  // AVERAGE FRAMES
  // -----------------------------------------------------------------------------------------------
  private SimpleSpectralArrays extractCentroidsForFrame(final long frameId, final int startScanNum,
      final int endScanNum) {
    if (handle == 0L) {
      throw new IllegalStateException("No tdf data file opened yet.");
    }

    AtomicReference<double[]> mzs = new AtomicReference<>();
    AtomicReference<float[]> intensities = new AtomicReference<>();
    Function function = new Function() {
      @Override
      public void apply(long id, int num_peaks, MemorySegment mz_values, MemorySegment area_values,
          MemorySegment user_data) {
        try {
          MemorySegment mzSegment = mz_values.reinterpret(
              (long) num_peaks * JAVA_DOUBLE.byteSize());
          MemorySegment areaSegment = area_values.reinterpret(
              (long) num_peaks * JAVA_FLOAT.byteSize());
          mzs.set(mzSegment.toArray(JAVA_DOUBLE));
          intensities.set(areaSegment.toArray(JAVA_FLOAT));

        } catch (Throwable t) {
          logger.log(Level.WARNING, t,
              () -> "Error extracting centroid scan for frame " + frameId + " for scans "
                  + startScanNum + " to " + endScanNum + ".");
        }
      }
    };

    try (var arena = Arena.ofShared()) {
      MemorySegment callback = msms_spectrum_function.allocate(function, arena);
      final long error = TDFLib.tims_extract_centroided_spectrum_for_frame_v2(handle, frameId,
          startScanNum, endScanNum, callback, MemorySegment.NULL);

      if (error == 0) {
        logger.warning(() -> "Could not extract centroid scan for frame " + frameId + " for scans "
            + startScanNum + " to " + endScanNum + ".");
        printLastError(error).throwOnError();
        return SimpleSpectralArrays.EMPTY;
      }
    }

    return new SimpleSpectralArrays(mzs.get(),
        ConversionUtils.convertFloatsToDoubles(intensities.get()));
  }


  /**
   * @param frameId             the frame id.
   * @param metaDataTable       {@link TDFMetaDataTable} to construct the frame.
   * @param frameTable          {@link FramePrecursorTable} to construct the frame.
   * @param maldiFrameInfoTable Nullable for LC-IMS-MS. Required in case a maldi file is loaded.
   * @param scanProcessorConfig
   * @return The frame.
   */
  @Nullable
  public SimpleFrame extractCentroidScanForTimsFrame(IMSRawDataFile newFile, final long frameId,
      @NotNull final TDFMetaDataTable metaDataTable, @NotNull final TDFFrameTable frameTable,
      @NotNull final FramePrecursorTable framePrecursorTable,
      @Nullable final TDFMaldiFrameInfoTable maldiFrameInfoTable,
      final ScanImportProcessorConfig scanProcessorConfig) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();

    final float rt = (float) (frameTable.getTimeColumn().get(frameIndex) / 60); // to minutes
    final PolarityType polarity = PolarityType.fromSingleChar(
        (String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));
    final int msLevel = getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    final String scanDefinition =
        metaDataTable.getInstrumentType() + " - " + BrukerScanMode.fromScanMode(
            frameTable.getScanModeColumn().get(frameIndex).intValue()) + " Frame #" + frameId
            + " RT: " + rtFormat.format(rt);
    final float accumulationTime = frameTable.getAccumulationTimeColumn().get(frameIndex)
        .floatValue();

    Range<Double> mzRange = metaDataTable.getMzRange();

    SimpleFrame frame;
    if (maldiFrameInfoTable == null || maldiFrameInfoTable.getFrameIdColumn().isEmpty()) {
      // regular frame
      frame = new SimpleFrame(newFile, Math.toIntExact(frameId), msLevel, rt, null, null,
          MassSpectrumType.CENTROIDED, polarity, scanDefinition, mzRange, MobilityType.TIMS, null,
          accumulationTime);
    } else {
      // IMAGING
      frame = new SimpleImagingFrame(newFile, Math.toIntExact(frameId), msLevel, rt, null, null,
          MassSpectrumType.CENTROIDED, polarity,
          scanDefinition + " " + maldiFrameInfoTable.getSpotNameColumn().get(frameIndex), mzRange,
          MobilityType.TIMS, null, accumulationTime);
      Coordinates coords = new Coordinates(maldiFrameInfoTable.getTransformedXIndexPos(frameIndex),
          maldiFrameInfoTable.getTransformedYIndexPos(frameIndex), 0);
      ((SimpleImagingFrame) frame).setCoordinates(coords);
    }

    // filters do not contain this frame
    if (!scanProcessorConfig.scanFilter().matches(frame)) {
      return null;
    }

    // load data after filters applied
    SimpleSpectralArrays data = extractCentroidsForFrame(frameId, 0, numScans);

    // process data?
    if (scanProcessorConfig.hasProcessors()) {
      data = scanProcessorConfig.processor().processScan(frame, data);
    }
    // finally set data and mobilities
    frame.setDataPoints(data.mzs(), data.intensities());

    final double[] mobilities = convertScanNumsToOneOverK0(handle, frameId,
        createPopulatedArrayFrom1(numScans));
    frame.setMobilities(mobilities);

    return frame;
  }

  @Nullable
  public int[] extractProfileForFrame(final long frameId, final int startScanNum,
      final int endScanNum) {

    final AtomicReference<int[]> intensities = new AtomicReference<>();
    var function = new msms_profile_spectrum_function.Function() {
      @Override
      public void apply(long id, int num_points, MemorySegment intensity_values,
          MemorySegment user_data) {
        MemorySegment intensitySegment = intensity_values.reinterpret(
            num_points * JAVA_DOUBLE.byteSize());
        intensities.set(intensitySegment.toArray(JAVA_INT));
      }
    };

    try (var arena = Arena.ofShared()) {
      MemorySegment callback = msms_profile_spectrum_function.allocate(function, arena);
      int error = TDFLib.tims_extract_profile_for_frame(handle, frameId, startScanNum, endScanNum,
          callback, MemorySegment.NULL);
      if (error == 0) {
        logger.warning(() -> "Could not extract profile for frame " + frameId + ".");
        printLastError(error).throwOnError();
        return null;
      }
    }
    return intensities.get();
  }

  /**
   * @return A pseudo profile spectrum
   */
  public SimpleFrame extractProfileScanForFrame(IMSRawDataFile newFile, final long frameId,
      @NotNull final TDFMetaDataTable metaDataTable, @NotNull final TDFFrameTable frameTable,
      @NotNull final FramePrecursorTable framePrecursorTable,
      @Nullable final TDFMaldiFrameInfoTable maldiFrameInfoTable,
      @NotNull final ScanImportProcessorConfig scanProcessorConfig) {

    final int frameIndex = frameTable.getFrameIdColumn().indexOf(frameId);
    final int numScans = frameTable.getNumScansColumn().get(frameIndex).intValue();

    final float rt = (float) (frameTable.getTimeColumn().get(frameIndex) / 60); // to minutes
    final PolarityType polarity = PolarityType.fromSingleChar(
        (String) frameTable.getColumn(TDFFrameTable.POLARITY).get(frameIndex));
    final int msLevel = getMZmineMsLevelFromBrukerMsMsType(
        frameTable.getMsMsTypeColumn().get(frameIndex).intValue());
    final String scanDefinition =
        metaDataTable.getInstrumentType() + " - " + BrukerScanMode.fromScanMode(
            frameTable.getScanModeColumn().get(frameIndex).intValue()) + " Frame #" + frameId
            + " RT: " + rtFormat.format(rt);
    final Range<Double> mzRange = metaDataTable.getMzRange();
    final float accumulationTime = frameTable.getAccumulationTimeColumn().get(frameIndex)
        .floatValue();

    SimpleFrame frame;
    if (maldiFrameInfoTable == null || maldiFrameInfoTable.getFrameIdColumn().isEmpty()) {
      frame = new SimpleFrame(newFile, Math.toIntExact(frameId), msLevel, rt, null, null,
          MassSpectrumType.PROFILE, polarity, scanDefinition, mzRange, MobilityType.TIMS, null,
          accumulationTime);
    } else {
      frame = new SimpleImagingFrame(newFile, Math.toIntExact(frameId), msLevel, rt, null, null,
          MassSpectrumType.PROFILE, polarity, scanDefinition, mzRange, MobilityType.TIMS, null,
          accumulationTime);
      Coordinates coords = new Coordinates(maldiFrameInfoTable.getTransformedXIndexPos(frameIndex),
          maldiFrameInfoTable.getTransformedYIndexPos(frameIndex), 0);
      ((SimpleImagingFrame) frame).setCoordinates(coords);
    }

    // filters do not contain this frame
    if (!scanProcessorConfig.scanFilter().matches(frame)) {
      return null;
    }

    // load data and process
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

    // load data after filters applied
    final double[] profileMzs = convertIndicesToMZ(handle, frameId, filteredMzIndices.toIntArray());

    var data = new SimpleSpectralArrays(profileMzs, filteredIntensities.toDoubleArray());

    // process data?
    if (scanProcessorConfig.hasProcessors()) {
      data = scanProcessorConfig.processor().processScan(frame, data);

      if (scanProcessorConfig.isMassDetectActive(frame.getMSLevel())) {
        frame.setSpectrumType(MassSpectrumType.CENTROIDED);
      }
    }
    // finally set data and mobilities
    frame.setDataPoints(data.mzs(), data.intensities());

    final double[] mobilities = convertScanNumsToOneOverK0(handle, frameId,
        createPopulatedArrayFrom1(numScans));
    frame.setMobilities(mobilities);

    return frame;
  }

  // ---------------------------------------------------------------------------------------------
  // CONVERSION FUNCTIONS
  // -----------------------------------------------------------------------------------------------
  private double[] convertIndicesToMZ(final long handle, final long frameId, final int[] indices) {

    final var buffer = offHeap.allocate(JAVA_DOUBLE, indices.length);

    final long error = TDFLib.tims_index_to_mz(handle, frameId,
        offHeap.allocateFrom(TDFLib.C_DOUBLE, Arrays.stream(indices).asDoubleStream().toArray()),
        buffer, indices.length);
    if (error == 0) {
      printLastError(error).throwOnError();
      logger.warning(() -> "Could not convert indices to mzs for frame " + frameId);
    }
    return buffer.toArray(JAVA_DOUBLE);
  }

  /**
   * Converts extracted indices to mz values while employing a cache to limit the number and size of
   * API calls. Indices may only belong to a single frame. This method uses caching to convert
   * indices faster.
   */
  private double[] convertIndicesToMZ_v2(final long handle, final long frameId,
      final int[] indices) {

    DoubleList unknownIndices = null;
    final double[] mzs = new double[indices.length];

    indicesToIndexMap.clear();

    for (int i = 0; i < indices.length; i++) {
      final double mz = indexToMzBuffer.get(indices[i]);
      if (mz != 0) {
        mzs[i] = mz;
      } else {
        if (unknownIndices == null) {
          unknownIndices = new DoubleArrayList(indices.length / 2);
        }
        indicesToIndexMap.put(indices[i], i);
        unknownIndices.add(indices[i]);
      }
    }

    if (unknownIndices != null) {
      unknownIndices.toDoubleArray();

      final MemorySegment buffer = offHeap.allocate(JAVA_DOUBLE, unknownIndices.size());
      final long error = TDFLib.tims_index_to_mz(handle, frameId,
          offHeap.allocateFrom(JAVA_DOUBLE, unknownIndices.toDoubleArray()), buffer,
          unknownIndices.size());

      if (error == 0) {
        logger.warning(() -> "Could not convert indices to mzs for frame " + frameId);
        printLastError(error).throwOnError();
      }

      // index in the newly converted mz buffer
      for (int i = 0; i < unknownIndices.size(); i++) {
        final int peakIndex = (int) unknownIndices.getDouble(i);
        indexToMzBuffer.put(peakIndex, buffer.getAtIndex(JAVA_DOUBLE, i));
        mzs[indicesToIndexMap.get(peakIndex)] = buffer.getAtIndex(JAVA_DOUBLE, i);
      }
    }

    Arrays.sort(mzs);
    return mzs;
  }

  private double[] convertIndicesToMZ_v3(final long handle, final long frameId,
      final MemorySegment intIndices) {
    // reusing the buffers saves ~4s on a file with 3000 frames and 1000 mobility scans

    if (intIndices.byteSize() > mzIndexDoubleBuffer.byteSize() / 2) {
      // ensure buffer capacity
      mzIndexDoubleBuffer = offHeap.allocate(JAVA_DOUBLE,
          intIndices.byteSize() / JAVA_INT.byteSize());
      mobScanMzDoubleBuffer = offHeap.allocate(JAVA_DOUBLE,
          intIndices.byteSize() / JAVA_INT.byteSize());
    }
    for (int i = 0; i < intIndices.byteSize() / JAVA_INT.byteSize(); i++) {
      mzIndexDoubleBuffer.setAtIndex(JAVA_DOUBLE, i, intIndices.getAtIndex(JAVA_INT, i));
    }

    final long error = TDFLib.tims_index_to_mz(handle, frameId, mzIndexDoubleBuffer,
        mobScanMzDoubleBuffer, (int) (intIndices.byteSize() / JAVA_INT.byteSize()));
    if (error == 0) {
      printLastError(error).throwOnError();
      logger.warning(() -> "Could not convert indices to mzs for frame " + frameId);
    }
    return mobScanMzDoubleBuffer.asSlice(0, intIndices.byteSize() * 2).toArray(JAVA_DOUBLE);
  }

  // ---------------------------------------------------------------------------------------------
  // SQL-RELATED FUNCTIONS
  // -----------------------------------------------------------------------------------------------

  public double[] convertScanNumsToOneOverK0(final long handle, final long frameId,
      final int[] scanNums) {
    MemorySegment buffer = offHeap.allocate(JAVA_DOUBLE, scanNums.length);

    long error = TDFLib.tims_scannum_to_oneoverk0(handle, frameId,
        offHeap.allocateFrom(TDFLib.C_DOUBLE, Arrays.stream(scanNums).asDoubleStream().toArray()),
        buffer, scanNums.length);
    if (error == 0) {
      logger.warning(() -> "Could not convert scan nums to 1/K0 for frame " + frameId);
      printLastError(error).throwOnError();
    }
    return buffer.toArray(JAVA_DOUBLE);
  }

  // ---------------------------------------------------------------------------------------------
  // UTILITY FUNCTIONS
  // -----------------------------------------------------------------------------------------------

  public Float calculateCCS(double ook0, int charge, double mz) {
    try {
      return (float) TDFLib.tims_oneoverk0_to_ccs_for_mz(ook0, charge, mz);
    } catch (Exception e) {
      return null;
    }
  }

  public Float calculateOok0(double ccs, int charge, double mz) {
    try {
      return (float) TDFLib.tims_ccs_to_oneoverk0_for_mz(ccs, charge, mz);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * @param errorCode return value of tims library methods
   * @return true if an error occurred
   */
  private Result printLastError(long errorCode) {
    if (errorCode == 0 || errorCode > BUFFER_SIZE) {
      final MemorySegment errorBuffer = offHeap.allocate(OfByte.JAVA_BYTE, 256);
      long len = TDFLib.tims_get_last_error_string(errorBuffer, (int) errorBuffer.byteSize());
      try {
        final String errorMessage = new String(errorBuffer.toArray(ValueLayout.JAVA_BYTE),
            "UTF-8").substring(0, (int) len - 1);
//        logger.fine(() -> "Last TDF import error: " + errorMessage + " length: " + len
//            + ". Required buffer size: " + errorCode + " actual size: " + BUFFER_SIZE);
        if (errorMessage.contains("CorruptFrameDataError")) {
          return Result.warning("Error reading tdf raw data. " + errorMessage);
        } else if (errorMessage.contains("no error")) {
          return Result.ok();
        } else if (errorMessage.contains("Invalid UTF-8 code unit")) {
          Pattern indexPattern = Pattern.compile("[.*]?+unit at index ([\\d]+):");
          Matcher matcher = indexPattern.matcher(errorMessage);
          if (matcher.find()) {
            String group = matcher.group(1);
            int index = Integer.parseInt(group);
            char invalid = file.getAbsolutePath().charAt(index);
            return Result.error(
                "Error while importing TDF file %s. %s. Invalid character is %s".formatted(
                    file.getAbsolutePath(), errorMessage,
                    StringUtils.inQuotes(String.valueOf(invalid))));
          }
          return Result.error(
              "Error while importing TDF file %s. %s".formatted(file.getAbsolutePath(),
                  errorMessage));
        } else {
          return Result.error(
              "Error while importing TDF file %s. %s".formatted(file.getAbsolutePath(),
                  errorMessage));
        }
      } catch (UnsupportedEncodingException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }
    }

    // error returns 0
    return Result.ok();
  }

  private void setNumThreads(int numThreads) {
    try {
      if (numThreads >= 1) {
//      logger.finest(() -> "Setting number of threads per file to " + numThreads);
        TDFLib.tims_set_num_threads(numThreads);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }
}
