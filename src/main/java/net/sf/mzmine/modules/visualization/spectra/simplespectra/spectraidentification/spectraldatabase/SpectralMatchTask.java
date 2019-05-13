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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.exceptions.MissingMassListException;
import net.sf.mzmine.util.maths.similarity.SpectraSimilarity;
import net.sf.mzmine.util.scans.ScanAlignment;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;

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
  private final MZTolerance mzTolerance;
  private int finishedSteps = 0;
  private Scan currentScan;
  private SpectraPlot spectraPlot;
  private List<DataPoint[]> alignedSignals;
  private String massListName;

  private final double noiseLevel;
  private final double minSimilarity;
  private final int minMatch;
  private List<SpectralDBEntry> list;
  private int totalSteps;

  private Map<SpectralDBEntry, Double> matches;
  private SpectraIdentificationResultsWindow resultWindow;

  private int count = 0;
  private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");

  // as this module is started in a series the start entry is saved to track progress
  private int startEntry;
  private int listsize;

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
    mzTolerance = parameters
        .getParameter(SpectraIdentificationSpectralDatabaseParameters.mzTolerance).getValue();
    noiseLevel = parameters.getParameter(SpectraIdentificationSpectralDatabaseParameters.noiseLevel)
        .getValue();

    minMatch = parameters.getParameter(SpectraIdentificationSpectralDatabaseParameters.minMatch)
        .getValue();
    minSimilarity = parameters
        .getParameter(SpectraIdentificationSpectralDatabaseParameters.minCosine).getValue();
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
    setStatus(TaskStatus.PROCESSING);
    try {
      totalSteps = list.size();
      matches = new HashMap<SpectralDBEntry, Double>();
      for (SpectralDBEntry ident : list) {
        SpectraSimilarity sim = spectraDBMatch(currentScan, ident);
        if (sim != null) {
          count++;
          matches.put(ident, sim.getCosine());
        }
        // check for max error (missing masslist)
        if (errorCounter > MAX_ERROR) {
          logger.log(Level.WARNING, "Spectral data base matching failed");
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Spectral data base matching failed");
          list = null;
          return;
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
   * @param currentScan
   * @param ident
   * @return spectral similarity or null if no match
   */
  private SpectraSimilarity spectraDBMatch(Scan scan, SpectralDBEntry ident) {
    try {
      // check spectra similarity
      DataPoint[] spectraMassList = getDataPoints(scan);
      SpectraSimilarity sim = SpectraSimilarity.createMS2Sim(mzTolerance, ident.getDataPoints(),
          spectraMassList, minMatch);
      if (sim != null && sim.getCosine() >= minSimilarity)
        return sim;
    } catch (MissingMassListException e) {
      logger.log(Level.WARNING, "No mass list for the selected spectrum", e);
      errorCounter++;
      return null;
    }
    return null;
  }

  private DataPoint[] getDataPoints(Scan scan) throws MissingMassListException {
    MassList massList = scan.getMassList(massListName);
    return massList.getDataPoints();
  }

  private void addIdentities(Map<SpectralDBEntry, Double> matches) {

    for (Map.Entry<SpectralDBEntry, Double> match : matches.entrySet()) {
      try {
        // get data points of matching scans
        DataPoint[] spectraMassList = getDataPoints(currentScan);
        List<DataPoint[]> alignedDataPoints =
            ScanAlignment.align(mzTolerance, match.getKey().getDataPoints(), spectraMassList);
        alignedSignals = ScanAlignment.removeUnaligned(alignedDataPoints);
        // add new mass list to the spectra for match
        DataPoint[] dataset = new DataPoint[alignedSignals.size()];
        for (int i = 0; i < dataset.length; i++) {
          dataset[i] = alignedSignals.get(i)[1];
        }

        String compoundName = match.getKey().getField(DBEntryField.NAME).toString();
        DataPointsDataSet detectedCompoundsDataset = new DataPointsDataSet(
            compoundName.substring(compoundName.indexOf("[") + 1, compoundName.indexOf("]")) + " "
                + "Score: " + COS_FORM.format(match.getValue()),
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
