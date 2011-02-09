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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.LinkedList;

import java.util.List;
import java.util.logging.Level;
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
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskEvent;

import org.jfree.data.xy.AbstractXYDataset;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.clusterers.FarthestFirst;
import weka.clusterers.HierarchicalClusterer;
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
        private int[] groupsForSelectedRawDataFiles, groupsForSelectedVariables;
        private Object[] parameterValuesForGroups;
        private int numberOfGroups, finalNumberOfGroups;
        private String datasetTitle;
        private int xAxisDimension = 1;
        private int yAxisDimension = 2;
        private TaskStatus status = TaskStatus.WAITING;
        private String errorMessage;
        private ProjectionStatus projectionStatus;
        private ClusteringAlgorithmsEnum clusteringAlgorithm;
        private String visualizationType;
        private String typeOfData, linkType, distances;
        private float progress = 0.0f;

        public ClusteringDataset(ClusteringParameters parameters) {

                this.parameters = parameters;

                selectedRawDataFiles = parameters.getSelectedDataFiles();
                selectedRows = parameters.getSelectedRows();
                numberOfGroups = (Integer) parameters.getParameterValue(ClusteringParameters.numberOfGroups);
                clusteringAlgorithm = (ClusteringAlgorithmsEnum) parameters.getParameterValue(ClusteringParameters.clusteringAlgorithm);
                visualizationType = (String) parameters.getParameterValue(ClusteringParameters.visualization);
                typeOfData = (String) parameters.getParameterValue(ClusteringParameters.typeOfData);
                linkType = (String) parameters.getParameterValue(ClusteringParameters.linkType);
                distances = (String) parameters.getParameterValue(ClusteringParameters.distances);

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
                if (typeOfData.equals("Variables")) {
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
                if (typeOfData.equals("Variables")) {
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
                Instances dataset;
                if (typeOfData.equals("Variables")) {
                        rawData = createMatrix(false);
                        dataset = createVariableWekaDataset(rawData);
                } else {
                        rawData = createMatrix(true);
                        dataset = createSampleWekaDataset(rawData);
                }
                progress = 0.05f;

                // Running cluster algorithms
                String cluster = "";
                if (clusteringAlgorithm == ClusteringAlgorithmsEnum.HIERARCHICAL) {
                        cluster = this.getHierarchicalClustering(dataset);
                        cluster = cluster.replaceAll("Newick:", "");
                        if (typeOfData.equals("Samples")) {
                                cluster = addMissingSamples(cluster);
                        }                     
                        if (cluster != null) {
                                Desktop desktop = MZmineCore.getDesktop();
                                TreeViewJ visualizer = new TreeViewJ(640, 480);
                                cluster += ";";
                                visualizer.openMenuAction(cluster);
                                desktop.addInternalFrame(visualizer);
                        }
                        progress = 0.12f;
                } else {

                        List<Integer> clusteringResult = getClusterer(clusteringAlgorithm, dataset);

                        // Report window
                        Desktop desktop = MZmineCore.getDesktop();
                        if (typeOfData.equals("Samples")) {
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

                        progress = 0.12f;

                        // Visualization
                        if (typeOfData.equals("Variables")) {
                                for (int ind = 0; ind < selectedRows.length; ind++) {
                                        groupsForSelectedVariables[ind] = clusteringResult.get(ind);
                                }

                        } else {
                                for (int ind = 0; ind < selectedRawDataFiles.length; ind++) {
                                        groupsForSelectedRawDataFiles[ind] = clusteringResult.get(ind);
                                }
                        }

                        parameterValuesForGroups = new Object[finalNumberOfGroups];
                        for (int i = 0; i < finalNumberOfGroups; i++) {
                                parameterValuesForGroups[i] = "Group " + i;
                        }

                        int numComponents = xAxisDimension;
                        if (yAxisDimension > numComponents) {
                                numComponents = yAxisDimension;
                        }

                        if (visualizationType.contains(ClusteringParameters.visualizationPCA)) {
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
                        } else if (visualizationType.contains(ClusteringParameters.visualizationSammon)) {
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

                        ProjectionPlotWindow newFrame = new ProjectionPlotWindow(desktop, this,
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
                if (parameters.getParameterValue(ClusteringParameters.peakMeasurementType) == ClusteringParameters.PeakMeasurementTypeArea) {
                        useArea = true;
                }
                if (parameters.getParameterValue(ClusteringParameters.peakMeasurementType) == ClusteringParameters.PeakMeasurementTypeHeight) {
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

                if (clusteringAlgorithm == ClusteringAlgorithmsEnum.HIERARCHICAL) {
                        Attribute name = new Attribute("name", (FastVector) null);
                        attributes.addElement(name);
                }
                Instances data = new Instances("Dataset", attributes, 0);

                for (int i = 0; i < rawData.length; i++) {
                        double[] values = new double[data.numAttributes()];
                        for (int e = 0; e < rawData[0].length; e++) {
                                values[e] = rawData[i][e];
                        }
                        if (clusteringAlgorithm == ClusteringAlgorithmsEnum.HIERARCHICAL) {
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

                if (clusteringAlgorithm == ClusteringAlgorithmsEnum.HIERARCHICAL) {
                        Attribute name = new Attribute("name", (FastVector) null);
                        attributes.addElement(name);
                }
                Instances data = new Instances("Dataset", attributes, 0);

                for (int i = 0; i < selectedRows.length; i++) {
                        double[] values = new double[data.numAttributes()];
                        for (int e = 0; e < rawData[0].length; e++) {
                                values[e] = rawData[i][e];
                        }


                        if (clusteringAlgorithm == ClusteringAlgorithmsEnum.HIERARCHICAL) {
                                DecimalFormat twoDForm = new DecimalFormat("#.##");
                                double MZ = Double.valueOf(twoDForm.format(selectedRows[i].getAverageMZ()));
                                double RT = Double.valueOf(twoDForm.format(selectedRows[i].getAverageRT()));
                                String rowName = "MZ->" + MZ + "/RT->" + RT;
                                if (selectedRows[i].getPeakIdentities() != null && selectedRows[i].getPeakIdentities().length > 0) {
                                        rowName += "/Name->" + selectedRows[i].getPeakIdentities()[0].getName();
                                }

                                values[data.numAttributes() - 1] = data.attribute("name").addStringValue(rowName);
                        }
                        Instance inst = new SparseInstance(1.0, values);
                        data.add(inst);
                }
                return data;
        }

        /**
         * Constructs the clustering algorithm using Weka library
         * @param algorithm Type of clustering algorithm
         * @param wekaData Weka data set
         * @return List of the cluster number of each selected Raw data file.
         */
        private List<Integer> getClusterer(ClusteringAlgorithmsEnum algorithm, Instances wekaData) {
                List<Integer> clusters = new ArrayList<Integer>();
                String[] options = new String[2];
                Clusterer clusterer = null;
                switch (algorithm) {
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

        public String getHierarchicalClustering(Instances wekaData) {

                Clusterer clusterer = new HierarchicalClusterer();
                String[] options = new String[5];
                options[0] = "-L";
                options[1] = linkType;
                options[2] = "-A";
                if (distances.equals("Euclidian")) {
                        options[3] = "weka.core.EuclideanDistance";
                } else if (distances.equals("Chebyshev")) {
                        options[3] = "weka.core.ChebyshevDistance";
                } else if (distances.equals("Manhattan")) {
                        options[3] = "weka.core.ManhattanDistance";
                } else if (distances.equals("Minkowski")) {
                        options[3] = "weka.core.MinkowskiDistance";
                }
                options[4] = "-P";
                // options[5] = "-B";
                try {
                        ((HierarchicalClusterer) clusterer).setOptions(options);
                } catch (Exception ex) {
                        Logger.getLogger(ClusteringDataset.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                        clusterer.buildClusterer(wekaData);
                        return ((HierarchicalClusterer) clusterer).graph();
                } catch (Exception ex) {
                        Logger.getLogger(ClusteringDataset.class.getName()).log(Level.SEVERE, null, ex);
                        return null;
                }
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
                        return progress;
                }
                if (visualizationType.contains(ClusteringParameters.visualizationPCA)) {
                        return projectionStatus.getFinishedPercentage() + progress;
                } else {
                        return projectionStatus.getFinishedPercentage();
                }
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
}
