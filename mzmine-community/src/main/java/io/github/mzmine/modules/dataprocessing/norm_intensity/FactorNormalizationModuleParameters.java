/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.CheckComboParameter;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class FactorNormalizationModuleParameters extends SimpleParameterSet {

  public static final CheckComboParameter<SampleType> sampleTypes = new CheckComboParameter<>(
      "Reference samples", """
      Select all sample types that shall be used to calculate the recalibration from.
      The recalibration of all other samples will be based on the acquisition order, which is
      determined by the acquisition type column in the metadata (CTRL/CMD + M).
      """, SampleType.values(), List.of(SampleType.QC));

  public FactorNormalizationModuleParameters() {
    super(sampleTypes);
  }

  public static @NotNull FactorNormalizationModuleParameters create(
      final @NotNull List<SampleType> selectedSampleTypes) {
    final FactorNormalizationModuleParameters parameters = (FactorNormalizationModuleParameters) new FactorNormalizationModuleParameters().cloneParameterSet();
    parameters.setParameter(FactorNormalizationModuleParameters.sampleTypes, selectedSampleTypes);
    return parameters;
  }
}
