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

package io.github.mzmine.modules.visualization.projectmetadata.io;

import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ProjectMetadataColumnMappingsParameter implements
    UserParameter<List<ProjectMetadataColumnMapping>, ProjectMetadataColumnMappingsComponent> {

  private static final String NAME = "Export column mappings";
  private static final String DESCRIPTION = """
      Map project metadata columns to additional or existing export columns. Empty source values are replaced by the default value.""";
  private static final String ELEMENT_MAPPING = "mapping";
  private static final String ATTRIBUTE_SOURCE = "source";
  private static final String ATTRIBUTE_TARGET = "target";
  private static final String ATTRIBUTE_DEFAULT_VALUE = "default";

  private @NotNull List<ProjectMetadataColumnMapping> value;

  public ProjectMetadataColumnMappingsParameter() {
    this(List.of());
  }

  private ProjectMetadataColumnMappingsParameter(
      @Nullable final List<ProjectMetadataColumnMapping> value) {
    setValue(value);
  }

  @Override
  public @NotNull String getName() {
    return NAME;
  }

  @Override
  public @NotNull List<ProjectMetadataColumnMapping> getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable final List<ProjectMetadataColumnMapping> newValue) {
    value = newValue == null ? List.of()
        : newValue.stream().filter(mapping -> mapping != null && !mapping.isEmpty()).toList();
  }

  @Override
  public boolean checkValue(@NotNull final Collection<String> errorMessages) {
    for (final ProjectMetadataColumnMapping mapping : value) {
      if (!mapping.isActive()) {
        errorMessages.add("Metadata export mappings require source and target column names.");
        return false;
      }
    }
    return true;
  }

  @Override
  public void loadValueFromXML(@NotNull final Element xmlElement) {
    final NodeList items = xmlElement.getElementsByTagName(ELEMENT_MAPPING);
    final List<ProjectMetadataColumnMapping> loaded = new ArrayList<>();
    for (int i = 0; i < items.getLength(); i++) {
      final Element mappingElement = (Element) items.item(i);
      loaded.add(new ProjectMetadataColumnMapping(mappingElement.getAttribute(ATTRIBUTE_SOURCE),
          mappingElement.getAttribute(ATTRIBUTE_TARGET),
          mappingElement.getAttribute(ATTRIBUTE_DEFAULT_VALUE)));
    }
    setValue(loaded);
  }

  @Override
  public void saveValueToXML(@NotNull final Element xmlElement) {
    final Document parentDocument = xmlElement.getOwnerDocument();
    for (final ProjectMetadataColumnMapping mapping : value) {
      final Element mappingElement = parentDocument.createElement(ELEMENT_MAPPING);
      mappingElement.setAttribute(ATTRIBUTE_SOURCE, mapping.sourceColumn());
      mappingElement.setAttribute(ATTRIBUTE_TARGET, mapping.targetColumn());
      mappingElement.setAttribute(ATTRIBUTE_DEFAULT_VALUE, mapping.defaultValue());
      xmlElement.appendChild(mappingElement);
    }
  }

  @Override
  public @NotNull String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public @NotNull ProjectMetadataColumnMappingsComponent createEditingComponent() {
    return new ProjectMetadataColumnMappingsComponent();
  }

  @Override
  public void setValueFromComponent(
      @NotNull final ProjectMetadataColumnMappingsComponent component) {
    setValue(component.getValue());
  }

  @Override
  public void setValueToComponent(@NotNull final ProjectMetadataColumnMappingsComponent component,
      @Nullable final List<ProjectMetadataColumnMapping> newValue) {
    component.setValue(newValue);
  }

  @Override
  public @NotNull ProjectMetadataColumnMappingsParameter cloneParameter() {
    return new ProjectMetadataColumnMappingsParameter(value);
  }
}
