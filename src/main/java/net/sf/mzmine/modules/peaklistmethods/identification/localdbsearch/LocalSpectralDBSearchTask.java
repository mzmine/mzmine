/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.localdbsearch;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit.LibrarySubmitIonParameters;
import net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit.LibrarySubmitParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

class LocalSpectralDBSearchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final PeakList peakList;
  private final String massListName;
  private final File dataBaseFile;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final boolean useRT;
  private int finishedRows = 0;
  private final int totalRows;

  private ParameterSet parameters;

  LocalSpectralDBSearchTask(PeakList peakList, ParameterSet parameters) {
    this.peakList = peakList;
    this.parameters = parameters;
    dataBaseFile = parameters.getParameter(LocalSpectralDBSearchParameters.dataBaseFile).getValue();
    massListName = parameters.getParameter(LocalSpectralDBSearchParameters.massList).getValue();
    useRT = parameters.getParameter(LocalSpectralDBSearchParameters.rtTolerance).getValue();
    mzTolerance = parameters.getParameter(LocalSpectralDBSearchParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(LocalSpectralDBSearchParameters.rtTolerance)
        .getEmbeddedParameter().getValue();
    totalRows = peakList.getNumberOfRows();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0;
    return ((double) finishedRows) / totalRows;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "MS/MS spectral database identification of " + peakList + " using database "
        + dataBaseFile;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      List<SpectralDBPeakIdentity> list = new ArrayList<>();
      List<String> lines = Files.readLines(dataBaseFile, Charsets.UTF_8);
      for (String l : lines) {
        JsonReader reader = Json.createReader(new StringReader(l));
        JsonObject json = reader.readObject();
        list.add(getPeakIdentity(json));
      }

      for (PeakListRow row : peakList.getRows()) {
        if (row.getBestFragmentation() != null) {
          for (SpectralDBPeakIdentity ident : list) {
            if (spectraDBMatch(row, ident)) {
              addIdentity(row, ident);
            }
          }
        }
        // next row
        finishedRows++;
      }

    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not read file " + dataBaseFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return;
    }

    // Add task description to peakList
    peakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
        "Peak identification using MS/MS spectral database " + dataBaseFile, parameters));

    // Repaint the window to reflect the change in the peak list
    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();

    setStatus(TaskStatus.FINISHED);

  }


  private boolean spectraDBMatch(PeakListRow row, SpectralDBPeakIdentity ident) {
    if (!useRT || rtTolerance.checkWithinTolerance(ident.getRetentionTime(), row.getAverageRT())) {
      if (mzTolerance.checkWithinTolerance(ident.getMz(), row.getAverageMZ())) {
        // check MS2 similarity
      }
    }
    // TODO Auto-generated method stub
    return false;
  }

  public static DataPoint[] getDataPoints(JsonObject main) {
    JsonArray data = main.getJsonArray("peaks");
    DataPoint[] dps = new DataPoint[data.size()];
    for (int i = 0; i < data.size(); i++) {
      double mz = data.getJsonArray(i).getJsonNumber(0).doubleValue();
      double intensity = data.getJsonArray(i).getJsonNumber(1).doubleValue();
      dps[i] = new SimpleDataPoint(mz, intensity);
    }
    return dps;
  }

  public static SpectralDBPeakIdentity getPeakIdentity(JsonObject main) {
    String adduct = main.getString(LibrarySubmitIonParameters.ADDUCT.getName(), "");
    JsonNumber mz = main.getJsonNumber(LibrarySubmitIonParameters.MZ.getName());
    JsonNumber rt = main.getJsonNumber(LibrarySubmitIonParameters.RT.getName());
    String formula = main.getString(LibrarySubmitParameters.FORMULA.getName(), "");
    String name = main.getString(LibrarySubmitParameters.COMPOUND_NAME.getName(), "");
    if (formula.equals("N/A"))
      formula = "";

    DataPoint[] dps = getDataPoints(main);

    return new SpectralDBPeakIdentity(name, mz == null ? 0 : mz.doubleValue(),
        rt == null ? 0 : rt.doubleValue(), adduct, formula, "local MS/MS database", dps);
  }

  public void addIdentity(PeakListRow row, PeakIdentity ident) {
    // add new identity to the row
    row.addPeakIdentity(ident, true);

    // Notify the GUI about the change in the project
    MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);
  }

}
