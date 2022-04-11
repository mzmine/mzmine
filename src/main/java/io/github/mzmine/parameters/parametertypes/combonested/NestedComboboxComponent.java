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

package io.github.mzmine.parameters.parametertypes.combonested;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class NestedComboboxComponent extends BorderPane {

  protected final Map<String, Node> parametersAndComponents = new HashMap<>();
  private final ComboBox<String> comboBox;
  private final StackPane currentChoiceParametersPane;
  protected TreeMap<String, ParameterSet> choices;
  protected TreeMap<String, Region> choicesParametersPane = new TreeMap<>();
  protected Integer prefWidth;

  public NestedComboboxComponent(TreeMap<String, ParameterSet> choices, String defaultChoice,
                                 boolean showNestedParamsOnStart) {
    this(choices, defaultChoice, showNestedParamsOnStart, null);
  }

  public NestedComboboxComponent(TreeMap<String, ParameterSet> choices, String defaultChoice,
                                 boolean showNestedParamsOnStart, Integer prefWidth) {
    this.choices = choices;
    this.prefWidth = prefWidth;

    currentChoiceParametersPane = new StackPane();
    for (String choice : choices.keySet()) {
      Region parametersPane = createChoiceParametersPane(choice);
      parametersPane.setVisible(false);
      choicesParametersPane.put(choice, parametersPane);
      currentChoiceParametersPane.getChildren().add(parametersPane);
    }

    this.setStyle("-fx-border-width: 0 0 0 2; -fx-border-color: black;");
    this.setPadding(new Insets(0, 0, 0, 5));

    comboBox = new ComboBox<>(FXCollections.observableArrayList(choices.keySet()));
    comboBox.setOnAction(e -> {
      for (Region pane : choicesParametersPane.values()) {
        pane.setVisible(false);
      }
      String currentChoice = comboBox.getSelectionModel().getSelectedItem();
      choicesParametersPane.get(currentChoice).setVisible(true);
      // updateParameterSetFromComponents();
    });

    if (defaultChoice == null) {
      comboBox.getSelectionModel().selectFirst();
    } else {
      comboBox.getSelectionModel().select(defaultChoice);
    }
    if (showNestedParamsOnStart) {
      comboBox.fireEvent(new ActionEvent());
    }

    setTop(comboBox);
    setCenter(currentChoiceParametersPane);
  }

  protected Region createChoiceParametersPane(String choice) {
    ParameterSet parameterSet = choices.get(choice);
    GridPane paramsPane = new GridPane();

    int rowCounter = 0;
    for (Parameter<?> p : parameterSet.getParameters()) {

      if (!(p instanceof UserParameter)) {
        continue;
      }
      UserParameter up = (UserParameter) p;

      Node comp = up.createEditingComponent();
      if (comp instanceof Control) {
        ((Control) comp).setTooltip(new Tooltip(up.getDescription()));
      }
      if (comp instanceof Region) {
        ((Region) comp).setMinWidth(0);
        ((Region) comp).setPrefWidth(prefWidth);
      }
      GridPane.setMargin(comp, new Insets(5.0, 0.0, 5.0, 0.0));

      Object value = up.getValue();
      if (value != null) {
        up.setValueToComponent(comp, value);
      }

      parametersAndComponents.put(p.getName(), comp);

      Label label = new Label(p.getName());
      label.minWidthProperty().bind(label.widthProperty());
      label.setPadding(new Insets(0.0, 10.0, 0.0, 0.0));

      label.setStyle("-fx-font-weight: bold");
      paramsPane.add(label, 0, rowCounter);
      label.setLabelFor(comp);

      paramsPane.add(comp, 1, rowCounter, 1, 1);
      rowCounter++;
    }

    if (rowCounter == 0) {
      Label label = new Label("No nested parameters");
      label.minWidthProperty().bind(label.widthProperty());
      label.setPadding(new Insets(0.0, 10.0, 0.0, 0.0));
      paramsPane.add(label, 0, rowCounter);
    }

    return paramsPane;
  }

  @SuppressWarnings("unchecked")
  public <ComponentType extends Node> ComponentType getComponentForParameter(
          UserParameter<?, ComponentType> p) {
    return (ComponentType) parametersAndComponents.get(p.getName());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void updateChoiceParameterSetFromComponents(String choice) {
    ParameterSet parameterSet = choices.get(choice);
    for (Parameter<?> p : parameterSet.getParameters()) {
      if (!(p instanceof UserParameter) && !(p instanceof HiddenParameter)) {
        continue;
      }
      UserParameter up;
      if (p instanceof UserParameter) {
        up = (UserParameter) p;
      } else {
        up = (UserParameter) ((HiddenParameter) p).getEmbeddedParameter();
      }

      Node component = parametersAndComponents.get(p.getName());
      if (component != null) {
        up.setValueFromComponent(component);
      }
    }
  }

  protected void updateParameterSetFromComponents() {
    for (String choice : choices.keySet()) {
      updateChoiceParameterSetFromComponents(choice);
    }
  }

  protected String getCurrentChoice() {
    return comboBox.getSelectionModel().getSelectedItem();
  }

  protected void setCurrentChoice(String choice) {
    comboBox.getSelectionModel().select(choice);
  }

  public void setToolTipText(String toolTip) {
    comboBox.setTooltip(new Tooltip(toolTip));
  }

}
