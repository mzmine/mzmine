/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
