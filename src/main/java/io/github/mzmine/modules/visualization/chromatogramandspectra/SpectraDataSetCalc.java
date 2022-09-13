/*
 * Copyright 2006-2022 The MZmine Development Team
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
