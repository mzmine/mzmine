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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots;

import java.util.Vector;
import java.util.logging.Logger;

import org.jfree.data.xy.AbstractXYDataset;

import jmprojection.PCA;
import jmprojection.Preprocess;
import jmprojection.ProjectionStatus;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakMeasurementType;

public class PCADataset extends AbstractXYDataset
        implements ProjectionPlotDataset {

    private static final long serialVersionUID = 1L;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private double[] component1Coords;
    private double[] component2Coords;

    private ParameterSet parameters;
    private PeakList peakList;

    private ColoringType coloringType;

    private RawDataFile[] selectedRawDataFiles;
    private PeakListRow[] selectedRows;

    private int[] groupsForSelectedRawDataFiles;
    private Object[] parameterValuesForGroups;
    int numberOfGroups;

    private String datasetTitle;
    private int xAxisPC;
    private int yAxisPC;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private ProjectionStatus projectionStatus;

    public PCADataset(MZmineProject project, ParameterSet parameters) {

        this.peakList = parameters
                .getParameter(ProjectionPlotParameters.peakLists).getValue()
                .getMatchingPeakLists()[0];
        this.parameters = parameters;

        this.xAxisPC = parameters
                .getParameter(ProjectionPlotParameters.xAxisComponent)
                .getValue();
        this.yAxisPC = parameters
                .getParameter(ProjectionPlotParameters.yAxisComponent)
                .getValue();

        coloringType = parameters
                .getParameter(ProjectionPlotParameters.coloringType).getValue();

        selectedRawDataFiles = parameters
                .getParameter(ProjectionPlotParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();
        selectedRows = peakList.getRows();

        datasetTitle = "Principal component analysis";

        // Determine groups for selected raw data files
        groupsForSelectedRawDataFiles = new int[selectedRawDataFiles.length];

        if (coloringType.equals(ColoringType.NOCOLORING)) {
            // All files to a single group
            for (int ind = 0; ind < selectedRawDataFiles.length; ind++)
                groupsForSelectedRawDataFiles[ind] = 0;

            numberOfGroups = 1;
        }

        if (coloringType.equals(ColoringType.COLORBYFILE)) {
            // Each file to own group
            for (int ind = 0; ind < selectedRawDataFiles.length; ind++)
                groupsForSelectedRawDataFiles[ind] = ind;

            numberOfGroups = selectedRawDataFiles.length;
        }

        if (coloringType.isByParameter()) {
            // Group files with same parameter value to same group
            Vector<Object> availableParameterValues = new Vector<Object>();
            UserParameter<?, ?> selectedParameter = coloringType.getParameter();
            for (RawDataFile rawDataFile : selectedRawDataFiles) {
                Object paramValue = project.getParameterValue(selectedParameter,
                        rawDataFile);
                if (!availableParameterValues.contains(paramValue))
                    availableParameterValues.add(paramValue);
            }

            for (int ind = 0; ind < selectedRawDataFiles.length; ind++) {
                Object paramValue = project.getParameterValue(selectedParameter,
                        selectedRawDataFiles[ind]);
                groupsForSelectedRawDataFiles[ind] = availableParameterValues
                        .indexOf(paramValue);
            }
            parameterValuesForGroups = availableParameterValues.toArray();

            numberOfGroups = parameterValuesForGroups.length;
        }

    }

    public String toString() {
        return datasetTitle;
    }

    public String getXLabel() {
        if (xAxisPC == 1)
            return "1st PC";
        if (xAxisPC == 2)
            return "2nd PC";
        if (xAxisPC == 3)
            return "3rd PC";
        return "" + xAxisPC + "th PC";
    }

    public String getYLabel() {
        if (yAxisPC == 1)
            return "1st PC";
        if (yAxisPC == 2)
            return "2nd PC";
        if (yAxisPC == 3)
            return "3rd PC";
        return "" + yAxisPC + "th PC";
    }

    @Override
    public int getSeriesCount() {
        return 1;
    }

    @Override
    public Comparable<Integer> getSeriesKey(int series) {
        return 1;
    }

    public int getItemCount(int series) {
        return component1Coords.length;
    }

    public Number getX(int series, int item) {
        return component1Coords[item];
    }

    public Number getY(int series, int item) {
        return component2Coords[item];
    }

    public String getRawDataFile(int item) {
        return selectedRawDataFiles[item].getName();
    }

    public int getGroupNumber(int item) {
        return groupsForSelectedRawDataFiles[item];
    }

    public Object getGroupParameterValue(int groupNumber) {
        if (parameterValuesForGroups == null)
            return null;
        if ((parameterValuesForGroups.length - 1) < groupNumber)
            return null;
        return parameterValuesForGroups[groupNumber];
    }

    public int getNumberOfGroups() {
        return numberOfGroups;
    }

    public void run() {

        status = TaskStatus.PROCESSING;

        logger.info("Computing PCA projection plot");

        // Generate matrix of raw data (input to PCA)
        final boolean useArea = (parameters
                .getParameter(ProjectionPlotParameters.peakMeasurementType)
                .getValue() == PeakMeasurementType.AREA);

        if (selectedRows.length == 0) {
            this.status = TaskStatus.ERROR;
            errorMessage = "No peaks selected for PCA plot";
            return;
        }
        if (selectedRawDataFiles.length == 0) {
            this.status = TaskStatus.ERROR;
            errorMessage = "No raw data files selected for PCA plot";
            return;
        }

        double[][] rawData = new double[selectedRawDataFiles.length][selectedRows.length];
        for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
            PeakListRow peakListRow = selectedRows[rowIndex];
            for (int fileIndex = 0; fileIndex < selectedRawDataFiles.length; fileIndex++) {
                RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
                Feature p = peakListRow.getPeak(rawDataFile);
                if (p != null) {
                    if (useArea)
                        rawData[fileIndex][rowIndex] = p.getArea();
                    else
                        rawData[fileIndex][rowIndex] = p.getHeight();
                }
            }
        }

        int numComponents = xAxisPC;
        if (yAxisPC > numComponents)
            numComponents = yAxisPC;

        // Scale data and do PCA
        Preprocess.scaleToUnityVariance(rawData);

        // Replace NaN values with 0.0
        for (int i = 0; i < rawData.length; i++) {
            for (int j = 0; j < rawData[i].length; j++) {
                if (Double.isNaN(rawData[i][j]))
                    rawData[i][j] = 0.0;
            }
        }

        PCA pcaProj = new PCA(rawData, numComponents);

        projectionStatus = pcaProj.getProjectionStatus();

        double[][] result = pcaProj.getState();

        if (status == TaskStatus.CANCELED)
            return;

        component1Coords = result[xAxisPC - 1];
        component2Coords = result[yAxisPC - 1];

        ProjectionPlotWindow newFrame = new ProjectionPlotWindow(peakList, this,
                parameters);
        newFrame.setVisible(true);

        status = TaskStatus.FINISHED;
        logger.info("Finished computing projection plot.");

    }

    public void cancel() {
        if (projectionStatus != null)
            projectionStatus.cancel();
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "PCA projection";
    }

    public double getFinishedPercentage() {
        if (projectionStatus == null)
            return 0;
        return projectionStatus.getFinishedPercentage();
    }

}
