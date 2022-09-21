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

package io.github.mzmine.modules.io.export_scans;

import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.FileType;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.SimpleRawDataFile;
import io.github.msdk.io.mzml.MzMLFileExportMethod;
import io.github.msdk.io.mzml.data.MzMLCompressionType;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.MZmineToMSDKMsScan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exports a spectrum to a file.
 */
public class ExportScansTask extends AbstractTask {

  // Logger
  private static final Logger logger = Logger.getLogger(ExportScansTask.class.getName());

  private final File exportFile;
  private final Scan[] scans;
  private final String extension;

  private int progress;
  private int progressMax;

  private boolean useMassList;
  private MzMLFileExportMethod method;

  public ExportScansTask(Scan[] scans, ParameterSet parameters) {
    super(null, Instant.now()); // no new data stored -> null, date irrelevant (not used in batch)
    progress = 0;
    progressMax = 0;
    this.scans = scans;
    useMassList = parameters.getParameter(ExportScansParameters.export_masslist).getValue();
    extension = parameters.getParameter(ExportScansParameters.formats).getValue().toString();

    this.exportFile = FileAndPathUtil
        .getRealFilePath(parameters.getParameter(ExportScansParameters.file).getValue(), extension);
  }

  @Override
  public String getTaskDescription() {
    if (scans == null) {
      return "";
    }
    if (scans.length == 1) {
      return "Exporting spectrum # " + scans[0].getScanNumber() + " for "
          + scans[0].getDataFile().getName();
    } else {
      return "Exporting " + scans.length + " spectra";
    }
  }

  @Override
  public double getFinishedPercentage() {
    if(method != null) {
      return Objects.requireNonNullElse(method.getFinishedPercentage(), 0f);
    } else if(progressMax != 0) {
      return (double) progress / (double) progressMax;
    }
    return 0;
  }

  @Override
  public void run() {

    // Update the status of this task
    setStatus(TaskStatus.PROCESSING);

    // Handle text export below
    try {

      // Handle mzML export
      if (extension.equalsIgnoreCase("mzML")) {
        exportmzML();
      } else {
        // Handle text export
        exportText();
      }
      // Success
      logger.info("Export of spectra finished");

      setStatus(TaskStatus.FINISHED);

    } catch (Throwable t) {

      logger.log(Level.SEVERE, "Spectrum export error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
    }
  }

  /**
   * Export the chromatogram - text formats
   *
   * @throws IOException if there are i/o problems.
   */
  public void exportText() throws IOException {

    // Open the writer - append data if file already exists
    final BufferedWriter writer = new BufferedWriter(new FileWriter(exportFile, true));
    try {
      for (Scan scan : scans) {
        logger.info("Exporting scan #" + scan.getScanNumber() + " of raw file: "
            + scan.getDataFile().getName());
        // Write Header row
        switch (extension) {
          case "txt":
            writer.write("Name: Scan#: " + scan.getScanNumber() + ", RT: " + scan.getRetentionTime()
                + " min");
            writer.newLine();
            break;
          case "mgf":
            writer.write("BEGIN IONS");
            writer.newLine();
            writer.write("PEPMASS=" + Objects.requireNonNullElse(scan.getPrecursorMz(), 0));
            writer.newLine();
            writer.write("CHARGE=" + Objects.requireNonNullElse(scan.getPrecursorCharge(), 0));
            writer.newLine();
            writer.write("MSLEVEL=" + scan.getMSLevel());
            writer.newLine();
            writer.write("Title: Scan#: " + scan.getScanNumber() + ", RT: "
                + scan.getRetentionTime() + " min");
            writer.newLine();
            break;
          case "msp":
            break;
        }

        // Write the data points
        DataPoint[] dataPoints = null;
        if (useMassList) {
          MassList list = scan.getMassList();
          if (list != null) {
            dataPoints = list.getDataPoints();
          }
        }
        if (dataPoints == null) {
          dataPoints = ScanUtils.extractDataPoints(scan);
        }

        final int itemCount = dataPoints.length;
        progressMax = itemCount;

        for (int i = 0; i < itemCount; i++) {

          // Write data point row
          writer.write(dataPoints[i].getMZ() + " " + dataPoints[i].getIntensity());
          writer.newLine();

          progress = i + 1;
        }

        // Write footer row
        if (extension.equals("mgf")) {
          writer.write("END IONS");
          writer.newLine();
        }

        writer.newLine();
      }
    } catch (Exception e) {
      throw (new IOException(e));
    } finally {
      // Close
      writer.close();
    }
  }

  /**
   * Export the chromatogram - mzML format
   *
   * @throws IOException if there are i/o problems.
   */

  public void exportmzML() throws MSDKException {

    progressMax = scans.length;
    // Initialize objects
    SimpleRawDataFile msdkRawFile =
        new SimpleRawDataFile("MZmine mzML export", Optional.empty(), FileType.MZML);
    for (Scan scan : scans) {
      MsScan MSDKscan = new MZmineToMSDKMsScan(scan);
      msdkRawFile.addScan(MSDKscan);
    }
    // Actually write to disk
    method = new MzMLFileExportMethod(msdkRawFile, exportFile,
        MzMLCompressionType.ZLIB, MzMLCompressionType.ZLIB);
    method.execute();
  }
}
