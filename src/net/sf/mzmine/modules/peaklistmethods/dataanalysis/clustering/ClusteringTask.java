/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering;

import figs.treeVisualization.TreeViewJ;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jmprojection.PCA;
import jmprojection.Preprocess;
import jmprojection.ProjectionStatus;
import jmprojection.Sammons;
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.ProjectionPlotDataset;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.ProjectionPlotWindow;
import net.sf.mzmine.taskcontrol.TaskEvent;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskStatus;

import net.sf.mzmine.util.PeakMeasurementType;
import org.jfree.data.xy.AbstractXYDataset;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class ClusteringTask extends AbstractXYDataset implements
        ProjectionPlotDataset {

        private Logger logger = Logger.getLogger(this.getClass().getName());
        private LinkedList<TaskListener> taskListeners = new LinkedList<TaskListener>();
        private double[] component1Coords;
        private double[] component2Coords;
        private ClusteringParameters parameters;
        private RawDataFile[] selectedRawDataFiles;
        private PeakListRow[] selectedRows;
        private int[] groupsForSelectedRawDataFiles, groupsForSelectedVariables;
        private Object[] parameterValuesForGroups;
        private int finalNumberOfGroups;
        private String datasetTitle;
        private int xAxisDimension = 1;
        private int yAxisDimension = 2;
        private TaskStatus status = TaskStatus.WAITING;
        private String errorMessage;
        private ProjectionStatus projectionStatus;
        private ClusteringAlgorithm clusteringAlgorithm;
        private ClusteringDataType typeOfData;
        private Instances dataset;
        private int progress;

        public ClusteringTask(ClusteringParameters parameters, RawDataFile[] selectedRawDataFiles, PeakListRow[] selectedRows) {

                this.parameters = parameters;

                this.selectedRawDataFiles = selectedRawDataFiles;
                this.selectedRows = selectedRows;
                clusteringAlgorithm = parameters.getParameter(ClusteringParameters.clusteringAlgorithm).getValue();
                typeOfData = parameters.getParameter(ClusteringParameters.typeOfData).getValue();

                datasetTitle = "Clustering";

                // Determine groups for selected raw data files
                groupsForSelectedRawDataFiles = new int[selectedRawDataFiles.length];
                groupsForSelectedVariables = new int[selectedRows.length];
        }

        @Override
        public String toString() {
                return datasetTitle;
        }

        public String getXLabel() {
                return "1st projected dimension";
        }

        public String getYLabel() {
                return "2nd projected dimension";
        }

        @Override
        public int getSeriesCount() {
                return 1;
        }

        @Override
        public Comparable getSeriesKey(int series) {
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
                if (typeOfData == ClusteringDataType.VARIABLES) {
                        String name = "ID: " + this.selectedRows[item].getID();
                        name += " M/Z: " + this.selectedRows[item].getAverageMZ() + " RT:" + this.selectedRows[item].getAverageRT();
                        if (selectedRows[item].getPeakIdentities() != null && selectedRows[item].getPeakIdentities().length > 0) {
                                name += " CompoundName: " + selectedRows[item].getPeakIdentities()[0].getName();
                        }
                        return name;
                } else {
                        return selectedRawDataFiles[item].getName();
                }
        }

        public int getGroupNumber(int item) {
                if (typeOfData == ClusteringDataType.VARIABLES) {
                        return groupsForSelectedVariables[item];
                } else {
                        return groupsForSelectedRawDataFiles[item];
                }
        }

        public Object getGroupParameterValue(int groupNumber) {
                if (parameterValuesForGroups == null) {
                        return null;
                }
                if ((parameterValuesForGroups.length - 1) < groupNumber) {
                        return null;
                }
                return parameterValuesForGroups[groupNumber];
        }

        public int getNumberOfGroups() {
                return finalNumberOfGroups;
        }

        public void run() {

                status = TaskStatus.PROCESSING;

                logger.info("Clustering");

                double[][] rawData;

                // Creating weka dataset using samples or metabolites (variables)

                if (typeOfData == ClusteringDataType.VARIABLES) {
                        rawData = createMatrix(false);
                        dataset = createVariableWekaDataset(rawData);
                } else {
                        rawData = createMatrix(true);
                        dataset = createSampleWekaDataset(rawData);
                }

                // Running cluster algorithms
                String cluster = "";
                if (clusteringAlgorithm.toString().equals("Hierarchical Clusterer")) {
                        progress = 0;                        
                        cluster = clusteringAlgorithm.getHierarchicalCluster(dataset);
                        progress = 50;                     
                        cluster = cluster.replaceAll("Newick:", "");
                        if (typeOfData == ClusteringDataType.SAMPLES) {
                                cluster = addMissingSamples(cluster);
                        }
                        progress = 85;
                        if (cluster != null) {
                                Desktop desktop = MZmineCore.getDesktop();
                                TreeViewJ visualizer = new TreeViewJ(640, 480);
                                cluster += ";";
                                visualizer.openMenuAction(cluster);
                                desktop.addInternalFrame(visualizer);
                        }
                } else {

                        List<Integer> clusteringResult = clusteringAlgorithm.getClusterGroups(dataset);

                        // Report window
                        Desktop desktop = MZmineCore.getDesktop();
                        if (typeOfData == ClusteringDataType.SAMPLES) {
                                String[] sampleNames = new String[selectedRawDataFiles.length];
                                for (int i = 0; i < selectedRawDataFiles.length; i++) {
                                        sampleNames[i] = selectedRawDataFiles[i].getName();
                                }

                                ClusteringReportWindow reportWindow = new ClusteringReportWindow(sampleNames, (Integer[]) clusteringResult.toArray(new Integer[0]), "Clustering Report");
                                desktop.addInternalFrame(reportWindow);
                        } else {
                                String[] variableNames = new String[selectedRows.length];
                                for (int i = 0; i < selectedRows.length; i++) {
                                        variableNames[i] = selectedRows[i].getID() + " - " + selectedRows[i].getAverageMZ() + " - " + selectedRows[i].getAverageRT();
                                        if (selectedRows[i].getPeakIdentities() != null && selectedRows[i].getPeakIdentities().length > 0) {
                                                variableNames[i] += " - " + selectedRows[i].getPeakIdentities()[0].getName();
                                        }
                                }

                                ClusteringReportWindow reportWindow = new ClusteringReportWindow(variableNames, (Integer[]) clusteringResult.toArray(new Integer[0]), "Clustering Report");
                                desktop.addInternalFrame(reportWindow);

                        }

                        // Visualization
                        if (typeOfData == ClusteringDataType.VARIABLES) {
                                for (int ind = 0; ind < selectedRows.length; ind++) {
                                        groupsForSelectedVariables[ind] = clusteringResult.get(ind);
                                }

                        } else {
                                for (int ind = 0; ind < selectedRawDataFiles.length; ind++) {
                                        groupsForSelectedRawDataFiles[ind] = clusteringResult.get(ind);
                                }
                        }


                        this.finalNumberOfGroups = clusteringAlgorithm.getNumberOfGroups();
                        parameterValuesForGroups = new Object[finalNumberOfGroups];
                        for (int i = 0; i < finalNumberOfGroups; i++) {
                                parameterValuesForGroups[i] = "Group " + i;
                        }

                        int numComponents = xAxisDimension;
                        if (yAxisDimension > numComponents) {
                                numComponents = yAxisDimension;
                        }

                        if (clusteringAlgorithm.getVisualizationType() == VisualizationType.PCA) {
                                // Scale data and do PCA
                                Preprocess.scaleToUnityVariance(rawData);
                                PCA pcaProj = new PCA(rawData, numComponents);
                                projectionStatus = pcaProj.getProjectionStatus();

                                double[][] result = pcaProj.getState();

                                if (status == TaskStatus.CANCELED) {
                                        return;
                                }

                                component1Coords = result[xAxisDimension - 1];
                                component2Coords = result[yAxisDimension - 1];
                        } else if (clusteringAlgorithm.getVisualizationType() == VisualizationType.SAMMONS) {
                                // Scale data and do Sammon's mapping
                                Preprocess.scaleToUnityVariance(rawData);
                                Sammons sammonsProj = new Sammons(rawData);
                                projectionStatus = sammonsProj.getProjectionStatus();

                                sammonsProj.iterate(100);

                                double[][] result = sammonsProj.getState();

                                if (status == TaskStatus.CANCELED) {
                                        return;
                                }

                                component1Coords = result[xAxisDimension - 1];
                                component2Coords = result[yAxisDimension - 1];
                        }

                        ProjectionPlotWindow newFrame = new ProjectionPlotWindow(desktop.getSelectedPeakLists()[0], this,
                                parameters);
                        desktop.addInternalFrame(newFrame);
                }
                status = TaskStatus.FINISHED;
                logger.info("Finished computing Clustering visualization.");
        }

        /**
         * Creates a matrix of heights of areas
         * @param isForSamples
         * @return
         */
        private double[][] createMatrix(boolean isForSamples) {
                // Generate matrix of raw data (input to CDA)
                boolean useArea = true;
                if (parameters.getParameter(ClusteringParameters.peakMeasurementType).getValue() == PeakMeasurementType.AREA) {
                        useArea = true;
                }
                if (parameters.getParameter(ClusteringParameters.peakMeasurementType).getValue() == PeakMeasurementType.HEIGHT) {
                        useArea = false;
                }
                double[][] rawData;
                if (isForSamples) {
                        rawData = new double[selectedRawDataFiles.length][selectedRows.length];
                        for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
                                PeakListRow peakListRow = selectedRows[rowIndex];
                                for (int fileIndex = 0; fileIndex < selectedRawDataFiles.length; fileIndex++) {
                                        RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
                                        ChromatographicPeak p = peakListRow.getPeak(rawDataFile);
                                        if (p != null) {
                                                if (useArea) {
                                                        rawData[fileIndex][rowIndex] = p.getArea();
                                                } else {
                                                        rawData[fileIndex][rowIndex] = p.getHeight();
                                                }
                                        }
                                }
                        }
                } else {
                        rawData = new double[selectedRows.length][selectedRawDataFiles.length];
                        for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
                                PeakListRow peakListRow = selectedRows[rowIndex];
                                for (int fileIndex = 0; fileIndex < selectedRawDataFiles.length; fileIndex++) {
                                        RawDataFile rawDataFile = selectedRawDataFiles[fileIndex];
                                        ChromatographicPeak p = peakListRow.getPeak(rawDataFile);
                                        if (p != null) {
                                                if (useArea) {
                                                        rawData[rowIndex][fileIndex] = p.getArea();
                                                } else {
                                                        rawData[rowIndex][fileIndex] = p.getHeight();
                                                }
                                        }
                                }
                        }
                }

                return rawData;
        }

        /**
         * Creates the weka data set for clustering of samples
         * @param rawData Data extracted from selected Raw data files and rows.
         * @return Weka library data set
         */
        private Instances createSampleWekaDataset(double[][] rawData) {
                FastVector attributes = new FastVector();

                for (int i = 0; i < rawData[0].length; i++) {
                        String varName = "Var" + i;
                        Attribute var = new Attribute(varName);
                        attributes.addElement(var);
                }

                if (clusteringAlgorithm.toString().equals("Hierarchical Clusterer")) {
                        Attribute name = new Attribute("name", (FastVector) null);
                        attributes.addElement(name);
                }
                Instances data = new Instances("Dataset", attributes, 0);

                for (int i = 0; i < rawData.length; i++) {
                        double[] values = new double[data.numAttributes()];
                        System.arraycopy(rawData[i], 0, values, 0, rawData[0].length);
                        if (clusteringAlgorithm.toString().equals("Hierarchical Clusterer")) {
                                values[data.numAttributes() - 1] = data.attribute("name").addStringValue(this.selectedRawDataFiles[i].getName());
                        }
                        Instance inst = new SparseInstance(1.0, values);
                        data.add(inst);
                }
                return data;
        }

        /**
         * Creates the weka data set for clustering of variables (metabolites)
         * @param rawData Data extracted from selected Raw data files and rows.
         * @return Weka library data set
         */
        private Instances createVariableWekaDataset(double[][] rawData) {
                FastVector attributes = new FastVector();

                for (int i = 0; i < this.selectedRawDataFiles.length; i++) {
                        String varName = "Var" + i;
                        Attribute var = new Attribute(varName);
                        attributes.addElement(var);
                }

                if (clusteringAlgorithm.toString().equals("Hierarchical Clusterer")) {
                        Attribute name = new Attribute("name", (FastVector) null);
                        attributes.addElement(name);
                }
                Instances data = new Instances("Dataset", attributes, 0);

                for (int i = 0; i < selectedRows.length; i++) {
                        double[] values = new double[data.numAttributes()];
                        System.arraycopy(rawData[i], 0, values, 0, rawData[0].length);

                        if (clusteringAlgorithm.toString().equals("Hierarchical Clusterer")) {
                                DecimalFormat twoDForm = new DecimalFormat("#.##");
                                double MZ = Double.valueOf(twoDForm.format(selectedRows[i].getAverageMZ()));
                                double RT = Double.valueOf(twoDForm.format(selectedRows[i].getAverageRT()));
                                String rowName = "MZ->" + MZ + "/RT->" + RT;
                                values[data.numAttributes() - 1] = data.attribute("name").addStringValue(rowName);
                        }
                        Instance inst = new SparseInstance(1.0, values);
                        data.add(inst);
                }
                return data;
        }

        public void cancel() {
                if (projectionStatus != null) {
                        projectionStatus.cancel();
                }

                status = TaskStatus.CANCELED;
        }

        public String getErrorMessage() {
                return errorMessage;
        }

        public TaskStatus getStatus() {
                return status;
        }

        public Object[] getCreatedObjects() {
                return null;
        }

        /**
         * Adds a TaskListener to this Task
         *
         * @param t The TaskListener to add
         */
        public void addTaskListener(TaskListener t) {
                this.taskListeners.add(t);
        }

        /**
         * Returns all of the TaskListeners which are listening to this task.
         *
         * @return An array containing the TaskListeners
         */
        public TaskListener[] getTaskListeners() {
                return this.taskListeners.toArray(new TaskListener[this.taskListeners.size()]);
        }

        private void fireTaskEvent() {
                TaskEvent event = new TaskEvent(this);
                for (TaskListener t : this.taskListeners) {
                        t.statusChanged(event);
                }

        }

        /**
         * @see net.sf.mzmine.taskcontrol.Task#setStatus()
         */
        public void setStatus(TaskStatus newStatus) {
                this.status = newStatus;
                this.fireTaskEvent();
        }

        public boolean isCanceled() {
                return status == TaskStatus.CANCELED;
        }

        public boolean isFinished() {
                return status == TaskStatus.FINISHED;
        }

        /**
         * Adds the missing samples (these samples have no relation with the rest of the samples)
         * to the cluster using the maximun distance inside the cluster result plus 1.
         * @param cluster String with the cluster result in Newick format.
         * @return String with the cluster result after adding the missing samples in Newick format.
         */
        private String addMissingSamples(String cluster) {
                String[] data = cluster.split(":");
                double max = 0;
                for (String d : data) {
                        double value = -1;
                        Pattern p = Pattern.compile("^\\d+(.\\d+)");
                        Matcher m = p.matcher(d);
                        if (m.find()) {
                                value = Double.parseDouble(d.substring(m.start(), m.end()));
                        }
                        if (value > max) {
                                max = value;
                        }
                }
                Pattern p = Pattern.compile("^\\d+(.\\d+)?");
                Matcher m = p.matcher(data[data.length - 1]);
                double lastValue = 0.0;
                if (m.find()) {
                        lastValue = Double.parseDouble(data[data.length - 1].substring(m.start(), m.end()));
                }

                max += lastValue;
                for (int i = 0; i < this.selectedRawDataFiles.length; i++) {
                        if (!cluster.contains(this.selectedRawDataFiles[i].getName())) {
                                max++;
                                cluster = "(" + cluster + ":1.0," + this.selectedRawDataFiles[i].getName() + ":" + max + ")";
                        }
                }

                return cluster;
        }

        @Override
        public String getTaskDescription() {
                return "Clustering visualization " + datasetTitle;
        }

        @Override
        public double getFinishedPercentage() {
                if (this.projectionStatus != null) {
                        if (projectionStatus.getFinishedPercentage() > 1.0) {
                                return 1.0;
                        }
                        return projectionStatus.getFinishedPercentage();
                } else {
                        if (progress > 100) {
                                return 1.0;
                        }
                        progress++;
                        return ((double) progress / 100);
                }
        }
}
