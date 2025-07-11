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
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;

public class AbundanceDataTablePreparationParameters extends SimpleParameterSet {

  public static final AbundanceMeasureParameter abundanceMeasure = new AbundanceMeasureParameter(
      "Abundance measure",
      "Select the abundance measure (height or area) to use for any test that uses abundance.",
      AbundanceMeasure.values(), AbundanceMeasure.Area);


  public static final ComboParameter<ImputationFunctions> missingValueImputation = new ComboParameter<>(
      "Missing value imputation",
      "Missing values will be changed for other small values like an estimated LOD (portion of the smalles value across the dataset) or feature-wise lowest values.",
      ImputationFunctions.values(), ImputationFunctions.GLOBAL_LIMIT_OF_DETECTION);

  public AbundanceDataTablePreparationParameters() {
    super(abundanceMeasure, missingValueImputation);
  }

  public AbundanceDataTablePreparationConfig createConfig() {
    return new AbundanceDataTablePreparationConfig(getValue(abundanceMeasure),
        getValue(missingValueImputation));
  }

  public void setAll(AbundanceMeasure measure, ImputationFunctions imputationFunction) {
    setParameter(abundanceMeasure, measure);
    setParameter(missingValueImputation, imputationFunction);
  }
}
