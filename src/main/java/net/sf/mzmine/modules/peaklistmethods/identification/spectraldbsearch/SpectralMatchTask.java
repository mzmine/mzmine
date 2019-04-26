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

package net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.DBEntryField;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.SpectraSimilarity;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.SpectralDBEntry;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.SpectralDBPeakIdentity;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.sorting.ScanSortMode;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.exceptions.MissingMassListException;

public class SpectralMatchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String METHOD = "MS/MS spectral DB search";
  private static final int MAX_ERROR = 3;
  private int errorCounter = 0;
  private final PeakList peakList;
  private final @Nonnull String massListName;
  private final File dataBaseFile;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final boolean useRT;
  private int finishedRows = 0;
  private final int totalRows;

  private ParameterSet parameters;

  private final double noiseLevel;
  private final double minSimilarity;
  private final int minMatch;
  private List<SpectralDBEntry> list;

  private int count = 0;

  // as this module is started in a series the start entry is saved to track progress
  private int startEntry;

  public SpectralMatchTask(PeakList peakList, ParameterSet parameters, int startEntry,
      List<SpectralDBEntry> list) {
    this.peakList = peakList;
    this.parameters = parameters;
    this.startEntry = startEntry;
    this.list = list;
    dataBaseFile = parameters.getParameter(LocalSpectralDBSearchParameters.dataBaseFile).getValue();
    massListName = parameters.getParameter(LocalSpectralDBSearchParameters.massList).getValue();
    mzTolerance = parameters.getParameter(LocalSpectralDBSearchParameters.mzTolerance).getValue();
    noiseLevel = parameters.getParameter(LocalSpectralDBSearchParameters.noiseLevelMS2).getValue();

    useRT = parameters.getParameter(LocalSpectralDBSearchParameters.rtTolerance).getValue();
    rtTolerance = parameters.getParameter(LocalSpectralDBSearchParameters.rtTolerance)
        .getEmbeddedParameter().getValue();

    minMatch = parameters.getParameter(LocalSpectralDBSearchParameters.minMatch).getValue();
    minSimilarity = parameters.getParameter(LocalSpectralDBSearchParameters.minCosine).getValue();
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
    return MessageFormat.format(
        "(entry {2}-{3}) MS/MS spectral database identification in {0} using database {1}",
        peakList.getName(), dataBaseFile.getName(), startEntry, startEntry + list.size() - 1);
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    try {
      for (PeakListRow row : peakList.getRows()) {
        if (row.getBestFragmentation() != null) {
          for (SpectralDBEntry ident : list) {
            SpectraSimilarity sim = spectraDBMatch(row, ident);
            if (sim != null) {
              count++;
              addIdentity(row, ident, sim);
            }
            // check for max error (missing masslist)
            if (errorCounter > MAX_ERROR) {
              logger.log(Level.WARNING,
                  "MS/MS data base matching failed. To many missing mass lists ");
              setStatus(TaskStatus.ERROR);
              setErrorMessage("MS/MS data base matching failed. To many missing mass lists ");
              list = null;
              return;
            }
          }
        }
        // next row
        finishedRows++;
      }
      logger.info("Added " + count + " spectral library matches");
    } catch (Exception e) {
    }
    // Repaint the window to reflect the change in the peak list
    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();

    list = null;
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * 
   * @param row
   * @param ident
   * @return spectral similarity or null if no match
   */
  private SpectraSimilarity spectraDBMatch(PeakListRow row, SpectralDBEntry ident) {
    // retention time
    Double rt = (Double) ident.getField(DBEntryField.RT).orElse(null);
    if (!useRT || rt == null || rtTolerance.checkWithinTolerance(rt, row.getAverageRT())) {
      // precursor mz
      if (mzTolerance.checkWithinTolerance(ident.getPrecursorMZ(), row.getAverageMZ())) {
        try {
          // check MS2 similarity
          DataPoint[] rowMassList = getDataPoints(row);
          SpectraSimilarity sim = SpectraSimilarity.createMS2Sim(mzTolerance, ident.getDataPoints(),
              rowMassList, minMatch);
          if (sim != null && sim.getCosine() >= minSimilarity)
            return sim;
        } catch (MissingMassListException e) {
          logger.log(Level.WARNING, "No mass list in MS2 spectrum for rowID=" + row.getID(), e);
          errorCounter++;
          return null;
        }
      }
    }
    return null;
  }


  /**
   * Thresholded masslist
   * 
   * @param row
   * @return
   * @throws MissingMassListException
   */
  private DataPoint[] getDataPoints(PeakListRow row) throws MissingMassListException {
    // first entry is the best scan
    List<Scan> scans = ScanUtils.listAllFragmentScans(row, massListName, noiseLevel, minMatch,
        ScanSortMode.MAX_TIC);
    if (scans.isEmpty())
      return new DataPoint[0];

    MassList masses = scans.get(0).getMassList(massListName);
    if (masses == null)
      return new DataPoint[0];

    DataPoint[] dps = masses.getDataPoints();
    return ScanUtils.getFiltered(dps, noiseLevel);
  }

  private void addIdentity(PeakListRow row, SpectralDBEntry ident, SpectraSimilarity sim) {
    // add new identity to the row
    row.addPeakIdentity(new SpectralDBPeakIdentity(ident, sim, METHOD), true);

    // Notify the GUI about the change in the project
    MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);
  }

  public int getCount() {
    return count;
  }

}
