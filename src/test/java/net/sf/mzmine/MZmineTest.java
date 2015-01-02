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

package net.sf.mzmine;

import java.io.File;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzDataReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzXMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.NetCDFReadTask;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

import org.junit.Assert;
import org.junit.BeforeClass;

public class MZmineTest {

    protected static final MZmineProject project = new MZmineProjectImpl();

    /**
     * Initialization method that is called before any set of tests is executed.
     * It creates a new MZmine project and loads all test raw data into it.
     */
    @BeforeClass
    public static void loadTestData() throws Exception {

	File inputFiles[] = new File("src/test/data").listFiles();

	Assert.assertNotNull(inputFiles);
	Assert.assertNotEquals(0, inputFiles.length);

	for (File inputFile : inputFiles) {
	    RawDataFileImpl newMZmineFile = new RawDataFileImpl(
		    inputFile.getName());
	    Task newTask = null;
	    String extension = inputFile.getName()
		    .substring(inputFile.getName().lastIndexOf(".") + 1)
		    .toLowerCase();

	    if (extension.endsWith("mzdata")) {
		newTask = new MzDataReadTask(project, inputFile, newMZmineFile);
	    }
	    if (extension.endsWith("mzxml")) {
		newTask = new MzXMLReadTask(project, inputFile, newMZmineFile);
	    }
	    if (extension.endsWith("mzml")) {
		newTask = new MzMLReadTask(project, inputFile, newMZmineFile);
	    }
	    if (extension.endsWith("cdf")) {
		newTask = new NetCDFReadTask(project, inputFile, newMZmineFile);
	    }
	    Assert.assertNotNull(newTask);
	    newTask.run();
	    Assert.assertEquals(TaskStatus.FINISHED, newTask.getStatus());

	}
    }

}
