/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_chemstation;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.builders.SimpleBuildingScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_chemstation.ChemStationMsParser.ChemStationScan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Imports legacy Agilent ChemStation GC-MS {@code .D/DATA.MS} folders directly into MZmine. */
public final class ChemStationImportTask extends AbstractTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(ChemStationImportTask.class.getName());

  private final MZmineProject project;
  private final File folder;
  private final ScanImportProcessorConfig scanProcessorConfig;
  private final Class<? extends MZmineModule> module;
  private final ParameterSet parameters;
  private final RawDataFileImpl rawDataFile;
  private volatile double progress;
  private int importedScans;

  public ChemStationImportTask(@NotNull MZmineProject project, @NotNull File folder,
      @NotNull ScanImportProcessorConfig scanProcessorConfig,
      @NotNull Class<? extends MZmineModule> module, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable MemoryMapStorage storage) {
    super(storage, moduleCallDate);
    this.project = project;
    this.folder = folder;
    this.scanProcessorConfig = scanProcessorConfig;
    this.module = module;
    this.parameters = parameters;
    rawDataFile = new RawDataFileImpl(folder.getName(), folder.getAbsolutePath(), storage);
  }

  @Override
  public String getTaskDescription() {
    return "Importing Agilent ChemStation data " + folder.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Started parsing ChemStation folder " + folder);
    ChemStationImportLog.write("IMPORT_START", folder,
        "processor=" + scanProcessorConfig);

    try (ChemStationMsParser parser = new ChemStationMsParser(folder)) {
      ChemStationImportLog.write("PARSER_OPENED", folder,
          "declared_scans=" + parser.getTotalScans());
      ChemStationScan sourceScan;
      int sourceScanNumber = 0;
      while (!isCanceled() && (sourceScan = parser.readNextScan()) != null) {
        sourceScanNumber++;
        progress = parser.getFinishedPercentage();

        final SimpleBuildingScan metadata = new SimpleBuildingScan(sourceScanNumber, 1,
            PolarityType.UNKNOWN, MassSpectrumType.CENTROIDED, sourceScan.retentionTime(), 0d, 0);
        metadata.scanId = "Agilent ChemStation GC-MS scan " + sourceScanNumber;
        if (!scanProcessorConfig.scanFilter().matches(metadata)) {
          continue;
        }

        final SimpleSpectralArrays processed = scanProcessorConfig.processor()
            .processScan(metadata,
                new SimpleSpectralArrays(sourceScan.mzValues(), sourceScan.intensityValues()));
        final SimpleScan scan = new SimpleScan(rawDataFile, sourceScanNumber, 1,
            sourceScan.retentionTime(), null, processed.mzs(), processed.intensities(),
            MassSpectrumType.CENTROIDED, PolarityType.UNKNOWN, metadata.scanId, null);
        if (scanProcessorConfig.isMassDetectActive(1)) {
          scan.addMassList(new ScanPointerMassList(scan));
        }
        rawDataFile.addScan(scan);
        importedScans++;
      }

      if (isCanceled()) {
        ChemStationImportLog.write("IMPORT_CANCELED", folder,
            "read_scans=" + parser.getReadScans() + " imported_scans=" + importedScans);
        return;
      }
      if (importedScans == 0) {
        final String message = "No ChemStation GC-MS scans matched the import settings in "
            + folder;
        ChemStationImportLog.write("IMPORT_NO_SCANS", folder,
            "read_scans=" + parser.getReadScans() + " declared_scans="
                + parser.getTotalScans());
        setErrorMessage(message + ". See the mzmine log for the detailed ChemStation import trace.");
        setStatus(TaskStatus.ERROR);
        return;
      }

      rawDataFile.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
      project.addFile(rawDataFile);
      progress = 1d;
      logger.info("Finished parsing %s, imported %d scans".formatted(folder, importedScans));
      ChemStationImportLog.write("IMPORT_SUCCESS", folder,
          "read_scans=" + parser.getReadScans() + " imported_scans=" + importedScans
              + " declared_scans=" + parser.getTotalScans());
      setStatus(TaskStatus.FINISHED);
    } catch (Throwable error) {
      logger.log(Level.WARNING, "Cannot import ChemStation folder " + folder, error);
      ChemStationImportLog.write("IMPORT_ERROR", folder,
          "imported_scans=" + importedScans, error);
      if (getStatus() == TaskStatus.PROCESSING) {
        setErrorMessage(ExceptionUtils.exceptionToString(error) + System.lineSeparator()
            + "See the mzmine log for the detailed ChemStation import trace.");
        setStatus(TaskStatus.ERROR);
      }
    }
  }

  @Override
  public @NotNull List<RawDataFile> getImportedRawDataFiles() {
    return getStatus() == TaskStatus.FINISHED ? List.of(rawDataFile) : List.of();
  }
}
