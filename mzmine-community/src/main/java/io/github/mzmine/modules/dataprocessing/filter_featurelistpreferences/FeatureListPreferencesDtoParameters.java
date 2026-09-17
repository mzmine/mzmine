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
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomParameter;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonTypeRankingParameter;
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

  /**
   * decision: the plain parameters live here and not in {@link FeatureListPreferencesParameters},
   * which wraps clones of them in a {@link DefaultOffCustomParameter}. This class is the
   * persistence shape and always stores the resolved value.
   */
  public static final SampleTypeFilterParameter rsdSampleTypes = new SampleTypeFilterParameter(
      "Samples for RSD columns", """
      Select all sample types (in %s metadata column) that are used to calculate the relative standard deviation (RSD)
      columns, e.g., the area RSD. The sample type is defined by the sample type column in the
      metadata (CTRL/CMD + M).""".formatted(MetadataColumn.SAMPLE_TYPE_HEADER),
      SampleTypeFilter.qc(), true);

  public static final IonTypeRankingParameter ionTypeRanking = new IonTypeRankingParameter();

  public FeatureListPreferencesDtoParameters() {
    super(rsdSampleTypes, ionTypeRanking);
  }

  @Nullable
  public static FeatureListPreferencesDtoParameters loadFromXML(@Nullable Element element) {
    if (element == null || !element.getTagName().equals(CONST.XML_FLIST_PREFERENCES_ELEMENT)) {
      return null;
    }

    // the default so that all parameters that are not loaded are set to the actual default
    // like new parameters added later
    final FeatureListPreferencesDtoParameters params = FeatureListPreferencesDtoParameters.fromPreferences(
        FeatureListPreferences.createDefault());
    params.loadValuesFromXML(element);
    return params;
  }

  public @NotNull FeatureListPreferences toPreferences() {
    return new FeatureListPreferences(getValue(rsdSampleTypes), getValue(ionTypeRanking));
  }

  public static @NotNull FeatureListPreferencesDtoParameters fromPreferences(
      @NotNull final FeatureListPreferences preferences) {
    final FeatureListPreferencesDtoParameters param = (FeatureListPreferencesDtoParameters) new FeatureListPreferencesDtoParameters().cloneParameterSet();
    param.setParameter(rsdSampleTypes, preferences.getRsdSampleTypeFilter());
    param.setParameter(ionTypeRanking, preferences.getIonTypeRanking());
    return param;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
