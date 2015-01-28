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

import java.io.File;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzDataReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzXMLReadTask;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

import org.junit.Assert;
import org.junit.Test;

public class ScanUtilsTest {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Test the detectSpectrumType() method
     */
    @Test
    public void testDetectSpectrumType() throws Exception {

	File inputFiles[] = new File("src/test/resources").listFiles();

	Assert.assertNotNull(inputFiles);
	Assert.assertNotEquals(0, inputFiles.length);

	int filesTested = 0;

	for (File inputFile : inputFiles) {

	    MassSpectrumType trueType;
	    if (inputFile.getName().startsWith("centroided"))
		trueType = MassSpectrumType.CENTROIDED;
	    else if (inputFile.getName().startsWith("thresholded"))
		trueType = MassSpectrumType.THRESHOLDED;
	    else if (inputFile.getName().startsWith("profile"))
		trueType = MassSpectrumType.PROFILE;
	    else
		continue;

	    logger.info("Checking autodetection of centroided/thresholded/profile scans on file "
		    + inputFile.getName());

	    MZmineProject project = new MZmineProjectImpl();

	    Task newTask = null;
	    String extension = inputFile.getName()
		    .substring(inputFile.getName().lastIndexOf(".") + 1)
		    .toLowerCase();

	    RawDataFileImpl file = new RawDataFileImpl(inputFile.getName());

	    if (extension.endsWith("mzdata")) {
		newTask = new MzDataReadTask(project, inputFile, file);
	    }

	    if (extension.endsWith("mzxml")) {
		newTask = new MzXMLReadTask(project, inputFile, file);
	    }

	    if (extension.endsWith("mzml")) {
		newTask = new MzMLReadTask(project, inputFile, file);
	    }

	    Assert.assertNotNull(newTask);
	    newTask.run();
	    Assert.assertEquals(TaskStatus.FINISHED, newTask.getStatus());
	    Assert.assertEquals(1, project.getDataFiles().length);

	    int scanNumbers[] = file.getScanNumbers(1);
	    for (int scanNumber : scanNumbers) {
		Scan scan = file.getScan(scanNumber);
		DataPoint dataPoints[] = scan.getDataPoints();
		MassSpectrumType detectedType = ScanUtils
			.detectSpectrumType(dataPoints);

		Assert.assertEquals("Scan type wrongly detected for scan "
			+ scanNumber + " in " + file.getName(), trueType,
			detectedType);
	    }
	    filesTested++;
	}

	// make sure we tested 10+ files
	Assert.assertTrue(filesTested > 10);
    }

}
