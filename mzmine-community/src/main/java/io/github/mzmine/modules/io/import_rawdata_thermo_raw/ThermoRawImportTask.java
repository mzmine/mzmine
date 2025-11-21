/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * This module binds spawns a separate process that dumps the native format's data in a text+binary
 * form into its standard output. This class then reads the output of that process.
 */
public class ThermoRawImportTask extends AbstractTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(ThermoRawImportTask.class.getName());

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
    assert parameters instanceof AllSpectralDataImportParameters;
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

  private @Nullable ProcessBuilder createProcessFromThermoFileParser() {
    taskDescription = "Opening file " + fileToOpen;

    final File parserPath = getParserPathForOs();
    final String thermoRawFileParserCommand = parserPath.getAbsolutePath();

    if (Platform.isWindows() && !thermoRawFileParserCommand.endsWith("ThermoRawFileParser.exe")) {
      error("Invalid raw file parser setting for windows. Please select the windows parser.");
      return null;
    } else if (Platform.isLinux() && !thermoRawFileParserCommand.endsWith("ThermoRawFileParser")) {
      error("Invalid raw file parser setting for linux. Please select the linux parser.");
      return null;
    } else if (Platform.isMac() && !thermoRawFileParserCommand.endsWith("ThermoRawFileParser")) {
      error("Invalid raw file parser setting for mac. Please select the mac parser.");
      return null;
    } else if (!Platform.isWindows() && !Platform.isLinux() && !Platform.isMac()) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Unsupported platform: JNA ID " + Platform.getOSType());
      return null;
    }

    final List<String> cmdLine = new ArrayList<>(); //
    cmdLine.add(thermoRawFileParserCommand); // program to run
    cmdLine.add("-s"); // output mzML to stdout
    if (!parameters.getValue(AllSpectralDataImportParameters.applyVendorCentroiding)) {
      cmdLine.add("-p"); // no peak picking
    }
    cmdLine.add("-z"); // no zlib compression (higher speed)
    cmdLine.add("-f=1"); // no index, https://github.com/compomics/ThermoRawFileParser/issues/118
    cmdLine.add("--allDetectors"); // include all detector data
    cmdLine.add("-i"); // input RAW file name coming next
    cmdLine.add(fileToOpen.getPath()); // input RAW file name

    // Create a separate process and execute ThermoRawFileParser.
    // Use thermoRawFileParserDir as working directory; this is essential, otherwise the process will fail.
    ProcessBuilder builder = new ProcessBuilder(cmdLine).directory(
        new File(thermoRawFileParserCommand).getParentFile());
    return builder;
  }

  @Override
  public String getTaskDescription() {
    return taskDescription;
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

  private File getParserPathForOs() {
    final Optional<File> prefPath = ConfigService.getPreferences()
        .getOptionalValue(MZminePreferences.thermoRawFileParserPath);
    if (prefPath.isPresent()) {
      return prefPath.get();
    }

    File parserDirectory = FileAndPathUtil.resolveInExternalToolsDir("thermo_raw_file_parser/");
    if (!parserDirectory.exists()) {
      throw new IllegalStateException(
          "ThermoRawFileParser directory not found. When running from the IDE run gradle build or gradle test before to download the thermo raw file parser to the external_tools directory."
              + "Expected one of: '<app>/external_tools/thermo_raw_file_parser/', 'external_tools/thermo_raw_file_parser/', '../external_tools/thermo_raw_file_parser/'.");
    }

    if (Platform.isWindows()) {
      return new File(parserDirectory, "ThermoRawFileParser.exe");
    }
    if (Platform.isLinux() || Platform.isMac()) {
      // both platforms have different runners but the same file name
      // mzmine >4.8.0 download only the matching dependency now
      return new File(parserDirectory, "ThermoRawFileParser");
    }
    throw new IllegalStateException(
        "Invalid operating system for parsing thermo files via the ThermoRawFileParser.");
  }

  @Override
  public @NotNull List<RawDataFile> getImportedRawDataFiles() {
    return getStatus() == TaskStatus.FINISHED ? msdkTask.getImportedRawDataFiles() : List.of();
  }
}
