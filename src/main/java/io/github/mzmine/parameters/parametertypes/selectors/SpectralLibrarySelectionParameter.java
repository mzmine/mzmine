/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.base.Strings;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Nullable;
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
      @Nullable SpectralLibrarySelection newValue) {
    component.setValue(newValue);
  }

}
