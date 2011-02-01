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

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.ProjectionPlotParameters;

public class ClusteringParameters extends ProjectionPlotParameters {

        private static ClusteringAlgorithmsEnum[] val = ClusteringAlgorithmsEnum.values();
        public static String visualizationPCA = "PCA";
        public static String visualizationSammon = "Sammon's projection";
        private static String[] visualizationType = {visualizationPCA, visualizationSammon};
        private static String[] clusteringData = {"Samples", "Variables"};
        private static String[] hierarchicalLinkType = {"Single", "Complete", "Average", "Mean", "Centroid", "Ward", "Adjusted complete", "Neighbor Joining"};
        private static String[] hierarchicaldistances = {"Euclidian", "Chebyshev", "Manhattan", "Minkowski"};
        public static final Parameter clusteringAlgorithm = new SimpleParameter(
                ParameterType.STRING, "Select the algorithm",
                "Select the algorithm you want to use for clustering", null, val);
        public static final Parameter typeOfData = new SimpleParameter(
                ParameterType.STRING, "Type of data",
                "Specify the type of data used for the clustering: samples or variables", null, clusteringData);
        public static final Parameter linkType = new SimpleParameter(
                ParameterType.STRING, "Type of link",
                "", null, hierarchicalLinkType);
        public static final Parameter distances = new SimpleParameter(
                ParameterType.STRING, "Distances",
                "", null, hierarchicaldistances);
        public static final Parameter numberOfGroups = new SimpleParameter(
                ParameterType.INTEGER, "Number of clusters to generate",
                "Specify the number of clusters to generate.",
                new Integer(3));
        public static final Parameter visualization = new SimpleParameter(
                ParameterType.STRING, "Select the visualization type",
                "Select the kind of visualization for the clustering result", null, visualizationType);

        public ClusteringParameters(PeakList sourcePeakList) {
                this();
                this.sourcePeakList = sourcePeakList;
                this.selectedDataFiles = sourcePeakList.getRawDataFiles();
                this.selectedRows = sourcePeakList.getRows();
                this.selectedParameter = null;
        }

        public ClusteringParameters() {
                super(new Parameter[]{peakMeasurementType, clusteringAlgorithm, typeOfData, linkType, distances, numberOfGroups, visualization});
        }
}
