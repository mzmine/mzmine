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

package net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.cropper;

import javax.annotation.Nonnull;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.ScanFilter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.Range;

public class CropFilter implements ScanFilter {

    public Scan filterScan(Scan scan, ParameterSet parameters) {

	Range mzRange = parameters.getParameter(CropFilterParameters.mzRange)
		.getValue();

	// Check if whole m/z range is within cropping region or
	// scan is a fragmentation scan. In such case we copy the
	// scan unmodified.
	if ((scan.getMSLevel() > 1)
		|| (mzRange.containsRange(scan.getMZRange()))) {
	    return scan;
	}

	// Pickup datapoints inside the m/z range

	DataPoint croppedDataPoints[] = scan.getDataPointsByMass(mzRange);

	// Create updated scan
	SimpleScan newScan = new SimpleScan(scan);
	newScan.setDataPoints(croppedDataPoints);

	return newScan;

    }

    public @Nonnull String getName() {
	return "Crop filter";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return CropFilterParameters.class;
    }
}
