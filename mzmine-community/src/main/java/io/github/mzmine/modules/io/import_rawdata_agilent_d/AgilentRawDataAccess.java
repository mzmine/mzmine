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

package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Read access to one Agilent {@code .d} file for {@link AgilentImportTask}, implemented by the
 * native AgilentReader ({@link AgilentDataAccess}) and the mzio reader (mzmine pro). Frames are
 * 1-based (1..{@link #getFrameCount()}), scans 0-based (0..{@link #getScanCount()}-1).
 */
public interface AgilentRawDataAccess extends AutoCloseable {

  boolean isIms();

  int getFrameCount();

  long getScanCount();

  /**
   * Whether the file has any non-MRM MS scans to import as spectra.
   */
  boolean hasMsScans();

  /**
   * Whether the file has MRM/SRM transitions (imported as other time series, not spectra).
   */
  boolean hasMrm();

  @NotNull RawDataFileImpl createDataFile();

  @Nullable CCSCalibration getCcsCalibration();

  @Nullable LocalDateTime getAcqDate();

  void applyToFileMetadata(@NotNull ModularDataModel metadata);

  /**
   * @return the frame or null if filtered out by the scan selection
   */
  @Nullable SimpleFrame readFrame(@NotNull IMSRawDataFileImpl file, int frameId);

  /**
   * @return the scan or null if filtered out by the scan selection or an MRM reading
   */
  @Nullable SimpleScan readScan(@NotNull RawDataFileImpl file, int id);

  void readMrms(@NotNull RawDataFileImpl dataFile);

  void readAnalogChannels(@NotNull RawDataFileImpl dataFile);

  @Override
  void close();
}
