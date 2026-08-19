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

package io.github.mzmine.parameters.parametertypes.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter.Mode;
import io.github.mzmine.util.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Proves that batch files written by the previous {@code CheckComboParameter<SampleType>} still
 * load into the new parameter with identical filtering behaviour, and that the new all/none modes
 * survive a save/load round trip.
 */
class SampleTypeFilterParameterTest {

  private static SampleTypeFilterParameter newParameter() {
    return new SampleTypeFilterParameter("Sample types", "description", SampleTypeFilter.all());
  }

  /**
   * Builds the exact XML shape the old {@code CheckComboParameter} produced: only
   * {@code <selected>} children, no mode attribute.
   */
  private static Element legacyElement(Document document, String... selected) {
    final Element root = document.createElement("parameter");
    root.setAttribute("name", "Sample types");
    document.appendChild(root);
    for (String value : selected) {
      final Element item = document.createElement("selected");
      item.setTextContent(value);
      root.appendChild(item);
    }
    return root;
  }

  @Test
  void loadsLegacyXmlWithoutModeAttributeAsExplicitList() throws Exception {
    final Document document = XMLUtils.newDocument();
    // this is what a batch saved before the free text sample types looks like
    final Element root = legacyElement(document, "blank", "sample", "qc", "calibration");

    final SampleTypeFilterParameter parameter = newParameter();
    parameter.loadValueFromXML(root);

    final SampleTypeFilter value = parameter.getValue();
    assertEquals(Mode.LIST, value.getMode());
    assertEquals(Set.of("blank", "sample", "qc", "calibration"), value.getValues());
  }

  @Test
  void legacyXmlFiltersExactlyAsBefore() throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element root = legacyElement(document, "qc");

    final SampleTypeFilterParameter parameter = newParameter();
    parameter.loadValueFromXML(root);

    final SampleTypeFilter filter = parameter.getValue();
    assertTrue(filter.matchesValue("qc"));
    // case insensitivity is the one intended behaviour change, everything else must match as before
    assertTrue(filter.matchesValue("QC"));
    assertFalse(filter.matchesValue("blank"));
    assertFalse(filter.matchesValue("sample"));
    assertFalse(filter.matchesValue(null));
  }

  @Test
  void legacyAllTypesSelectionStaysAListAndDoesNotBecomeAll() throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element root = legacyElement(document,
        SampleType.allValueStrings().toArray(String[]::new));

    final SampleTypeFilterParameter parameter = newParameter();
    parameter.loadValueFromXML(root);

    // an old batch listed the types it knew - it must keep meaning exactly those, not "everything",
    // otherwise loading it would silently widen the filter
    assertEquals(Mode.LIST, parameter.getValue().getMode());
    assertFalse(parameter.getValue().matchesValue("a custom group"));
  }

  @Test
  void keepsCustomValuesThatAreNotPredefinedTypes() throws Exception {
    final Document document = XMLUtils.newDocument();
    // the old parameter dropped anything that was not an enum constant and only logged a warning
    final Element root = legacyElement(document, "qc", "pooled_qc", "my custom group");

    final SampleTypeFilterParameter parameter = newParameter();
    parameter.loadValueFromXML(root);

    assertEquals(Set.of("qc", "pooled_qc", "my custom group"), parameter.getValue().getValues());
    assertTrue(parameter.getValue().matchesValue("Pooled_QC"));
  }

  @Test
  void roundTripsListOfValues() throws Exception {
    final SampleTypeFilterParameter saved = newParameter();
    saved.setValue(SampleTypeFilter.ofValues("qc", "media_blank"));

    assertEquals(SampleTypeFilter.ofValues("qc", "media_blank"), saveAndLoad(saved).getValue());
  }

  @Test
  void savesNormalizedValuesSoSpellingDoesNotLeakIntoTheBatchFile() throws Exception {
    final SampleTypeFilterParameter saved = newParameter();
    saved.setValue(SampleTypeFilter.ofValues(" Pooled_QC ", "Blank"));

    final Document document = XMLUtils.newDocument();
    final Element root = document.createElement("parameter");
    document.appendChild(root);
    saved.saveValueToXML(root);

    final var items = root.getElementsByTagName(SampleTypeFilterParameter.XML_ITEM_TAG);
    final List<String> written = new ArrayList<>();
    for (int i = 0; i < items.getLength(); i++) {
      written.add(items.item(i).getTextContent());
    }
    // normalized and sorted, so the same selection always produces byte identical XML
    assertEquals(List.of("blank", "pooled_qc"), written);
  }

  @Test
  void roundTripsAllMode() throws Exception {
    final SampleTypeFilterParameter saved = newParameter();
    saved.setValue(SampleTypeFilter.all());

    final SampleTypeFilter loaded = saveAndLoad(saved).getValue();
    assertEquals(Mode.ALL, loaded.getMode());
    assertTrue(loaded.matchesValue("anything at all"));
  }

  @Test
  void roundTripsNoneMode() throws Exception {
    final SampleTypeFilterParameter saved = newParameter();
    saved.setValue(SampleTypeFilter.none());

    final SampleTypeFilter loaded = saveAndLoad(saved).getValue();
    assertEquals(Mode.NONE, loaded.getMode());
    assertFalse(loaded.matchesValue("qc"));
  }

  @Test
  void unknownModeAttributeFallsBackToTheListedValues() throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element root = legacyElement(document, "qc");
    root.setAttribute("mode", "something_new_from_the_future");

    final SampleTypeFilterParameter parameter = newParameter();
    parameter.loadValueFromXML(root);

    assertEquals(Mode.LIST, parameter.getValue().getMode());
    assertEquals(Set.of("qc"), parameter.getValue().getValues());
  }

  @Test
  void cloneKeepsValueAndRequiredFlag() {
    final SampleTypeFilterParameter parameter = new SampleTypeFilterParameter("Sample types",
        "description", SampleTypeFilter.ofValues("qc"), true);
    final SampleTypeFilterParameter clone = parameter.cloneParameter();

    assertEquals(parameter.getValue(), clone.getValue());
    // the old CheckComboParameter dropped requiresSelection when cloning
    clone.setValue(SampleTypeFilter.none());
    assertFalse(clone.checkValue(new ArrayList<>()));
  }

  @Test
  void checkValueOnlyFailsWhenSelectionIsRequired() {
    final SampleTypeFilterParameter optional = newParameter();
    optional.setValue(SampleTypeFilter.none());
    assertTrue(optional.checkValue(new ArrayList<>()));

    final SampleTypeFilterParameter required = new SampleTypeFilterParameter("Sample types",
        "description", SampleTypeFilter.all(), true);
    assertTrue(required.checkValue(new ArrayList<>()));
    required.setValue(SampleTypeFilter.ofValues(List.of()));
    assertFalse(required.checkValue(new ArrayList<>()));
  }

  private static SampleTypeFilterParameter saveAndLoad(SampleTypeFilterParameter parameter)
      throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element root = document.createElement("parameter");
    document.appendChild(root);
    parameter.saveValueToXML(root);

    final SampleTypeFilterParameter loaded = newParameter();
    loaded.loadValueFromXML(root);
    return loaded;
  }
}
