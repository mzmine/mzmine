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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter.Mode;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Selects sample types - the values of the {@code mzmine_sample_type} metadata column - as free
 * text group names plus the open ended {@link Mode#ALL} and {@link Mode#NONE} choices. The value is
 * a {@link SampleTypeFilter}, so tasks can use {@link #getValue()} directly as the matcher.
 * <p>
 * Replaces the former {@code CheckComboParameter<SampleType>}, which could only offer the four
 * predefined enum constants. {@link io.github.mzmine.parameters.parametertypes.CheckComboParameter}
 * remains in use for genuinely closed domains such as ion types.
 *
 * <h3>Batch file compatibility</h3>
 * The XML shape of the old parameter is a plain list of {@code <selected>} elements:
 * <pre>{@code
 * <parameter name="Sample types">
 *   <selected>qc</selected>
 *   <selected>blank</selected>
 * </parameter>
 * }</pre>
 * That shape still loads and yields {@link Mode#LIST} with exactly those values, so existing batch
 * files keep working. New files additionally carry a {@code mode} attribute; a missing attribute
 * therefore means {@link Mode#LIST}. Unlike the old parameter, values that are not among the
 * predefined types are <b>kept</b> instead of being dropped - the whole point is that the user may
 * select group names mzmine does not know. Values are written in the normalized (trimmed, lower
 * cased) and sorted form of {@link SampleTypeFilter}, which makes the saved XML independent of how
 * the group was spelled when it was selected.
 */
public class SampleTypeFilterParameter implements
    UserParameter<SampleTypeFilter, SampleTypeFilterComponent> {

  private static final Logger logger = Logger.getLogger(SampleTypeFilterParameter.class.getName());

  /**
   * Same element name the old {@code CheckComboParameter} used, this is what makes old batch files
   * load.
   */
  public static final String XML_ITEM_TAG = "selected";
  public static final String XML_MODE_ATTR = "mode";

  private final String name;
  private final String description;
  private final boolean requiresSelection;
  private @NotNull SampleTypeFilter value;

  public SampleTypeFilterParameter(String name, String description) {
    this(name, description, SampleTypeFilter.all());
  }

  public SampleTypeFilterParameter(String name, String description,
      @Nullable SampleTypeFilter defaultValue) {
    this(name, description, defaultValue, false);
  }

  public SampleTypeFilterParameter(String name, String description,
      @Nullable SampleTypeFilter defaultValue, boolean requiresSelection) {
    this.name = name;
    this.description = description;
    this.requiresSelection = requiresSelection;
    this.value = requireNonNullElse(defaultValue, SampleTypeFilter.all());
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
  public @NotNull SampleTypeFilter getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable SampleTypeFilter newValue) {
    this.value = requireNonNullElse(newValue, SampleTypeFilter.all());
  }

  @Override
  public SampleTypeFilterComponent createEditingComponent() {
    return new SampleTypeFilterComponent(value);
  }

  @Override
  public void setValueFromComponent(SampleTypeFilterComponent component) {
    this.value = component.getValue();
  }

  @Override
  public void setValueToComponent(SampleTypeFilterComponent component,
      @Nullable SampleTypeFilter newValue) {
    component.setValue(requireNonNullElse(newValue, SampleTypeFilter.all()));
  }

  @Override
  public SampleTypeFilterParameter cloneParameter() {
    // value is immutable so it can be shared, requiresSelection must be carried over
    return new SampleTypeFilterParameter(name, description, value, requiresSelection);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!requiresSelection || !value.isEmpty()) {
      return true;
    }
    errorMessages.add("No sample type selected for parameter %s.".formatted(name));
    return false;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    final NodeList itemElements = xmlElement.getElementsByTagName(XML_ITEM_TAG);
    final List<String> selected = new ArrayList<>();
    for (int i = 0; i < itemElements.getLength(); i++) {
      final Node itemElement = itemElements.item(i);
      final String text = StringUtils.normalizeStripLowerCase(itemElement.getTextContent());
      if (!text.isEmpty()) {
        // keep custom values, mzmine does not need to know the group to filter for it
        selected.add(text);
      }
    }

    // no mode attribute means the file predates the all/none choices -> plain list of values
    this.value = switch (parseMode(xmlElement.getAttribute(XML_MODE_ATTR))) {
      case ALL -> SampleTypeFilter.all();
      case NONE -> SampleTypeFilter.none();
      case LIST -> SampleTypeFilter.ofValues(selected);
    };
  }

  private @NotNull Mode parseMode(@Nullable final String attribute) {
    return UniqueIdSupplier.parseOrElse(attribute, Mode.values(), Mode.LIST);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setAttribute(XML_MODE_ATTR, value.getMode().getUniqueID());

    final var document = xmlElement.getOwnerDocument();
    for (final String item : value.getValues()) {
      final Element element = document.createElement(XML_ITEM_TAG);
      element.setTextContent(item);
      xmlElement.appendChild(element);
    }
  }

  @Override
  public String toString() {
    return name;
  }
}
