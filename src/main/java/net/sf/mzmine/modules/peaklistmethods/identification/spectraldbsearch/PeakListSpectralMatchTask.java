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
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoper;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.exceptions.MissingMassListException;
import net.sf.mzmine.util.maths.similarity.spectra.SpectraSimilarity;
import net.sf.mzmine.util.maths.similarity.spectra.SpectralSimilarityFunction;
import net.sf.mzmine.util.scans.ScanAlignment;
import net.sf.mzmine.util.scans.ScanUtils;
import net.sf.mzmine.util.scans.sorting.ScanSortMode;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

public class PeakListSpectralMatchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String METHOD = "Spectral DB search";
  private static final int MAX_ERROR = 3;
  private int errorCounter = 0;
  private final PeakList peakList;
  private final @Nonnull String massListName;
  private final File dataBaseFile;
  private final MZTolerance mzToleranceSpectra;
  private final MZTolerance mzTolerancePrecursor;
  private final RTTolerance rtTolerance;
  private final boolean useRT;
  private int finishedRows = 0;
  private final int totalRows;

  private ParameterSet parameters;

  private final int msLevel;
  private final double noiseLevel;
  private final int minMatch;
  private List<SpectralDBEntry> list;

  private int count = 0;

  // as this module is started in a series the start entry is saved to track progress
  private int startEntry;
  private int listsize;
  private MZmineProcessingStep<SpectralSimilarityFunction> simFunction;

  // remove 13C isotopes
  private boolean removeIsotopes;
  private MassListDeisotoperParameters deisotopeParam;

  private final boolean cropSpectraToOverlap;

  public PeakListSpectralMatchTask(PeakList peakList, ParameterSet parameters, int startEntry,
      List<SpectralDBEntry> list) {
    this.peakList = peakList;
    this.parameters = parameters;
    this.startEntry = startEntry;
    this.list = list;
    listsize = list.size();
    dataBaseFile = parameters.getParameter(LocalSpectralDBSearchParameters.dataBaseFile).getValue();
    massListName = parameters.getParameter(LocalSpectralDBSearchParameters.massList).getValue();
    mzToleranceSpectra =
        parameters.getParameter(LocalSpectralDBSearchParameters.mzTolerance).getValue();
    msLevel = parameters.getParameter(LocalSpectralDBSearchParameters.msLevel).getValue();
    noiseLevel = parameters.getParameter(LocalSpectralDBSearchParameters.noiseLevel).getValue();

    useRT = parameters.getParameter(LocalSpectralDBSearchParameters.rtTolerance).getValue();
    rtTolerance = parameters.getParameter(LocalSpectralDBSearchParameters.rtTolerance)
        .getEmbeddedParameter().getValue();

    minMatch = parameters.getParameter(LocalSpectralDBSearchParameters.minMatch).getValue();
    simFunction =
        parameters.getParameter(LocalSpectralDBSearchParameters.similarityFunction).getValue();
    removeIsotopes =
        parameters.getParameter(LocalSpectralDBSearchParameters.deisotoping).getValue();
    deisotopeParam = parameters.getParameter(LocalSpectralDBSearchParameters.deisotoping)
        .getEmbeddedParameters();
    cropSpectraToOverlap =
        parameters.getParameter(LocalSpectralDBSearchParameters.cropSpectraToOverlap).getValue();
    if (msLevel > 1)
      mzTolerancePrecursor =
          parameters.getParameter(LocalSpectralDBSearchParameters.mzTolerancePrecursor).getValue();
    else
      mzTolerancePrecursor = null;

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
        "(entry {2}-{3}) spectral database identification in {0} using database {1}",
        peakList.getName(), dataBaseFile.getName(), startEntry, startEntry + listsize - 1);
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    for (PeakListRow row : peakList.getRows()) {
      if (isCanceled()) {
        logger.info("Added " + count + " spectral library matches (before being cancelled)");
        repaintWindow();
        return;
      }

      // check for MS1 or MSMS scan
      Scan scan;
      if (msLevel == 1) {
        scan = row.getBestPeak().getRepresentativeScan();
      } else if (msLevel >= 2) {
        scan = row.getBestFragmentation();
      } else {
        logger.log(Level.WARNING, "Data base matching failed. MS level is not set correctly");
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Data base matching failed. MS level is not set correctly");
        return;
      }
      if (scan != null) {
        try {
          // get mass list and perform deisotoping if active
          DataPoint[] rowMassList = getDataPoints(row, true);
          if (removeIsotopes)
            rowMassList = removeIsotopes(rowMassList);

          // match against all library entries
          for (SpectralDBEntry ident : list) {
            SpectraSimilarity sim = spectraDBMatch(row, rowMassList, ident);
            if (sim != null) {
              count++;
              addIdentity(row, ident, sim);
            }
          }
        } catch (MissingMassListException e) {
          logger.log(Level.WARNING, "No mass list in spectrum for rowID=" + row.getID(), e);
          errorCounter++;
        }
        // check for max error (missing masslist)
        if (errorCounter > MAX_ERROR) {
          logger.log(Level.WARNING, "Data base matching failed. To many missing mass lists ");
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Data base matching failed. To many missing mass lists ");
          list = null;
          return;
        }
      }
      // next row
      finishedRows++;
    }
    if (count > 0)
      logger.info("Added " + count + " spectral library matches");

    // Repaint the window to reflect the change in the peak list
    repaintWindow();

    list = null;
    setStatus(TaskStatus.FINISHED);
  }

  private void repaintWindow() {
    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();
  }

  /**
   * Remove 13C isotopes from masslist
   * 
   * @param a
   * @return
   */
  private DataPoint[] removeIsotopes(DataPoint[] a) {
    return MassListDeisotoper.filterIsotopes(a, deisotopeParam);
  }

  /**
   * 
   * @param row
   * @param ident
   * @return spectral similarity or null if no match
   */
  private SpectraSimilarity spectraDBMatch(PeakListRow row, DataPoint[] rowMassList,
      SpectralDBEntry ident) {
    // retention time
    // MS level 1 or check precursorMZ
    if (checkRT(row, ident) && (msLevel == 1 || checkPrecursorMZ(row, ident))) {
      DataPoint[] library = ident.getDataPoints();
      if (removeIsotopes)
        library = removeIsotopes(library);

      // crop the spectra to their overlapping mz range
      // helpful when comparing spectra, acquired with different fragmentation energy
      DataPoint[] query = rowMassList;
      if (cropSpectraToOverlap) {
        DataPoint[][] cropped = ScanAlignment.cropToOverlap(mzToleranceSpectra, library, query);
        library = cropped[0];
        query = cropped[1];
      }

      // check spectra similarity
      SpectraSimilarity sim = createSimilarity(library, query);
      if (sim != null)
        return sim;
    }
    return null;
  }

  /**
   * Uses the similarity function and filter to create similarity.
   * 
   * @param a
   * @param b
   * @return positive match with similarity or null if criteria was not met
   */
  private SpectraSimilarity createSimilarity(DataPoint[] library, DataPoint[] query) {
    return simFunction.getModule().getSimilarity(simFunction.getParameterSet(), mzToleranceSpectra,
        minMatch, library, query);
  }

  private boolean checkPrecursorMZ(PeakListRow row, SpectralDBEntry ident) {
    if (ident.getPrecursorMZ() == null)
      return false;
    else
      return mzTolerancePrecursor.checkWithinTolerance(ident.getPrecursorMZ(), row.getAverageMZ());
  }

  private boolean checkRT(PeakListRow row, SpectralDBEntry ident) {
    Double rt = (Double) ident.getField(DBEntryField.RT).orElse(null);
    return (!useRT || rt == null || rtTolerance.checkWithinTolerance(rt, row.getAverageRT()));
  }

  /**
   * Thresholded masslist
   * 
   * @param row
   * @return
   * @throws MissingMassListException
   */
  private DataPoint[] getDataPoints(PeakListRow row, boolean noiseFilter)
      throws MissingMassListException {
    Scan scan = getScan(row);
    if (scan == null || scan.getMassList(massListName) == null)
      return new DataPoint[0];

    MassList masses = scan.getMassList(massListName);
    DataPoint[] dps = masses.getDataPoints();
    if (noiseFilter)
      return ScanUtils.getFiltered(dps, noiseLevel);
    else
      return dps;
  }

  public Scan getScan(PeakListRow row) throws MissingMassListException {
    final Scan scan;
    if (msLevel == 1) {
      scan = row.getBestPeak().getRepresentativeScan();
    } else if (msLevel >= 2) {
      // first entry is the best scan
      List<Scan> scans = ScanUtils.listAllFragmentScans(row, massListName, noiseLevel, minMatch,
          ScanSortMode.MAX_TIC);
      if (scans.isEmpty())
        return null;
      else
        scan = scans.get(0);
    } else
      scan = null;

    return scan;
  }

  private void addIdentity(PeakListRow row, SpectralDBEntry ident, SpectraSimilarity sim)
      throws MissingMassListException {
    // add new identity to the row
    row.addPeakIdentity(new SpectralDBPeakIdentity(getScan(row), massListName, ident, sim, METHOD),
        false);

    // Notify the GUI about the change in the project
    MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);
  }

  public int getCount() {
    return count;
  }

}
