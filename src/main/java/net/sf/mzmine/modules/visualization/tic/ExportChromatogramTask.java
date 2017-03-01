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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.visualization.tic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Exports a chromatogram to a CSV file.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ExportChromatogramTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger
	    .getLogger(ExportChromatogramTask.class.getName());

    private final File exportFile;
    private final TICDataSet dataSet;

    private int progress;
    private int progressMax;

    /**
     * Create the task.
     *
     * @param data
     *            data set to export.
     * @param file
     *            file to write to.
     */
    public ExportChromatogramTask(final TICDataSet data, final File file) {

	dataSet = data;
	exportFile = file;
	progress = 0;
	progressMax = 0;
    }

    @Override
    public String getTaskDescription() {
	return "Exporting chromatogram for " + dataSet.getDataFile().getName();
    }

    @Override
    public double getFinishedPercentage() {
	return progressMax == 0 ? 0.0 : (double) progress
		/ (double) progressMax;
    }

    @Override
    public void run() {

	// Update the status of this task
	setStatus(TaskStatus.PROCESSING);

	try {

	    // Do the export.
	    export();

	    // Success.
	    LOG.info("Exported chromatogram for "
		    + dataSet.getDataFile().getName());
	    setStatus(TaskStatus.FINISHED);

	} catch (Throwable t) {

	    LOG.log(Level.SEVERE, "Chromatogram export error", t);
	    setStatus(TaskStatus.ERROR);
	    setErrorMessage(t.getMessage());
	}
    }

    /**
     * Export the chromatogram.
     *
     * @throws IOException
     *             if there are i/o problems.
     */
    public void export() throws IOException {

	// Open the writer.
	final BufferedWriter writer = new BufferedWriter(new FileWriter(
		exportFile));
	try {

	    // Write the header row.
	    writer.write("RT,I");
	    writer.newLine();

	    // Write the data points.
	    final int itemCount = dataSet.getItemCount(0);
	    progressMax = itemCount;
	    for (int i = 0; i < itemCount; i++) {

		// Write (x, y) data point row.
		writer.write(dataSet.getX(0, i) + "," + dataSet.getY(0, i));
		writer.newLine();

		progress = i + 1;
	    }
	} finally {

	    // Close.
	    writer.close();
	}
    }
}
