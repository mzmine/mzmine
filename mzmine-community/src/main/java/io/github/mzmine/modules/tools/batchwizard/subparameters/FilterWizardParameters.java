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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.FilterWizardParameterFactory;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesInMetadataParameter;
import io.github.mzmine.parameters.parametertypes.MinimumSamplesParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import java.util.Map;

/**
 * Filtering steps throughout the batch mode
 */
public final class FilterWizardParameters extends WizardStepParameters {

  public static final MinimumSamplesParameter minNumberOfSamples = new MinimumSamplesParameter();

  public static final OptionalParameter<MinimumSamplesInMetadataParameter> minNumberOfSamplesInAnyGroup = new OptionalParameter<>(
      new MinimumSamplesInMetadataParameter(), false);

  public static final BooleanParameter rsdQcFilter = new BooleanParameter(
      "Remove features with RSD > 20% in QC",
      "Filters out all rows that have a relative standard deviation (RSD) > 20% in QC samples. Define QC in sample metadata or by adding _qc to the filenames.",
      false);

  public static final BooleanParameter filter13C = new BooleanParameter(
      "Only keep features with 13C",
      "Filters out all rows that have no feature with a 13C isotope pattern", false);

  public static final BooleanParameter goodPeaksOnly = new BooleanParameter(
      "Apply strict shape filtering",
      "If selected, only peaks with a top-to-edge ratio of >= 2 and high similarity to a Gaussian, or fronting/tailing Gaussian will be retained after feature detection.");

  public static final OriginalFeatureListHandlingParameter handleOriginalFeatureLists = new OriginalFeatureListHandlingParameter(
      false, OriginalFeatureListOption.REMOVE);

  public FilterWizardParameters() {
    super(WizardPart.FILTER, FilterWizardParameterFactory.Filters, handleOriginalFeatureLists,
        minNumberOfSamples, minNumberOfSamplesInAnyGroup, rsdQcFilter, filter13C, goodPeaksOnly);
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    final Map<String, Parameter<?>> map = super.getNameParameterMap();
    map.put("Min samples per aligned feature", getParameter(minNumberOfSamples));
    return map;
  }
}