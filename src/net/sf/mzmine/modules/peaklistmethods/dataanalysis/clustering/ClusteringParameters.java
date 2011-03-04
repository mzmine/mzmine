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

import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.em.EMClusterer;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.farthestfirst.FarthestFirstClusterer;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.hierarchical.HierarClusterer;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.simplekmeans.SimpleKMeansClusterer;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.ProjectionPlotParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;

public class ClusteringParameters extends SimpleParameterSet {

	private static ClusteringAlgorithm algorithms[] = new ClusteringAlgorithm[] {
			new EMClusterer(), new FarthestFirstClusterer(),
			new SimpleKMeansClusterer(), new HierarClusterer() };

	public static final ModuleComboParameter<ClusteringAlgorithm> clusteringAlgorithm = new ModuleComboParameter<ClusteringAlgorithm>(
			"Clustering algorithm",
			"Select the algorithm you want to use for clustering", algorithms);

	public static final ComboParameter<ClusteringDataType> typeOfData = new ComboParameter<ClusteringDataType>(
			"Type of data",
			"Specify the type of data used for the clustering: samples or variables",
			ClusteringDataType.values());

	public ClusteringParameters() {
		super(
				new Parameter[] { ProjectionPlotParameters.dataFiles,
						ProjectionPlotParameters.peakMeasurementType,
						ProjectionPlotParameters.rows, clusteringAlgorithm,
						typeOfData });
	}
}
