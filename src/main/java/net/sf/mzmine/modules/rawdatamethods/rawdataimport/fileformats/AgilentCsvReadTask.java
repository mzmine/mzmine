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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats;

import java.io.File;
import java.util.Scanner;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;

public class AgilentCsvReadTask extends AbstractTask {

    protected String dataSource;
    private File file;
    private MZmineProject project;
    private RawDataFileImpl newMZmineFile;
    private RawDataFile finalRawDataFile;

    private int totalScans, parsedScans;

    /**
     * Creates a new AgilentCSVReadTask
     * 
     * @param file
     *            A File instance containing the file to be read
     */
    public AgilentCsvReadTask(MZmineProject project, File fileToOpen,
	    RawDataFileWriter newMZmineFile) {
	this.project = project;
	this.file = fileToOpen;
	this.newMZmineFile = (RawDataFileImpl) newMZmineFile;
    }

    /**
     * Reads the file.
     */
    public void run() {

	setStatus(TaskStatus.PROCESSING);
	Scanner scanner;

	try {

	    scanner = new Scanner(this.file);

	    this.dataSource = this.getMetaData(scanner, "file name");

	    String[] range = this.getMetaData(scanner, "mass range").split(",");
	    newMZmineFile.setMZRange(
		    1,
		    Range.closed(Double.parseDouble(range[0]),
			    Double.parseDouble(range[1])));
	    range = this.getMetaData(scanner, "time range").split(",");
	    newMZmineFile.setRTRange(
		    1,
		    Range.closed(Double.parseDouble(range[0]),
			    Double.parseDouble(range[1])));
	    totalScans = Integer.parseInt(this.getMetaData(scanner,
		    "number of spectra"));

	    // advance to the spectrum data...
	    while (!scanner.nextLine().trim().equals("[spectra]")) {
	    }

	    scanner.useDelimiter(",");

	    for (parsedScans = 0; parsedScans < totalScans; parsedScans++) {

		if (isCanceled()) {
		    return;
		} // if the task is canceled.

		double retentionTime = scanner.nextDouble();
		int msLevel = scanner.nextInt(); // not sure about this value
		scanner.next();
		scanner.next();
		int charge = (scanner.next().equals("+") ? 1 : -1);
		scanner.next();

		int spectrumSize = scanner.nextInt();
		DataPoint[] dataPoints = new DataPoint[spectrumSize];
		for (int j = 0; j < spectrumSize; j++) {
		    dataPoints[j] = new SimpleDataPoint(scanner.nextDouble(),
			    scanner.nextDouble());
		}
		newMZmineFile.addScan(new SimpleScan(null, parsedScans + 1,
			msLevel, retentionTime, 0.0, charge, null,
			dataPoints, ScanUtils.detectSpectrumType(dataPoints),
			PolarityType.UNKNOWN, "", null));

		scanner.nextLine();
	    }

	    finalRawDataFile = newMZmineFile.finishWriting();
	    project.addFile(finalRawDataFile);

	} catch (Exception e) {
	    setErrorMessage(e.getMessage());
	    this.setStatus(TaskStatus.ERROR);
	    return;
	}

	this.setStatus(TaskStatus.FINISHED);

    }

    /**
     * Reads meta information on the file. This must be called with the keys in
     * order, as it does not reset the scanner position after reading.
     * 
     * @param scanner
     *            The Scanner which is reading this AgilentCSV file.
     * @param key
     *            The key for the metadata to return the value of.
     */
    private String getMetaData(Scanner scanner, String key) {
	String line = "";
	while (!line.trim().startsWith(key) && scanner.hasNextLine()) {
	    line = scanner.nextLine();
	    if (line.trim().startsWith(key))
		return line.split(",", 2)[1].trim();
	}
	return null;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    @Override
    public double getFinishedPercentage() {
	return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
    }

    @Override
    public String getTaskDescription() {
	return "Opening file " + file;
    }

}
