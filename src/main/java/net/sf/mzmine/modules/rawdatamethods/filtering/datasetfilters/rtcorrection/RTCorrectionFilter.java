/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters.rtcorrection;

import javax.annotation.Nonnull;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters.RawDataSetFilter;
import net.sf.mzmine.parameters.ParameterSet;

public class RTCorrectionFilter implements RawDataSetFilter {

    @Override
    public RawDataFile filterDatafile(RawDataFile dataFile,
	    RawDataFileWriter rawDataFileWriter, ParameterSet parameters) {

	try {
	    int[] scanNumbers = dataFile.getScanNumbers(1);
	    int totalScans = scanNumbers.length;

	    for (int i = 0; i < totalScans; i++) {
		Scan scan = dataFile.getScan(scanNumbers[i]);
		if (scan != null) {
		    rawDataFileWriter.addScan(new SimpleScan(scan));
		}
	    }

	    return rawDataFileWriter.finishWriting();

	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }

    @Override
    public double getProgress() {
	return 0.5f;
    }

    @Override
    public @Nonnull String getName() {
	return "RT correction filter";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return RTCorrectionFilterParameters.class;
    }
}
