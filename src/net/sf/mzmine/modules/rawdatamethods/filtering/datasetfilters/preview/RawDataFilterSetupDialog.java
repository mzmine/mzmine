/*
 * Copyright 2006-2010 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters.preview;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters.RawDataFilteringParameters;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.DialogWithChromatogramParameters;
import net.sf.mzmine.util.dialogs.ParameterSetupDialogWithChromatogramPreview;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected raw data filter and his parameters works
 * over the raw data file.
 */
public class RawDataFilterSetupDialog extends ParameterSetupDialogWithChromatogramPreview {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    protected SimpleParameterSet mdParameters;
    // Raw Data Filter;
    private RawDataFilter rawDataFilter;
    private int rawDataFilterTypeNumber;

    /**
     * @param parameters
     * @param rawDataFilterTypeNumber
     */
    public RawDataFilterSetupDialog(RawDataFilteringParameters parameters,
            int rawDataFilterTypeNumber) {

        super(
                RawDataFilteringParameters.rawDataFilterNames[rawDataFilterTypeNumber] + "'s parameter setup dialog ",
                parameters.getRawDataFilteringParameters(rawDataFilterTypeNumber),
                RawDataFilteringParameters.rawDataFilterHelpFiles[rawDataFilterTypeNumber]);


        this.rawDataFilterTypeNumber = rawDataFilterTypeNumber;

        // Parameters of local raw data filter to get preview values
        mdParameters = parameters.getRawDataFilteringParameters(rawDataFilterTypeNumber);


    }
 
    protected void loadPreview(RawDataFile dataFile) {
        String rawDataFilterClassName = RawDataFilteringParameters.rawDataFilterClasses[rawDataFilterTypeNumber];
        try {
            Class rawDataFilterClass = Class.forName(rawDataFilterClassName);
            Constructor rawDataFilterConstruct = rawDataFilterClass.getConstructors()[0];
            rawDataFilter = (RawDataFilter) rawDataFilterConstruct.newInstance(mdParameters);
        } catch (Exception e) {
            MZmineCore.getDesktop().displayErrorMessage(
                    "Error trying to make an instance of raw data filter " + rawDataFilterClassName);
            logger.warning("Error trying to make an instance of raw data filter " + rawDataFilterClassName);
            return;
        }

        RawDataFile newDataFile = rawDataFilter.getNewDataFiles(dataFile);


        Range rtRange = (Range) TICparameters.getParameterValue(DialogWithChromatogramParameters.retentionTimeRange);
        Range mzRange = (Range) TICparameters.getParameterValue(DialogWithChromatogramParameters.mzRange);

        Boolean setLegend = (Boolean) TICparameters.getParameterValue(DialogWithChromatogramParameters.plotLegend);

        if (!setLegend) {
            legend.setVisible(false);
        } else {
            legend.setVisible(true);
        }

        int level = (Integer) TICparameters.getParameterValue(DialogWithChromatogramParameters.msLevel);
        if (newDataFile != null) {
            this.addRawDataFile(newDataFile, level, mzRange, rtRange);
        }

        Boolean orginalRawDataView = (Boolean) TICparameters.getParameterValue(DialogWithChromatogramParameters.originalRawData);

        if (orginalRawDataView) {
            this.addRawDataFile(dataFile, level, mzRange, rtRange);
        } else {
            /* for (int index = 0; index < rawDataList.size(); index++) {
            ticPlot.getXYPlot().setDataset(index,
            null);

            }*/
        }


    }
}