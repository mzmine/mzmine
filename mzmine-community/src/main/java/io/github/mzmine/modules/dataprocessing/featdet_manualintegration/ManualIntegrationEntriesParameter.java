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

package io.github.mzmine.modules.dataprocessing.featdet_manualintegration;

import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.util.XMLUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Stores the list of {@link ManualIntegrationEntry manual integrations} captured in the integration
 * dashboard so they can be serialized in an applied method and replayed. Modeled on
 * {@link io.github.mzmine.parameters.parametertypes.RegionsParameter}: the value is not
 * hand-authored but produced by the dashboard, so the
 * {@link ManualIntegrationEntriesComponent editing component} is a read-only summary.
 */
public class ManualIntegrationEntriesParameter implements
    UserParameter<List<ManualIntegrationEntry>, ManualIntegrationEntriesComponent> {

  private static final Logger logger = Logger.getLogger(
      ManualIntegrationEntriesParameter.class.getName());

  private static final String ENTRY_ELEMENT = "entry";
  private static final String RAWFILE_ELEMENT = "rawfile";
  private static final String MZ_ELEMENT = "mz";
  private static final String RT_ELEMENT = "rt";
  private static final String MOBILITY_ELEMENT = "mobility";
  private static final String ROW_ID_ATTR = "rowId";
  private static final String ROW_MZ_ATTR = "rowMz";
  private static final String ROW_RT_ATTR = "rowRt";
  private static final String ROW_MOBILITY_ATTR = "rowMobility";
  private static final String DELETED_ATTR = "deleted";

  private final String name;
  private final String description;
  private @NotNull List<ManualIntegrationEntry> value = new ArrayList<>();

  public ManualIntegrationEntriesParameter() {
    this("Manual integrations",
        "The manual (re-)integrations captured in the integration dashboard.");
  }

  public ManualIntegrationEntriesParameter(@NotNull String name, @NotNull String description) {
    this.name = name;
    this.description = description;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public @NotNull List<ManualIntegrationEntry> getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable List<ManualIntegrationEntry> newValue) {
    this.value = newValue != null ? new ArrayList<>(newValue) : new ArrayList<>();
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    final Document doc = xmlElement.getOwnerDocument();
    for (final ManualIntegrationEntry entry : value) {
      final Element entryElement = doc.createElement(ENTRY_ELEMENT);
      final FeatureRecord feature = entry.feature();
      entryElement.setAttribute(ROW_ID_ATTR, String.valueOf(feature.rowId()));
      entryElement.setAttribute(ROW_MZ_ATTR, String.valueOf(feature.mz()));
      entryElement.setAttribute(ROW_RT_ATTR, String.valueOf(feature.rt()));
      if (feature.mobility() != null) {
        entryElement.setAttribute(ROW_MOBILITY_ATTR, String.valueOf(feature.mobility()));
      }
      entryElement.setAttribute(DELETED_ATTR, String.valueOf(entry.deleted()));

      final Element rawFileElement = doc.createElement(RAWFILE_ELEMENT);
      entry.rawFile().saveToXML(rawFileElement);
      entryElement.appendChild(rawFileElement);

      XMLUtils.appendSimpleDoubleRange(entryElement, MZ_ELEMENT, entry.mzRange());
      if (entry.rtRange() != null) {
        XMLUtils.appendSimpleFloatRange(entryElement, RT_ELEMENT, entry.rtRange());
      }
      if (entry.mobilityRange() != null) {
        XMLUtils.appendSimpleFloatRange(entryElement, MOBILITY_ELEMENT, entry.mobilityRange());
      }

      xmlElement.appendChild(entryElement);
    }
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    final NodeList entryElements = xmlElement.getElementsByTagName(ENTRY_ELEMENT);
    final List<ManualIntegrationEntry> loaded = new ArrayList<>();
    for (int i = 0; i < entryElements.getLength(); i++) {
      final Element entryElement = (Element) entryElements.item(i);

      final int rowId = Integer.parseInt(entryElement.getAttribute(ROW_ID_ATTR));
      final double rowMz = Double.parseDouble(entryElement.getAttribute(ROW_MZ_ATTR));
      final float rowRt = Float.parseFloat(entryElement.getAttribute(ROW_RT_ATTR));
      final String mobilityStr = entryElement.getAttribute(ROW_MOBILITY_ATTR);
      final Float rowMobility = mobilityStr.isBlank() ? null : Float.parseFloat(mobilityStr);
      final boolean deleted = Boolean.parseBoolean(entryElement.getAttribute(DELETED_ATTR));

      final Element rawFileElement = (Element) entryElement.getElementsByTagName(RAWFILE_ELEMENT)
          .item(0);
      final RawDataFilePlaceholder rawFile = RawDataFilePlaceholder.loadFromXML(rawFileElement);

      final SimpleDoubleRange mzRange = XMLUtils.loadSimpleDoubleRange(
          XMLUtils.childElement(entryElement, MZ_ELEMENT));
      final SimpleFloatRange rtRange = XMLUtils.loadSimpleFloatRange(
          XMLUtils.childElement(entryElement, RT_ELEMENT));
      final SimpleFloatRange mobilityRange = XMLUtils.loadSimpleFloatRange(
          XMLUtils.childElement(entryElement, MOBILITY_ELEMENT));

      if (mzRange == null || rtRange == null || mobilityRange == null) {
        logger.warning(
            "Manual integration in file %s for row %d cannot be loaded. Invalid integration ranges.".formatted(
                rawFile.getName(), rowId));
        continue;
      }

      loaded.add(
          new ManualIntegrationEntry(new FeatureRecord(rowId, rowMz, rowRt, rowMobility), rawFile,
              mzRange, rtRange, mobilityRange, deleted));
    }
    this.value = loaded;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value.isEmpty()) {
      errorMessages.add("No manual integrations are defined.");
      return false;
    }
    return true;
  }

  @Override
  public ManualIntegrationEntriesComponent createEditingComponent() {
    final ManualIntegrationEntriesComponent component = new ManualIntegrationEntriesComponent();
    component.setValue(value);
    return component;
  }

  @Override
  public void setValueFromComponent(ManualIntegrationEntriesComponent component) {
    // read-only component: the value it holds is unchanged
    this.value = new ArrayList<>(component.getValue());
  }

  @Override
  public void setValueToComponent(ManualIntegrationEntriesComponent component,
      @Nullable List<ManualIntegrationEntry> newValue) {
    component.setValue(newValue);
  }

  @Override
  public ManualIntegrationEntriesParameter cloneParameter() {
    final ManualIntegrationEntriesParameter clone = new ManualIntegrationEntriesParameter(name,
        description);
    clone.setValue(new ArrayList<>(value));
    return clone;
  }
}
