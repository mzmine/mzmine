/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import java.awt.Color;
import java.io.File;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.LocalSpectralDBSearchParameters;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoper;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.exceptions.MissingMassListException;
import net.sf.mzmine.util.maths.similarity.spectra.SpectraSimilarity;
import net.sf.mzmine.util.maths.similarity.spectra.SpectralSimilarityFunction;
import net.sf.mzmine.util.scans.ScanAlignment;
import net.sf.mzmine.util.scans.ScanUtils;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

/**
 * Spectral match task to compare single spectra with spectral databases
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectralMatchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final int MAX_ERROR = 3;
  private int errorCounter = 0;
  private final File dataBaseFile;
  private final MZTolerance mzToleranceSpectra;
  private final MZTolerance mzTolerancePrecursor;
  private int finishedSteps = 0;
  private Scan currentScan;
  private SpectraPlot spectraPlot;
  private List<DataPoint[]> alignedSignals;
  private String massListName;

  private final double noiseLevel;
  private final int minMatch;
  private List<SpectralDBEntry> list;
  private int totalSteps;

  private List<SpectralDBPeakIdentity> matches;
  private SpectraIdentificationResultsWindow resultWindow;

  private int count = 0;
  private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");

  // as this module is started in a series the start entry is saved to track progress
  private int startEntry;
  private int listsize;

  // precursor mz as user parameter (extracted from scan and then checked by user)
  private double precursorMZ;
  private boolean usePrecursorMZ;

  private MZmineProcessingStep<SpectralSimilarityFunction> simFunction;
  // deisotoping of masslists
  private boolean removeIsotopes;
  private MassListDeisotoperParameters deisotopeParam;

  // crop to overlapping range (+- mzTol)
  private final boolean cropSpectraToOverlap;

  public SpectralMatchTask(ParameterSet parameters, int startEntry, List<SpectralDBEntry> list,
      SpectraPlot spectraPlot, Scan currentScan, SpectraIdentificationResultsWindow resultWindow) {
    this.startEntry = startEntry;
    this.list = list;
    this.currentScan = currentScan;
    this.spectraPlot = spectraPlot;
    this.resultWindow = resultWindow;

    listsize = list.size();
    dataBaseFile = parameters
        .getParameter(SpectraIdentificationSpectralDatabaseParameters.dataBaseFile).getValue();
    massListName = parameters.getParameter(SpectraIdentificationSpectralDatabaseParameters.massList)
        .getValue();
    mzToleranceSpectra = parameters
        .getParameter(SpectraIdentificationSpectralDatabaseParameters.mzTolerance).getValue();
    mzTolerancePrecursor = parameters
        .getParameter(SpectraIdentificationSpectralDatabaseParameters.mzTolerancePrecursor)
        .getValue();

    noiseLevel = parameters.getParameter(SpectraIdentificationSpectralDatabaseParameters.noiseLevel)
        .getValue();

    minMatch = parameters.getParameter(SpectraIdentificationSpectralDatabaseParameters.minMatch)
        .getValue();
    usePrecursorMZ = parameters
        .getParameter(SpectraIdentificationSpectralDatabaseParameters.usePrecursorMZ).getValue();
    precursorMZ =
        parameters.getParameter(SpectraIdentificationSpectralDatabaseParameters.usePrecursorMZ)
            .getEmbeddedParameter().getValue();
    simFunction =
        parameters.getParameter(LocalSpectralDBSearchParameters.similarityFunction).getValue();
    removeIsotopes =
        parameters.getParameter(LocalSpectralDBSearchParameters.deisotoping).getValue();
    deisotopeParam = parameters.getParameter(LocalSpectralDBSearchParameters.deisotoping)
        .getEmbeddedParameters();
    cropSpectraToOverlap =
        parameters.getParameter(LocalSpectralDBSearchParameters.cropSpectraToOverlap).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalSteps == 0)
      return 0;
    return ((double) finishedSteps) / totalSteps;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return MessageFormat.format(
        "(entry {2}-{3}) spectral database identification in {0} using database {1}", "spectrum",
        dataBaseFile.getName(), startEntry, startEntry + listsize - 1);
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    // check for mass list
    DataPoint[] spectraMassList;
    try {
      spectraMassList = getDataPoints(currentScan);
    } catch (MissingMassListException e) {
      // no mass list
      setStatus(TaskStatus.ERROR);
      setErrorMessage(MessageFormat.format("No masslist for name: {0} in scan {1} of raw file {2}",
          massListName, currentScan.getScanNumber(), currentScan.getDataFile().getName()));
      return;
    }

    // remove 13C isotopes
    if (removeIsotopes)
      spectraMassList = removeIsotopes(spectraMassList);

    setStatus(TaskStatus.PROCESSING);
    try {
      totalSteps = list.size();
      matches = new ArrayList<>();
      for (SpectralDBEntry ident : list) {
        if (isCanceled()) {
          logger.info("Added " + count + " spectral library matches (before being cancelled)");
          repaintWindow();
          return;
        }

        SpectraSimilarity sim = spectraDBMatch(spectraMassList, ident);
        if (sim != null) {
          count++;
          // use SpectralDBPeakIdentity to store all resutls similar to peaklist method
          matches.add(new SpectralDBPeakIdentity(ident, sim,
              SpectraIdentificationSpectralDatabaseModule.MODULE_NAME));
        }
        // next row
        finishedSteps++;
      }
      addIdentities(matches);
      logger.info("Added " + count + " spectral library matches");

      // check if no match was found
      if (count == 0) {
        logger.log(Level.WARNING, "No data base matches found");
        setErrorMessage("No data base matches found. Spectral data base matching failed");
        list = null;
        return;
      }
    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE, "Spectral data base matching failed", e);
      setErrorMessage("Spectral data base matching failed");
      return;
    }

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
   * 
   * @param currentScan
   * @param ident
   * @return spectral similarity or null if no match
   */
  private SpectraSimilarity spectraDBMatch(DataPoint[] spectraMassList, SpectralDBEntry ident) {
    // do not check precursorMZ or precursorMZ within tolerances
    if (!usePrecursorMZ || (checkPrecursorMZ(precursorMZ, ident))) {
      DataPoint[] library = ident.getDataPoints();
      if (removeIsotopes)
        library = removeIsotopes(library);

      DataPoint[] query = spectraMassList;
      if (cropSpectraToOverlap) {
        DataPoint[][] cropped = ScanAlignment.cropToOverlap(mzToleranceSpectra, library, query);
        library = cropped[0];
        query = cropped[1];
      }

      // check spectra similarity
      return createSimilarity(library, query);
    }
    return null;
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
   * Uses the similarity function and filter to create similarity.
   * 
   * @param library
   * @param query
   * @return positive match with similarity or null if criteria was not met
   */
  private SpectraSimilarity createSimilarity(DataPoint[] library, DataPoint[] query) {
    return simFunction.getModule().getSimilarity(simFunction.getParameterSet(), mzToleranceSpectra,
        minMatch, library, query);
  }

  private boolean checkPrecursorMZ(double precursorMZ, SpectralDBEntry ident) {
    return ident.getPrecursorMZ() != null
        && mzTolerancePrecursor.checkWithinTolerance(ident.getPrecursorMZ(), precursorMZ);
  }



  /**
   * Get data points of mass list
   * 
   * @param scan
   * @return
   * @throws MissingMassListException
   */
  private DataPoint[] getDataPoints(Scan scan) throws MissingMassListException {
    MassList massList = scan.getMassList(massListName);
    if (massList == null)
      throw new MissingMassListException(massListName);
    else {
      // thresholded list
      DataPoint[] dps = massList.getDataPoints();
      return ScanUtils.getFiltered(dps, noiseLevel);
    }
  }

  private void addIdentities(List<SpectralDBPeakIdentity> matches) {

    for (SpectralDBPeakIdentity match : matches) {
      try {
        // TODO put into separate method and add comments
        // get data points of matching scans
        DataPoint[] spectraMassList = getDataPoints(currentScan);
        List<DataPoint[]> alignedDataPoints = ScanAlignment.align(mzToleranceSpectra,
            match.getEntry().getDataPoints(), spectraMassList);
        alignedSignals = ScanAlignment.removeUnaligned(alignedDataPoints);
        // add new mass list to the spectra for match
        DataPoint[] dataset = new DataPoint[alignedSignals.size()];
        for (int i = 0; i < dataset.length; i++) {
          dataset[i] = alignedSignals.get(i)[1];
        }

        String compoundName = match.getEntry().getField(DBEntryField.NAME).toString();
        String shortName = compoundName;
        // TODO remove or specify more - special naming format?
        int start = compoundName.indexOf("[");
        int end = compoundName.indexOf("]");
        if (start != -1 && start + 1 < compoundName.length() && end != -1
            && end < compoundName.length())
          shortName = compoundName.substring(start + 1, end);

        DataPointsDataSet detectedCompoundsDataset = new DataPointsDataSet(
            shortName + " " + "Score: " + COS_FORM.format(match.getSimilarity().getCosine()),
            dataset);
        spectraPlot.addDataSet(detectedCompoundsDataset,
            new Color((int) (Math.random() * 0x1000000)), true);


      } catch (MissingMassListException e) {
        logger.log(Level.WARNING, "No mass list for the selected spectrum", e);
        errorCounter++;
      }
    }
    resultWindow.addMatches(currentScan, matches);
    resultWindow.revalidate();
    resultWindow.repaint();
    setStatus(TaskStatus.FINISHED);
  }

  public int getCount() {
    return count;
  }

}
