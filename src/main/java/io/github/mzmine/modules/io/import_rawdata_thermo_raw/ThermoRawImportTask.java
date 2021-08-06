/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.import_rawdata_thermo_raw;

import com.google.common.collect.Range;
import com.sun.jna.Platform;
import io.github.mzmine.datamodel.MZmineException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.io.import_rawdata_mzml.MzMLImportTask;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.ZipUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;


/**
 * This module binds spawns a separate process that dumps the native format's data in a text+binary
 * form into its standard output. This class then reads the output of that process.
 */
public class ThermoRawImportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private File fileToOpen;
  private MZmineProject project;
  private RawDataFile newMZmineFile;

  private Process dumper = null;

  private String taskDescription;

  /*
   * These variables are used during parsing of the RAW dump.
   */
  private int scanNumber = 0, msLevel = 0, precursorCharge = 0, numOfDataPoints;
  private String scanId;
  private PolarityType polarity;
  private Range<Double> mzRange;
  private float retentionTime = 0;
  private double precursorMZ = 0;

  private MzMLImportTask mzMlImportTask;

  public ThermoRawImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile) {
    super(null); // storage in raw data file
    this.project = project;
    this.fileToOpen = fileToOpen;
    taskDescription = "Opening file " + fileToOpen;
    this.newMZmineFile = newMZmineFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (mzMlImportTask == null) {
      return 0.0;
    } else {
      return mzMlImportTask.getFinishedPercentage();
    }
  }

  /**
   * @see java.lang.Runnable#run()
   */
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
            thermoRawFileParserDir + File.separator
                + "ThermoRawFileParser.exe";
      } else if (Platform.isLinux()) {
        thermoRawFileParserCommand =
            thermoRawFileParserDir + File.separator
                + "ThermoRawFileParserLinux";
      } else if (Platform.isMac()) {
        thermoRawFileParserCommand =
            thermoRawFileParserDir + File.separator
                + "ThermoRawFileParserMac";
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
      InputStream mzMLStream = dumper.getInputStream();
      BufferedInputStream bufStream = new BufferedInputStream(mzMLStream);
      assert bufStream != null;

      mzMlImportTask = new MzMLImportTask(project, bufStream, newMZmineFile);
      mzMlImportTask.run();

      if (mzMlImportTask.getStatus() != TaskStatus.FINISHED) {
        throw (new MZmineException("ThermoRawFileParser process crashed before all scans were extracted"));
      }

      // Finish
      dumper.destroy();

      if (isCanceled()) {
        return;
      }

      if (newMZmineFile.getNumOfScans() == 0) {
        throw (new Exception("No scans found"));
      }

    } catch (Throwable e) {

      e.printStackTrace();

      if (dumper != null) {
        dumper.destroy();
      }

      if (getStatus() == TaskStatus.PROCESSING) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(ExceptionUtils.exceptionToString(e));
      }

      return;
    }

    logger.info("Finished parsing " + fileToOpen + ", parsed " + newMZmineFile.getNumOfScans() + " scans");
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
      thermoRawFileParserFolder = Files.createTempDirectory("mzmine_thermo_raw_parser").toFile();
    }

    logger.finest("Unpacking ThermoRawFileParser to folder " + thermoRawFileParserFolder);
    InputStream zipStream = getClass()
        .getResourceAsStream("/vendorlib/thermo/ThermoRawFileParser.zip");
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

    if (mzMlImportTask != null) {
      mzMlImportTask.cancel();
    }

    // Try to destroy the dumper process
    if (dumper != null) {
      dumper.destroy();
    }
  }

}
