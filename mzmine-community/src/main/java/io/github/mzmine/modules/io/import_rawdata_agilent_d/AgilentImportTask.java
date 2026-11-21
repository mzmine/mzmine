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

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileUtils;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports a single Agilent {@code .d} file (regular MS or ion mobility) through an
 * {@link AgilentRawDataAccess}: by default the AgilentBridge subprocess
 * ({@link AgilentDataAccess}), the mzio reader in mzmine pro. Modeled on
 * {@code MassLynxImportTask}. One task imports exactly one file.
 */
public class AgilentImportTask extends AbstractTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(AgilentImportTask.class.getName());

  private final File rawFile;
  @NotNull
  private final Class<? extends MZmineModule> module;
  @NotNull
  private final ParameterSet parameters;
  @NotNull
  private final MZmineProject project;
  @Nullable
  private final ScanImportProcessorConfig processor;
  @NotNull
  private final AgilentRawDataAccessFactory dataAccessFactory;

  private long totalItems = 0;
  private long loadedItems = 0;
  private RawDataFileImpl dataFile;

  public AgilentImportTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      File rawFile, @NotNull Class<? extends MZmineModule> module, @NotNull ParameterSet parameters,
      @NotNull MZmineProject project, @Nullable ScanImportProcessorConfig processor) {
    this(storage, moduleCallDate, rawFile, module, parameters, project, processor,
        AgilentDataAccess::new);
  }

  /**
   * @param dataAccessFactory opens the file, e.g. with the native AgilentReader or the mzio reader
   */
  public AgilentImportTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      File rawFile, @NotNull Class<? extends MZmineModule> module, @NotNull ParameterSet parameters,
      @NotNull MZmineProject project, @Nullable ScanImportProcessorConfig processor,
      @NotNull AgilentRawDataAccessFactory dataAccessFactory) {
    super(storage, moduleCallDate);
    this.rawFile = rawFile;
    this.module = module;
    this.parameters = parameters;
    this.project = project;
    this.processor = processor;
    this.dataAccessFactory = dataAccessFactory;
  }

  @Override
  public String getTaskDescription() {
    return "Importing Agilent raw data file %s. %d/%d".formatted(rawFile.getName(), loadedItems,
        totalItems);
  }

  @Override
  public double getFinishedPercentage() {
    return totalItems == 0 ? 0 : loadedItems / (double) totalItems;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    final VendorImportParameters vendorParameters = parameters.getValue(
        AllSpectralDataImportParameters.vendorOptions);

    try (final AgilentRawDataAccess da = dataAccessFactory.open(rawFile, vendorParameters, storage,
        processor)) {

      dataFile = da.createDataFile();
      dataFile.setStartTimeStamp(da.getAcqDate());
      da.applyToFileMetadata(dataFile.getFileMetadata());
      final boolean readScans = da.isIms() || da.hasMsScans();
      totalItems = !readScans ? 0 : (da.isIms() ? da.getFrameCount() : da.getScanCount());

      final List<SimpleScan> scans = new ArrayList<>();
      if (da.isIms()) {
        final IMSRawDataFileImpl imsFile = (IMSRawDataFileImpl) dataFile;
        for (int frame = 1; frame <= da.getFrameCount(); frame++) {
          if (isCanceled()) {
            return;
          }
          final SimpleFrame loadedFrame = da.readFrame(imsFile, frame);
          if (loadedFrame != null) {
            scans.add(loadedFrame);
          }
          loadedItems++;
        }
      } else {
        // MRM-only files have no MS spectra to import — they are read below as OtherTimeSeries
        for (int id = 0; readScans && id < da.getScanCount(); id++) {
          if (isCanceled()) {
            return;
          }
          final SimpleScan loadedScan = da.readScan(dataFile, id);
          if (loadedScan != null) {
            scans.add(loadedScan);
          }
          loadedItems++;
        }
      }

      // non-MS data: MRM/SRM transitions (as OtherTimeSeries) and analog (DAD/UV, pump, curve)
      // channels. These live in the device directories regardless of MS mode, so they are read for
      // IMS files too (the bridge serves them via an MHDAC sidecar over the same file).
      if (isCanceled()) {
        return;
      }
      if (da.hasMrm()) {
        da.readMrms(dataFile);
      }
      da.readAnalogChannels(dataFile);

      if (dataFile instanceof IMSRawDataFile imsFile) {
        final CCSCalibration ccs = da.getCcsCalibration();
        if (ccs != null) {
          imsFile.setCCSCalibration(ccs);
        }
      }

      final List<SimpleScan> sortedScans = scans.stream().sorted(
              Comparator.comparingDouble(Scan::getRetentionTime).thenComparing(Scan::getScanDefinition))
          .toList();
      for (int i = 0; i < sortedScans.size(); i++) {
        final SimpleScan scan = sortedScans.get(i);
        scan.setScanNumber(i + 1);
        dataFile.addScan(scan);
      }

      dataFile.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
      // acquisition m/z + RT range and file format, after all scans were added
      RawDataFileUtils.addAdditionalFileMetadata(dataFile);

      if (isCanceled()) {
        return;
      }

      project.addFile(dataFile);
      setStatus(TaskStatus.FINISHED);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error while reading Agilent raw file " + rawFile.getAbsolutePath(),
          e);
      error(e.getMessage());
    }
  }

  @Override
  public @NotNull List<RawDataFile> getImportedRawDataFiles() {
    if (!isFinished()) {
      return List.of();
    }
    return List.of(dataFile);
  }
}
