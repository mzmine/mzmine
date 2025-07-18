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

package io.github.mzmine.parameters.parametertypes.statistics;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;

public class AbundanceDataTablePreparationConfigParameter extends
    ParameterSetParameter<AbundanceDataTablePreparationConfigSubParameters> {

  public AbundanceDataTablePreparationConfigParameter() {
    this("""
        These parameters are used to prepare abundance data by extraction, missing value imputation, etc.""");
  }

  public AbundanceDataTablePreparationConfigParameter(String description) {
    this("Abundance data preparation", description);
  }

  public AbundanceDataTablePreparationConfigParameter(String name, String description) {
    super(name, description, new AbundanceDataTablePreparationConfigSubParameters());
  }

  public AbundanceDataTablePreparationConfig createConfig() {
    return getEmbeddedParameters().createConfig();
  }

  public void setAll(AbundanceMeasure measure, ImputationFunctions imputationFunction) {
    getEmbeddedParameters().setAll(measure, imputationFunction);
  }

  @Override
  public AbundanceDataTablePreparationConfigParameter cloneParameter() {
    final AbundanceDataTablePreparationConfigSubParameters embeddedParametersClone = (AbundanceDataTablePreparationConfigSubParameters) getEmbeddedParameters().cloneParameterSet();
    final AbundanceDataTablePreparationConfigParameter copy = new AbundanceDataTablePreparationConfigParameter(
        getName(), getDescription());
    copy.setValue(embeddedParametersClone);
    return copy;
  }

}
