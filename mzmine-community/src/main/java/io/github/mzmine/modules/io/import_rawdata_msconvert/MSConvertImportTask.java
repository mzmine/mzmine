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

package io.github.mzmine.modules.io.import_rawdata_msconvert;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.WatersLockmassParameters;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
  private MSDKmzMLImportTask msdkTask;
  private Boolean convertToFile = ConfigService.getConfiguration().getPreferences()
      .getValue(MZminePreferences.keepConvertedFile);

  public MSConvertImportTask(final @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, File path, ScanImportProcessorConfig config,
      MZmineProject project, Class<? extends MZmineModule> module, ParameterSet parameters) {
    super(storage, moduleCallDate);
    this.rawFilePath = path;
    this.config = config;
    this.project = project;
    this.module = module;
    this.parameters = parameters;
  }

  public static @NotNull List<String> buildCommandLine(File filePath, File msConvertPath,
      boolean convertToFile) {
    final File mzMLFile = getMzMLFileName(filePath);
    final RawDataFileType fileType = RawDataFileTypeDetector.detectDataFileType(filePath);

    List<String> cmdLine = new ArrayList<>();
    cmdLine.addAll(List.of(inQuotes(msConvertPath.toString()), // MSConvert path
        inQuotes(filePath.getAbsolutePath()) // raw file path
    )); // vendor peak-picking

    if (convertToFile) {
      cmdLine.addAll(List.of(
          "--outdir", inQuotes(mzMLFile.getParent()), // need to set dir here
          "--outfile", inQuotes(mzMLFile.getName()))); // only file name here
    } else {
      cmdLine.addAll(List.of("-o", "-")); /* to stdout */
    }

    if (convertToFile) {
      cmdLine.add("--zlib");
      cmdLine.add("--numpressPic");
      cmdLine.add("--numpressLinear");
    }

    if (fileType == RawDataFileType.AGILENT_D_IMS || fileType == RawDataFileType.WATERS_RAW_IMS) {
      cmdLine.addAll(List.of("--combineIonMobilitySpectra"));
    }

    if (ConfigService.getPreferences().getValue(MZminePreferences.applyPeakPicking)
        && isPeakPickingSupported(fileType)) {
      cmdLine.addAll(List.of("--filter", "\"peakPicking vendor msLevel=1-\""));
    }

    if (fileType == RawDataFileType.WATERS_RAW || fileType == RawDataFileType.WATERS_RAW_IMS) {
      addWatersOptions(filePath, cmdLine);
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
      case MZML_IMS -> true;
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
      case WATERS_RAW_IMS, AGILENT_D_IMS -> false;
    };
  }

  private static void addWatersOptions(File rawFolder, List<String> cmdLine) {
    final WatersAcquisitionInfo acquisitionInfo = RawDataFileTypeDetector.detectWatersAcquisitionType(
        rawFolder);
    PolarityType polarity = acquisitionInfo.polarity();

    final MZminePreferences preferences = ConfigService.getPreferences();
    final Boolean lockmassEnabled = preferences.getValue(MZminePreferences.watersLockmass);
    final WatersLockmassParameters lockmassParameters = preferences.getEmbeddedParameterValue(
        MZminePreferences.watersLockmass);
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
   * Some versions of msconvert output information into stdout before the mzml is parsed. Therefore,
   * we need to find the mzml header and skip to it's start.
   */
  private static void skipToMzmlStart(InputStream mzMLStream) throws IOException {
    if (mzMLStream.markSupported()) {
      // set a mark so we can later return to the start of the file
      mzMLStream.mark(Integer.MAX_VALUE);
      final byte[] xmlHeader = "<?xml".getBytes(StandardCharsets.UTF_8);
      final byte[] buffer = new byte[256];
      int headerStartIndex = -1;
      int headerStartOffset = 0;

      while (headerStartIndex == -1) {
        final int read = mzMLStream.read(buffer);
        for (int i = 0; i < read; i++) {
          if (buffer[i] == xmlHeader[0]) {
            final byte[] bytes = Arrays.copyOfRange(buffer, i,
                Math.min(i + xmlHeader.length, read));
            if (Arrays.equals(bytes, xmlHeader)) {
              headerStartIndex = i;
              break;
            }
          }
        }
        if (headerStartIndex == -1) {
          if (read != -1) {
            logger.finest(() -> "Skipping text before mzml header: %s".formatted(
                new String(buffer, 0, read, StandardCharsets.UTF_8)));
            headerStartOffset += read;
          } else {
            logger.finest("No data recieved from MSConvert. Current header offset: %d".formatted(
                headerStartOffset));
          }
        }
      }
      // return to start of file and skip ahead to the index where the mzml starts
      mzMLStream.reset();
      mzMLStream.skipNBytes(headerStartOffset + headerStartIndex);
    }
  }

  /**
   * @param file
   * @param keepConverted
   * @return
   */
  public static File applyMsConvertImportNameChanges(File file, boolean keepConverted) {
    if (keepConverted && getSupportedFileTypes().contains(
        RawDataFileTypeDetector.detectDataFileType(file))) {
      return getMzMLFileName(file);
    } else {
      return file;
    }
  }

  @Override
  public String getTaskDescription() {
    return msdkTask != null ? msdkTask.getTaskDescription()
        : "Waiting for conversion/Importing MS data file %s".formatted(rawFilePath);
  }

  @Override
  public double getFinishedPercentage() {
    return msdkTask != null ? msdkTask.getFinishedPercentage() : 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final File msConvertPath = MSConvert.getMsConvertPath();
    if (msConvertPath == null) {
      error("MSConvert not found. Please install MSConvert.");
      return;
    }
    final File mzMLFile = getMzMLFileName(rawFilePath);

    if (mzMLFile.exists()) {
      logger.finest(
          "Discovered mzml file for MS data file %s. Importing mzml file from %s.".formatted(
              rawFilePath, mzMLFile));
      importFromMzML(mzMLFile);
      return;
    }

    final List<String> cmdLine = buildCommandLine(rawFilePath, msConvertPath, convertToFile);

    if (convertToFile) {
      ProcessBuilder builder = new ProcessBuilder(cmdLine);
      try {
        final Process process = builder.start();
        while (process.isAlive()) { // wait for conversion to finish
          if (isCanceled()) {
            process.destroy();
          }
          TimeUnit.MILLISECONDS.sleep(100);
        }
      } catch (IOException | InterruptedException e) {
        logger.log(Level.WARNING, "Error while converting %s to mzML file.".formatted(rawFilePath),
            e);
        setStatus(TaskStatus.ERROR);
        return;
      }
      importFromMzML(mzMLFile);
    } else {
      importFromStream(rawFilePath, cmdLine);
    }

    if (!isCanceled()) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private void importFromMzML(File mzMLFile) {
    RawDataFile dataFile = null;
    ParameterUtils.replaceRawFileName(parameters, rawFilePath, mzMLFile);

    if (project.getCurrentRawDataFiles().stream()
        .anyMatch(file -> file.getAbsolutePath().equals(mzMLFile.getAbsolutePath()))) {
      // we should only get to this point if someone imported raw files with the "keep mzml" option,
      // creates the mzml, then disables that option and imports the vendor file again.
      return;
    }

    msdkTask = new MSDKmzMLImportTask(project, mzMLFile, config, module, parameters, moduleCallDate,
        storage);

    this.addTaskStatusListener((_, _, _) -> {
      if (isCanceled()) {
        msdkTask.cancel();
      }
    });

    dataFile = msdkTask.importStreamOrFile();
    if (msdkTask.isCanceled()) {
      setStatus(msdkTask.getStatus());
      if (msdkTask.getStatus() == TaskStatus.ERROR) {
        setErrorMessage(msdkTask.getErrorMessage());
      }
      return;
    }

    if (dataFile == null || isCanceled()) {
      return;
    }

    var totalScans = msdkTask.getTotalScansInMzML();
    var parsedScans = msdkTask.getParsedMzMLScans();
    var convertedScans = msdkTask.getConvertedScansAfterFilter();

    if (parsedScans == 0 && dataFile.getOtherDataFiles().isEmpty()) {
      throw new IllegalStateException(
          "No scans or chromatograms found in file %s".formatted(dataFile.getName()));
    }

    if (parsedScans != totalScans) {
      throw (new RuntimeException(
          "MSConvert process crashed before all scans were extracted (" + parsedScans + " out of "
          + totalScans + ")"));
    }
    msdkTask.addAppliedMethodAndAddToProject(dataFile);
  }

  private void importFromStream(File rawFilePath, List<String> cmdLine) {
    ProcessBuilder builder = new ProcessBuilder(cmdLine);
    Process process = null;
    try {
      process = builder.start();

      // Get the stdout of MSConvert process as InputStream
      RawDataFile dataFile = null;
      try (InputStream mzMLStream = process.getInputStream()) //
      {
        skipToMzmlStart(mzMLStream);
        msdkTask = new MSDKmzMLImportTask(project, rawFilePath, mzMLStream, config, module,
            parameters, moduleCallDate, storage);

        this.addTaskStatusListener((_, _, _) -> {
          if (isCanceled()) {
            msdkTask.cancel();
          }
        });
        dataFile = msdkTask.importStreamOrFile();
      }

      if (dataFile == null || isCanceled()) {
        return;
      }

      var totalScans = msdkTask.getTotalScansInMzML();
      var parsedScans = msdkTask.getParsedMzMLScans();
      var convertedScans = msdkTask.getConvertedScansAfterFilter();

      // Finish
      process.destroy();

      if (parsedScans == 0 && dataFile.getOtherDataFiles().isEmpty()) {
        throw (new RuntimeException("No scans found"));
      }

      if (parsedScans != totalScans) {
        throw (new RuntimeException(
            "ThermoRawFileParser/MSConvert process crashed before all scans were extracted ("
            + parsedScans + " out of " + totalScans + ")"));
      }

      msdkTask.addAppliedMethodAndAddToProject(dataFile);

    } catch (Throwable e) {
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

  public static Set<RawDataFileType> getSupportedFileTypes() {
    return Set.of(RawDataFileType.WATERS_RAW, RawDataFileType.WATERS_RAW_IMS,
        RawDataFileType.SCIEX_WIFF, RawDataFileType.SCIEX_WIFF2, RawDataFileType.AGILENT_D,
        RawDataFileType.AGILENT_D_IMS, RawDataFileType.THERMO_RAW);
  }

  @Override
  public RawDataFile getImportedRawDataFile() {
    return getStatus() == TaskStatus.FINISHED ? msdkTask.getImportedRawDataFile() : null;
  }
}
