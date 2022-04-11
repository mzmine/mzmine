/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataanalysis.clustering;

import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureSelectionParameter;
import java.util.Arrays;

import io.github.mzmine.modules.dataanalysis.clustering.em.EMClusterer;
import io.github.mzmine.modules.dataanalysis.clustering.farthestfirst.FarthestFirstClusterer;
import io.github.mzmine.modules.dataanalysis.clustering.hierarchical.HierarClusterer;
import io.github.mzmine.modules.dataanalysis.clustering.simplekmeans.SimpleKMeansClusterer;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.FeatureMeasurementType;

public class ClusteringParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final ComboParameter<FeatureMeasurementType> featureMeasurementType =
      new ComboParameter<FeatureMeasurementType>("Peak measurement type", "Measure features using",
          FeatureMeasurementType.values());

  public static final RawDataFilesParameter dataFiles =
      new RawDataFilesParameter(new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES));

  public static final FeatureSelectionParameter rows =
      new FeatureSelectionParameter("Feature list rows", "Feature list rows to include in calculation",
          Arrays.asList(new FeatureSelection[] {new FeatureSelection(null, null, null, null)}));

  private static ClusteringAlgorithm algorithms[] = new ClusteringAlgorithm[] {new EMClusterer(),
      new FarthestFirstClusterer(), new SimpleKMeansClusterer(), new HierarClusterer()};

  public static final ModuleComboParameter<ClusteringAlgorithm> clusteringAlgorithm =
      new ModuleComboParameter<ClusteringAlgorithm>("Clustering algorithm",
          "Select the algorithm you want to use for clustering", algorithms, algorithms[0]);

  public static final ComboParameter<ClusteringDataType> typeOfData =
      new ComboParameter<ClusteringDataType>("Type of data",
          "Specify the type of data used for the clustering: samples or variables",
          ClusteringDataType.values());

  public ClusteringParameters() {
    super(new Parameter[] {featureLists, featureMeasurementType, dataFiles, rows, clusteringAlgorithm,
        typeOfData});
  }

}
