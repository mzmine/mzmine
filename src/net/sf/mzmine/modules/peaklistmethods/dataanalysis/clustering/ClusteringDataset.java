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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.LinkedList;

import java.util.List;
import java.util.logging.Level;
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
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskEvent;

import org.jfree.data.xy.AbstractXYDataset;
import weka.clusterers.Clusterer;
import weka.clusterers.Cobweb;
import weka.clusterers.EM;
import weka.clusterers.FarthestFirst;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class ClusteringDataset extends AbstractXYDataset implements
        ProjectionPlotDataset {

        private Logger logger = Logger.getLogger(this.getClass().getName());
        private LinkedList<TaskListener> taskListeners = new LinkedList<TaskListener>();
        private double[] component1Coords;
        private double[] component2Coords;
        private ClusteringParameters parameters;
        private RawDataFile[] selectedRawDataFiles;
        private PeakListRow[] selectedRows;
        private int[] groupsForSelectedRawDataFiles;
        private Object[] parameterValuesForGroups;
        int numberOfGroups, finalNumberOfGroups;
        private String datasetTitle;
        private int xAxisDimension = 1;
        private int yAxisDimension = 2;
        private TaskStatus status = TaskStatus.WAITING;
        private String errorMessage;
        private ProjectionStatus projectionStatus;
        private ClusteringAlgorithmsEnum clusteringAlgorithm;
        private String visualizationType;

        public ClusteringDataset(ClusteringParameters parameters) {

                this.parameters = parameters;

                selectedRawDataFiles = parameters.getSelectedDataFiles();
                selectedRows = parameters.getSelectedRows();
                numberOfGroups = (Integer) parameters.getParameterValue(ClusteringParameters.numberOfGroups);
                clusteringAlgorithm = (ClusteringAlgorithmsEnum) parameters.getParameterValue(ClusteringParameters.clusteringAlgorithm);
                visualizationType = (String) parameters.getParameterValue(ClusteringParameters.visualization);

                datasetTitle = "Clustering";

                // Determine groups for selected raw data files
                groupsForSelectedRawDataFiles = new int[selectedRawDataFiles.length];

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

        public RawDataFile getRawDataFile(int item) {
                return selectedRawDataFiles[item];
        }

        public int getGroupNumber(int item) {
                return groupsForSelectedRawDataFiles[item];
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

                // Generate matrix of raw data (input to CDA)
                boolean useArea = true;
                if (parameters.getParameterValue(ClusteringParameters.peakMeasurementType) == ClusteringParameters.PeakMeasurementTypeArea) {
                        useArea = true;
                }
                if (parameters.getParameterValue(ClusteringParameters.peakMeasurementType) == ClusteringParameters.PeakMeasurementTypeHeight) {
                        useArea = false;
                }

                double[][] rawData = new double[selectedRawDataFiles.length][selectedRows.length];
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
                List<Integer> clusteringResult = getClusterer(clusteringAlgorithm, createWekaDataset(rawData));

                for (int ind = 0; ind < selectedRawDataFiles.length; ind++) {
                        groupsForSelectedRawDataFiles[ind] = clusteringResult.get(ind);
                }

                parameterValuesForGroups = new Object[finalNumberOfGroups];
                for (int i = 0; i < finalNumberOfGroups; i++) {
                        parameterValuesForGroups[i] = "Group " + i;
                }

                int numComponents = xAxisDimension;
                if (yAxisDimension > numComponents) {
                        numComponents = yAxisDimension;
                }

                // Scale data and do PCA
                if (visualizationType.contains(ClusteringParameters.visualizationPCA)) {
                        Preprocess.scaleToUnityVariance(rawData);
                        PCA pcaProj = new PCA(rawData, numComponents);
                        projectionStatus = pcaProj.getProjectionStatus();

                        double[][] result = pcaProj.getState();

                        if (status == TaskStatus.CANCELED) {
                                return;
                        }


                        component1Coords = result[xAxisDimension - 1];
                        component2Coords = result[yAxisDimension - 1];
                } else if (visualizationType.contains(ClusteringParameters.visualizationSammon)) {
                        // Scale data and do Sammon's mapping
                        Preprocess.scaleToUnityVariance(rawData);
                        Sammons sammonsProj = new Sammons(rawData);

                        projectionStatus = sammonsProj.getProjectionStatus();

                        sammonsProj.iterate(100);

                        if (status == TaskStatus.CANCELED) {
                                return;
                        }

                        double[][] result = sammonsProj.getState();

                        if (status == TaskStatus.CANCELED) {
                                return;
                        }

                        component1Coords = result[xAxisDimension - 1];
                        component2Coords = result[yAxisDimension - 1];
                }
                Desktop desktop = MZmineCore.getDesktop();
                ProjectionPlotWindow newFrame = new ProjectionPlotWindow(desktop, this,
                        parameters);
                desktop.addInternalFrame(newFrame);

                status = TaskStatus.FINISHED;
                logger.info("Finished computing Clustering visualization.");

        }

        private Instances createWekaDataset(double[][] rawData) {
                FastVector attributes = new FastVector();
                int cont = 1;
                for (int i = 0; i < rawData[0].length; i++) {
                        String rowName = "";
                        if (rowName == null || rowName.isEmpty()) {
                                rowName = "Var";
                        }
                        rowName += cont++;

                        Attribute var = new Attribute(rowName);
                        attributes.addElement(var);
                }

                //Creates the dataset
                Instances data = new Instances("Dataset", attributes, 0);

                for (int i = 0; i < rawData.length; i++) {
                        double[] values = new double[data.numAttributes()];
                        for (int e = 0; e < rawData[0].length; e++) {
                                values[e] = rawData[i][e];
                        }
                        Instance inst = new SparseInstance(1.0, values);
                        data.add(inst);
                }
                return data;
        }

        private List<Integer> getClusterer(ClusteringAlgorithmsEnum algorithm, Instances wekaData) {
                List<Integer> clusters = new ArrayList<Integer>();
                String[] options = new String[2];
                Clusterer clusterer = null;
                switch (algorithm) {
                        case COBWEB:
                                clusterer = new Cobweb();
                                break;
                        case EM:
                                clusterer = new EM();
                                options[0] = "-I";
                                options[1] = "100";
                                try {
                                        ((EM) clusterer).setOptions(options);
                                } catch (Exception ex) {
                                        Logger.getLogger(ClusteringDataset.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                break;
                        case FARTHESTFIRST:
                                clusterer = new FarthestFirst();
                                options[0] = "-N";
                                options[1] = String.valueOf(numberOfGroups);
                                try {
                                        ((FarthestFirst) clusterer).setOptions(options);
                                } catch (Exception ex) {
                                        Logger.getLogger(ClusteringDataset.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                break;
                        case SIMPLEKMEANS:
                                clusterer = new SimpleKMeans();
                                options[0] = "-N";
                                options[1] = String.valueOf(numberOfGroups);
                                try {
                                        ((SimpleKMeans) clusterer).setOptions(options);
                                } catch (Exception ex) {
                                        Logger.getLogger(ClusteringDataset.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                break;
                }

                try {
                        clusterer.buildClusterer(wekaData);
                        finalNumberOfGroups = clusterer.numberOfClusters();
                        Enumeration e = wekaData.enumerateInstances();
                        while (e.hasMoreElements()) {
                                clusters.add(clusterer.clusterInstance((Instance) e.nextElement()));
                        }
                } catch (Exception ex) {
                        ex.printStackTrace();
                }
                return clusters;


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

        public String getTaskDescription() {
                if ((parameters == null) || (parameters.getSourcePeakList() == null)) {
                        return "Clustering visualization";
                }
                return "Clustering visualization " + parameters.getSourcePeakList();
        }

        public double getFinishedPercentage() {
                if (projectionStatus == null) {
                        return 0;
                }
                return projectionStatus.getFinishedPercentage();
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
}
