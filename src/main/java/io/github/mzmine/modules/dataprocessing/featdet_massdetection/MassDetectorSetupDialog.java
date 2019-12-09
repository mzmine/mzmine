/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import java.awt.Window;
import java.util.ArrayList;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerWindow;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithScanPreview;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected mass detector and his parameters works
 * over the raw data file.
 */
public class MassDetectorSetupDialog
        extends ParameterSetupDialogWithScanPreview {

    private static final long serialVersionUID = 1L;
    private MassDetector massDetector;
    private ParameterSet parameters;

    /**
     * @param parameters
     * @param massDetectorTypeNumber
     */
    public MassDetectorSetupDialog(Window parent, boolean valueCheckRequired,
            Class<?> massDetectorClass, ParameterSet parameters) {

        super(parent, valueCheckRequired, parameters);

        this.parameters = parameters;

        for (MassDetector detector : MassDetectionParameters.massDetectors) {
            if (detector.getClass().equals(massDetectorClass)) {
                this.massDetector = detector;
            }
        }
    }

    protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {

        ScanDataSet spectraDataSet = new ScanDataSet(previewScan);

        // Set plot mode only if it hasn't been set before
        // if the scan is centroided, switch to centroid mode
        spectrumPlot.setPlotMode(previewScan.getSpectrumType());

        spectrumPlot.removeAllDataSets();
        spectrumPlot.addDataSet(spectraDataSet,
                SpectraVisualizerWindow.scanColor, false);

        // If there is some illegal value, do not load the preview but just exit
        ArrayList<String> errorMessages = new ArrayList<String>();
        boolean paramsOK = parameterSet.checkParameterValues(errorMessages);
        if (!paramsOK)
            return;

        DataPoint[] mzValues = massDetector.getMassValues(previewScan,
                parameters);

        DataPointsDataSet peaksDataSet = new DataPointsDataSet("Detected peaks",
                mzValues);

        spectrumPlot.addDataSet(peaksDataSet,
                SpectraVisualizerWindow.peaksColor, false);

    }

}
