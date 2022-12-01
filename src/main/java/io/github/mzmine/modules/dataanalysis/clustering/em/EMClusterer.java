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

package io.github.mzmine.modules.dataanalysis.clustering.em;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.modules.dataanalysis.clustering.ClusteringAlgorithm;
import io.github.mzmine.modules.dataanalysis.clustering.ClusteringResult;
import io.github.mzmine.parameters.ParameterSet;
import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;

public class EMClusterer implements ClusteringAlgorithm {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String MODULE_NAME = "Density-based clusterer";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public ClusteringResult performClustering(Instances dataset, ParameterSet parameters) {

    List<Integer> clusters = new ArrayList<Integer>();
    String[] options = new String[2];
    EM clusterer = new EM();

    int numberOfIterations =
        parameters.getParameter(EMClustererParameters.numberOfIterations).getValue();
    options[0] = "-I";
    options[1] = String.valueOf(numberOfIterations);

    try {
      clusterer.setOptions(options);
      clusterer.buildClusterer(dataset);
      Enumeration<?> e = dataset.enumerateInstances();
      while (e.hasMoreElements()) {
        clusters.add(clusterer.clusterInstance((Instance) e.nextElement()));
      }
      ClusteringResult result = new ClusteringResult(clusters, null, clusterer.numberOfClusters(),
          parameters.getParameter(EMClustererParameters.visualization).getValue());
      return result;

    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return EMClustererParameters.class;
  }
}
