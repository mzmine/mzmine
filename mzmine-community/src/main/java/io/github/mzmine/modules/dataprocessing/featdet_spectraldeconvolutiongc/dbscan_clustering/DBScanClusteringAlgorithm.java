/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.dbscan_clustering;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionAlgorithm;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.hierarchicalclustering.HierarchicalClusteringParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Not yet optimal for clustering of GC data. DBScan can grow further than the set rtTolerance.
 * Maybe needs to be combined with other filters or max width. If starting with the node with the
 * highest number of neighbors, this may be a node sitting in between two bigger clusters. This is
 * why we start with the node that has the lowest mean distance to all its neighbors
 */
public class DBScanClusteringAlgorithm implements SpectralDeconvolutionAlgorithm {

  private final RTTolerance rtTolerance;
  private final int minNumberOfSignals;

  /**
   * Required default parameter for modules via reflection
   */
  public DBScanClusteringAlgorithm() {
    this(new RTTolerance(0.02f, Unit.MINUTES), 8);
  }

  public DBScanClusteringAlgorithm(ParameterSet parameters) {
    var rtTolerance = parameters.getValue(HierarchicalClusteringParameters.RT_TOLERANCE);
    var minNumberOfSignals = parameters.getValue(
        HierarchicalClusteringParameters.MIN_NUMBER_OF_SIGNALS);
    this(rtTolerance, minNumberOfSignals);
  }

  public DBScanClusteringAlgorithm(RTTolerance rtTolerance, int minNumberOfSignals) {
    this.rtTolerance = rtTolerance;
    this.minNumberOfSignals = minNumberOfSignals;
  }

  @Override
  public List<List<ModularFeature>> groupFeatures(List<ModularFeature> features) {
    DBScan<ModularFeature> dbScan = new DBScan<>(rtTolerance.getToleranceInMinutes(),
        minNumberOfSignals, ModularFeature::getRT);
    return dbScan.clusterSorted(features);
  }

  @Override
  public RTTolerance getRtTolerance() {
    return rtTolerance;
  }

  @Override
  public @NotNull String getName() {
    return "DBScan clustering";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return DBScanClusteringParameters.class;
  }
}

