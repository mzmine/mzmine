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
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureSelectionParameter;
import java.util.List;

public class StandardCompoundNormalizationTypeParameters extends SimpleParameterSet {

  public static final CheckComboParameter<SampleType> sampleTypes = new CheckComboParameter<>(
      "Reference samples", """
      Select all sample types that shall be used to calculate the recalibration from.
      The recalibration of all other samples will be based on the acquisition order, which is
      determined by the acquisition type column in the metadata (CTRL/CMD + M).
      """, SampleType.values(), List.of(SampleType.values()));

  public static final ComboParameter<StandardUsageType> standardUsageType = new ComboParameter<>(
      "Normalization type", "Normalize intensities using", StandardUsageType.values());

  public static final DoubleParameter mzVsRtBalance = new DoubleParameter("m/z vs RT balance",
      "Used in distance measuring as multiplier of m/z difference");

  public static final FeatureSelectionParameter standardCompounds = new FeatureSelectionParameter(
      "Standard compounds", "List of features for choosing the normalization standards", null);

  public StandardCompoundNormalizationTypeParameters() {
    super(sampleTypes, standardUsageType, mzVsRtBalance, standardCompounds);
  }
}

