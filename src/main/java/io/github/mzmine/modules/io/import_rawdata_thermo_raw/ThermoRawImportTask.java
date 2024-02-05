/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.ZipUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;


/**
 * This module binds spawns a separate process that dumps the native format's data in a text+binary
 * form into its standard output. This class then reads the output of that process.
 */
public class ThermoRawImportTask extends AbstractTask {

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


  public ThermoRawImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, @NotNull ScanImportProcessorConfig scanProcessorConfig) {
    super(null, moduleCallDate); // storage in raw data file
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

    // Unzip ThermoRawFileParser
    try {
      final File thermoRawFileParserDir = unzipThermoRawFileParser();
      String thermoRawFileParserCommand;

      if (Platform.isWindows()) {
        thermoRawFileParserCommand =
            thermoRawFileParserDir + File.separator + "ThermoRawFileParser.exe";
      } else if (Platform.isLinux()) {
        thermoRawFileParserCommand =
            thermoRawFileParserDir + File.separator + "ThermoRawFileParserLinux";
      } else if (Platform.isMac()) {
        thermoRawFileParserCommand =
            thermoRawFileParserDir + File.separator + "ThermoRawFileParserMac";
      } else {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Unsupported platform: JNA ID " + Platform.getOSType());
        return;
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
      dumper = Runtime.getRuntime().exec(cmdLine, null, thermoRawFileParserDir);

      // Get the stdout of ThermoRawFileParser process as InputStream
      RawDataFile dataFile = null;
      try (InputStream mzMLStream = dumper.getInputStream()) //
//          BufferedInputStream bufStream = new BufferedInputStream(mzMLStream))//
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
      dumper.destroy();

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
        STR."Finished parsing \{fileToOpen}, parsed \{parsedScans} scans and after filtering remained \{convertedScans}");
    setStatus(TaskStatus.FINISHED);

  }

  @Override
  public String getTaskDescription() {
    return taskDescription;
  }

  private File unzipThermoRawFileParser() throws IOException {
    final String tmpPath = System.getProperty("java.io.tmpdir");
    File thermoRawFileParserFolder = new File(tmpPath, "mzmine_thermo_raw_parser");
    final File thermoRawFileParserExe = new File(thermoRawFileParserFolder,
        "ThermoRawFileParser.exe");

    // Check if it has already been unzipped
    if (thermoRawFileParserFolder.exists() && thermoRawFileParserFolder.isDirectory()
        && thermoRawFileParserFolder.canRead() && thermoRawFileParserExe.exists()
        && thermoRawFileParserExe.isFile() && thermoRawFileParserExe.canExecute()) {
      logger.finest("ThermoRawFileParser found in folder " + thermoRawFileParserFolder);
      return thermoRawFileParserFolder;
    }

    // In case the folder already exists, unzip to a different folder
    if (thermoRawFileParserFolder.exists()) {
      logger.finest("Folder " + thermoRawFileParserFolder + " exists, creating a new one");
      thermoRawFileParserFolder = FileAndPathUtil.createTempDirectory("mzmine_thermo_raw_parser")
          .toFile();
    }

    logger.finest("Unpacking ThermoRawFileParser to folder " + thermoRawFileParserFolder);
    InputStream zipStream = getClass().getResourceAsStream(
        "/vendorlib/thermo/ThermoRawFileParser.zip");
    if (zipStream == null) {
      throw new IOException(
          "Failed to open the resource /vendorlib/thermo/ThermoRawFileParser.zip");
    }
    ZipInputStream zipInputStream = new ZipInputStream(zipStream);
    ZipUtils.unzipStream(zipInputStream, thermoRawFileParserFolder);
    zipInputStream.close();

    // Delete the temporary folder on application exit
    FileUtils.forceDeleteOnExit(thermoRawFileParserFolder);

    return thermoRawFileParserFolder;
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

}
