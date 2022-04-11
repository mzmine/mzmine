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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectrumPlotType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithScanPreview;
import java.awt.Color;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This is used to preview
 * how the selected raw data filter and his parameters works over the raw data file.
 */
public class ScanFilterSetupDialog extends ParameterSetupDialogWithScanPreview {

  private ParameterSet filterParameters;
  private ScanFilter rawDataFilter;
  private RawDataFile tmpFile;

  /**
   */
  public ScanFilterSetupDialog(boolean valueCheckRequired, ParameterSet filterParameters,
      Class<? extends ScanFilter> filterClass) {

    super(valueCheckRequired, filterParameters);
    this.filterParameters = filterParameters;

    try {
      this.rawDataFilter = filterClass.getDeclaredConstructor().newInstance();
      this.tmpFile = MZmineCore.createNewFile("tmp", null, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This function set all the information into the plot chart
   *
   * @param
   */
  @Override
  protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {

    Scan newScan = rawDataFilter.filterScan(tmpFile, previewScan, filterParameters);

    ScanDataSet spectraDataSet = new ScanDataSet("Filtered scan", newScan);
    ScanDataSet spectraOriginalDataSet = new ScanDataSet("Original scan", previewScan);

    spectrumPlot.removeAllDataSets();

    spectrumPlot.addDataSet(spectraOriginalDataSet, SpectraVisualizerTab.scanColor, true, true);
    spectrumPlot.addDataSet(spectraDataSet, Color.green, true, true);

    // if the scan is centroided, switch to centroid mode
    spectrumPlot.setPlotMode(SpectrumPlotType.fromScan(previewScan));

  }
}
