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

package io.github.mzmine.parameters;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.controlsfx.control.CheckListView;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SpectralLibrariesSelectionParameter implements
    UserParameter<List<SpectralLibrary>, CheckListView<SpectralLibrary>> {

  private static final Logger logger = Logger
      .getLogger(SpectralLibrariesSelectionParameter.class.getName());
  private final String name;
  private final String description;
  private final int minNumber;
  private List<SpectralLibrary> values = new ArrayList<>();
  private List<SpectralLibrary> choices;

  public SpectralLibrariesSelectionParameter(int minimumSelection) {
    this("Spectral libraries", "List of preloaded spectral libraries", minimumSelection);
  }

  public SpectralLibrariesSelectionParameter(@NotNull String name, @NotNull String description,
      int minimumSelection) {
    this.name = name;
    this.description = description;
    this.minNumber = minimumSelection;
  }

  @NotNull
  private static List<SpectralLibrary> getCurrentSpectralLibraries() {
    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    return project == null ? List.of() : project.getCurrentSpectralLibraries();
  }

  @Override
  public String getName() {
    return name;
  }

  public List<SpectralLibrary> getChoices() {
    final List<SpectralLibrary> current = getCurrentSpectralLibraries();
    if (!current.equals(choices)) {
      setChoices(current);
    }
    return choices;
  }

  public void setChoices(List<SpectralLibrary> choices) {
    this.choices = choices;
    // reflect to selection
    values.removeIf(v -> !choices.contains(v));
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public CheckListView<SpectralLibrary> createEditingComponent() {
    final ObservableList<SpectralLibrary> choicesList =
        FXCollections.observableArrayList(getChoices());
    final CheckListView<SpectralLibrary> comp = new CheckListView<>(choicesList);
    comp.setPrefHeight(150);
    return comp;
  }

  @Override
  public List<SpectralLibrary> getValue() {
    return values;
  }

  @Override
  public void setValue(List<SpectralLibrary> values) {
    this.values = values;
  }

  @Override
  public SpectralLibrariesSelectionParameter cloneParameter() {
    SpectralLibrariesSelectionParameter copy =
        new SpectralLibrariesSelectionParameter(name, description, minNumber);
    copy.setChoices(new ArrayList<>(this.getChoices()));
    copy.setValue(new ArrayList<>(this.getValue()));
    return copy;
  }

  @Override
  public void setValueFromComponent(CheckListView<SpectralLibrary> component) {
    ObservableList<SpectralLibrary> selectedList = component.getCheckModel().getCheckedItems();
    this.values = new ArrayList<>(selectedList);
  }

  @Override
  public void setValueToComponent(CheckListView<SpectralLibrary> component,
      List<SpectralLibrary> newValue) {
    component.getSelectionModel().clearSelection();
    for (SpectralLibrary v : newValue) {
      component.getSelectionModel().select(v);
    }
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList items = xmlElement.getElementsByTagName("item");
    ArrayList<SpectralLibrary> newValues = new ArrayList<>();
    for (int i = 0; i < items.getLength(); i++) {
      String pathString = items.item(i).getTextContent();
      final List<SpectralLibrary> choices = getChoices();
      for (var lib : choices) {
        if (lib.getPath().getAbsolutePath().equals(pathString)) {
          newValues.add(lib);
        }
      }
    }
    setValue(newValues);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (values == null) {
      return;
    }
    Document parentDocument = xmlElement.getOwnerDocument();
    for (SpectralLibrary item : values) {
      Element newElement = parentDocument.createElement("item");
      newElement.setTextContent(item.getPath().getAbsolutePath());
      xmlElement.appendChild(newElement);
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (values == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    if (values.size() < minNumber) {
      errorMessages.add("At least " + minNumber + " option(s) must be selected for " + name);
      return false;
    }
    return true;
  }
}
