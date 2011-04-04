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
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.hierarchical;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringAlgorithm;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.VisualizationType;
import net.sf.mzmine.parameters.ParameterSet;
import weka.clusterers.Clusterer;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Instances;

public class HierarClusterer implements ClusteringAlgorithm {

        private ParameterSet parameters;

        public HierarClusterer() {
                parameters = new HierarClustererParameters();
        }

        public String toString() {
                return "Hierarchical Clusterer";
        }

        @Override
        public ParameterSet getParameterSet() {
                return parameters;
        }

        public List<Integer> getClusterGroups(Instances dataset) {
                return null;
        }

        public String getHierarchicalCluster(Instances dataset) {
                Clusterer clusterer = new HierarchicalClusterer();
                String[] options = new String[5];
                LinkType link = parameters.getParameter(HierarClustererParameters.linkType).getValue();
                DistanceType distanceType = parameters.getParameter(HierarClustererParameters.distanceType).getValue();
                options[0] = "-L";
                options[1] = link.name();
                options[2] = "-A";
                switch (distanceType) {
                        case EUCLIDIAN:
                                options[3] = "weka.core.EuclideanDistance";
                                break;
                        case CHEBYSHEV:
                                options[3] = "weka.core.ChebyshevDistance";
                                break;
                        case MANHATTAN:
                                options[3] = "weka.core.ManhattanDistance";
                                break;
                        case MINKOWSKI:
                                options[3] = "weka.core.MinkowskiDistance";
                                break;
                }

                options[4] = "-P";
                try {
                        ((HierarchicalClusterer) clusterer).setOptions(options);
                        clusterer.buildClusterer(dataset);
                        return ((HierarchicalClusterer) clusterer).graph();
                } catch (Exception ex) {
                        Logger.getLogger(HierarClusterer.class.getName()).log(Level.SEVERE, null, ex);
                        return null;
                }
        }

        public VisualizationType getVisualizationType() {
                return null;
        }

        public int getNumberOfGroups() {
                return 1;
        }


}
