/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.rawdataimport.fileformats;

import java.io.File;
import java.util.Scanner;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;

public class AgilentCsvReadTask extends AbstractTask {

  protected String dataSource;
  private File file;
  private MZmineProject project;
  private RawDataFile newMZmineFile;

  private int totalScans, parsedScans;

  /**
   * Creates a new AgilentCSVReadTask
   *
   * @param project
   * @param fileToOpen A File instance containing the file to be read
   * @param newMZmineFile
   */
  public AgilentCsvReadTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile) {
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = newMZmineFile;
  }

  /**
   * Reads the file.
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    Scanner scanner;

    try {

      scanner = new Scanner(this.file);

      this.dataSource = this.getMetaData(scanner, "file name");

      String[] range = this.getMetaData(scanner, "mass range").split(",");
      newMZmineFile.setMZRange(1,
          Range.closed(Double.parseDouble(range[0]), Double.parseDouble(range[1])));
      range = this.getMetaData(scanner, "time range").split(",");
      newMZmineFile.setRTRange(1,
          Range.closed((float) Double.parseDouble(range[0]), (float) Double.parseDouble(range[1])));
      totalScans = Integer.parseInt(this.getMetaData(scanner, "number of spectra"));

      // advance to the spectrum data...
      while (!scanner.nextLine().trim().equals("[spectra]")) {
      }

      scanner.useDelimiter(",");

      for (parsedScans = 0; parsedScans < totalScans; parsedScans++) {

        if (isCanceled()) {
          return;
        } // if the task is canceled.

        float retentionTime = (float) scanner.nextDouble();
        int msLevel = scanner.nextInt(); // not sure about this value
        scanner.next();
        scanner.next();
        int charge = (scanner.next().equals("+") ? 1 : -1);
        scanner.next();

        int spectrumSize = scanner.nextInt();
        double mzValues[] = new double[spectrumSize];
        double intensityValues[] = new double[spectrumSize];

        for (int j = 0; j < spectrumSize; j++) {
          mzValues[j] = scanner.nextDouble();
          intensityValues[j] = scanner.nextDouble();
        }
        newMZmineFile.addScan(
            new SimpleScan(newMZmineFile, parsedScans + 1, msLevel, retentionTime, 0.0, charge,
                mzValues, intensityValues, ScanUtils.detectSpectrumType(mzValues, intensityValues),
                PolarityType.UNKNOWN, "", null));

        scanner.nextLine();
      }

      project.addFile(newMZmineFile);

    } catch (Exception e) {
      setErrorMessage(e.getMessage());
      this.setStatus(TaskStatus.ERROR);
      return;
    }

    this.setStatus(TaskStatus.FINISHED);

  }

  /**
   * Reads meta information on the file. This must be called with the keys in order, as it does not
   * reset the scanner position after reading.
   *
   * @param scanner The Scanner which is reading this AgilentCSV file.
   * @param key The key for the metadata to return the value of.
   */
  private String getMetaData(Scanner scanner, String key) {
    String line = "";
    while (!line.trim().startsWith(key) && scanner.hasNextLine()) {
      line = scanner.nextLine();
      if (line.trim().startsWith(key)) {
        return line.split(",", 2)[1].trim();
      }
    }
    return null;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Opening file " + file;
  }

}
