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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.id_spectraldbsearch.LocalSpectralDBSearchParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoper;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsWindowFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;
import java.awt.Color;
import java.io.File;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

/**
 * Spectral match task to compare single spectra with spectral databases
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectralMatchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public final static double[] DELTA_ISOTOPES = new double[]{1.0034, 1.0078, 2.0157, 1.9970};

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
  private SpectraIdentificationResultsWindowFX resultWindow;

  private int count = 0;
  private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");

  // as this module is started in a series the start entry is saved to track
  // progress
  private int startEntry;
  private int listsize;

  // precursor mz as user parameter (extracted from scan and then checked by
  // user)
  private double precursorMZ;
  private boolean usePrecursorMZ;

  private MZmineProcessingStep<SpectralSimilarityFunction> simFunction;
  // deisotoping of masslists
  private boolean removeIsotopes;
  private MassListDeisotoperParameters deisotopeParam;

  // crop to overlapping range (+- mzTol)
  private final boolean cropSpectraToOverlap;

  // needs any signals within mzToleranceSpectra for
  // 13C, H, 2H or Cl
  private boolean needsIsotopePattern;
  private int minMatchedIsoSignals;

  public SpectralMatchTask(ParameterSet parameters, int startEntry, List<SpectralDBEntry> list,
      SpectraPlot spectraPlot, Scan currentScan,
      SpectraIdentificationResultsWindowFX resultWindow) {
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
        parameters.getParameter(SpectraIdentificationSpectralDatabaseParameters.similarityFunction)
            .getValue();
    needsIsotopePattern =
        parameters.getParameter(SpectraIdentificationSpectralDatabaseParameters.needsIsotopePattern)
            .getValue();
    minMatchedIsoSignals = !needsIsotopePattern ? 0
        : parameters.getParameter(LocalSpectralDBSearchParameters.needsIsotopePattern)
            .getEmbeddedParameter().getValue();
    removeIsotopes = parameters
        .getParameter(SpectraIdentificationSpectralDatabaseParameters.deisotoping).getValue();
    deisotopeParam =
        parameters.getParameter(SpectraIdentificationSpectralDatabaseParameters.deisotoping)
            .getEmbeddedParameters();
    cropSpectraToOverlap = parameters
        .getParameter(SpectraIdentificationSpectralDatabaseParameters.cropSpectraToOverlap)
        .getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalSteps == 0) {
      return 0;
    }
    return ((double) finishedSteps) / totalSteps;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
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
    if (removeIsotopes) {
      spectraMassList = removeIsotopes(spectraMassList);
    }

    setStatus(TaskStatus.PROCESSING);
    try {
      totalSteps = list.size();
      matches = new ArrayList<>();
      for (SpectralDBEntry ident : list) {
        if (isCanceled()) {
          logger.info("Added " + count + " spectral library matches (before being cancelled)");
          return;
        }

        SpectralSimilarity sim = spectraDBMatch(spectraMassList, ident);
        if (sim != null && (!needsIsotopePattern
            || checkForIsotopePattern(sim, mzToleranceSpectra, minMatchedIsoSignals))) {
          count++;
          // use SpectralDBPeakIdentity to store all results similar
          // to peaklist method
          matches.add(new SpectralDBPeakIdentity(currentScan, massListName, ident, sim,
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

    list = null;
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Checks for isotope pattern in matched signals within mzToleranceSpectra
   *
   * @param sim
   * @return
   */
  public static boolean checkForIsotopePattern(SpectralSimilarity sim,
      MZTolerance mzToleranceSpectra, int minMatchedIsoSignals) {
    // use mzToleranceSpectra
    DataPoint[][] aligned = sim.getAlignedDataPoints();
    aligned = ScanAlignment.removeUnaligned(aligned);

    // find something in range of:
    // 13C 1.0034
    // H ( for M+ and M+H or -H -H2)
    // 2H 1.0078 2.0157
    // Cl 1.9970
    // just check one

    int matches = 0;
    DataPoint[] lib = aligned[0];
    for (int i = 0; i < lib.length - 1; i++) {
      double a = lib[i].getMZ();
      // each lib[i] can only have one match to each isotope dist
      for (double dIso : DELTA_ISOTOPES) {
        boolean matchedIso = false;
        for (int k = i + 1; k < lib.length && !matchedIso; k++) {
          double dmz = Math.abs(a - lib[k].getMZ());
          // any match?
          if (mzToleranceSpectra.checkWithinTolerance(dIso, dmz)) {
            matchedIso = true;
            matches++;
            if (matches >= minMatchedIsoSignals) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }


  /**
   * @param spectraMassList
   * @param ident
   * @return spectral similarity or null if no match
   */
  private SpectralSimilarity spectraDBMatch(DataPoint[] spectraMassList, SpectralDBEntry ident) {
    // do not check precursorMZ or precursorMZ within tolerances
    if (!usePrecursorMZ || (checkPrecursorMZ(precursorMZ, ident))) {
      DataPoint[] library = ident.getDataPoints();
      if (removeIsotopes) {
        library = removeIsotopes(library);
      }

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
  private SpectralSimilarity createSimilarity(DataPoint[] library, DataPoint[] query) {
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
    if (massList == null) {
      throw new MissingMassListException(massListName);
    } else {
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
            && end < compoundName.length()) {
          shortName = compoundName.substring(start + 1, end);
        }

        DataPointsDataSet detectedCompoundsDataset = new DataPointsDataSet(
            shortName + " " + "Score: " + COS_FORM.format(match.getSimilarity().getScore()),
            dataset);
        spectraPlot.addDataSet(detectedCompoundsDataset,
            new Color((int) (Math.random() * 0x1000000)), true);

      } catch (MissingMassListException e) {
        logger.log(Level.WARNING, "No mass list for the selected spectrum", e);
        errorCounter++;
      }
    }
    Platform.runLater(() -> resultWindow.addMatches(matches));
//    resultWindow.revalidate();
//    resultWindow.repaint();
    setStatus(TaskStatus.FINISHED);
  }

  public int getCount() {
    return count;
  }

}
