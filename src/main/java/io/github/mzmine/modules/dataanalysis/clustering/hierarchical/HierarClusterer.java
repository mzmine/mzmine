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

package io.github.mzmine.modules.dataanalysis.clustering.hierarchical;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.modules.dataanalysis.clustering.ClusteringAlgorithm;
import io.github.mzmine.modules.dataanalysis.clustering.ClusteringResult;
import io.github.mzmine.parameters.ParameterSet;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Instances;

public class HierarClusterer implements ClusteringAlgorithm {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String MODULE_NAME = "Hierarchical clusterer";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public ClusteringResult performClustering(Instances dataset, ParameterSet parameters) {
    HierarchicalClusterer clusterer = new HierarchicalClusterer();
    String[] options = new String[5];
    LinkType link = parameters.getParameter(HierarClustererParameters.linkType).getValue();
    DistanceType distanceType =
        parameters.getParameter(HierarClustererParameters.distanceType).getValue();
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
      clusterer.setPrintNewick(true);
      clusterer.buildClusterer(dataset);
      // clusterer.graph() gives only the first cluster and in the case
      // there
      // are more than one cluster the variables in the second cluster are
      // missing.
      // I'm using clusterer.toString() which contains all the clusters in
      // Newick format.
      ClusteringResult result =
          new ClusteringResult(null, clusterer.toString(), clusterer.getNumClusters(), null);
      return result;
    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return HierarClustererParameters.class;
  }

}
