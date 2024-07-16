/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.hierarchicalclustering.HierarchicalClusteringAlgorithm;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.rtgroupingandsharecorrelation.RtGroupingAndShapeCorrelationAlgorithm;
import io.github.mzmine.parameters.ParameterSet;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public enum SpectralDeconvolutionAlgorithms {

  RT_GROUPING_AND_SHAPE_CORRELATION(RtGroupingAndShapeCorrelationAlgorithm.class),//
  HIERARCHICAL_CLUSTERING(HierarchicalClusteringAlgorithm.class); //

  private final SpectralDeconvolutionAlgorithm spectralDeconvolutionAlgorithm;

  SpectralDeconvolutionAlgorithms(
      final Class<? extends SpectralDeconvolutionAlgorithm> spectralDeconvolutionAlgorithmClass) {
    this.spectralDeconvolutionAlgorithm = MZmineCore.getModuleInstance(
        spectralDeconvolutionAlgorithmClass);
  }

  @NotNull
  public static List<SpectralDeconvolutionAlgorithm> listModules() {
    return Arrays.stream(values()).map(SpectralDeconvolutionAlgorithms::getDefaultModule).toList();
  }

  @NotNull
  public ParameterSet getParametersCopy() {
    return MZmineCore.getConfiguration()
        .getModuleParameters(spectralDeconvolutionAlgorithm.getClass()).cloneParameterSet();
  }

  @NotNull
  public SpectralDeconvolutionAlgorithm getDefaultModule() {
    return spectralDeconvolutionAlgorithm;
  }

  @Override
  public String toString() {
    return spectralDeconvolutionAlgorithm.getName();
  }
}
