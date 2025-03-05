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

package io.github.mzmine.modules.io.import_rawdata_thermo_raw;

import com.sun.jna.Platform;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.download.AssetGroup;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.ZipUtils;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * This module binds spawns a separate process that dumps the native format's data in a text+binary
 * form into its standard output. This class then reads the output of that process.
 */
public class ThermoRawImportTask extends AbstractTask implements RawDataImportTask {

  public static final String THERMO_RAW_PARSER_DIR = "mzmine_thermo_raw_parser";

  private static final Logger logger = Logger.getLogger(ThermoRawImportTask.class.getName());
  private static final CloseableReentrantReadWriteLock unzipLock = new CloseableReentrantReadWriteLock();
  private static final File DEFAULT_PARSER_DIR = new File(
      FileAndPathUtil.resolveInMzmineDir("external_resources"), "thermo_raw_file_parser");

  private final File fileToOpen;
  private final MZmineProject project;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private final ScanImportProcessorConfig scanProcessorConfig;
  private Process dumper = null;
  private String taskDescription;
  private int parsedScans = 0;
  private MSDKmzMLImportTask msdkTask;
  private int convertedScans;

  protected ThermoRawImportTask(final @Nullable MemoryMapStorage storage, MZmineProject project,
      File fileToOpen, @NotNull final Class<? extends MZmineModule> module,
      @NotNull final ParameterSet parameters, @NotNull Instant moduleCallDate,
      @NotNull ScanImportProcessorConfig scanProcessorConfig) {
    super(storage, moduleCallDate); // storage in raw data file
    this.project = project;
    this.fileToOpen = fileToOpen;
    taskDescription = "Opening file " + fileToOpen;
    this.parameters = parameters;
    this.module = module;
    this.scanProcessorConfig = scanProcessorConfig;
  }

  @Override
  public double getFinishedPercentage() {
    if (msdkTask == null) {
      return 0.0;
    } else {
      return msdkTask.getFinishedPercentage();
    }
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Opening file " + fileToOpen);

    try {
      final ProcessBuilder builder = createProcessFromThermoFileParser();
      if (builder == null) {
        error("Unable to create thermo parser from MSConvert or the ThermoRawFileParser.");
        return;
      }

      dumper = builder.start();

      // Get the stdout of ThermoRawFileParser process as InputStream
      RawDataFile dataFile = null;
      try (InputStream mzMLStream = dumper.getInputStream()) //
      {
        msdkTask = new MSDKmzMLImportTask(project, fileToOpen, mzMLStream, scanProcessorConfig,
            module, parameters, moduleCallDate, storage);

        this.addTaskStatusListener((task, newStatus, oldStatus) -> {
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
      parsedScans = msdkTask.getParsedMzMLScans();
      convertedScans = msdkTask.getConvertedScansAfterFilter();
      // Finish
      if (dumper != null) {
        dumper.destroy();
      }

      if (parsedScans == 0) {
        throw (new RuntimeException("No scans found"));
      }

      if (parsedScans != totalScans) {
        throw (new RuntimeException(
            "ThermoRawFileParser process crashed before all scans were extracted (" + parsedScans
                + " out of " + totalScans + ")"));
      }
      msdkTask.addAppliedMethodAndAddToProject(dataFile);
    } catch (Throwable e) {
      if (dumper != null) {
        dumper.destroy();
      }

      if (getStatus() == TaskStatus.PROCESSING) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(ExceptionUtils.exceptionToString(e));
      }

      return;
    }

    logger.info(
        "Finished parsing %s, parsed %d scans and after filtering remained %d".formatted(fileToOpen,
            parsedScans, convertedScans));
    setStatus(TaskStatus.FINISHED);

  }

  private @Nullable ProcessBuilder createProcessFromThermoFileParser() throws IOException {
    taskDescription = "Opening file " + fileToOpen;

    if (!checkParserExistenceAndUnzipBlocking()) {
      return null;
    }

    final File parserPath = ConfigService.getPreference(MZminePreferences.thermoRawFileParserPath);
    if (!isValidParserPathForOs(parserPath)) {
      return null;
    }
    final String thermoRawFileParserCommand = parserPath.getAbsolutePath();

    if (Platform.isWindows() && !thermoRawFileParserCommand.endsWith("ThermoRawFileParser.exe")) {
      error("Invalid raw file parser setting for windows. Please select the windows parser.");
      return null;
    } else if (Platform.isLinux() && !thermoRawFileParserCommand.endsWith(
        "ThermoRawFileParserLinux")) {
      error("Invalid raw file parser setting for linux. Please select the linux parser.");
      return null;
    } else if (Platform.isMac() && !thermoRawFileParserCommand.endsWith("ThermoRawFileParserMac")) {
      error("Invalid raw file parser setting for mac. Please select the mac parser.");
      return null;
    } else if (!Platform.isWindows() && !Platform.isLinux() && !Platform.isMac()) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Unsupported platform: JNA ID " + Platform.getOSType());
      return null;
    }

    final String cmdLine[] = new String[]{ //
        thermoRawFileParserCommand, // program to run
        "-s", // output mzML to stdout
        "-p", // no peak picking
        "-z", // no zlib compression (higher speed)
        "-f=1", // no index, https://github.com/compomics/ThermoRawFileParser/issues/118
        "-i", // input RAW file name coming next
        fileToOpen.getPath() // input RAW file name
    };

    // Create a separate process and execute ThermoRawFileParser.
    // Use thermoRawFileParserDir as working directory; this is essential, otherwise the process will fail.
    ProcessBuilder builder = new ProcessBuilder(cmdLine).directory(
        new File(thermoRawFileParserCommand).getParentFile());
    return builder;
  }

  /**
   * Checks if the raw file parser is already unzipped. If not, the parser is either unzipped or the
   * user is prompted to download the parser. If a zip is selected, the zip is unpacked to the raw
   * file parser dir in the mzmine folder.
   *
   * @return
   * @throws IOException
   */
  private boolean checkParserExistenceAndUnzipBlocking() throws IOException {
    File parserPath = null;

    try (var _ = unzipLock.lockWrite()) {
      // update after acquiring lock
      parserPath = ConfigService.getPreference(MZminePreferences.thermoRawFileParserPath);
      if (!isValidParserPathForOs(parserPath)) {
        logger.fine("Invalid thermo raw file parser path (%s).".formatted(parserPath));
        // check if an older version is available in the mzmine dir
        final File defaultLocation = new File(DEFAULT_PARSER_DIR, getParserNameForOs());
        if (defaultLocation.exists() && defaultLocation.canRead() && isValidParserPathForOs(
            defaultLocation)) {

          logger.fine(() -> "Thermo raw file parser found in default location %s.".formatted(
              defaultLocation.getAbsolutePath()));

          ConfigService.getPreferences()
              .setParameter(MZminePreferences.thermoRawFileParserPath, defaultLocation);
          return true;
        }

        // instructions for headless mode.
        if (DesktopService.isHeadLess()) {
          logger.severe(
              "Cannot find thermo raw file parser. Download the parser from %s and unzip the content into %s or edit the mzmine config file (parameter %s).".formatted(
                  StringUtils.inQuotes(AssetGroup.ThermoRawFileParser.getDownloadInfoPage()),
                  StringUtils.inQuotes(DEFAULT_PARSER_DIR.toString()),
                  StringUtils.inQuotes(MZminePreferences.thermoRawFileParserPath.getName())));
          return false;
        }

        logger.fine(
            () -> "Parser not found in default location. Waiting for user to set the parser location.");

        // make the user download and set the parser location
        AtomicReference<ExitCode> exitCode = new AtomicReference<>(ExitCode.UNKNOWN);
        FxThread.runOnFxThreadAndWait(() -> {
          DialogLoggerUtil.showErrorDialog("Thermo raw file parser not found.", """
              Thermo raw file parser not found. Please download the parser (link provided in the preferences)
              and set the file path or install and use MSConvert.""");
          exitCode.set(ConfigService.getPreferences().showSetupDialog(true, "Thermo raw file"));
        });

        if (exitCode.get() != ExitCode.OK) {
          logger.fine(() -> "User aborted raw file parser location setting. Aborting import.");
          return false;
        }
        parserPath = ConfigService.getPreference(MZminePreferences.thermoRawFileParserPath);
        logger.fine("Raw file parser path updated to %s.".formatted(parserPath.getAbsolutePath()));
      }

      if (parserPath.getName().endsWith(".zip")) {
        final File unzipped = unzipThermoRawFileParser(parserPath);
        ConfigService.getPreferences()
            .setParameter(MZminePreferences.thermoRawFileParserPath, unzipped);
      }
    }
    return true;
  }

  @Override
  public String getTaskDescription() {
    return taskDescription;
  }

  private File unzipThermoRawFileParser(File zipPath) throws IOException {

    final File thermoRawFileParserFolder = DEFAULT_PARSER_DIR;

    final File thermoRawFileParserExe = new File(thermoRawFileParserFolder, getParserNameForOs());

    // Check if it has already been unzipped
    if (thermoRawFileParserFolder.exists() && thermoRawFileParserFolder.isDirectory()
        && thermoRawFileParserFolder.canRead() && thermoRawFileParserExe.exists()
        && thermoRawFileParserExe.isFile() && thermoRawFileParserExe.canExecute()) {
      logger.finest("ThermoRawFileParser found in folder " + thermoRawFileParserFolder);
      return thermoRawFileParserExe;
    }

    logger.finest(
        "Unpacking ThermoRawFileParser to folder %s".formatted(thermoRawFileParserFolder));
    taskDescription = "Unpacking thermo raw file parser.";

    ZipUtils.unzipFile(zipPath, thermoRawFileParserFolder);
    logger.finest(
        "Finished unpacking ThermoRawFileParser to folder %s".formatted(thermoRawFileParserFolder));

    return thermoRawFileParserExe;
  }


  @Override
  public void cancel() {
    super.cancel();

    if (msdkTask != null) {
      msdkTask.cancel();
    }

    // Try to destroy the dumper process
    if (dumper != null) {
      dumper.destroy();
    }
  }

  private String getParserNameForOs() {
    if (Platform.isWindows()) {
      return "ThermoRawFileParser.exe";
    }
    if (Platform.isLinux()) {
      return "ThermoRawFileParserLinux";
    }
    if (Platform.isMac()) {
      return "ThermoRawFileParserMac";
    }
    throw new IllegalStateException("Invalid operating system for parsing thermo files.");
  }

  /**
   * checks if the path matches the OS and the files exist.
   */
  private boolean isValidParserPathForOs(File path) {
    if (path != null && path.exists() && (path.toPath().endsWith(getParserNameForOs())
        || path.toPath().endsWith("ThermoRawFileParser.zip"))) {
      return true;
    }
    return false;
  }

  @Override
  public RawDataFile getImportedRawDataFile() {
    return getStatus() == TaskStatus.FINISHED ? msdkTask.getImportedRawDataFile() : null;
  }
}
