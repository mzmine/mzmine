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

package io.github.mzmine.modules.io.import_rawdata_msconvert;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.MassLynxImportOptions;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.gui.preferences.WatersLockmassParameters;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MzMLImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.RawDataFileTypeDetector.WatersAcquisitionInfo;
import io.github.mzmine.util.RawDataFileTypeDetector.WatersAcquisitionType;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MSConvertImportTask extends AbstractTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(MSConvertImportTask.class.getName());

  private final File rawFilePath;
  private final ScanImportProcessorConfig config;
  private final MZmineProject project;
  private final Class<? extends MZmineModule> module;
  private final ParameterSet parameters;
  private final boolean convertToFile = ConfigService.getConfiguration().getPreferences()
      .getValue(MZminePreferences.keepConvertedFile);
  private MzMLImportTask mzMLTask;

  public MSConvertImportTask(final @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, File path, ScanImportProcessorConfig config,
      MZmineProject project, Class<? extends MZmineModule> module, ParameterSet parameters) {
    super(storage, moduleCallDate);
    this.rawFilePath = path;
    this.config = config;
    this.project = project;
    this.module = module;
    this.parameters = parameters;
    assert parameters instanceof AllSpectralDataImportParameters;
  }

  public static @NotNull List<String> buildCommandLine(File filePath, File msConvertPath,
      boolean convertToFile, final VendorImportParameters vendorParameters) {
    final File mzMLFile = getMzMLFileName(filePath);
    final RawDataFileType fileType = RawDataFileTypeDetector.detectDataFileType(filePath);

    List<String> cmdLine = new ArrayList<>();
    cmdLine.addAll(List.of(inQuotes(msConvertPath.toString()), // MSConvert path
        inQuotes(filePath.getAbsolutePath()) // raw file path
    )); // vendor peak-picking

    if (convertToFile) {
      cmdLine.addAll(List.of("--outdir", inQuotes(mzMLFile.getParent()), // need to set dir here
          "--outfile", inQuotes(mzMLFile.getName()))); // only file name here
    } else {
      cmdLine.addAll(List.of("-o", "-")); /* to stdout */
    }

    if (convertToFile) {
      cmdLine.add("--zlib");
      cmdLine.add("--numpressPic");
      cmdLine.add("--numpressLinear");
    }

    if (fileType == RawDataFileType.AGILENT_D_IMS || fileType == RawDataFileType.WATERS_RAW_IMS
        || fileType == RawDataFileType.MBI) {
      cmdLine.addAll(List.of("--combineIonMobilitySpectra"));
    }

    if (vendorParameters.getValue(VendorImportParameters.applyVendorCentroiding)
        && isPeakPickingSupported(fileType)) {
      cmdLine.addAll(List.of("--filter", "\"peakPicking vendor msLevel=1-\""));
    }

    if (fileType == RawDataFileType.WATERS_RAW || fileType == RawDataFileType.WATERS_RAW_IMS) {
      addWatersOptions(filePath, cmdLine, vendorParameters);
    }

//    cmdLine.addAll(List.of("--filter",
//        "\"titleMaker <RunId>.<ScanNumber>.<ScanNumber>.<ChargeState> File:\"\"\"^<SourcePath^>\"\"\", NativeID:\"\"\"^<Id^>\"\"\"\""));

    cmdLine.add("--ignoreUnknownInstrumentError");
    logger.finest("Running msconvert with command line: %s".formatted(cmdLine.toString()));
    return cmdLine;
  }

  private static boolean isPeakPickingSupported(RawDataFileType fileType) {
    return switch (fileType) {
      case MZML -> true;
      case IMZML -> true;
      case MZXML -> true;
      case MZDATA -> true;
      case NETCDF -> true;
      case THERMO_RAW -> true;
      case WATERS_RAW -> true;
      case MZML_ZIP -> true;
      case MZML_GZIP -> true;
      case ICPMSMS_CSV -> true;
      case BRUKER_TDF -> true;
      case BRUKER_TSF -> true;
      case BRUKER_BAF -> true;
      case SCIEX_WIFF -> true;
      case SCIEX_WIFF2 -> true;
      case AGILENT_D -> true;
      case MBI -> false;
      case SHIMADZU_LCD -> true;
      case WATERS_RAW_IMS, AGILENT_D_IMS -> false;
    };
  }

  private static void addWatersOptions(File rawFolder, List<String> cmdLine,
      final VendorImportParameters vendorParameters) {
    final WatersAcquisitionInfo acquisitionInfo = RawDataFileTypeDetector.detectWatersAcquisitionType(
        rawFolder);
    PolarityType polarity = acquisitionInfo.polarity();

    final boolean lockmassEnabled = vendorParameters.getValue(
        VendorImportParameters.watersLockmass);
    final ParameterSet lockmassParameters = vendorParameters.getParameter(
        VendorImportParameters.watersLockmass).getEmbeddedParameter().getEmbeddedParameters();
    final double positiveLockmass = lockmassParameters.getValue(WatersLockmassParameters.positive);
    final double negativeLockmass = lockmassParameters.getValue(WatersLockmassParameters.negative);

    if (lockmassEnabled && polarity == PolarityType.POSITIVE) {
      logger.finest(
          "Determined polarity of file %s to be %s. Applying lockmass correction with lockmass %.6f.".formatted(
              rawFolder.getName(), polarity, positiveLockmass));
      cmdLine.addAll(List.of("--filter",
          inQuotes("lockmassRefiner mz=%.6f tol=0.1".formatted(positiveLockmass))));
    } else if (lockmassEnabled && polarity == PolarityType.NEGATIVE) {
      logger.finest(
          "Determined polarity of file %s to be %s. Applying lockmass correction with lockmass %.6f.".formatted(
              rawFolder.getName(), polarity, negativeLockmass));
      cmdLine.addAll(List.of("--filter",
          inQuotes("lockmassRefiner mz=%.6f tol=0.1".formatted(negativeLockmass))));
    }

    final WatersAcquisitionType type = acquisitionInfo.acquisitionType();
    logger.finest("Determined acquisition type of file %s to be %s".formatted(rawFolder.getName(),
        type.name()));
    switch (type) {
      case MS_ONLY, MSE -> {
        cmdLine.addAll(
            List.of(inQuotes("--ignoreCalibrationScans"), "--filter", inQuotes("metadataFixer")));
      }
      case DDA -> {
        cmdLine.addAll(List.of("--ddaProcessing", "--filter", inQuotes("metadataFixer")));
      }
    }
  }

  public static @NotNull File getMzMLFileName(File filePath) {
    final String fileName = filePath.getName();
    final String mzMLName = FileAndPathUtil.getRealFileName(fileName, "mzML");
    final File mzMLFile = new File(filePath.getParent(), mzMLName);
    return mzMLFile;
  }

  /**
   * @param stdout converter output, possibly beginning with log lines
   * @return a buffered stream positioned on the first XML declaration or mzML root
   * @throws IOException when the converter ends without producing XML
   */
  static @NotNull InputStream mzMLStream(@NotNull final InputStream stdout) throws IOException {
    final var input = new BufferedInputStream(stdout);
    final byte[] prefix = new byte["<indexedmzML".length()];
    while (true) {
      input.mark(prefix.length);
      final int first = input.read();
      if (first < 0) {
        throw new IOException("MSConvert output ended before the mzML document");
      }
      if (first == '<') {
        prefix[0] = '<';
        final int length = 1 + input.readNBytes(prefix, 1, prefix.length - 1);
        final String tag = new String(prefix, 0, length, StandardCharsets.US_ASCII);
        input.reset();
        if (tag.startsWith("<?xml") || tag.startsWith("<mzML") || tag.startsWith("<indexedmzML")) {
          return input;
        }
        input.read();
      }
    }
  }

  /**
   * @param file          the actual file
   * @param keepConverted keep files after conversion
   * @param type          file type detected by
   *                      {@link RawDataFileTypeDetector#detectDataFileType(File)}
   */
  @NotNull
  public static File applyMsConvertImportNameChanges(@NotNull final File file,
      final boolean keepConverted, @Nullable final RawDataFileType type,
      @NotNull final VendorImportParameters vendorImportParameters) {
    if (type != null && keepConverted && getSupportedFileTypes().contains(type)) {
      if ((type == RawDataFileType.WATERS_RAW || type == RawDataFileType.WATERS_RAW_IMS) && (
          vendorImportParameters.getValue(VendorImportParameters.massLynxImportChoice)
              != MassLynxImportOptions.MSCONVERT)) {
        // if waters mass lynx files are imported via the sdk, apply no remapping
        return file;
      }
      return getMzMLFileName(file);
    } else {
      return file;
    }
  }

  public static Set<RawDataFileType> getSupportedFileTypes() {
    return Set.of(RawDataFileType.WATERS_RAW, RawDataFileType.WATERS_RAW_IMS,
        /*RawDataFileType.SCIEX_WIFF,*/ RawDataFileType.AGILENT_D, RawDataFileType.AGILENT_D_IMS,
        RawDataFileType.THERMO_RAW, RawDataFileType.SHIMADZU_LCD);
  }

  @Override
  public String getTaskDescription() {
    return mzMLTask != null ? mzMLTask.getTaskDescription()
        : "Waiting for conversion/Importing MS data file %s".formatted(rawFilePath);
  }

  @Override
  public double getFinishedPercentage() {
    return mzMLTask != null ? mzMLTask.getFinishedPercentage() : 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final File mzMLFile = getMzMLFileName(rawFilePath);

    if (mzMLFile.exists()) {
      logger.finest(
          "Discovered mzml file for MS data file %s. Importing mzml file from %s.".formatted(
              rawFilePath, mzMLFile));
      importFromMzML(mzMLFile);
      if (getStatus() == TaskStatus.PROCESSING) {
        setStatus(TaskStatus.FINISHED);
      }
      return;
    }

    final File msConvertPath = MSConvert.getMsConvertPath();
    if (msConvertPath == null) {
      error("MSConvert not found. Please install MSConvert.");
      return;
    }

    final List<String> cmdLine = buildCommandLine(rawFilePath, msConvertPath, convertToFile,
        (VendorImportParameters) parameters.getEmbeddedParameterValue(
            AllSpectralDataImportParameters.vendorOptions));

    if (convertToFile) {
      final ProcessBuilder builder = new ProcessBuilder(cmdLine).directory(
              FileAndPathUtil.getTempDir()).redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.INHERIT);
      Process process = null;
      try {
        process = builder.start();
        while (process.isAlive()) { // wait for conversion to finish
          if (isCanceled()) {
            process.destroy();
            return;
          }
          TimeUnit.MILLISECONDS.sleep(100);
        }
        if (process.exitValue() != 0) {
          error("MSConvert exited with code " + process.exitValue() + " for " + rawFilePath);
          return;
        }
      } catch (IOException | InterruptedException e) {
        // an interrupt would otherwise leave msconvert running after mzmine exits
        if (process != null) {
          process.destroy();
        }
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        logger.log(Level.WARNING, "Error while converting %s to mzML file.".formatted(rawFilePath),
            e);
        error("Error converting " + rawFilePath + ": " + e.getMessage());
        return;
      }
      importFromMzML(mzMLFile);
    } else {
      importFromStream(rawFilePath, cmdLine);
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private void importFromMzML(@NotNull final File mzMLFile) {
    ParameterUtils.replaceRawFileName(parameters, rawFilePath, mzMLFile);

    if (project.getCurrentRawDataFiles().stream()
        .anyMatch(file -> file.getAbsolutePath().equals(mzMLFile.getAbsolutePath()))) {
      // we should only get to this point if someone imported raw files with the "keep mzml" option,
      // creates the mzml, then disables that option and imports the vendor file again.
      return;
    }

    mzMLTask = new MzMLImportTask(project, mzMLFile, config, module, parameters, moduleCallDate,
        storage);

    this.addTaskStatusListener((_, _, _) -> {
      if (isCanceled()) {
        mzMLTask.cancel();
      }
    });

    final RawDataFile result = mzMLTask.importConvertedData();
    if (isCanceled()) {
      return;
    }
    if (result == null) {
      error(mzMLTask.getErrorMessage());
      return;
    }
    mzMLTask.addAppliedMethodAndAddToProject(result);
  }

  private void importFromStream(@NotNull final File rawFilePath,
      @NotNull final List<String> cmdLine) {
    final ProcessBuilder builder = new ProcessBuilder(cmdLine).directory(
        FileAndPathUtil.getTempDir()).redirectError(ProcessBuilder.Redirect.INHERIT);
    Process process = null;
    try {
      process = builder.start();

      final Process converter = process;
      this.addTaskStatusListener((_, _, _) -> {
        if (isCanceled()) {
          converter.destroy();
          if (mzMLTask != null) {
            mzMLTask.cancel();
          }
        }
      });
      if (isCanceled()) {
        converter.destroy();
        return;
      }
      final RawDataFile dataFile;
      try (InputStream mzMLStream = mzMLStream(process.getInputStream())) {
        mzMLTask = new MzMLImportTask(project, rawFilePath, mzMLStream, config, module,
            parameters, moduleCallDate, storage);

        if (isCanceled()) {
          converter.destroy();
          return;
        }
        dataFile = mzMLTask.importConvertedData();
      }

      if (dataFile == null || isCanceled()) {
        process.destroy();
        if (!isCanceled()) {
          error(mzMLTask.getErrorMessage());
        }
        return;
      }

      final int exitCode = process.waitFor();
      if (isCanceled()) {
        return;
      }
      if (exitCode != 0) {
        dataFile.close();
        error("MSConvert exited with code " + exitCode + " for " + rawFilePath);
        return;
      }
      mzMLTask.addAppliedMethodAndAddToProject(dataFile);

    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      if (process != null) {
        process.destroy();
      }

      if (getStatus() == TaskStatus.PROCESSING) {
        logger.log(Level.SEVERE, "Error while parsing file %s".formatted(rawFilePath), e);
        setErrorMessage(ExceptionUtils.exceptionToString(e));
        setStatus(TaskStatus.ERROR);
      }
    }
  }

  @Override
  public @NotNull List<RawDataFile> getImportedRawDataFiles() {
    return getStatus() == TaskStatus.FINISHED && mzMLTask != null
        ? mzMLTask.getImportedRawDataFiles() : List.of();
  }
}
