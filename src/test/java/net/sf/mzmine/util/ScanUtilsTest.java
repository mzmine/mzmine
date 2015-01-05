/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.util;

import java.util.logging.Logger;

import net.sf.mzmine.MZmineTest;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

import org.junit.Assert;
import org.junit.Test;

public class ScanUtilsTest extends MZmineTest {

    private static final Logger logger = Logger.getLogger(ScanUtilsTest.class
	    .getName());

    /**
     * Test the isCentroided() method
     */
    @Test
    public void testDetectSpectrumType() throws Exception {

	RawDataFile files[] = project.getDataFiles();
	Assert.assertNotEquals(0, files.length);
	int filesTested = 0;
	for (RawDataFile file : files) {
	    MassSpectrumType trueType;
	    if (file.getName().startsWith("centroid"))
		trueType = MassSpectrumType.CENTROIDED;
	    else if (file.getName().startsWith("profile"))
		trueType = MassSpectrumType.PROFILE;
	    else
		continue;
	    logger.finest("Checking autodetection of centroid/profile scans on file "
		    + file.getName());
	    int scanNumbers[] = file.getScanNumbers(1);
	    for (int scanNumber : scanNumbers) {
		Scan scan = file.getScan(scanNumber);
		DataPoint dataPoints[] = scan.getDataPoints();
		MassSpectrumType detectedType = ScanUtils
			.detectSpectrumType(dataPoints);

		Assert.assertEquals("Scan " + file.getName() + "#" + scanNumber
			+ " wrongly detected as " + detectedType + ".",
			detectedType, trueType);
	    }
	    filesTested++;
	}

	// make sure we tested 10+ files
	Assert.assertTrue(filesTested > 10);
    }

}
