/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.rawdataimport.fileformats;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataFileWriter;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

public class CsvReadTask extends AbstractTask {

    private Logger logger = Logger.getLogger(CsvReadTask.class.getName());

    protected String dataSource;
    private File file;
    private MZmineProject project;
    private RawDataFileImpl newMZmineFile;
    private RawDataFile finalRawDataFile;

    private int totalScans, parsedScans;

    public CsvReadTask(MZmineProject project, File fileToOpen,
            RawDataFileWriter newMZmineFile) {
        this.project = project;
        this.file = fileToOpen;
        this.newMZmineFile = (RawDataFileImpl) newMZmineFile;
    }

    @Override
    public String getTaskDescription() {
        return null;
    }

    @Override
    public double getFinishedPercentage() {
        return 0;
    }

    @Override
    public void run() {
        setStatus(TaskStatus.PROCESSING);
        Scanner scanner;

        logger.setLevel(Level.ALL);

        try {
            scanner = new Scanner(file);

            dataSource = getFileName(scanner);
            if (dataSource == null) {
                setErrorMessage(
                        "Could not open data file " + file.getAbsolutePath());
                setStatus(TaskStatus.ERROR);
                return;
            }
            logger.info("opening raw file " + dataSource);

            String acquisitionDate = getAcqusitionDate(scanner);
            if (acquisitionDate == null) {
                setErrorMessage("Could not find acquisition date in file "
                        + file.getAbsolutePath());
                setStatus(TaskStatus.ERROR);
                return;
            }

            logger.info("Date of acquisition " + acquisitionDate);

            // scanner.useDelimiter(",");

            List<String> mzsList = new ArrayList<String>();
            String mstype = "";
            String ions = "";
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                logger.fine("checking line: " + line + " for 'Time'...");
                if (line.startsWith("Time")) {
                    String[] axes = line.split(",");
                    logger.fine("Found axes" + Arrays.toString(axes));
                    for (int i = 1; i < axes.length; i++) {
                        String axis = axes[i];
                        ions += axis + ", ";
                        if (axis.contains("->")) {
                            mstype = "MS/MS";
                            logger.fine("axis " + axis + " is an ms^2 scan");
                            String mz = axis.substring(axis.indexOf("-> ") + 3);
                            mz.trim();
                            logger.fine("Axis " + axis
                                    + " was scanned at m/z = '" + mz + "'");
                            mzsList.add(mz);
                        } else {
                            String mz = axis.replaceAll("[^0-9]", "");
                            logger.fine(
                                    "axis " + axis + " was scanned at " + mz);
                            mzsList.add(mz);
                        }
                    }
                    break;
                }
            }

            int[] mzs = new int[mzsList.size()];
            for (int i = 0; i < mzsList.size(); i++)
                mzs[i] = Integer.valueOf(mzsList.get(i));

            Range<Double> mzRange = Range.closed((double) mzs[0] - 10,
                    (double) mzs[1] + 10);

            int scanNumber = 1;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line == null || line.trim().equals(""))
                    continue;
                String[] columns = line.split(",");
                if (columns == null || columns.length != mzs.length + 1)
                    continue;

                double rt = Double.valueOf(columns[0]) / 60;

                DataPoint dataPoints[] = new SimpleDataPoint[mzs.length];
                for (int i = 0; i < dataPoints.length; i++) {
                    String intensity = columns[i + 1];
                    dataPoints[i] = new SimpleDataPoint(mzs[i],
                            Double.valueOf(intensity));
                }

                Scan scan = new SimpleScan(null, scanNumber, 1, rt, 0.0, 1,
                        null, dataPoints, MassSpectrumType.CENTROIDED,
                        PolarityType.POSITIVE,
                        "ICP-" + mstype + " "
                                + ions.substring(0, ions.length() - 2),
                        mzRange);

                newMZmineFile.addScan(scan);
                scanNumber++;
            }

            finalRawDataFile = newMZmineFile.finishWriting();

            project.addFile(finalRawDataFile);

        } catch (Exception e) {
            setErrorMessage(e.getMessage());
            setStatus(TaskStatus.ERROR);
            return;
        }

        this.setStatus(TaskStatus.FINISHED);
    }

    private @Nullable String getFileName(@Nonnull Scanner scanner) {
        String path = null;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains(":") && line.contains("\\")) {
                path = line;
                return path;
            }
        }
        return path;
    }

    private @Nullable String getAcqusitionDate(@Nonnull Scanner scanner) {
        String acquisitionDate = null;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("Acquired")) {
                int begin = line.indexOf(":") + 2;
                line.subSequence(begin,
                        begin + (new String("00/00/0000 00:00:00")).length());
                return line;
            }
        }
        return acquisitionDate;
    }

}
