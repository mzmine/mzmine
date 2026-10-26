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
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomOption;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomValue;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import org.jetbrains.annotations.NotNull;

/**
 * Every preference is a {@link DefaultOffCustomParameter} without the OFF option and starts on
 * {@link DefaultOffCustomOption#KEEP_AS_IS}. A preference always has to have a value, so OFF option
 * is removed, while KEEP_AS_IS leaves the preference of the feature list untouched and DEFAULT lets
 * the mzmine default change without every project pinning the old value. The custom input starts on
 * the mzmine default so switching to CUSTOM shows a sensible value.
 * <p>
 * The setup dialog always starts over on KEEP_AS_IS, no matter which values were used the last
 * time, so that running the module never redefines a preference the user did not touch. Only the
 * parameters of a batch step keep the selection of the user, see
 * {@link #setAsBatchStepParameters()}.
 */
public class FeatureListPreferencesParameters extends SimpleParameterSet {

  private static final @NotNull FeatureListPreferences DEFAULTS = FeatureListPreferences.createDefault();

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final DefaultOffCustomParameter<SampleTypeFilter> rsdSampleTypes = new DefaultOffCustomParameter<>(
      FeatureListPreferencesDtoParameters.rsdSampleTypes.cloneParameter(),
      DEFAULTS.getRsdSampleTypeFilter(), null, false, true);

  public static final DefaultOffCustomParameter<IonTypeRanking> ionTypeRanking = new DefaultOffCustomParameter<>(
      FeatureListPreferencesDtoParameters.ionTypeRanking.cloneParameter(),
      DEFAULTS.getIonTypeRanking(), null, false, true);

  public static final DefaultOffCustomParameter<List<String>> tagLabels = new DefaultOffCustomParameter<>(
      FeatureListPreferencesDtoParameters.tagLabels.cloneParameter(), DEFAULTS.getTagLabels(), null,
      false, true);

  public FeatureListPreferencesParameters() {
    super(flists, rsdSampleTypes, ionTypeRanking, tagLabels);
  }

  @Override
  public ExitCode showSetupDialog(final boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if (!isBatchStepParameters()) {
      // opened from the menu, the quick search, the feature list context menu, the summary, or
      // while a batch step is added. Redefine nothing until the user picks a preference
      setAllKeepAsIs();
    }

    final ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this,
        getMessage());
    addKeepAllAsIsButton(dialog);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  /**
   * Adds a button that sets every preference of the dialog back to
   * {@link DefaultOffCustomOption#KEEP_AS_IS}, so that the module redefines nothing.
   */
  public void addKeepAllAsIsButton(@NotNull final ParameterSetupDialog dialog) {
    final Button button = FxButtons.createButton("Keep all as is",
        "Sets every preference to \"%s\", so that the feature list keeps the values it has".formatted(
            DefaultOffCustomOption.KEEP_AS_IS), () -> {
          // take over what the user typed so the custom inputs still show those values
          dialog.updateParameterSetFromComponents();
          setAllKeepAsIs();
          dialog.setParameterValuesToComponents();
        });
    // same button data as OK and presets, so the button bar places it right next presets
    ButtonBar.setButtonData(button, ButtonData.OK_DONE);
    dialog.getButtonBar().getButtons().add(button);
  }

  /**
   * Sets every preference to {@link DefaultOffCustomOption#KEEP_AS_IS}. The custom inputs are kept,
   * so switching one to CUSTOM starts on the value that was shown.
   */
  public void setAllKeepAsIs() {
    for (final Parameter<?> parameter : getParameters()) {
      if (parameter instanceof DefaultOffCustomParameter<?> preference) {
        setKeepAsIs(preference);
      }
    }
  }

  private static <V> void setKeepAsIs(@NotNull final DefaultOffCustomParameter<V> parameter) {
    final DefaultOffCustomValue<V> current = parameter.getValue();
    parameter.setValue(new DefaultOffCustomValue<>(DefaultOffCustomOption.KEEP_AS_IS,
        current == null ? null : current.custom()));
  }

  /**
   * A parameter set that redefines nothing. The custom inputs are preloaded with {@code current},
   * so switching a preference to CUSTOM starts on the value that is in effect for the feature
   * list.
   *
   * @param current the preferences of the feature list this parameter set is opened for
   */
  public static @NotNull FeatureListPreferencesParameters keepAllAsIs(
      @NotNull final FeatureListPreferences current) {
    final FeatureListPreferencesParameters param = (FeatureListPreferencesParameters) new FeatureListPreferencesParameters().cloneParameterSet();
    param.setParameter(rsdSampleTypes,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.KEEP_AS_IS,
            current.getRsdSampleTypeFilter()));
    param.setParameter(ionTypeRanking,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.KEEP_AS_IS,
            current.getIonTypeRanking()));
    param.setParameter(tagLabels,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.KEEP_AS_IS, current.getTagLabels()));
    return param;
  }

  /**
   * @param currentValue the value that is in effect for the target feature list, used for
   *                     {@link DefaultOffCustomOption#KEEP_AS_IS}
   * @param defaultValue fallback, a preference may never be null
   */
  private static <V> @NotNull V resolve(@NotNull final DefaultOffCustomParameter<V> parameter,
      @NotNull final V currentValue, @NotNull final V defaultValue) {
    if (parameter.isKeepAsIs()) {
      return currentValue;
    }
    return Objects.requireNonNullElse(parameter.resolveValue(), defaultValue);
  }

  /**
   * Resolves every parameter, so the mzmine default is used where
   * {@link DefaultOffCustomOption#DEFAULT} is selected, the typed value where
   * {@link DefaultOffCustomOption#CUSTOM} is, and the value of {@code current} where
   * {@link DefaultOffCustomOption#KEEP_AS_IS} is.
   *
   * @param current the preferences that are currently in effect for the target feature list
   */
  public @NotNull FeatureListPreferences toPreferences(
      @NotNull final FeatureListPreferences current) {
    return new FeatureListPreferences(
        resolve(getParameter(rsdSampleTypes), current.getRsdSampleTypeFilter(),
            DEFAULTS.getRsdSampleTypeFilter()),
        resolve(getParameter(ionTypeRanking), current.getIonTypeRanking(),
            DEFAULTS.getIonTypeRanking()),
        resolve(getParameter(tagLabels), current.getTagLabels(), DEFAULTS.getTagLabels()));
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
