/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.baf2sql;


import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.baf2sql.BafLib.baf2sql_array_close_storage;
import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.baf2sql.BafLib.baf2sql_array_get_num_elements;
import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.baf2sql.BafLib.baf2sql_array_open_storage;
import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.baf2sql.BafLib.baf2sql_array_read_double;
import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.baf2sql.BafLib.baf2sql_get_last_error_string;
import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.baf2sql.BafLib.baf2sql_get_sqlite_cache_filename_v2;

import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.BafPropertiesTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.Ms2Table;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.SpectraAcquisitionStepsTable;
import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.JDBC;

public class BafDataAccess {

  private static final Logger logger = Logger.getLogger(BafDataAccess.class.getName());
  private long handle;
  private final SpectraAcquisitionStepsTable spectraTable = new SpectraAcquisitionStepsTable();
  private final BafPropertiesTable metadata = new BafPropertiesTable();
  private final Ms2Table ms2Table = new Ms2Table();

  private final Arena arena;
  private final MemorySegment mzSizeBuffer;
  private final MemorySegment intensitySizeBuffer;

  private MemorySegment mzBufferSegment;
  private MemorySegment intensityBufferSegment;

  public BafDataAccess(Arena arena) {
    this.arena = arena;
    mzSizeBuffer = arena.allocate(BafLib.uint64_t);
    intensitySizeBuffer = arena.allocate(BafLib.uint64_t);
    mzBufferSegment = arena.allocate(0);
    intensityBufferSegment = arena.allocate(0);
  }

  public String getLastErrorString() {
    int length = baf2sql_get_last_error_string(MemorySegment.NULL, 0);

    final MemorySegment buffer = arena.allocate(length);
    baf2sql_get_last_error_string(buffer, (int) buffer.byteSize());
    return String.valueOf(buffer.getString(0, StandardCharsets.UTF_8));
  }

  /**
   * Creates the sqlite cache file or returns the path to it.
   *
   * @param folderOrBaf
   * @return
   */
  @Nullable
  public File getSqliteCacheFile(@NotNull final File folderOrBaf) {
    final File bafFile = getBafFromFolderOrFile(folderOrBaf);
    if (bafFile == null) {
      return null;
    }

    final MemorySegment inputBuffer = arena.allocateFrom(bafFile.getAbsolutePath().toString());
    logger.fine("Baf path in utf8: " + inputBuffer.getString(0, StandardCharsets.UTF_8));

    int errorCodeOrPathLength = baf2sql_get_sqlite_cache_filename_v2(MemorySegment.NULL, 0,
        inputBuffer, 1);
    if (errorCodeOrPathLength == 0) {
      logger.severe(getLastErrorString());
      return null;
    }

    final MemorySegment outputBuffer = arena.allocate(errorCodeOrPathLength);

    errorCodeOrPathLength = baf2sql_get_sqlite_cache_filename_v2(outputBuffer,
        errorCodeOrPathLength, inputBuffer, 1);
    if (errorCodeOrPathLength == 0) {
      logger.severe(getLastErrorString());
      return null;
    }

    return new File(String.valueOf(outputBuffer.getString(0, StandardCharsets.UTF_8)));
  }

  public static @Nullable File getBafFromFolderOrFile(@NotNull File folderOrBaf) {
    if (!folderOrBaf.exists() || !folderOrBaf.canRead()) {
      logger.info(
          () -> "Baf file with path %s does not exist.".formatted(folderOrBaf.getAbsolutePath()));
      return null;
    }

    final File bafFile;
    if (folderOrBaf.isDirectory() && folderOrBaf.getName().endsWith(".d")) {
      logger.fine("Folder path detected, defaulting to analysis.baf file.");
      bafFile = new File(folderOrBaf, "analysis.baf");
    } else {
      bafFile = folderOrBaf;
    }

    if (!bafFile.getName().endsWith(".baf") || !bafFile.exists() || !bafFile.canRead()) {
      logger.info(
          () -> "Baf file with path %s does not exist.".formatted(bafFile.getAbsolutePath()));
      return null;
    }
    return bafFile;
  }

  public boolean openBafFile(File path) {
    final File baf = getBafFromFolderOrFile(path);
    if (baf == null) {
      return false;
    }
    final File cache = getSqliteCacheFile(baf);
    if (cache == null || !cache.exists()) {
      return false;
    }

    var pathBuffer = arena.allocateFrom(baf.getAbsolutePath());
    handle = baf2sql_array_open_storage(0, pathBuffer);
    if (handle == 0) {
      logger.severe("Error opening baf file: %s".formatted(getLastErrorString()));
    }

    logger.finest(() -> "Initialising SQL...");
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      logger.log(Level.SEVERE, "Could not load sqlite.JDBC.", e);
      return false;
    }
    logger.finest(() -> "SQL initialised.");

    logger.finest(() -> "Establishing SQL connection to " + baf.getName());

    synchronized (JDBC.class) {
      try (Connection connection = DriverManager.getConnection(
          "jdbc:sqlite:" + cache.getAbsolutePath())) {
        logger.finest(() -> "Connection established. " + connection.toString());
        spectraTable.executeQuery(connection);
        metadata.executeQuery(connection);
        ms2Table.executeQuery(connection);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    return handle != 0;
  }

  public boolean valid() {
    return handle != 0;
  }

  public SimpleSpectralArrays loadPeakData(int index) {
    assert valid();

    final long mzIds = spectraTable.getMzIds(index);
    final long intensityIds = spectraTable.getIntensityIds(index);

    long success = baf2sql_array_get_num_elements(handle, mzIds, mzSizeBuffer);
    if (success == 0) {
      logger.info(getLastErrorString());
      return null;
    }
    success = baf2sql_array_get_num_elements(handle, intensityIds, intensitySizeBuffer);
    if (success == 0) {
      logger.info(getLastErrorString());
      return null;
    }

    final long numMzs = mzSizeBuffer.get(BafLib.uint64_t, 0);
    final long numIntensities = intensitySizeBuffer.get(BafLib.uint64_t, 0);

    assert numMzs == numIntensities;

    if (numMzs * Double.BYTES > mzBufferSegment.byteSize()) {
      mzBufferSegment = arena.allocate(numMzs * Double.BYTES);
      intensityBufferSegment = arena.allocate(numIntensities * Double.BYTES);
    }

    int error = baf2sql_array_read_double(handle, mzIds, mzBufferSegment);
    if (error == 0) {
      logger.info(getLastErrorString());
      return null;
    }

    error = baf2sql_array_read_double(handle, intensityIds, intensityBufferSegment);
    if (error == 0) {
      logger.info(getLastErrorString());
      return null;
    }

    double[] mzs = mzBufferSegment.asSlice(0, numMzs * Double.BYTES)
        .toArray(ValueLayout.JAVA_DOUBLE);
    double[] intensities = intensityBufferSegment.asSlice(0, numIntensities * Double.BYTES)
        .toArray(ValueLayout.JAVA_DOUBLE);

    return new SimpleSpectralArrays(mzs, intensities);
  }

  public SpectraAcquisitionStepsTable getSpectraTable() {
    return spectraTable;
  }

  public BafPropertiesTable getMetadata() {
    return metadata;
  }

  public void closeHandle() {
    if (handle == 0L) {
      return;
    }
    baf2sql_array_close_storage(handle);
  }

  public Ms2Table getMs2Table() {
    return ms2Table;
  }
}
