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

import io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences.FeatureListPreferencesDtoParameters;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.util.XMLUtils;
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
    final FeatureListPreferences preferences = new FeatureListPreferences(filter);

    final FeatureListPreferences loaded = saveAndLoad(preferences);

    Assertions.assertEquals(preferences, loaded);
    // mode and values are checked separately, equals may only compare one of them
    Assertions.assertEquals(filter.getMode(), loaded.getRsdSampleTypeFilter().getMode());
    Assertions.assertEquals(filter.getValues(), loaded.getRsdSampleTypeFilter().getValues());
  }

  @Test
  void testSavedXmlIsParameterSetShape() throws ParserConfigurationException {
    final Element element = newPreferencesElement();
    new FeatureListPreferences(SampleTypeFilter.qc()).saveToXML(element);

    final NodeList parameters = element.getElementsByTagName(SimpleParameterSet.parameterElement);
    Assertions.assertEquals(1, parameters.getLength());
    Assertions.assertEquals(FeatureListPreferencesDtoParameters.rsdSampleTypes.getName(),
        ((Element) parameters.item(0)).getAttribute(SimpleParameterSet.nameAttribute));
  }

  @Test
  void testSaveDoesNotModifySharedParameter() throws ParserConfigurationException {
    // the dto parameter set holds a static parameter, saving must work on clones only
    final SampleTypeFilter before = FeatureListPreferencesDtoParameters.rsdSampleTypes.getValue();

    saveAndLoad(new FeatureListPreferences(SampleTypeFilter.ofValues("some other group")));

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

  @Test
  void testEmptyElementFallsBackToDefaults() throws ParserConfigurationException {
    // parameters that are missing in the xml, e.g. added after the project was saved, keep the
    // default value instead of failing the load
    Assertions.assertEquals(FeatureListPreferences.createDefault(),
        FeatureListPreferences.loadFromXML(newPreferencesElement()));
  }
}
