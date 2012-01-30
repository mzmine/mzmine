/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters;

import java.io.IOException;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.tic.TICDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialogWithChromatogramPreview;
import net.sf.mzmine.util.Range;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected raw data filter and his parameters works
 * over the raw data file.
 */
public class DataSetFilterSetupDialog extends
	ParameterSetupDialogWithChromatogramPreview {

    private ParameterSet filterParameters;
    private RawDataSetFilter rawDataFilter;

    /**
     * @param parameters
     * @param rawDataFilterTypeNumber
     */
    public DataSetFilterSetupDialog(ParameterSet filterParameters,
	    Class<? extends RawDataSetFilter> filterClass) {

	super(filterParameters);
	this.filterParameters = filterParameters;

	try {
	    this.rawDataFilter = filterClass.newInstance();
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    protected void loadPreview(TICPlot ticPlot, RawDataFile dataFile,
	    Range rtRange, Range mzRange) {

	// First, remove all current data sets
	ticPlot.removeAllTICDataSets();

	// Add the original raw data file
	int scanNumbers[] = dataFile.getScanNumbers(1, rtRange);
	TICDataSet ticDataset = new TICDataSet(dataFile, scanNumbers, mzRange,
		null);
	ticPlot.addTICDataset(ticDataset);

	try {
	    // Create a new filtered raw data file
	    RawDataFileWriter rawDataFileWriter = MZmineCore
		    .createNewFile(dataFile.getName() + " filtered");
	    RawDataFile newDataFile = rawDataFilter.filterDatafile(dataFile,
		    rawDataFileWriter, filterParameters);

	    // If successful, add the new data file
	    if (newDataFile != null) {
		int newScanNumbers[] = newDataFile.getScanNumbers(1, rtRange);
		TICDataSet newDataset = new TICDataSet(newDataFile,
			newScanNumbers, mzRange, null);
		ticPlot.addTICDataset(newDataset);
	    }

	} catch (IOException e) {
	    e.printStackTrace();
	    return;
	}

    }
}
