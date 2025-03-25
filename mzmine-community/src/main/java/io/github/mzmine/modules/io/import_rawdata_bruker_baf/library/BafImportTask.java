/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_bruker_baf.library;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.builders.SimpleBuildingScan;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.baf2sql.BafDataAccess;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.BafPropertiesTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.BafPropertiesTable.Values;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.Ms2Table;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.SpectraAcquisitionStepsTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_uv.BrukerUvReader;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BafImportTask extends AbstractTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(BafImportTask.class.getName());

  private final File bafFileOrFolder;
  private final Class<? extends MZmineModule> callingModule;
  private final ParameterSet parameters;
  private final MZmineProject project;
  private final ScanImportProcessorConfig scanProcessorConfig;
  int totalScans = 0;
  int importedScans = 0;
  private NumberFormats formats = ConfigService.getGuiFormats();
  private RawDataFileImpl file;

  public BafImportTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      final File bafFileOrFolder, @NotNull final Class<? extends MZmineModule> callingModule,
      @NotNull final ParameterSet parameters, MZmineProject project,
      @NotNull ScanImportProcessorConfig scanProcessorConfig) {
    super(storage, moduleCallDate);
    this.bafFileOrFolder = bafFileOrFolder;
    this.callingModule = callingModule;
    this.parameters = parameters;
    this.project = project;

    this.scanProcessorConfig = scanProcessorConfig;
  }

  @Override
  public String getTaskDescription() {
    return "Importing file " + bafFileOrFolder.getName() + " - " + importedScans + "/" + totalScans;
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans != 0 ? (double) importedScans / (double) totalScans : 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final File folderPath =
        bafFileOrFolder.isDirectory() ? bafFileOrFolder : bafFileOrFolder.getParentFile();
    file = new RawDataFileImpl(folderPath.getName(),
        folderPath.getAbsolutePath(), getMemoryMapStorage());

    try (BafDataAccess baf = new BafDataAccess()) {

      final boolean b = baf.openBafFile(folderPath);
      if (!b) {
        setErrorMessage(baf.getLastErrorString());
        setStatus(TaskStatus.CANCELED);
        return;
      }

      final SpectraAcquisitionStepsTable scanTable = baf.getSpectraTable();
      final BafPropertiesTable metadata = baf.getMetadata();
      final Ms2Table ms2Table = baf.getMs2Table();

      totalScans = scanTable.getNumberOfScans();
      for (int i = 0; i < scanTable.getNumberOfScans(); i++) {
        final int id = scanTable.getId(i);

        final MsMsInfo msMsInfo = ms2Table.getMsMsInfo(id);
        final SimpleBuildingScan metadataScan = new SimpleBuildingScan(id, scanTable.getMsLevel(i),
            scanTable.getPolarity(i), scanTable.getSpectrumType(), scanTable.getRt(i), 0d, 0);

        if (scanProcessorConfig.scanFilter().matches(metadataScan)) {
          final SimpleSpectralArrays mzIntensities = baf.loadPeakData(i);
          final SimpleSpectralArrays arrays = scanProcessorConfig.processor()
              .processScan(metadataScan,
                  new SimpleSpectralArrays(mzIntensities.mzs(), mzIntensities.intensities()));

          final MassSpectrumType spectrumType =
              metadataScan.getSpectrumType() == MassSpectrumType.CENTROIDED
                  || scanProcessorConfig.isMassDetectActive(metadataScan.getMSLevel())
                  ? MassSpectrumType.CENTROIDED : MassSpectrumType.PROFILE;
          final Range<Double> scanMzRange = Range.closed(scanTable.getAcqMzRangeLower(i),
              scanTable.getAcqMzRangeUpper(i));

          final Scan scan = new SimpleScan(file, metadataScan.getScanNumber(),
              metadataScan.getMSLevel(), metadataScan.getRetentionTime(), msMsInfo, arrays.mzs(),
              arrays.intensities(), spectrumType, metadataScan.getPolarity(),
              "%s [%s - %s]".formatted(metadata.getValue(Values.InstrumentName),
                  formats.mz(scanMzRange.lowerEndpoint()), formats.mz(scanMzRange.upperEndpoint())),
              scanMzRange);
          file.addScan(scan);
        }
        importedScans++;
      }

      synchronized (org.sqlite.JDBC.class) {
        BrukerUvReader.loadAndAddForFile(folderPath, file, getMemoryMapStorage());
      }

      file.setStartTimeStamp(
          ZonedDateTime.parse(metadata.getValue(Values.AcquisitionDateTime)).toLocalDateTime());

      file.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(callingModule, parameters, getModuleCallDate()));
    } catch (Exception e) {
      error("Error while reading BAF file.", e);
      return;
    }

    project.addFile(file);
    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public RawDataFile getImportedRawDataFile() {
    return getStatus() == TaskStatus.FINISHED ? file : null;
  }
}
