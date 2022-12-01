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
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
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

  private static final Logger logger = Logger.getLogger(
      SingleSpectrumLibrarySearchTask.class.getName());
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
      resultWindow.setTitle("Matched " + fcount + " compounds for scan#" + scan.getScanNumber());
      resultWindow.setMatchingFinished();
    });

    if (!isCanceled()) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  @Override
  protected void addIdentities(FeatureListRow row, List<SpectralDBAnnotation> matches) {
    // we dont need row here
    addIdentities(matches);
  }

  private void addIdentities(List<SpectralDBAnnotation> matches) {
    for (SpectralDBAnnotation match : matches) {
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

        MZmineCore.runLater(() -> spectraPlot.addDataSet(detectedCompoundsDataset,
            new Color((int) (Math.random() * 0x1000000)), true, true));

      } catch (MissingMassListException e) {
        logger.log(Level.WARNING, "No mass list for the selected spectrum", e);
      }
    }
    MZmineCore.runLater(() -> resultWindow.addMatches(matches));
    setStatus(TaskStatus.FINISHED);
  }
}
