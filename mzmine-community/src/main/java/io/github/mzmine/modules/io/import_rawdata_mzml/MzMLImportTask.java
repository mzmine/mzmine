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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.header.MzMLFileMetadataMapper;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileUtils;
import io.github.mzmine.util.date.DateTimeUtils;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports a file or converter stream, then publishes the validated result.
 */
@SuppressWarnings("UnstableApiUsage")
public final class MzMLImportTask extends AbstractTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(MzMLImportTask.class.getName());

  // File is always set even if the input stream may be already opened, e.g., from a converter
  private final @NotNull File file;
  private final @Nullable InputStream fis;
  // advanced processing will apply mass detection directly to the scans
  private final MZmineProject project;
  private final @NotNull ScanImportProcessorConfig scanProcessorConfig;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private int totalScansAfterFilter = 0, convertedScansAfterFilter;
  private String description;

  private volatile @Nullable MzMLReader reader;
  private int totalSpectra;
  private int parsedSpectra;
  private final @Nullable XMLInputFactory factory;
  private @Nullable RawDataFile publishedFile;

  /**
   * Create for file
   */
  public MzMLImportTask(@NotNull final MZmineProject project, @NotNull final File fileToOpen,
      @NotNull final ScanImportProcessorConfig scanProcessorConfig,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull final Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    this(project, fileToOpen, null, scanProcessorConfig, module, parameters, moduleCallDate,
        storage);
  }

  /**
   * @param project        destination for a published import
   * @param source         file to read or vendor file identifying the supplied stream
   * @param input          converter or archive stream, or null to open the source file
   * @param config         scan selection and processing
   * @param module         calling import module
   * @param parameters     applied parameters
   * @param moduleCallDate time of the module invocation
   * @param storage        destination for peak data
   */
  public MzMLImportTask(@NotNull final MZmineProject project, @NotNull final File source,
      @Nullable final InputStream input, @NotNull final ScanImportProcessorConfig config,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull final Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    this(project, source, input, config, module, parameters, moduleCallDate, storage, null);
  }

  /**
   * Uses a caller-selected StAX provider for this import.
   *
   * @param factory factory owned by this task, or null for the standard mzML import configuration
   */
  public MzMLImportTask(@NotNull final MZmineProject project, @NotNull final File source,
      @Nullable final InputStream input, @NotNull final ScanImportProcessorConfig config,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull final Instant moduleCallDate, @Nullable final MemoryMapStorage storage,
      @Nullable final XMLInputFactory factory) {
    super(storage, moduleCallDate);
    this.project = project;
    this.file = source;
    this.fis = input;
    this.scanProcessorConfig = config;
    this.module = module;
    this.parameters = parameters;
    this.description = "Importing raw data file: " + source.getName();
    this.factory = factory;
  }

  @Override
  public void run() {
    if (isCanceled()) {
      return;
    }

    setStatus(TaskStatus.PROCESSING);

    final RawDataFile dataFile = importStreamOrFile();

    if (dataFile == null || isCanceled()) {
      return;
    }

    addAppliedMethodAndAddToProject(dataFile);

    setStatus(TaskStatus.FINISHED);
  }

  public void addAppliedMethodAndAddToProject(@NotNull final RawDataFile dataFile) {
    RawDataFileUtils.addAdditionalFileMetadata(dataFile);
    dataFile.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
    project.addFile(dataFile);
    publishedFile = dataFile;
  }

  /**
   * Import mzml from InputStream (if not null) or from file otherwise. Does not add the applied
   * method and does not add the data file to project
   *
   * @return the raw data file, only if successful and not canceled
   */
  @Nullable
  public RawDataFile importStreamOrFile() {
    try {
      final MzMLImportData data = read();
      if (isCanceled()) {
        return null;
      }

      final RawDataFileImpl newMZmineFile;
      if (data.frames().isEmpty()) {
        totalScansAfterFilter = data.spectra().size();
        newMZmineFile = data.spectra().finish(this::isCanceled);
        convertedScansAfterFilter = newMZmineFile.getNumOfScans();
      } else {
        totalScansAfterFilter = data.frames().size();
        newMZmineFile = buildIonMobilityFile(data);
      }
      if (isCanceled()) {
        return null;
      }

      final List<OtherDataFile> otherDataFiles = ConversionUtils.convertOtherSpectra(newMZmineFile,
          data.otherSpectra());
      final List<OtherDataFile> otherTraceFiles = ConversionUtils.convertOtherTraces(newMZmineFile,
          data.chromatograms());
      newMZmineFile.addOtherDataFiles(otherDataFiles);
      newMZmineFile.addOtherDataFiles(otherTraceFiles);

      newMZmineFile.setStartTimeStamp(
          data.timestamp() == null ? null : DateTimeUtils.parseOrElse(data.timestamp(), null));
      try {
        MzMLFileMetadataMapper.apply(data.header(), newMZmineFile.getFileMetadata());
      } catch (RuntimeException e) {
        // decision: missing file metadata should never fail the import
        logger.log(Level.WARNING,
            "Cannot read file metadata from mzML header of " + file + ": " + e.getMessage(), e);
      }
      logger.info("Finished parsing " + file + ", parsed " + convertedScansAfterFilter + " scans");

      if (totalScansAfterFilter == 0 && newMZmineFile.getOtherDataFiles().isEmpty()) {
        var activeFilter = scanProcessorConfig.scanFilter().isActiveFilter();
        String filter = activeFilter ? """
            Scan filters were active in import and filtered out %d scans,
            either deactivate the filters or remove this file from the import list""".formatted(
            getTotalScansInMzML()) : "Scan filters were off.";

        String msg = "%s had 0 scans after import. %s".formatted(file.getName(), filter);
        DialogLoggerUtil.showMessageDialogForTime("Empty file", msg);
        error(msg);
        return null;
      }

      return newMZmineFile;
    } catch (CancellationException e) {
      return null;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error during mzML read: " + e.getMessage(), e);
      error("Error parsing " + file + ": " + ExceptionUtils.exceptionToString(e));
      return null;
    }
  }

  /**
   * @return the converted file if the converter delivered every declared spectrum, or null on
   * cancellation or failure. A converter can produce a valid chromatogram-only run.
   */
  public @Nullable RawDataFile importConvertedData() {
    final RawDataFile result = importStreamOrFile();
    if (result != null && parsedSpectra != totalSpectra) {
      result.close();
      error("Incomplete mzML from converter for %s: read %d of %d spectra".formatted(file,
          parsedSpectra, totalSpectra));
      return null;
    }
    return result;
  }

  private @NotNull IMSRawDataFileImpl buildIonMobilityFile(@NotNull final MzMLImportData data)
      throws IOException {
    String descriptionTemplate = description = "Importing %s, total / parsed is %d / ".formatted(
        file.getName(), totalScansAfterFilter);
    IMSRawDataFileImpl newImsFile = new IMSRawDataFileImpl(file.getName(),
        this.file.getAbsolutePath(), storage);

    final List<BuildingMobilityScanStorage> framesMobilityScans = data.frames();
    final var axes = MzMLMobilityAxis.forFrames(framesMobilityScans);
    if (data.mobilityWarning() != null) {
      newImsFile.addWarning(data.mobilityWarning());
    }

    for (final BuildingMobilityScanStorage frameStorage : framesMobilityScans) {
      if (isCanceled()) {
        return newImsFile;
      }

      final var axis = axes.get(MzMLMobilityAxis.function(frameStorage));
      MzMLFrameAssembler.appendFrame(newImsFile, frameStorage, axis,
          scanProcessorConfig.isMassDetectActive(frameStorage.getMsLevel()));
      convertedScansAfterFilter++;
      description = descriptionTemplate + convertedScansAfterFilter;
    }

    return newImsFile;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    final int total = getTotalScansInMzML();
    final double readProgress =
        total == 0 ? 0d : Math.min(1d, (double) getParsedMzMLScans() / total);
    final double parsingProgress = totalScansAfterFilter == 0 ? 0.0
        : (double) convertedScansAfterFilter / totalScansAfterFilter;
    return (readProgress * 0.95) + (parsingProgress * 0.05);
  }


  /**
   * This is the number of already converted scans, after filtering
   */
  public int getConvertedScansAfterFilter() {
    return convertedScansAfterFilter;
  }

  /**
   * @return declared spectrum count for converter completeness checks
   */
  public int getTotalScansInMzML() {
    final MzMLReader active = reader;
    return active == null ? totalSpectra : active.declaredSpectra();
  }

  /**
   * @return consumed spectrum count before filtering
   */
  public int getParsedMzMLScans() {
    final MzMLReader active = reader;
    return active == null ? parsedSpectra : active.parsedSpectra();
  }

  private @NotNull MzMLImportData read() throws IOException, XMLStreamException {
    try (final Reader input = new InputStreamReader(
        fis == null ? Files.newInputStream(file.toPath()) : fis, StandardCharsets.UTF_8)) {
      final XMLStreamReader xml = (factory == null ? defaultFactory()
          : factory).createXMLStreamReader(input);
      try {
        reader = new MzMLReader(xml, file, scanProcessorConfig, storage, this::isCanceled);
        return reader.readDocument();
      } finally {
        if (reader != null) {
          totalSpectra = reader.declaredSpectra();
          parsedSpectra = reader.parsedSpectra();
          reader = null;
        }
        xml.close();
      }
    }
  }

  private static @NotNull XMLInputFactory defaultFactory() {
    final var factory = new InputFactoryImpl();
    factory.configureForSpeed();
    return factory;
  }

  @Override
  public @NotNull List<RawDataFile> getImportedRawDataFiles() {
    return publishedFile == null ? List.of() : List.of(publishedFile);
  }
}
