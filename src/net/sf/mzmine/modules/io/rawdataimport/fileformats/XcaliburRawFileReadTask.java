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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.io.rawdataimport.fileformats;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.TextUtils;

import com.mindprod.ledatastream.LEDataInputStream;

/**
 * This module binds to the XRawfile2.dll library of Xcalibur and reads directly
 * the contents of the Thermo RAW file. We use external utility (RAWdump.exe) to
 * perform the binding to the Xcalibur DLL. RAWdump.exe is a 32-bit application
 * running in separate process from the JVM, therefore it can bind to Xcalibur
 * DLL (also 32-bit) even when JVM is running in 64-bit mode.
 */
public class XcaliburRawFileReadTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	private File originalFile;

	private RawDataFileWriter newMZmineFile;
	private RawDataFile finalRawDataFile;

	private int totalScans = 0, parsedScans = 0;

	/*
	 * This array is used to set the number of fragments that one single scan
	 * can have. The initial size of array is set to 10, but it depends of
	 * fragmentation level.
	 */
	private int parentTreeValue[] = new int[10];

	/*
	 * This FIFO queue stores the scans until information about fragments is
	 * added. After completing fragment info, the scans can be added to the raw
	 * data file.
	 */
	private LinkedList<SimpleScan> parentStack;

	/*
	 * These variables are used during parsing of the RAW dump.
	 */
	private int scanNumber = 0, msLevel = 0, precursorCharge = 0;
	private double retentionTime = 0, precursorMZ = 0;

	public XcaliburRawFileReadTask(File fileToOpen) {
		originalFile = fileToOpen;
		parentStack = new LinkedList<SimpleScan>();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Opening file " + originalFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		// Check that we are running on Windows
		String osName = System.getProperty("os.name").toUpperCase();
		if (!osName.toUpperCase().contains("WINDOWS")) {
			status = TaskStatus.ERROR;
			errorMessage = "Thermo RAW file import only works on Windows";
			return;
		}

		status = TaskStatus.PROCESSING;
		logger.info("Started parsing file " + originalFile);

		String rawDumpPath = System.getProperty("user.dir") + File.separator
				+ "lib" + File.separator + "RAWdump.exe";

		String cmdLine[] = { rawDumpPath, originalFile.getPath() };
		Process dumper = null;

		try {

			// Create a separate process and execute RAWdump.exe
			dumper = Runtime.getRuntime().exec(cmdLine);

			// Get the stdout of RAWdump.exe process as InputStream
			InputStream dumpStream = dumper.getInputStream();
			BufferedInputStream bufStream = new BufferedInputStream(dumpStream);

			// Create new raw data file
			newMZmineFile = MZmineCore.createNewFile(originalFile.getName());

			// Read the dump data
			readRAWDump(bufStream);

			// Finish
			bufStream.close();

			if (status == TaskStatus.CANCELED) {
				dumper.destroy();
				return;
			}

			if (parsedScans == 0) {
				throw (new Exception("No scans found"));
			}

			// Close file
			finalRawDataFile = newMZmineFile.finishWriting();
			MZmineCore.getCurrentProject().addFile(finalRawDataFile);

		} catch (Throwable e) {

			if (dumper != null)
				dumper.destroy();

			if (status == TaskStatus.PROCESSING) {
				status = TaskStatus.ERROR;
				errorMessage = ExceptionUtils.exceptionToString(e);
			}

			return;
		}

		logger.info("Finished parsing " + originalFile + ", parsed "
				+ parsedScans + " scans");
		status = TaskStatus.FINISHED;

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		logger.info("Cancelling opening of RAW file " + originalFile);
		status = TaskStatus.CANCELED;
	}

	/**
	 * This method reads the dump of the RAW data file produced by RAWdump.exe
	 * utility (see RAWdump.cpp source for details).
	 */
	private void readRAWDump(InputStream dumpStream) throws IOException {

		String line;
		while ((line = TextUtils.readLineFromStream(dumpStream)) != null) {

			if (status == TaskStatus.CANCELED) {
				return;
			}

			if (line.startsWith("ERROR: ")) {
				throw (new IOException(line.substring("ERROR: ".length())));
			}

			if (line.startsWith("NUMBER OF SCANS: ")) {
				totalScans = Integer.parseInt(line
						.substring("NUMBER OF SCANS: ".length()));
			}

			if (line.startsWith("SCAN NUMBER: ")) {
				scanNumber = Integer.parseInt(line.substring("SCAN NUMBER: "
						.length()));
			}

			if (line.startsWith("SCAN FILTER: ")) {

				if (line.contains("ms ")) {
					msLevel = 1;
				} else {
					/*
					 * Typical filter line looks like this:
					 * 
					 * ITMS - c ESI d Full ms3 587.03@cid35.00 323.00@cid35.00
					 */
					Pattern p = Pattern.compile("ms(\\d).* (\\d+\\.\\d+)@");
					Matcher m = p.matcher(line);
					if (!m.find()) {
						throw new IOException("Unexpected format: " + line);
					}
					msLevel = Integer.parseInt(m.group(1));

					// Initially we obtain precursor m/z from this filter line,
					// even though the precision is not good. Later more precise
					// precursor m/z may be reported using PRECURSOR: line, but
					// sometimes it is missing (equal to 0)
					precursorMZ = Double.parseDouble(m.group(2));
				}

			}

			if (line.startsWith("RETENTION TIME: ")) {
				// Retention time in the RAW file is reported in minutes, but in
				// MZmine we use seconds representation, so we need to multiply
				// by 60
				retentionTime = Double.parseDouble(line
						.substring("RETENTION TIME: ".length())) * 60;
			}

			if (line.startsWith("PRECURSOR: ")) {
				String tokens[] = line.split(" ");
				double token2 = Double.parseDouble(tokens[1]);
				int token3 = Integer.parseInt(tokens[2]);
				if (token2 > 0) {
					precursorMZ = token2;
					precursorCharge = token3;
				}
			}

			if (line.startsWith("DATA POINTS: ")) {
				int numOfDataPoints = Integer.parseInt(line
						.substring("DATA POINTS: ".length()));

				DataPoint completeDataPoints[] = new DataPoint[numOfDataPoints];

				// Because Intel CPU is using little endian natively, we
				// need to use LEDataInputStream instead of normal Java
				// DataInputStream, which is big-endian.
				LEDataInputStream dis = new LEDataInputStream(dumpStream);
				for (int i = 0; i < numOfDataPoints; i++) {
					double mz = dis.readDouble();
					double intensity = dis.readDouble();
					completeDataPoints[i] = new SimpleDataPoint(mz, intensity);
				}

				boolean centroided = ScanUtils.isCentroided(completeDataPoints);

				DataPoint optimizedDataPoints[] = ScanUtils
						.removeZeroDataPoints(completeDataPoints, centroided);

				/*
				 * If this scan is a full scan (ms level = 1), it means that the
				 * previous scans stored in the stack, are complete and ready to
				 * be written to the raw data file.
				 */
				if (msLevel == 1) {
					while (!parentStack.isEmpty()) {
						SimpleScan currentScan = parentStack.removeFirst();
						newMZmineFile.addScan(currentScan);
					}
				}

				// Setting the current parentScan
				int parentScan = -1;
				if (msLevel > 1) {
					parentScan = parentTreeValue[msLevel - 1];

					if (!parentStack.isEmpty()) {
						for (SimpleScan s : parentStack) {
							if (s.getScanNumber() == parentScan) {
								s.addFragmentScan(scanNumber);
							}
						}
					}
				}

				// Setting the parent scan number for this level of fragments
				parentTreeValue[msLevel] = scanNumber;

				SimpleScan newScan = new SimpleScan(null, scanNumber, msLevel,
						retentionTime, parentScan, precursorMZ,
						precursorCharge, null, optimizedDataPoints, centroided);

				parentStack.add(newScan);
				parsedScans++;

				// Clean the variables for next scan
				scanNumber = 0;
				msLevel = 0;
				retentionTime = 0;
				precursorMZ = 0;
				precursorCharge = 0;

			}

		}

		// Add remaining scans in the parentStack
		while (!parentStack.isEmpty()) {
			SimpleScan currentScan = parentStack.removeFirst();
			newMZmineFile.addScan(currentScan);
		}

	}

	public Object[] getCreatedObjects() {
		return new Object[] { finalRawDataFile };
	}

}
