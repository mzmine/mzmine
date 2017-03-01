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

import jmprojection.CDA;
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

public class CDADataset extends AbstractXYDataset
        implements ProjectionPlotDataset {

    private static final long serialVersionUID = 1L;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private double[] component1Coords;
    private double[] component2Coords;

    private final ParameterSet parameters;
    private final PeakList peakList;

    private ColoringType coloringType;

    private RawDataFile[] selectedRawDataFiles;
    private PeakListRow[] selectedRows;

    private int[] groupsForSelectedRawDataFiles;
    private Object[] parameterValuesForGroups;
    int numberOfGroups;

    private String datasetTitle;
    private int xAxisDimension;
    private int yAxisDimension;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private ProjectionStatus projectionStatus;

    public CDADataset(MZmineProject project, ParameterSet parameters) {

        this.peakList = parameters
                .getParameter(ProjectionPlotParameters.peakLists).getValue()
                .getMatchingPeakLists()[0];
        this.parameters = parameters;

        this.xAxisDimension = parameters
                .getParameter(ProjectionPlotParameters.xAxisComponent)
                .getValue();
        this.yAxisDimension = parameters
                .getParameter(ProjectionPlotParameters.yAxisComponent)
                .getValue();

        coloringType = parameters
                .getParameter(ProjectionPlotParameters.coloringType).getValue();
        selectedRawDataFiles = parameters
                .getParameter(ProjectionPlotParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();
        selectedRows = peakList.getRows();

        datasetTitle = "Curvilinear distance analysis";

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
        if (xAxisDimension == 1)
            return "1st projected dimension";
        if (xAxisDimension == 2)
            return "2nd projected dimension";
        if (xAxisDimension == 3)
            return "3rd projected dimension";
        return "" + xAxisDimension + "th projected dimension";
    }

    public String getYLabel() {
        if (yAxisDimension == 1)
            return "1st projected dimension";
        if (yAxisDimension == 2)
            return "2nd projected dimension";
        if (yAxisDimension == 3)
            return "3rd projected dimension";
        return "" + yAxisDimension + "th projected dimension";
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

        if (selectedRows.length == 0) {
            this.status = TaskStatus.ERROR;
            errorMessage = "No peaks selected for CDA plot";
            return;
        }
        if (selectedRawDataFiles.length == 0) {
            this.status = TaskStatus.ERROR;
            errorMessage = "No raw data files selected for CDA plot";
            return;
        }
        
        logger.info("Computing projection plot");

        // Generate matrix of raw data (input to CDA)
        boolean useArea = false;
        if (parameters
                .getParameter(ProjectionPlotParameters.peakMeasurementType)
                .getValue() == PeakMeasurementType.AREA)
            useArea = true;

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

        int numComponents = xAxisDimension;
        if (yAxisDimension > numComponents)
            numComponents = yAxisDimension;

        // Scale data and do CDA
        Preprocess.scaleToUnityVariance(rawData);
        CDA cdaProj = new CDA(rawData);
        cdaProj.iterate(100);

        if (status == TaskStatus.CANCELED)
            return;

        double[][] result = cdaProj.getState();

        if (status == TaskStatus.CANCELED)
            return;

        component1Coords = result[xAxisDimension - 1];
        component2Coords = result[yAxisDimension - 1];

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
        return "CDA projection";
    }

    public double getFinishedPercentage() {
        if (projectionStatus == null)
            return 0;
        return projectionStatus.getFinishedPercentage();
    }

}
