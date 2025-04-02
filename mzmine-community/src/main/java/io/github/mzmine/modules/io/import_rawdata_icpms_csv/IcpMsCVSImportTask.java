/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_icpms_csv;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IcpMsCVSImportTask extends AbstractTask implements RawDataImportTask {

  private Logger logger = Logger.getLogger(IcpMsCVSImportTask.class.getName());

  protected String dataSource;
  private File file;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private RawDataFile finalRawDataFile;

  private int totalScans, parsedScans;

  public IcpMsCVSImportTask(MZmineProject project, File fileToOpen,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable MemoryMapStorage storage) {
    super(storage, moduleCallDate); // storage in raw data file
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = new RawDataFileImpl(file.getName(), file.getAbsolutePath(),
        getMemoryMapStorage());
    this.parameters = parameters;
    this.module = module;
  }

  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    Scanner scanner;

    logger.setLevel(Level.ALL);

    try {
      scanner = new Scanner(file);

      dataSource = getFileName(scanner);
      if (dataSource == null) {
        setErrorMessage("Could not open data file " + file.getAbsolutePath());
        setStatus(TaskStatus.ERROR);
        return;
      }
      logger.info("opening raw file " + dataSource);

      String acquisitionDate = getAcqusitionDate(scanner);
      if (acquisitionDate == null) {
        setErrorMessage("Could not find acquisition date in file " + file.getAbsolutePath());
        setStatus(TaskStatus.ERROR);
        return;
      }

      logger.info("Date of acquisition " + acquisitionDate);

      // scanner.useDelimiter(",");

      List<String> mzsList = new ArrayList<String>();
      String mstype = "";
      String ions = "";
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        logger.fine("checking line: " + line + " for 'Time'...");
        if (line.startsWith("Time")) {
          String[] axes = line.split(",");
          logger.fine("Found axes" + Arrays.toString(axes));
          for (int i = 1; i < axes.length; i++) {
            String axis = axes[i];
            ions += axis + ", ";
            if (axis.contains("->")) {
              mstype = "MS/MS";
              logger.fine("axis " + axis + " is an ms^2 scan");
              String mz = axis.substring(axis.indexOf("-> ") + 3);
              mz.trim();
              logger.fine("Axis " + axis + " was scanned at m/z = '" + mz + "'");
              mzsList.add(mz);
            } else {
              String mz = axis.replaceAll("[^0-9]", "");
              logger.fine("axis " + axis + " was scanned at " + mz);
              mzsList.add(mz);
            }
          }
          break;
        }
      }

      double[] mzValues = new double[mzsList.size()];
      for (int i = 0; i < mzsList.size(); i++) {
        mzValues[i] = Integer.valueOf(mzsList.get(i));
      }

      Range<Double> mzRange = Range.closed(mzValues[0] - 10, mzValues[mzValues.length - 1] + 10);

      int scanNumber = 1;

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line == null || line.trim().equals("")) {
          continue;
        }
        String[] columns = line.split(",");
        if (columns == null || columns.length != mzValues.length + 1) {
          continue;
        }

        float rt = (float) (Double.valueOf(columns[0]) / 60);

        double intensityValues[] = new double[mzValues.length];
        for (int i = 0; i < intensityValues.length; i++) {
          String intensity = columns[i + 1];
          intensityValues[i] = Double.valueOf(intensity);
        }

        Scan scan = new SimpleScan(newMZmineFile, scanNumber, 1, rt, null, mzValues,
            intensityValues, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE,
            "ICP-" + mstype + " " + ions.substring(0, ions.length() - 2), mzRange);

        newMZmineFile.addScan(scan);
        scanNumber++;
      }

      newMZmineFile.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
      project.addFile(newMZmineFile);

    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
      return;
    }

    this.setStatus(TaskStatus.FINISHED);
  }

  private @Nullable String getFileName(@NotNull Scanner scanner) {
    String path = null;
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.contains(":") && line.contains("\\")) {
        path = line;
        return path;
      }
    }
    return path;
  }

  private @Nullable String getAcqusitionDate(@NotNull Scanner scanner) {
    String acquisitionDate = null;

    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.startsWith("Acquired")) {
        int begin = line.indexOf(":") + 2;
        line.subSequence(begin, begin + (new String("00/00/0000 00:00:00")).length());
        return line;
      }
    }
    return acquisitionDate;
  }

  @Override
  public RawDataFile getImportedRawDataFile() {
    return getStatus() == TaskStatus.FINISHED ? newMZmineFile : null;
  }
}
