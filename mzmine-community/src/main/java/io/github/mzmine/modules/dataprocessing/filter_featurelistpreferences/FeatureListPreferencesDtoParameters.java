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
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.metadata.SampleTypeFilterParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Parameters used for saving and loading {@link FeatureListPreferences}. Compared to
 * {@link FeatureListPreferencesParameters} this class contains all parameters that should be saved
 * to xml. Even those that may not be set via the preferences' module.
 */
public class FeatureListPreferencesDtoParameters extends SimpleParameterSet {

  public static final SampleTypeFilterParameter rsdSampleTypes = FeatureListPreferencesParameters.rsdSampleTypes.cloneParameter();

  public FeatureListPreferencesDtoParameters() {
    super(rsdSampleTypes);
  }

  public static FeatureListPreferencesDtoParameters loadFromXML(@Nullable Element element) {
    final ParameterSet params = new FeatureListPreferencesDtoParameters().cloneParameterSet();
    params.loadValuesFromXML(element);
    return (FeatureListPreferencesDtoParameters) params;
  }

  public @NotNull FeatureListPreferences toPreferences() {
    return new FeatureListPreferences(getValue(rsdSampleTypes));
  }

  public static @NotNull FeatureListPreferencesDtoParameters fromPreferences(
      @NotNull final FeatureListPreferences preferences) {
    final FeatureListPreferencesDtoParameters param = (FeatureListPreferencesDtoParameters) new FeatureListPreferencesDtoParameters().cloneParameterSet();
    param.setParameter(rsdSampleTypes, preferences.getRsdSampleTypeFilter());
    return param;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
