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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import org.jetbrains.annotations.NotNull;

class LocalCSVDatabaseSearchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private FeatureList peakList;

  private String[][] databaseValues;
  private int finishedLines = 0;

  private File dataBaseFile;
  private String fieldSeparator;
  private FieldItem[] fieldOrder;
  private boolean ignoreFirstLine;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private ParameterSet parameters;

  LocalCSVDatabaseSearchTask(FeatureList peakList, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.peakList = peakList;
    this.parameters = parameters;

    dataBaseFile =
        parameters.getParameter(LocalCSVDatabaseSearchParameters.dataBaseFile).getValue();
    fieldSeparator =
        parameters.getParameter(LocalCSVDatabaseSearchParameters.fieldSeparator).getValue();

    fieldOrder = parameters.getParameter(LocalCSVDatabaseSearchParameters.fieldOrder).getValue();

    ignoreFirstLine =
        parameters.getParameter(LocalCSVDatabaseSearchParameters.ignoreFirstLine).getValue();
    mzTolerance = parameters.getParameter(LocalCSVDatabaseSearchParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(LocalCSVDatabaseSearchParameters.rtTolerance).getValue();

  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (databaseValues == null)
      return 0;
    return ((double) finishedLines) / databaseValues.length;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Peak identification of " + peakList + " using database " + dataBaseFile;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    try {
      // read database contents in memory
      FileReader dbFileReader = new FileReader(dataBaseFile);
      databaseValues = CSVParser.parse(dbFileReader, fieldSeparator.charAt(0));
      if (ignoreFirstLine)
        finishedLines++;
      for (; finishedLines < databaseValues.length; finishedLines++) {
        if (isCanceled()) {
          dbFileReader.close();
          return;
        }
        try {
          processOneLine(databaseValues[finishedLines]);
        } catch (Exception e) {
          // ignore incorrect lines
        }
      }
      dbFileReader.close();

    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not read file " + dataBaseFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return;
    }

    // Add task description to peakList
    peakList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        "Peak identification using database " + dataBaseFile,
        LocalCSVDatabaseSearchModule.class, parameters, getModuleCallDate()));


    setStatus(TaskStatus.FINISHED);

  }

  private void processOneLine(String values[]) {

    int numOfColumns = Math.min(fieldOrder.length, values.length);

    String lineID = null, lineName = null, lineFormula = null;
    double lineMZ = 0, lineRT = 0;

    for (int i = 0; i < numOfColumns; i++) {
      if (fieldOrder[i] == FieldItem.FIELD_ID)
        lineID = values[i];
      if (fieldOrder[i] == FieldItem.FIELD_NAME)
        lineName = values[i];
      if (fieldOrder[i] == FieldItem.FIELD_FORMULA)
        lineFormula = values[i];
      if (fieldOrder[i] == FieldItem.FIELD_MZ)
        lineMZ = Double.parseDouble(values[i]);
      if (fieldOrder[i] == FieldItem.FIELD_RT)
        lineRT = Double.parseDouble(values[i]);
    }

    SimpleFeatureIdentity newIdentity =
        new SimpleFeatureIdentity(lineName, lineFormula, dataBaseFile.getName(), lineID, null);

    for (FeatureListRow peakRow : peakList.getRows()) {

      Range<Double> mzRange = mzTolerance.getToleranceRange(peakRow.getAverageMZ());
      Range<Float> rtRange = rtTolerance.getToleranceRange(peakRow.getAverageRT());

      boolean mzMatches = (lineMZ == 0d) || mzRange.contains(lineMZ);
      boolean rtMatches = (lineRT == 0d) || rtRange.contains((float) lineRT);

      if (mzMatches && rtMatches) {

        logger.finest("Found compound " + lineName + " (m/z " + lineMZ + ", RT " + lineRT + ")");

        // add new identity to the row
        peakRow.addFeatureIdentity(newIdentity, false);

      }
    }

  }
}
