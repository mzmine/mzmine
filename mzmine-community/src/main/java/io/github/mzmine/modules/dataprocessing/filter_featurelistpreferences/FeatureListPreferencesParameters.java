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
import io.github.mzmine.datamodel.identities.iontype.IonTypeRanking;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomOption;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomValue;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Every preference is a {@link DefaultOffCustomParameter} without the OFF option and starts on
 * {@link DefaultOffCustomOption#DEFAULT}. A preference always has to have a value, so OFF makes no
 * sense, and DEFAULT lets the mzmine default change without every project pinning the old value.
 * The custom input starts on the same default so switching to CUSTOM shows the value that was in
 * effect.
 */
public class FeatureListPreferencesParameters extends SimpleParameterSet {

  private static final @NotNull FeatureListPreferences DEFAULTS = FeatureListPreferences.createDefault();

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final DefaultOffCustomParameter<SampleTypeFilter> rsdSampleTypes = new DefaultOffCustomParameter<>(
      FeatureListPreferencesDtoParameters.rsdSampleTypes.cloneParameter(),
      DEFAULTS.getRsdSampleTypeFilter(), null, false);

  public static final DefaultOffCustomParameter<IonTypeRanking> ionTypeRanking = new DefaultOffCustomParameter<>(
      FeatureListPreferencesDtoParameters.ionTypeRanking.cloneParameter(),
      DEFAULTS.getIonTypeRanking(), null, false);

  public FeatureListPreferencesParameters() {
    super(flists, rsdSampleTypes, ionTypeRanking);
  }

  /**
   * Resolves every parameter, so the mzmine default is used where
   * {@link DefaultOffCustomOption#DEFAULT} is selected and the typed value where
   * {@link DefaultOffCustomOption#CUSTOM} is.
   */
  public @NotNull FeatureListPreferences toPreferences() {
    return new FeatureListPreferences(
        Objects.requireNonNullElse(getParameter(rsdSampleTypes).resolveValue(),
            DEFAULTS.getRsdSampleTypeFilter()),
        Objects.requireNonNullElse(getParameter(ionTypeRanking).resolveValue(),
            DEFAULTS.getIonTypeRanking()));
  }

  public static @NotNull FeatureListPreferencesParameters fromPreferences(
      @NotNull final FeatureListPreferences preferences) {
    final FeatureListPreferencesParameters param = (FeatureListPreferencesParameters) new FeatureListPreferencesParameters().cloneParameterSet();
    param.setParameter(rsdSampleTypes,
        defaultOrCustom(rsdSampleTypes, preferences.getRsdSampleTypeFilter()));
    param.setParameter(ionTypeRanking,
        defaultOrCustom(ionTypeRanking, preferences.getIonTypeRanking()));
    return param;
  }

  /**
   * @return DEFAULT if the value still is the mzmine default, otherwise CUSTOM with that value. The
   * custom value is always set so the input field shows the value that is in effect.
   */
  private static <V> @NotNull DefaultOffCustomValue<V> defaultOrCustom(
      @NotNull final DefaultOffCustomParameter<V> parameter, @NotNull final V value) {
    final DefaultOffCustomOption option =
        Objects.equals(value, parameter.getDefaultValue()) ? DefaultOffCustomOption.DEFAULT
            : DefaultOffCustomOption.CUSTOM;
    return new DefaultOffCustomValue<>(option, value);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
