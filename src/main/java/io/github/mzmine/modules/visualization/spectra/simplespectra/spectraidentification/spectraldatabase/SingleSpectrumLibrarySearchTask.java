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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.RowsSpectralMatchTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsWindowFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Task to compare single spectra with spectral libraries
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
class SingleSpectrumLibrarySearchTask extends RowsSpectralMatchTask {

  private static final Logger logger = Logger
      .getLogger(SingleSpectrumLibrarySearchTask.class.getName());
  private final SpectraPlot spectraPlot;
  private SpectraIdentificationResultsWindowFX resultWindow;

  SingleSpectrumLibrarySearchTask(ParameterSet parameters, Scan currentScan,
      SpectraPlot spectraPlot, @NotNull Instant moduleCallDate) {
    super(parameters, currentScan, moduleCallDate); // no new data stored -> null

    this.spectraPlot = spectraPlot;
  }

  @Override
  public String getTaskDescription() {
    return "Spectral libraries identification of spectrum " + scan.getScanDefinition()
           + " using libraries " + librariesJoined;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // add result frame
    MZmineCore.runOnFxThreadAndWait(() -> {
      resultWindow = new SpectraIdentificationResultsWindowFX();
      resultWindow.show();
    });

    // do the actual matching
    super.run();

    final int fcount = matches.get();
    MZmineCore.runLater(() -> {
      resultWindow
          .setTitle("Matched " + fcount + " compounds for scan#" + scan.getScanNumber());
      resultWindow.setMatchingFinished();
    });

    if (!isCanceled()) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  @Override
  protected void addIdentities(FeatureListRow row, List<SpectralDBFeatureIdentity> matches) {
    // we dont need row here
    addIdentities(matches);
  }

  private void addIdentities(List<SpectralDBFeatureIdentity> matches) {
    for (SpectralDBFeatureIdentity match : matches) {
      try {
        // TODO put into separate method and add comments
        // get data points of matching scans
        DataPoint[] spectraMassList = getDataPoints(scan, true);
        List<DataPoint[]> alignedDataPoints = ScanAlignment.align(mzToleranceSpectra,
            match.getEntry().getDataPoints(), spectraMassList);
        List<DataPoint[]> alignedSignals = ScanAlignment.removeUnaligned(alignedDataPoints);
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
            shortName + " " + "Score: " + MZmineCore.getConfiguration().getScoreFormat()
                .format(match.getSimilarity().getScore()), dataset);
        spectraPlot.addDataSet(detectedCompoundsDataset,
            new Color((int) (Math.random() * 0x1000000)), true, true);

      } catch (MissingMassListException e) {
        logger.log(Level.WARNING, "No mass list for the selected spectrum", e);
      }
    }
    MZmineCore.runLater(() -> resultWindow.addMatches(matches));
    setStatus(TaskStatus.FINISHED);
  }
}
