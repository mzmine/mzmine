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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.FilterWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;

/**
 * Filtering steps throughout the batch mode
 */
public final class FilterWizardParameters extends WizardStepParameters {

  public static final MinimumSamplesParameter minNumberOfSamples = new MinimumSamplesParameter(
      "Min samples per aligned feature",
      "The minimum number of samples in which a feature needs to be detected, e.g., 2-3 for triplicates.\n"
      + "Used in feature list rows filter and feature grouping.");


  public static final BooleanParameter filter13C = new BooleanParameter(
      "Only keep features with 13C",
      "Filters out all rows that have no feature with a 13C isotope pattern", false);

  public static final OriginalFeatureListHandlingParameter handleOriginalFeatureLists = new OriginalFeatureListHandlingParameter(
      false, OriginalFeatureListOption.REMOVE);


  public FilterWizardParameters() {
    super(WizardPart.FILTER, FilterWizardParameterFactory.Filters, handleOriginalFeatureLists,
        minNumberOfSamples, filter13C);
  }

}
