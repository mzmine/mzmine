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

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parameter holding a list of {@link MetadataRegexMapping}s used to extract sample metadata columns
 * from the file names or paths of raw data files.
 */
public class MetadataRegexExtractionParameter implements
    UserParameter<List<MetadataRegexMapping>, MetadataRegexExtractionComponent> {

  private static final String MAPPING_ELEMENT = "mapping";
  private static final String REGEX_ELEMENT = "regex";
  private static final String VALUE_MAPPING_ELEMENT = "valueMapping";

  private final String name;
  private final String description;
  private @NotNull List<MetadataRegexMapping> value;

  public MetadataRegexExtractionParameter(final String name, final String description) {
    this.name = name;
    this.description = description;
    this.value = List.of();
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
  public MetadataRegexExtractionComponent createEditingComponent() {
    return new MetadataRegexExtractionComponent();
  }

  @Override
  public void setValueFromComponent(final MetadataRegexExtractionComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(final MetadataRegexExtractionComponent component,
      @Nullable final List<MetadataRegexMapping> newValue) {
    component.setValue(newValue);
  }

  @Override
  public @NotNull List<MetadataRegexMapping> getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable final List<MetadataRegexMapping> newValue) {
    this.value = newValue != null ? newValue : List.of();
  }

  @Override
  public Priority getComponentVgrowPriority() {
    return Priority.ALWAYS;
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {
    if (value.isEmpty()) {
      errorMessages.add("Add at least one regex column mapping.");
      return false;
    }

    boolean valid = true;
    for (int i = 0; i < value.size(); i++) {
      final MetadataRegexMapping m = value.get(i);
      final String prefix = "Mapping " + (i + 1) + ": ";

      if (m.columnName().isBlank()) {
        errorMessages.add(prefix + "enter a target column name.");
        valid = false;
      } else if (m.columnName().equalsIgnoreCase(MetadataColumn.FILENAME_HEADER)) {
        errorMessages.add(prefix + "'" + MetadataColumn.FILENAME_HEADER
            + "' is a reserved column name and cannot be written to.");
        valid = false;
      }

      if (m.regex().isBlank()) {
        errorMessages.add(prefix + "enter a regular expression.");
        valid = false;
        continue;
      }

      try {
        Pattern.compile(m.regex());
      } catch (final PatternSyntaxException ex) {
        errorMessages.add(prefix + "invalid regex: " + ex.getMessage());
        valid = false;
      }
    }
    return valid;
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {
    final List<MetadataRegexMapping> loaded = new ArrayList<>();
    final NodeList mappingNodes = xmlElement.getElementsByTagName(MAPPING_ELEMENT);
    for (int i = 0; i < mappingNodes.getLength(); i++) {
      final Element mappingEl = (Element) mappingNodes.item(i);

      final RegexInputSource source = UniqueIdSupplier.parseOrElse(mappingEl.getAttribute("source"),
          RegexInputSource.values(), RegexInputSource.FILE_NAME);
      final ExtractColumnType type = UniqueIdSupplier.parseOrElse(mappingEl.getAttribute("type"),
          ExtractColumnType.values(), ExtractColumnType.AUTO);
      final String column = mappingEl.getAttribute("column");
      final String defaultValue = mappingEl.getAttribute("default");
      final DropUnmappedMode dropUnmapped = UniqueIdSupplier.parseOrElse(
          mappingEl.getAttribute("dropUnmapped"), DropUnmappedMode.values(),
          DropUnmappedMode.KEEP_UNMAPPED);

      String regex = "";
      final NodeList regexNodes = mappingEl.getElementsByTagName(REGEX_ELEMENT);
      if (regexNodes.getLength() > 0) {
        regex = regexNodes.item(0).getTextContent();
      }

      final List<MetadataValueMapping> valueMappings = new ArrayList<>();
      final NodeList vmNodes = mappingEl.getElementsByTagName(VALUE_MAPPING_ELEMENT);
      for (int j = 0; j < vmNodes.getLength(); j++) {
        final Element vmEl = (Element) vmNodes.item(j);
        valueMappings.add(
            new MetadataValueMapping(vmEl.getAttribute("from"), vmEl.getAttribute("to")));
      }

      loaded.add(new MetadataRegexMapping(source, column, type, regex, defaultValue, dropUnmapped,
          valueMappings));
    }
    value = loaded;
  }

  @Override
  public void saveValueToXML(final Element xmlElement) {
    final Document doc = xmlElement.getOwnerDocument();
    for (final MetadataRegexMapping m : value) {
      final Element mappingEl = doc.createElement(MAPPING_ELEMENT);
      mappingEl.setAttribute("source", m.inputSource().getUniqueID());
      mappingEl.setAttribute("column", m.columnName());
      mappingEl.setAttribute("type", m.type().getUniqueID());
      mappingEl.setAttribute("default", m.defaultValue());
      mappingEl.setAttribute("dropUnmapped", m.dropUnmapped().getUniqueID());

      // regex goes into a child element to avoid attribute-escaping surprises
      final Element regexEl = doc.createElement(REGEX_ELEMENT);
      regexEl.setTextContent(m.regex());
      mappingEl.appendChild(regexEl);

      for (final MetadataValueMapping vm : m.valueMappings()) {
        final Element vmEl = doc.createElement(VALUE_MAPPING_ELEMENT);
        vmEl.setAttribute("from", vm.from());
        vmEl.setAttribute("to", vm.to());
        mappingEl.appendChild(vmEl);
      }
      xmlElement.appendChild(mappingEl);
    }
  }

  @Override
  public MetadataRegexExtractionParameter cloneParameter() {
    final MetadataRegexExtractionParameter copy = new MetadataRegexExtractionParameter(name,
        description);
    copy.value = new ArrayList<>(value);
    return copy;
  }

}
