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

package io.github.mzmine.modules.io.import_rawdata_wiff2;

import com.sun.jna.Platform;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.Experiment;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.Sample;
import io.github.mzmine.modules.io.import_rawdata_wiff2.api.Spectrum;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractRawDataFileTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.date.LocalDateTimeParser;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.scene.control.Alert.AlertType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Wiff2ImportTask extends AbstractRawDataFileTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(Wiff2ImportTask.class.getName());

  private final File file;
  private final MZmineProject project;
  @NotNull
  private final ScanImportProcessorConfig scanProcessorConfig;
  private final List<RawDataFile> files = new ArrayList<>();
  private double progress = 0;
  private String taskStr = null;

  public Wiff2ImportTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      final File file, ParameterSet parameters, MZmineProject project,
      @NotNull ScanImportProcessorConfig scanProcessorConfig) {
    super(storage, moduleCallDate, parameters, AllSpectralDataImportModule.class);
    this.file = file;
    this.project = project;
    this.scanProcessorConfig = scanProcessorConfig;
  }

  private static @NotNull String getDataFileName(File file, Sample sample, List<Sample> samples) {
    String sampleName = sample.getSampleName();
    String userSampleID = sample.getUserSampleId();
    String filename = FileAndPathUtil.eraseFormat(file.getName());
    String extension = Optional.ofNullable(FileAndPathUtil.getExtension(file))
        .filter(e -> e.contains("wiff")).orElse("wiff2");

    if (samples.size() <= 1) {
      return file.getName();
    }

    // same sampleName is allowed, but they will have different ids. the id is:
    //wiff2:///<filePath>/<a number>
    String sampleId = sample.getId();
    int idSepIndex = sampleId.lastIndexOf('/');
    int id = -1;
    if (idSepIndex != -1) {
      String idStr = sampleId.substring(idSepIndex + 1);
      try {
        id = Integer.parseInt(idStr);
      } catch (NumberFormatException e) {
        // silent
      }
    }

    final StringBuilder b = new StringBuilder();
    b.append(filename);
    if (id != -1) {
      b.append("_").append(id); // sciex unique id
    }

    b.append("_").append(sampleName);
    if (!StringUtils.isBlank(userSampleID)) {
      b.append("_").append(userSampleID);
    }

    return b.append(".").append(extension).toString();
  }

  public static List<File> mapImportedFileNames(@NotNull File file, @NotNull RawDataFileType type) {
    if (type != RawDataFileType.SCIEX_WIFF2 && type != RawDataFileType.SCIEX_WIFF) {
      return List.of(file);
    }

    final List<File> imported = new ArrayList<>();
    try (var access = new Wiff2DataAccess(file, true, ScanImportProcessorConfig.createDefault())) {
      List<Sample> samples = access.getSamples();
      for (Sample sample : samples) {
        imported.add(new File(file.getParentFile(), getDataFileName(file, sample, samples)));
      }
    } catch (Exception e) {
      //
    }
    return imported;
  }

  @Override
  public String getTaskDescription() {
    return "Importing file " + file.getName() + ". " + taskStr;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  protected void process() {

    taskStr = "Launching WIFF API...";
    try (Wiff2DataAccess access = new Wiff2DataAccess(file,
        parameters.getEmbeddedParameterValue(AllSpectralDataImportParameters.vendorOptions)
            .getValue(VendorImportParameters.applyVendorCentroiding), scanProcessorConfig)) {

      taskStr = "Retrieving sample information...";
      final List<Sample> samples = access.getSamples();

      logger.finest("File %s contains %d samples".formatted(file, samples.size()));
      for (Sample sample : samples) {
        final int sampleIndex = samples.indexOf(sample);
        final double sampleProgress = (double) sampleIndex / samples.size();

        final RawDataFileImpl rawDataFile = new RawDataFileImpl(
            getDataFileName(file, sample, samples), file.getAbsolutePath(), getMemoryMapStorage());

        final List<SimpleScan> scans = new ArrayList<>();
        final String startTimestamp = sample.getStartTimestamp();
        rawDataFile.setStartTimeStamp(LocalDateTimeParser.parseAnyFirstDate(startTimestamp));

        final List<Experiment> experiments = access.getExperiments(sample);
        for (Experiment experiment : experiments) {
          taskStr =
              "Importing sample " + rawDataFile.getName() + " experiment " + experiments.indexOf(
                  experiment) + "/" + experiments.size();
          final Iterator<Spectrum> spectra = access.getSpectrumIterator(sample, experiment);
          while (spectra.hasNext()) {
            if (isCanceled()) {
              return;
            }

            final Spectrum spectrum = spectra.next();
            final SimpleScan scan = access.spectrumToMzmineScan(rawDataFile, sample, experiment,
                spectrum);
            if (scan != null) {
              scans.add(scan);
            }
          }

          final int experimentIndex = experiments.indexOf(experiment);
          final double experimentProgress = (double) experimentIndex / experiments.size();
          progress = sampleProgress + (1d / samples.size()) * experimentProgress;
        }

        scans.sort(Scan::compareTo);
        for (int i = 0; i < scans.size(); i++) {
          SimpleScan scan = scans.get(i);
          scan.setScanNumber(i + 1);
        }
        scans.forEach(rawDataFile::addScan);

        final List<@NotNull OtherDataFile> otherDataFiles = access.getAnalogTraces(sample,
            rawDataFile);
        rawDataFile.addOtherDataFiles(otherDataFiles);

        final List<OtherDataFile> channelTraces = access.getAnalogTracesFromSpectrumDetectors(
            sample, experiments, rawDataFile, storage);
        rawDataFile.addOtherDataFiles(channelTraces);

        access.loadAndAddMrms(sample, rawDataFile, experiments);

        files.add(rawDataFile);
      }
    } catch (Exception e) {
      error("Error while importing %s".formatted(file.getName()), e);
      if (Platform.isLinux()) {
        DialogLoggerUtil.showDialog(AlertType.ERROR, "Error while importing file " + file.getName(),
            """
                Sciex WIFF(2) file import failed. The following packages (or newer versions) may be required:
                
                Ubuntu:
                libunwind8, libuuid1, liblttng-ust0, libcurl3, libssl1.0.0, libkrb5-3, zlib1g, libicu60
                
                CentOS:
                libunwind, libuuid, lttng-ust, libcurl, openssl-libs, krb5-libs, zlib
                
                Exception:
                %s
                """.formatted(e.getMessage()));
      }
      throw new RuntimeException(e);
    }

    files.forEach(project::addFile);
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return files;
  }

  @Override
  public @NotNull List<RawDataFile> getImportedRawDataFiles() {
    return isFinished() ? files : List.of();
  }

  @Override
  protected void addAppliedMethod() {
    super.addAppliedMethod();
  }
}
