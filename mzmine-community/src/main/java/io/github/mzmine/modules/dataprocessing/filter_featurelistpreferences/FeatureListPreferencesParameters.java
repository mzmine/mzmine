/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences;

import io.github.mzmine.datamodel.features.preferences.FeatureListPreferences;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.metadata.SampleTypeFilterParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import org.jetbrains.annotations.NotNull;

public class FeatureListPreferencesParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final SampleTypeFilterParameter rsdSampleTypes = new SampleTypeFilterParameter(
      "Samples for RSD columns", """
      Select all sample types (in %s metadata column) that are used to calculate the relative standard deviation (RSD)
      columns, e.g., the area RSD. The sample type is defined by the sample type column in the
      metadata (CTRL/CMD + M).""".formatted(MetadataColumn.SAMPLE_TYPE_HEADER),
      SampleTypeFilter.qc(), true);

  public FeatureListPreferencesParameters() {
    super(flists, rsdSampleTypes);
  }

  public @NotNull FeatureListPreferences toPreferences() {
    return new FeatureListPreferences(getValue(rsdSampleTypes));
  }

  public static @NotNull FeatureListPreferencesParameters fromPreferences(
      @NotNull final FeatureListPreferences preferences) {
    final FeatureListPreferencesParameters param = (FeatureListPreferencesParameters) new FeatureListPreferencesParameters().cloneParameterSet();
    param.setParameter(rsdSampleTypes, preferences.getRsdSampleTypeFilter());
    return param;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
