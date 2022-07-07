/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.base.Strings;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SpectralLibrarySelectionParameter implements
    UserParameter<SpectralLibrarySelection, SpectralLibrarySelectionComponent> {

  private final String name = "Spectral libraries";

  private SpectralLibrarySelection value;

  public SpectralLibrarySelectionParameter() {
    this(new SpectralLibrarySelection());
  }

  public SpectralLibrarySelectionParameter(SpectralLibrarySelection value) {
    this.value = value;
  }

  @Override
  public SpectralLibrarySelection getValue() {
    return value;
  }

  @Override
  public void setValue(SpectralLibrarySelection newValue) {
    this.value = newValue;
  }

  public void setValue(SpectralLibrarySelectionType selectionType,
      SpectralLibrary[] specificFiles) {
    value = new SpectralLibrarySelection(selectionType,
        Arrays.stream(specificFiles).map(SpectralLibrary::getPath).toList());
  }

  @Override
  public SpectralLibrarySelectionParameter cloneParameter() {
    return new SpectralLibrarySelectionParameter(value);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return "Spectral libraries used by this module. Need to be imported first. Either ALL or specific libraries.";
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value.getSelectionType() == SpectralLibrarySelectionType.SPECIFIC
        && value.getSpecificLibraryNames().isEmpty()) {
      errorMessages.add(
          "When selecting specific libraries, setup the selected library files. Currently 0 are selected.");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    SpectralLibrarySelectionType selectionType;
    final String attrValue = xmlElement.getAttribute("type");

    if (Strings.isNullOrEmpty(attrValue)) {
      selectionType = SpectralLibrarySelectionType.ALL_IMPORTED;
    } else {
      selectionType = SpectralLibrarySelectionType.valueOf(attrValue);
    }
    if (selectionType == SpectralLibrarySelectionType.ALL_IMPORTED) {
      value = new SpectralLibrarySelection();
      return;
    }

    List<File> libPaths = new ArrayList<>();

    NodeList items = xmlElement.getElementsByTagName("specific_file");
    for (int i = 0; i < items.getLength(); i++) {
      String path = items.item(i).getTextContent();
      if (path != null && !path.isBlank()) {
        libPaths.add(new File(path));
      }
    }

    this.value = new SpectralLibrarySelection(selectionType, libPaths);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    Document parentDocument = xmlElement.getOwnerDocument();
    xmlElement.setAttribute("type", value.getSelectionType().name());

    if (value.getSelectionType() == SpectralLibrarySelectionType.SPECIFIC
        && value.getSpecificLibraryNames() != null) {
      for (File item : value.getSpecificLibraryNames()) {
        assert item != null;
        Element newElement = parentDocument.createElement("specific_file");
        newElement.setTextContent(item.getAbsolutePath());
        xmlElement.appendChild(newElement);
      }
    }

  }

  @Override
  public SpectralLibrarySelectionComponent createEditingComponent() {
    return new SpectralLibrarySelectionComponent();
  }

  @Override
  public void setValueFromComponent(SpectralLibrarySelectionComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(SpectralLibrarySelectionComponent component,
      SpectralLibrarySelection newValue) {
    component.setValue(newValue);
  }

}
