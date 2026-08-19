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

import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.util.XMLUtils;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class FeatureListPreferencesTest {

  private static Element newElement() throws ParserConfigurationException {
    final Document document = XMLUtils.newDocument();
    final Element element = document.createElement("preferences");
    document.appendChild(element);
    return element;
  }

  @Test
  void testDefaultIsQcOnly() {
    Assertions.assertEquals(SampleTypeFilter.qc(),
        FeatureListPreferences.createDefault().getRsdSampleTypeFilter());
  }

  @Test
  void testXmlRoundTrip() throws ParserConfigurationException {
    final FeatureListPreferences preferences = new FeatureListPreferences(
        SampleTypeFilter.of(List.of(SampleType.QC, SampleType.SAMPLE)));

    final Element element = newElement();
    preferences.saveToXML(element);

    Assertions.assertEquals(preferences, FeatureListPreferences.loadFromXML(element));
  }

  @Test
  void testXmlRoundTripEmptyFilter() throws ParserConfigurationException {
    final FeatureListPreferences preferences = new FeatureListPreferences(
        SampleTypeFilter.of(List.of()));

    final Element element = newElement();
    preferences.saveToXML(element);

    final FeatureListPreferences loaded = FeatureListPreferences.loadFromXML(element);
    Assertions.assertTrue(loaded.getRsdSampleTypeFilter().isEmpty());
    Assertions.assertEquals(preferences, loaded);
  }

  @Test
  void testXmlRoundTripCustomValues() throws ParserConfigurationException {
    // group names mzmine does not know are kept, they may contain any character
    final FeatureListPreferences preferences = new FeatureListPreferences(
        SampleTypeFilter.ofValues("qc", "my group, with comma"));

    final Element element = newElement();
    preferences.saveToXML(element);

    Assertions.assertEquals(preferences, FeatureListPreferences.loadFromXML(element));
  }

  @Test
  void testXmlRoundTripOpenEndedModes() throws ParserConfigurationException {
    for (final SampleTypeFilter filter : List.of(SampleTypeFilter.all(), SampleTypeFilter.none())) {
      final FeatureListPreferences preferences = new FeatureListPreferences(filter);

      final Element element = newElement();
      preferences.saveToXML(element);

      final FeatureListPreferences loaded = FeatureListPreferences.loadFromXML(element);
      Assertions.assertEquals(filter.getMode(), loaded.getRsdSampleTypeFilter().getMode());
      Assertions.assertEquals(preferences, loaded);
    }
  }

  @Test
  void testMissingElementIsNull() throws ParserConfigurationException {
    // null signals that the feature list keeps its default preferences
    Assertions.assertNull(FeatureListPreferences.loadFromXML(null));
    // element without any attribute, e.g. from a project saved before preferences existed
    Assertions.assertNull(FeatureListPreferences.loadFromXML(newElement()));
  }
}
