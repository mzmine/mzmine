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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.hierarchical;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringAlgorithm;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringResult;
import net.sf.mzmine.parameters.ParameterSet;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Instances;

public class HierarClusterer implements ClusteringAlgorithm {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final String MODULE_NAME = "Hierarchical clusterer";

    @Override
    public String getName() {
	return MODULE_NAME;
    }

    @Override
    public ClusteringResult performClustering(Instances dataset,
	    ParameterSet parameters) {
	HierarchicalClusterer clusterer = new HierarchicalClusterer();
	String[] options = new String[5];
	LinkType link = parameters.getParameter(
		HierarClustererParameters.linkType).getValue();
	DistanceType distanceType = parameters.getParameter(
		HierarClustererParameters.distanceType).getValue();
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
	    clusterer.setOptions(options);
	    clusterer.buildClusterer(dataset);
	    String clusterGraph = clusterer.graph();
	    ClusteringResult result = new ClusteringResult(null, clusterGraph,
		    1, null);
	    return result;
	} catch (Exception ex) {
	    logger.log(Level.SEVERE, null, ex);
	    return null;
	}
    }

    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
	return HierarClustererParameters.class;
    }

}
