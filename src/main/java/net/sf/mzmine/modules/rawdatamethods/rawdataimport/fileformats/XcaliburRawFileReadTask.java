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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats;

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
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.TextUtils;

import com.google.common.io.LittleEndianDataInputStream;

/**
 * This module binds to the XRawfile2.dll library of Xcalibur and reads directly
 * the contents of the Thermo RAW file. We use external utility (RAWdump.exe) to
 * perform the binding to the Xcalibur DLL. RAWdump.exe is a 32-bit application
 * running in separate process from the JVM, therefore it can bind to Xcalibur
 * DLL (also 32-bit) even when JVM is running in 64-bit mode.
 */
public class XcaliburRawFileReadTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File file;
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

    public XcaliburRawFileReadTask(File fileToOpen,
            RawDataFileWriter newMZmineFile) {
        parentStack = new LinkedList<SimpleScan>();
        this.file = fileToOpen;
        this.newMZmineFile = newMZmineFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);
        logger.info("Opening file " + file);

        // Check the OS we are running
        String osName = System.getProperty("os.name").toUpperCase();

        String rawDumpPath = System.getProperty("user.dir") + File.separator
                + "lib" + File.separator + "RAWdump.exe";
        String cmdLine[];

        if (osName.toUpperCase().contains("WINDOWS")) {
            cmdLine = new String[] { rawDumpPath, file.getPath() };
        } else {
            cmdLine = new String[] { "wine", rawDumpPath, file.getPath() };
        }

        Process dumper = null;

        try {

            // Create a separate process and execute RAWdump.exe
            dumper = Runtime.getRuntime().exec(cmdLine);

            // Get the stdout of RAWdump.exe process as InputStream
            InputStream dumpStream = dumper.getInputStream();
            BufferedInputStream bufStream = new BufferedInputStream(dumpStream);

            // Read the dump data
            readRAWDump(bufStream);

            // Finish
            bufStream.close();

            if (isCanceled()) {
                dumper.destroy();
                return;
            }

            if (parsedScans == 0) {
                throw (new Exception("No scans found"));
            }

            if (parsedScans != totalScans) {
                throw (new Exception(
                        "RAW dump process crashed before all scans were extracted ("
                                + parsedScans + " out of " + totalScans + ")"));
            }

            // Close file
            finalRawDataFile = newMZmineFile.finishWriting();

        } catch (Throwable e) {

            if (dumper != null)
                dumper.destroy();

            if (getStatus() == TaskStatus.PROCESSING) {
                setStatus(TaskStatus.ERROR);
                errorMessage = ExceptionUtils.exceptionToString(e);
            }

            return;
        }

        logger.info("Finished parsing " + file + ", parsed " + parsedScans
                + " scans");
        setStatus(TaskStatus.FINISHED);

    }

    public String getTaskDescription() {
        return "Opening file" + file;
    }

    public Object[] getCreatedObjects() {
        return new Object[] { finalRawDataFile };
    }

    /**
     * This method reads the dump of the RAW data file produced by RAWdump.exe
     * utility (see RAWdump.cpp source for details).
     */
    private void readRAWDump(InputStream dumpStream) throws IOException {

        String line;
        while ((line = TextUtils.readLineFromStream(dumpStream)) != null) {

            if (isCanceled()) {
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

                /*
                 * A typical filter line for MS/MS scan looks like this:
                 * 
                 * ITMS - c ESI d Full ms3 587.03@cid35.00 323.00@cid35.00
                 */
                Pattern p = Pattern.compile("ms(\\d).* (\\d+\\.\\d+)@");
                Matcher m = p.matcher(line);
                if (m.find()) {
                    msLevel = Integer.parseInt(m.group(1));

                    // Initially we obtain precursor m/z from this filter line,
                    // even though the precision is not good. Later more precise
                    // precursor m/z may be reported using PRECURSOR: line, but
                    // sometimes it is missing (equal to 0)
                    precursorMZ = Double.parseDouble(m.group(2));
                } else {
                    msLevel = 1;
                }

            }

            if (line.startsWith("RETENTION TIME: ")) {
                // Retention time in the RAW file is reported in minutes.
                retentionTime = Double.parseDouble(line
                        .substring("RETENTION TIME: ".length()));
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
                // need to use LittleEndianDataInputStream instead of normal
                // Java DataInputStream, which is big-endian.
                LittleEndianDataInputStream dis = new LittleEndianDataInputStream(
                        dumpStream);
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

}
