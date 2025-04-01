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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.modules.dataanalysis.utils.scaling.ScalingFunctions;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.CheckComboParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.RegionsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import java.awt.geom.Point2D;
import java.util.List;

public class PCALoadingsExtractionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter();
  public static final ComboParameter<ScalingFunctions> scaling = new ComboParameter<>(
      "Scaling function", "Select the scaling function.", ScalingFunctions.values());
  public static final ComboParameter<ImputationFunctions> imputations = new ComboParameter<>(
      "Imputation function", "Select the imputation function.", ImputationFunctions.values());
  public static final IntegerParameter domainPc = new IntegerParameter("Domain PC",
      "Select the domain PC to build the plot.");
  public static final IntegerParameter rangePc = new IntegerParameter("Range PC",
      "Select the domain PC to build the plot.");
  public static final AbundanceMeasureParameter abundance = new AbundanceMeasureParameter(
      "Abundance", "Select the abundance measure.", AbundanceMeasure.values());
  public static final CheckComboParameter<SampleType> sampleTypes = new CheckComboParameter<>(
      "Sample types", "Select the sample types for the PCA.", SampleType.values());
  public static final RegionsParameter regions = new RegionsParameter();

  public PCALoadingsExtractionParameters() {
    super(flist, scaling, imputations, domainPc, rangePc, abundance, sampleTypes, regions);
  }

  public static PCALoadingsExtractionParameters fromPcaModel(PCAModel pcaModel,
      List<List<Point2D>> regions) {
    final PCALoadingsExtractionParameters param = (PCALoadingsExtractionParameters) new PCALoadingsExtractionParameters().cloneParameterSet();
    param.setParameter(flist,
        new FeatureListsSelection((ModularFeatureList) pcaModel.getFlists().getFirst()));
    param.setParameter(scaling, pcaModel.getScalingFunction());
    param.setParameter(imputations, pcaModel.getImputationFunction());
    param.setParameter(domainPc, pcaModel.getDomainPc());
    param.setParameter(rangePc, pcaModel.getRangePc());
    param.setParameter(abundance, pcaModel.getAbundance());
    param.setParameter(sampleTypes, List.copyOf(pcaModel.getSampleTypeFilter().getTypes()));
    param.setParameter(PCALoadingsExtractionParameters.regions, regions);

    return param;
  }

  public PCAModel toPcaModel() {
    PCAModel pcaModel = new PCAModel();
    pcaModel.setFlists(List.of(getValue(flist).getMatchingFeatureLists()));
    pcaModel.setScalingFunction(getValue(scaling));
    pcaModel.setImputationFunction(getValue(imputations));
    pcaModel.setDomainPc(getValue(domainPc));
    pcaModel.setRangePc(getValue(rangePc));
    pcaModel.setAbundance(getValue(abundance));
    pcaModel.setSampleTypeFilter(new SampleTypeFilter(getValue(sampleTypes)));
    return pcaModel;
  }
}
