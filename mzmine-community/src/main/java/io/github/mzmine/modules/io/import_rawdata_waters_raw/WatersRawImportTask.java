/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_waters_raw;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.TextUtils;
import io.github.mzmine.util.ZipUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

/**
 * This module binds spawns a separate process that dumps the native format's data in a text+binary
 * form into its standard output. This class then reads the output of that process.
 */
public class WatersRawImportTask extends AbstractTask {

  public static final Logger logger = Logger.getLogger(WatersRawImportTask.class.getName());
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private File file;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private Process dumper = null;
  private int totalScans = 0, parsedScans = 0;
  /*
   * These variables are used during parsing of the RAW dump.
   */
  private int scanNumber = 0, msLevel = 0, precursorCharge = 0, numOfDataPoints;
  private double mobility = 0.0; // TODO add support!
  private String scanId;
  private PolarityType polarity;
  private Range<Double> mzRange;
  private float retentionTime = 0;
  private double precursorMZ = 0;

  public WatersRawImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // storage in raw data file
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = newMZmineFile;
    this.parameters = parameters;
    this.module = module;
  }

  public static File unzipWatersParser() throws IOException {
    final String tmpPath = System.getProperty("java.io.tmpdir");
    File watersRawFileParserFolder = new File(tmpPath, "mzmine_waters_raw_parser");
    final File watersRawFileParserExe = new File(watersRawFileParserFolder, "WatersRawDump.exe");

    // Check if it has already been unzipped
    if (watersRawFileParserFolder.exists() && watersRawFileParserFolder.isDirectory()
        && watersRawFileParserFolder.canRead() && watersRawFileParserExe.exists()
        && watersRawFileParserExe.isFile() && watersRawFileParserExe.canExecute()) {
      logger.finest("Waters RawFileParser found in folder " + watersRawFileParserFolder);
      return watersRawFileParserFolder;
    }
    synchronized (WatersRawImportTask.class) {
      // double checked
      if (watersRawFileParserFolder.exists() && watersRawFileParserFolder.isDirectory()
          && watersRawFileParserFolder.canRead() && watersRawFileParserExe.exists()
          && watersRawFileParserExe.isFile() && watersRawFileParserExe.canExecute()) {
        logger.finest("Waters RawFileParser found in folder " + watersRawFileParserFolder);
        return watersRawFileParserFolder;
      }

      // In case the folder already exists, unzip to a different folder
      if (watersRawFileParserFolder.exists()) {
        logger.finest("Folder " + watersRawFileParserFolder + " exists, creating a new one");
        watersRawFileParserFolder = FileAndPathUtil.createTempDirectory("mzmine_waters_raw_parser")
            .toFile();
      }

      logger.finest("Unpacking Waters RawFileParser to folder " + watersRawFileParserFolder);
      InputStream zipStream = WatersRawImportTask.class.getResourceAsStream(
          "/vendorlib/waters/waters.zip");
      if (zipStream == null) {
        throw new IOException("Failed to open the resource /vendorlib/waters/waters.zip");
      }
      ZipInputStream zipInputStream = new ZipInputStream(zipStream);
      ZipUtils.unzipStream(zipInputStream, watersRawFileParserFolder);
      zipInputStream.close();

      // Delete the temporary folder on application exit
      FileUtils.forceDeleteOnExit(watersRawFileParserFolder);

      return watersRawFileParserFolder;
    }
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Opening file " + file);

    // Check the OS we are running
    String osName = System.getProperty("os.name").toUpperCase();
    String rawDumpPath = "";

    try {
      final File folder = unzipWatersParser();
      rawDumpPath = new File(folder, "WatersRawDump.exe").getPath();
    } catch (IOException e) {
      final String msg = "Error while reading waters raw library. " + e.getMessage();
      logger.log(Level.WARNING, msg, e);
      setErrorMessage(msg);
      setStatus(TaskStatus.ERROR);
      return;
    }

    String cmdLine[];

    if (osName.toUpperCase().contains("WINDOWS")) {
      cmdLine = new String[]{rawDumpPath, this.file.getPath()};
    } else {
      cmdLine = new String[]{"wine", rawDumpPath, this.file.getPath()};
    }

    try {

      // Create a separate process and execute RAWdump.exe
      dumper = Runtime.getRuntime().exec(cmdLine);

      // Get the stdout of RAWdump.exe process as InputStream
      InputStream dumpStream = dumper.getInputStream();
      BufferedInputStream bufStream = new BufferedInputStream(dumpStream);

      // Read the dump data
      readRAWDump(bufStream);

      // Finish
      bufStream.close();

      if (isCanceled()) {
        dumper.destroy();
        return;
      }

      if (parsedScans == 0) {
        throw (new Exception("No scans found"));
      }

      if (parsedScans != totalScans) {
        throw (new Exception(
            "RAW dump process crashed before all scans were extracted (" + parsedScans + " out of "
                + totalScans + ")"));
      }

      newMZmineFile.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
      project.addFile(newMZmineFile);

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

    logger.info("Finished parsing " + this.file + ", parsed " + parsedScans + " scans");
    setStatus(TaskStatus.FINISHED);

  }

  @Override
  public String getTaskDescription() {
    return "Opening file " + file;
  }

  /**
   * This method reads the dump of the RAW data file produced by RAWdump.exe utility (see
   * RAWdump.cpp source for details).
   */
  private void readRAWDump(InputStream dumpStream) throws IOException {

    String line;
    byte byteBuffer[] = new byte[100000];
    double mzValuesBuffer[] = new double[10000];
    double intensityValuesBuffer[] = new double[10000];

    while ((line = TextUtils.readLineFromStream(dumpStream)) != null) {

      if (isCanceled()) {
        return;
      }

      if (line.startsWith("ERROR: ")) {
        throw (new IOException(file.getAbsolutePath() + line.substring("ERROR: ".length())));
      }

      if (line.startsWith("NUMBER OF SCANS: ")) {
        totalScans = Integer.parseInt(line.substring("NUMBER OF SCANS: ".length()));
      }

      if (line.startsWith("SCAN NUMBER: ")) {
        scanNumber = Integer.parseInt(line.substring("SCAN NUMBER: ".length()));
      }

      if (line.startsWith("SCAN ID: ")) {
        scanId = line.substring("SCAN ID: ".length());
      }

      if (line.startsWith("MS LEVEL: ")) {
        msLevel = Integer.parseInt(line.substring("MS LEVEL: ".length()));
      }

      if (line.startsWith("POLARITY: ")) {
        if (line.contains("-")) {
          polarity = PolarityType.NEGATIVE;
        } else if (line.contains("+")) {
          polarity = PolarityType.POSITIVE;
        } else {
          polarity = PolarityType.UNKNOWN;
        }

      }

      if (line.startsWith("RETENTION TIME: ")) {
        // Retention time is reported in minutes.
        retentionTime = (float) Double.parseDouble(line.substring("RETENTION TIME: ".length()));
      }

      if (line.startsWith("PRECURSOR: ")) {
        String tokens[] = line.split(" ");
        double token2 = Double.parseDouble(tokens[1]);
        int token3 = Integer.parseInt(tokens[2]);
        precursorMZ = token2;
        precursorCharge = token3;
      }

      if (line.startsWith("MASS VALUES: ")) {
        Pattern p = Pattern.compile("MASS VALUES: (\\d+) x (\\d+) BYTES");
        Matcher m = p.matcher(line);
        if (!m.matches()) {
          throw new IOException("Could not parse line " + line);
        }
        numOfDataPoints = Integer.parseInt(m.group(1));
        final int byteSize = Integer.parseInt(m.group(2));

        final int numOfBytes = numOfDataPoints * byteSize;
        if (byteBuffer.length < numOfBytes) {
          byteBuffer = new byte[numOfBytes * 2];
        }
        dumpStream.read(byteBuffer, 0, numOfBytes);

        ByteBuffer mzByteBuffer = ByteBuffer.wrap(byteBuffer, 0, numOfBytes)
            .order(ByteOrder.LITTLE_ENDIAN);
        if (mzValuesBuffer.length < numOfDataPoints) {
          mzValuesBuffer = new double[numOfDataPoints * 2];
        }

        for (int i = 0; i < numOfDataPoints; i++) {
          double newValue;
          if (byteSize == 8) {
            newValue = mzByteBuffer.getDouble();
          } else {
            newValue = mzByteBuffer.getFloat();
          }
          mzValuesBuffer[i] = newValue;
        }

      }

      if (line.startsWith("INTENSITY VALUES: ")) {
        Pattern p = Pattern.compile("INTENSITY VALUES: (\\d+) x (\\d+) BYTES");
        Matcher m = p.matcher(line);
        if (!m.matches()) {
          throw new IOException("Could not parse line " + line);
        }
        // numOfDataPoints must be same for MASS VALUES and INTENSITY
        // VALUES
        if (numOfDataPoints != Integer.parseInt(m.group(1))) {
          throw new IOException(
              "Scan " + scanNumber + " contained " + numOfDataPoints + " mass values, but "
                  + m.group(1) + " intensity values");
        }
        final int byteSize = Integer.parseInt(m.group(2));

        final int numOfBytes = numOfDataPoints * byteSize;
        if (byteBuffer.length < numOfBytes) {
          byteBuffer = new byte[numOfBytes * 2];
        }
        dumpStream.read(byteBuffer, 0, numOfBytes);

        ByteBuffer intensityByteBuffer = ByteBuffer.wrap(byteBuffer, 0, numOfBytes)
            .order(ByteOrder.LITTLE_ENDIAN);
        if (intensityValuesBuffer.length < numOfDataPoints) {
          intensityValuesBuffer = new double[numOfDataPoints * 2];
        }
        for (int i = 0; i < numOfDataPoints; i++) {
          double newValue;
          if (byteSize == 8) {
            newValue = intensityByteBuffer.getDouble();
          } else {
            newValue = intensityByteBuffer.getFloat();
          }
          intensityValuesBuffer[i] = newValue;
        }

        // INTENSITY VALUES was the last item of the scan, so now we can
        // convert the data to DataPoint[] array and create a new scan

        double mzValues[] = new double[numOfDataPoints];
        double intensityValues[] = new double[numOfDataPoints];
        System.arraycopy(mzValuesBuffer, 0, mzValues, 0, numOfDataPoints);
        System.arraycopy(intensityValuesBuffer, 0, intensityValues, 0, numOfDataPoints);

        // Auto-detect whether this scan is centroided
        MassSpectrumType spectrumType = ScanUtils.detectSpectrumType(mzValues, intensityValues);

        final DDAMsMsInfo info =
            msLevel != 1 && precursorMZ != 0d ? new DDAMsMsInfoImpl(precursorMZ, precursorCharge,
                null, null, null, msLevel, ActivationMethod.UNKNOWN, null) : null;

        SimpleScan newScan = new SimpleScan(newMZmineFile, scanNumber, msLevel, retentionTime, info,
            mzValues, intensityValues, spectrumType, polarity, scanId, mzRange);
        newMZmineFile.addScan(newScan);

        parsedScans++;

        // Clean the variables for next scan
        scanNumber = 0;
        scanId = null;
        polarity = null;
        mzRange = null;
        msLevel = 0;
        retentionTime = 0;
        precursorMZ = 0;
        precursorCharge = 0;
        numOfDataPoints = 0;
        mobility = 0;

      }

    }

  }

  @Override
  public void cancel() {
    super.cancel();
    // Try to destroy the dumper process
    if (dumper != null) {
      dumper.destroy();
    }
  }
}
