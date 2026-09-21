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

package io.github.mzmine.datamodel.features.preferences;

import io.github.mzmine.datamodel.identities.iontype.IonPartFrequency;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonTypeRanking;
import io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences.FeatureListPreferencesDtoParameters;
import io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences.FeatureListPreferencesModule;
import io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences.FeatureListPreferencesParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomOption;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomValue;
import io.github.mzmine.util.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * {@link FeatureListPreferences} is saved and loaded through
 * {@link FeatureListPreferencesDtoParameters}, therefore the xml is a regular parameter set inside
 * the {@link CONST#XML_FLIST_PREFERENCES_ELEMENT} element written by the project save task.
 */
class FeatureListPreferencesTest {

  /**
   * @return an element with the tag name used by the project save task
   */
  private static Element newPreferencesElement() throws ParserConfigurationException {
    return newElement(CONST.XML_FLIST_PREFERENCES_ELEMENT);
  }

  private static Element newElement(final String tagName) throws ParserConfigurationException {
    final Document document = XMLUtils.newDocument();
    final Element element = document.createElement(tagName);
    document.appendChild(element);
    return element;
  }

  /**
   * Saves and loads through the parameter set, like the project save and load tasks do.
   */
  private static FeatureListPreferences saveAndLoad(final FeatureListPreferences preferences)
      throws ParserConfigurationException {
    final Element element = newPreferencesElement();
    preferences.saveToXML(element);
    return FeatureListPreferences.loadFromXML(element);
  }

  private static List<Arguments> filters() {
    return List.of(Arguments.of("default qc", SampleTypeFilter.qc()), Arguments.of("multiple types",
            SampleTypeFilter.of(List.of(SampleType.QC, SampleType.SAMPLE))),
        // an empty list is not the same as the none mode, both must survive the round trip
        Arguments.of("empty list", SampleTypeFilter.of(List.of())),
        Arguments.of("all mode", SampleTypeFilter.all()),
        Arguments.of("none mode", SampleTypeFilter.none()),
        // group names mzmine does not know are kept, they may contain any character
        Arguments.of("custom group names",
            SampleTypeFilter.ofValues("qc", "my group, with comma")));
  }

  @Test
  void testDefaultIsQcOnly() {
    Assertions.assertEquals(SampleTypeFilter.qc(),
        FeatureListPreferences.createDefault().getRsdSampleTypeFilter());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("filters")
  void testXmlRoundTrip(final String name, final SampleTypeFilter filter)
      throws ParserConfigurationException {
    final FeatureListPreferences preferences = new FeatureListPreferences(filter,
        IonTypeRanking.createDefault());

    final FeatureListPreferences loaded = saveAndLoad(preferences);

    Assertions.assertEquals(preferences, loaded);
    // mode and values are checked separately, equals may only compare one of them
    Assertions.assertEquals(filter.getMode(), loaded.getRsdSampleTypeFilter().getMode());
    Assertions.assertEquals(filter.getValues(), loaded.getRsdSampleTypeFilter().getValues());
  }

  @Test
  void testSavedXmlIsParameterSetShape() throws ParserConfigurationException {
    final Element element = newPreferencesElement();
    new FeatureListPreferences(SampleTypeFilter.qc(), IonTypeRanking.createDefault()).saveToXML(
        element);

    final NodeList parameters = element.getElementsByTagName(SimpleParameterSet.parameterElement);
    final List<String> names = new ArrayList<>(parameters.getLength());
    for (int i = 0; i < parameters.getLength(); i++) {
      names.add(((Element) parameters.item(i)).getAttribute(SimpleParameterSet.nameAttribute));
    }
    Assertions.assertEquals(List.of(FeatureListPreferencesDtoParameters.rsdSampleTypes.getName(),
        FeatureListPreferencesDtoParameters.ionTypeRanking.getName()), names);
  }

  @Test
  void testIonTypeRankingRoundTrip() throws ParserConfigurationException {
    // a user defined ranking that differs from the default, with both polarities and both count
    // directions in the one list
    final IonTypeRanking ranking = new IonTypeRanking(
        List.of(IonPartFrequency.of(IonParts.H, 0.9f), IonPartFrequency.of(IonParts.NA, 0.4f),
            IonPartFrequency.of(IonParts.H_MINUS, 0.8f), IonPartFrequency.of(IonParts.CL, 0.25f)));

    final FeatureListPreferences loaded = saveAndLoad(
        new FeatureListPreferences(SampleTypeFilter.qc(), ranking));

    Assertions.assertEquals(ranking, loaded.getIonTypeRanking());
  }

  @Test
  void testDefaultRankingIsUsedWhenNotSaved() throws ParserConfigurationException {
    // save regularly, then strip the ranking parameter to simulate a project saved before the
    // ranking was introduced
    final Element element = newPreferencesElement();
    new FeatureListPreferences(SampleTypeFilter.all(), IonTypeRanking.createDefault()).saveToXML(
        element);
    final NodeList parameters = element.getElementsByTagName(SimpleParameterSet.parameterElement);
    for (int i = parameters.getLength() - 1; i >= 0; i--) {
      final Element parameter = (Element) parameters.item(i);
      if (FeatureListPreferencesDtoParameters.ionTypeRanking.getName()
          .equals(parameter.getAttribute(SimpleParameterSet.nameAttribute))) {
        parameter.getParentNode().removeChild(parameter);
      }
    }

    final FeatureListPreferences loaded = FeatureListPreferences.loadFromXML(element);
    Assertions.assertNotNull(loaded);
    Assertions.assertEquals(IonTypeRanking.createDefault(), loaded.getIonTypeRanking());
  }

  @Test
  void testSaveDoesNotModifySharedParameter() throws ParserConfigurationException {
    // the dto parameter set holds a static parameter, saving must work on clones only
    final SampleTypeFilter before = FeatureListPreferencesDtoParameters.rsdSampleTypes.getValue();

    saveAndLoad(new FeatureListPreferences(SampleTypeFilter.ofValues("some other group"),
        IonTypeRanking.createDefault()));

    Assertions.assertEquals(before, FeatureListPreferencesDtoParameters.rsdSampleTypes.getValue());
  }

  @Test
  void testMissingElementIsNull() {
    // null signals that the feature list keeps its default preferences
    Assertions.assertNull(FeatureListPreferences.loadFromXML(null));
  }

  @Test
  void testWrongElementIsNull() throws ParserConfigurationException {
    // guards against loading a different element of the feature list xml
    Assertions.assertNull(FeatureListPreferences.loadFromXML(newElement("something_else")));
  }

  /**
   * The module parameters wrap every preference in a {@link DefaultOffCustomParameter}. A set that
   * is opened for a feature list redefines nothing, but preloads the custom inputs with the
   * preferences that are in effect.
   */
  @Test
  void testKeepAllAsIsStartsOnKeepAsIsWithCurrentInputs() {
    final FeatureListPreferences current = new FeatureListPreferences(
        SampleTypeFilter.ofValues("some other group"),
        new IonTypeRanking(List.of(IonPartFrequency.of(IonParts.NA, 1f))));

    final FeatureListPreferencesParameters param = FeatureListPreferencesParameters.keepAllAsIs(
        current);

    for (Parameter<?> parameter : param.getParameters()) {
      checkSelectedOption(parameter, DefaultOffCustomOption.KEEP_AS_IS);
    }
    // the custom inputs show what is in effect, so switching to CUSTOM starts on that value
    Assertions.assertEquals(current.getRsdSampleTypeFilter(),
        param.getParameter(FeatureListPreferencesParameters.rsdSampleTypes).getValue().custom());
    Assertions.assertEquals(current.getIonTypeRanking(),
        param.getParameter(FeatureListPreferencesParameters.ionTypeRanking).getValue().custom());
  }

  /**
   * Only the parameters of a batch step keep the selection of the user, every other entry point
   * starts over on KEEP_AS_IS when the setup dialog is opened.
   */
  @Test
  void testOnlyBatchStepParametersKeepTheSelection() {
    final FeatureListPreferencesParameters param = (FeatureListPreferencesParameters) new FeatureListPreferencesParameters().cloneParameterSet();
    Assertions.assertFalse(param.isBatchStepParameters());

    new MZmineProcessingStepImpl<>(new FeatureListPreferencesModule(), param);
    Assertions.assertTrue(param.isBatchStepParameters());

    // a clone is a scratch copy again, like the one a new batch step is configured with
    Assertions.assertFalse(
        ((FeatureListPreferencesParameters) param.cloneParameterSet()).isBatchStepParameters());
  }

  /**
   * Starting over keeps the custom inputs, so switching back to CUSTOM shows the value again.
   */
  @Test
  void testSetAllKeepAsIsKeepsCustomInputs() {
    final SampleTypeFilter filter = SampleTypeFilter.ofValues("some other group");
    final FeatureListPreferencesParameters param = (FeatureListPreferencesParameters) new FeatureListPreferencesParameters().cloneParameterSet();
    param.setParameter(FeatureListPreferencesParameters.rsdSampleTypes,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.CUSTOM, filter));

    param.setAllKeepAsIs();

    final var value = param.getParameter(FeatureListPreferencesParameters.rsdSampleTypes)
        .getValue();
    Assertions.assertEquals(DefaultOffCustomOption.KEEP_AS_IS, value.getSelectedOption());
    Assertions.assertEquals(filter, value.custom());
  }

  private static void checkSelectedOption(Parameter<?> parameter, DefaultOffCustomOption expected) {
    switch (parameter) {
      case DefaultOffCustomParameter p ->
          Assertions.assertEquals(expected, p.getValue().getSelectedOption());
      default -> {
      }
    }
  }

  /**
   * CUSTOM applies the typed value, no matter which preferences the feature list has.
   */
  @Test
  void testCustomValuesAreApplied() {
    final FeatureListPreferences preferences = new FeatureListPreferences(
        SampleTypeFilter.ofValues("some other group"),
        new IonTypeRanking(List.of(IonPartFrequency.of(IonParts.NA, 1f))));

    final FeatureListPreferencesParameters param = (FeatureListPreferencesParameters) new FeatureListPreferencesParameters().cloneParameterSet();
    param.setParameter(FeatureListPreferencesParameters.rsdSampleTypes,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.CUSTOM,
            preferences.getRsdSampleTypeFilter()));
    param.setParameter(FeatureListPreferencesParameters.ionTypeRanking,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.CUSTOM,
            preferences.getIonTypeRanking()));

    Assertions.assertEquals(preferences,
        param.toPreferences(FeatureListPreferences.createDefault()));
  }

  /**
   * DEFAULT applies the mzmine default, so a later change of that default is picked up instead of
   * being pinned to the value the feature list has.
   */
  @Test
  void testDefaultAppliesTheMzmineDefault() {
    final FeatureListPreferences current = new FeatureListPreferences(
        SampleTypeFilter.ofValues("some other group"),
        new IonTypeRanking(List.of(IonPartFrequency.of(IonParts.NA, 1f))));

    final FeatureListPreferencesParameters param = (FeatureListPreferencesParameters) new FeatureListPreferencesParameters().cloneParameterSet();
    param.setParameter(FeatureListPreferencesParameters.rsdSampleTypes,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.DEFAULT, null));
    param.setParameter(FeatureListPreferencesParameters.ionTypeRanking,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.DEFAULT, null));

    Assertions.assertEquals(FeatureListPreferences.createDefault(), param.toPreferences(current));
  }

  /**
   * KEEP_AS_IS defines no value, the preferences of the feature list have to survive the module.
   */
  @Test
  void testKeepAsIsResolvesToCurrentPreferences() {
    final FeatureListPreferences current = new FeatureListPreferences(
        SampleTypeFilter.ofValues("some other group"),
        new IonTypeRanking(List.of(IonPartFrequency.of(IonParts.NA, 1f))));

    final FeatureListPreferencesParameters param = (FeatureListPreferencesParameters) new FeatureListPreferencesParameters().cloneParameterSet();

    Assertions.assertEquals(current, param.toPreferences(current));
  }

  /**
   * Only the parameters on KEEP_AS_IS are taken from the feature list, the others are applied.
   */
  @Test
  void testKeepAsIsIsResolvedPerParameter() {
    final SampleTypeFilter currentFilter = SampleTypeFilter.ofValues("some other group");
    final FeatureListPreferences current = new FeatureListPreferences(currentFilter,
        new IonTypeRanking(List.of(IonPartFrequency.of(IonParts.NA, 1f))));

    final FeatureListPreferencesParameters param = (FeatureListPreferencesParameters) new FeatureListPreferencesParameters().cloneParameterSet();
    // the ranking is redefined, the sample types stay on KEEP_AS_IS
    final IonTypeRanking newRanking = new IonTypeRanking(
        List.of(IonPartFrequency.of(IonParts.H, 1f)));
    param.setParameter(FeatureListPreferencesParameters.ionTypeRanking,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.CUSTOM, newRanking));

    final FeatureListPreferences resolved = param.toPreferences(current);

    Assertions.assertEquals(currentFilter, resolved.getRsdSampleTypeFilter());
    Assertions.assertEquals(newRanking, resolved.getIonTypeRanking());
  }

  @Test
  void testEmptyElementFallsBackToDefaults() throws ParserConfigurationException {
    // parameters that are missing in the xml, e.g. added after the project was saved, keep the
    // default value instead of failing the load
    Assertions.assertEquals(FeatureListPreferences.createDefault(),
        FeatureListPreferences.loadFromXML(newPreferencesElement()));
  }
}
