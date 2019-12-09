/*
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package io.github.mzmine.modules.io.adapmspexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class AdapMspExportTask extends AbstractTask {

    private static final Pattern ATTRIBUTE_NAME_PATTERN = Pattern
            .compile("^[\\w]+$");

    private final PeakList[] peakLists;
    private final File fileName;
    private final String plNamePattern = "{}";
    private final boolean addRetTime;
    private final String retTimeAttributeName;
    private final boolean addAnovaPValue;
    private final String anovaAttributeName;
    private final boolean integerMZ;
    private final String roundMode;

    AdapMspExportTask(ParameterSet parameters) {
        this.peakLists = parameters
                .getParameter(AdapMspExportParameters.PEAK_LISTS).getValue()
                .getMatchingPeakLists();

        this.fileName = parameters
                .getParameter(AdapMspExportParameters.FILENAME).getValue();

        this.addRetTime = parameters
                .getParameter(AdapMspExportParameters.ADD_RET_TIME).getValue();
        this.retTimeAttributeName = parameters
                .getParameter(AdapMspExportParameters.ADD_RET_TIME)
                .getEmbeddedParameter().getValue();

        this.addAnovaPValue = parameters
                .getParameter(AdapMspExportParameters.ADD_ANOVA_P_VALUE)
                .getValue();
        this.anovaAttributeName = parameters
                .getParameter(AdapMspExportParameters.ADD_ANOVA_P_VALUE)
                .getEmbeddedParameter().getValue();

        this.integerMZ = parameters
                .getParameter(AdapMspExportParameters.INTEGER_MZ).getValue();

        this.roundMode = parameters
                .getParameter(AdapMspExportParameters.INTEGER_MZ)
                .getEmbeddedParameter().getValue();
    }

    public double getFinishedPercentage() {
        return 0.0;
    }

    public String getTaskDescription() {
        return "Exporting feature list(s) " + Arrays.toString(peakLists)
                + " to MSP file(s)";
    }

    public void run() {
        setStatus(TaskStatus.PROCESSING);

        // Shall export several files?
        boolean substitute = fileName.getPath().contains(plNamePattern);

        /*
         * // Total number of rows for (PeakList peakList: peakLists) {
         * totalRows += peakList.getNumberOfRows(); }
         */

        // Process feature lists
        for (PeakList peakList : peakLists) {

            // Filename
            File curFile = fileName;
            if (substitute) {
                // Cleanup from illegal filename characters
                String cleanPlName = peakList.getName()
                        .replaceAll("[^a-zA-Z0-9.-]", "_");
                // Substitute
                String newFilename = fileName.getPath()
                        .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
                curFile = new File(newFilename);
            }

            // Open file
            FileWriter writer;
            try {
                writer = new FileWriter(curFile);
            } catch (Exception e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage(
                        "Could not open file " + curFile + " for writing.");
                return;
            }

            try {
                exportPeakList(peakList, writer, curFile);
            } catch (IOException | IllegalArgumentException e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Error while writing into file " + curFile
                        + ": " + e.getMessage());
                return;
            }

            // Cancel?
            if (isCanceled()) {
                return;
            }

            // Close file
            try {
                writer.close();
            } catch (Exception e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Could not close file " + curFile);
                return;
            }

            // If feature list substitution pattern wasn't found,
            // treat one feature list only
            if (!substitute)
                break;
        }

        if (getStatus() == TaskStatus.PROCESSING)
            setStatus(TaskStatus.FINISHED);
    }

    private void exportPeakList(PeakList peakList, FileWriter writer,
            File curFile) throws IOException {
        final String newLine = System.lineSeparator();

        for (PeakListRow row : peakList.getRows()) {
            IsotopePattern ip = row.getBestIsotopePattern();
            if (ip == null)
                continue;

            String name = row.toString();
            if (name != null)
                writer.write("Name: " + name + newLine);

            PeakIdentity identity = row.getPreferredPeakIdentity();
            if (identity != null) {
                // String name = identity.getName();
                // if (name != null) writer.write("Name: " + name + newLine);

                String formula = identity
                        .getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
                if (formula != null)
                    writer.write("Formula: " + formula + newLine);

                String id = identity.getPropertyValue(PeakIdentity.PROPERTY_ID);
                if (id != null)
                    writer.write("Comments: " + id + newLine);
            }

            String rowID = Integer.toString(row.getID());
            if (rowID != null)
                writer.write("DB#: " + rowID + newLine);

            if (addRetTime) {
                String attributeName = checkAttributeName(retTimeAttributeName);
                writer.write(
                        attributeName + ": " + row.getAverageRT() + newLine);
            }

            PeakInformation peakInformation = row.getPeakInformation();
            if (addAnovaPValue && peakInformation != null && peakInformation
                    .getAllProperties().containsKey("ANOVA_P_VALUE")) {
                String attributeName = checkAttributeName(anovaAttributeName);
                String value = peakInformation
                        .getPropertyValue("ANOVA_P_VALUE");
                if (value.trim().length() > 0)
                    writer.write(attributeName + ": " + value + newLine);
            }

            DataPoint[] dataPoints = ip.getDataPoints();

            if (integerMZ)
                dataPoints = integerDataPoints(dataPoints, roundMode);

            String numPeaks = Integer.toString(dataPoints.length);
            if (numPeaks != null)
                writer.write("Num Peaks: " + numPeaks + newLine);

            for (DataPoint point : dataPoints) {
                String line = point.getMZ() + " " + point.getIntensity();
                writer.write(line + newLine);
            }

            writer.write(newLine);
        }
    }

    private DataPoint[] integerDataPoints(final DataPoint[] dataPoints,
            final String mode) {
        int size = dataPoints.length;

        Map<Double, Double> integerDataPoints = new HashMap<>();

        for (int i = 0; i < size; ++i) {
            double mz = (double) Math.round(dataPoints[i].getMZ());
            double intensity = dataPoints[i].getIntensity();
            Double prevIntensity = integerDataPoints.get(mz);
            if (prevIntensity == null)
                prevIntensity = 0.0;

            switch (mode) {
            case AdapMspExportParameters.ROUND_MODE_SUM:
                integerDataPoints.put(mz, prevIntensity + intensity);
                break;

            case AdapMspExportParameters.ROUND_MODE_MAX:
                integerDataPoints.put(mz, Math.max(prevIntensity, intensity));
                break;
            }
        }

        DataPoint[] result = new DataPoint[integerDataPoints.size()];
        int count = 0;
        for (Entry<Double, Double> e : integerDataPoints.entrySet())
            result[count++] = new SimpleDataPoint(e.getKey(), e.getValue());

        return result;
    }

    private String checkAttributeName(String name) {
        Matcher matcher = ATTRIBUTE_NAME_PATTERN.matcher(name);
        if (matcher.find())
            return name;
        throw new IllegalArgumentException(String.format(
                "Incorrect attribute name \"%s\". Attribute name may contain only latin letters, digits, and underscore '_'",
                name));
    }
}
