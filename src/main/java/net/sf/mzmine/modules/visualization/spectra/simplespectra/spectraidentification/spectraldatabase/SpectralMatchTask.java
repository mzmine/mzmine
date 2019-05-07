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

package net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import java.awt.Color;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.DBEntryField;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.SpectralDBEntry;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetectorParameters;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetectorParameters;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.exceptions.MissingMassListException;
import net.sf.mzmine.util.maths.similarity.SpectraSimilarity;
import net.sf.mzmine.util.scans.ScanAlignment;

public class SpectralMatchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String METHOD = "MS/MS spectral DB search";
  private static final int MAX_ERROR = 3;
  private int errorCounter = 0;
  private final File dataBaseFile;
  private final MZTolerance mzTolerance;
  private int finishedRows = 0;
  private Scan currentScan;
  private SpectraPlot spectraPlot;
  private List<DataPoint[]> alignedSignals;

  private final double noiseLevel;
  private final double minSimilarity;
  private final int minMatch;
  private List<SpectralDBEntry> list;
  private int totalSteps;

  private int count = 0;

  // as this module is started in a series the start entry is saved to track progress
  private int startEntry;
  private int listsize;

  public SpectralMatchTask(ParameterSet parameters, int startEntry, List<SpectralDBEntry> list,
      SpectraPlot spectraPlot, Scan currentScan) {
    this.startEntry = startEntry;
    this.list = list;
    this.currentScan = currentScan;
    this.spectraPlot = spectraPlot;

    listsize = list.size();
    dataBaseFile = parameters
        .getParameter(SpectraIdentificationSpectralDatabaseParameters.dataBaseFile).getValue();
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
    return ((double) finishedRows) / totalSteps;
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
      for (SpectralDBEntry ident : list) {
        SpectraSimilarity sim = spectraDBMatch(currentScan, ident);
        if (sim != null) {
          count++;
          addIdentity(ident, sim);
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

      // get data points of matching scans
      List<DataPoint[]> alignedDataPoints =
          ScanAlignment.align(mzTolerance, ident.getDataPoints(), spectraMassList);
      alignedSignals = ScanAlignment.removeUnaligned(alignedDataPoints);
      if (sim != null && sim.getCosine() >= minSimilarity)
        return sim;
    } catch (MissingMassListException e) {
      logger.log(Level.WARNING, "No mass list for the selected spectrum", e);
      errorCounter++;
      return null;
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
  private DataPoint[] getDataPoints(Scan scan) throws MissingMassListException {

    // create mass list for scan
    DataPoint[] massList = null;
    MassDetector massDetector = null;

    // Create a new mass list for MS/MS scan. Check if sprectrum is profile or centroid mode
    if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED) {
      massDetector = new CentroidMassDetector();
      CentroidMassDetectorParameters parameters = new CentroidMassDetectorParameters();
      CentroidMassDetectorParameters.noiseLevel.setValue(noiseLevel);
      massList = massDetector.getMassValues(currentScan, parameters);
    } else {
      massDetector = new ExactMassDetector();
      ExactMassDetectorParameters parameters = new ExactMassDetectorParameters();
      ExactMassDetectorParameters.noiseLevel.setValue(noiseLevel);
      massList = massDetector.getMassValues(currentScan, parameters);
    }
    return massList;
  }

  private void addIdentity(SpectralDBEntry ident, SpectraSimilarity sim) {

    // add new mass list to the spectra for match
    DataPoint[] dataset = new DataPoint[alignedSignals.size()];
    for (int i = 0; i < dataset.length; i++) {
      dataset[i] = alignedSignals.get(i)[1];
    }

    String compoundName = ident.getField(DBEntryField.NAME).toString();
    DataPointsDataSet detectedCompoundsDataset = new DataPointsDataSet(
        compoundName.substring(compoundName.indexOf("[") + 1, compoundName.indexOf("]")) + " "
            + "Score: " + MZmineCore.getConfiguration(). //
                getRTFormat().//
                format(sim.getCosine()),
        dataset);
    spectraPlot.addDataSet(detectedCompoundsDataset, new Color((int) (Math.random() * 0x1000000)),
        true);
    setStatus(TaskStatus.FINISHED);

  }

  public int getCount() {
    return count;
  }

}
