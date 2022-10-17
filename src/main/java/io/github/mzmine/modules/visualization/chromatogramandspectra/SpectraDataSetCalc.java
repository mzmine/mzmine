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

package io.github.mzmine.modules.visualization.chromatogramandspectra;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramCursorPosition;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import javafx.beans.property.BooleanProperty;

public class SpectraDataSetCalc extends AbstractTask {

  private final ChromatogramCursorPosition pos;
  private final HashMap<RawDataFile, ScanDataSet> filesAndDataSets;
  private final Collection<RawDataFile> rawDataFiles;
  private final ScanSelection scanSelection;
  private final boolean showSpectraOfEveryRawFile;
  private final SpectraPlot spectrumPlot;
  private final BooleanProperty showMassListProperty;
  private int doneFiles;

  public SpectraDataSetCalc(final Collection<RawDataFile> rawDataFiles,
      final ChromatogramCursorPosition pos, final ScanSelection scanSelection,
      boolean showSpectraOfEveryRawFile, SpectraPlot spectrumPlot,
      BooleanProperty showMassListProperty) {
    super(null, Instant.now());
    filesAndDataSets = new HashMap<>();
    this.rawDataFiles = rawDataFiles;
    this.pos = pos;
    doneFiles = 0;
    this.showSpectraOfEveryRawFile = showSpectraOfEveryRawFile;
    this.scanSelection = scanSelection;
    this.spectrumPlot = spectrumPlot;
    this.showMassListProperty = showMassListProperty;
    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return "Calculating scan data sets for " + rawDataFiles.size() + " raw data file(s).";
  }

  @Override
  public double getFinishedPercentage() {
    // + 1 because we count the generation of the data sets, too.
    return doneFiles / (double) (rawDataFiles.size() + 1);
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    // is MS level>2 spectrum? then directly show
    if (pos.getScan() != null && pos.getScan().getMSLevel() > 1) {
      ScanDataSet dataSet = new ScanDataSet(pos.getScan());
      filesAndDataSets.put(pos.getDataFile(), dataSet);
    } else if (showSpectraOfEveryRawFile) {
      double rt = pos.getRetentionTime();
      rawDataFiles.forEach(rawDataFile -> {
        Scan scan = null;
        if (scanSelection.getMsLevel() != null) {
          scan = rawDataFile.getScanNumberAtRT((float) rt, scanSelection.getMsLevel());
        } else {
          scan = rawDataFile.getScanNumberAtRT((float) rt);
        }
        if (scan != null) {
          ScanDataSet dataSet = new ScanDataSet(scan);
          filesAndDataSets.put(rawDataFile, dataSet);
        }
        doneFiles++;

        if (getStatus() == TaskStatus.CANCELED) {
          return;
        }

      });
    } else {
      ScanDataSet dataSet = new ScanDataSet(pos.getScan());
      filesAndDataSets.put(pos.getDataFile(), dataSet);
      doneFiles++;
    }

    MZmineCore.runLater(() -> {
      if (getStatus() == TaskStatus.CANCELED) {
        return;
      }
      // apply changes after all updates
      spectrumPlot.applyWithNotifyChanges(false, true, () -> {
        spectrumPlot.removeAllDataSets();

        filesAndDataSets.keySet().forEach(rawDataFile -> {
          spectrumPlot.addDataSet(filesAndDataSets.get(rawDataFile), rawDataFile.getColorAWT(),
              false, false);

          // If the scan contains a mass list then add dataset for it
          if (showMassListProperty.getValue()) {
            MassList massList = filesAndDataSets.get(rawDataFile).getScan().getMassList();
            if (massList != null) {
              MassListDataSet massListDataset = new MassListDataSet(massList);

              spectrumPlot.addDataSet(massListDataset, rawDataFile.getColorAWT(), false, false);
            }
          }
        });
      });
    });
    setStatus(TaskStatus.FINISHED);
  }
}
