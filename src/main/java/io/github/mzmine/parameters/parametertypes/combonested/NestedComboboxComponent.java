/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.combonested;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

//public class NestedComboboxComponent extends FlowPane {
public class NestedComboboxComponent extends BorderPane {

  protected TreeMap<String, ParameterSet> choices;
//  protected TreeMap<String, Pane> choicesParametersPane = new TreeMap<>();
  protected TreeMap<String, Region> choicesParametersPane = new TreeMap<>();

  private final ComboBox<String> comboBox;
  private final StackPane currentChoiceParametersPane;

  protected final Map<String, Node> parametersAndComponents = new HashMap<>();

  public NestedComboboxComponent(TreeMap<String, ParameterSet> choices, String defaultChoice) {
//    super(Orientation.VERTICAL);
//    super();
    this.choices = choices;

    this.setStyle("-fx-border-color: black");
//    this.setStyle("fx-pref-height: 100");
//    this.setPrefHeight(0);
//    this.setPrefWidth(0);
//    this.setStyle("-fx-border-width: 0 0 0 5; -fx-border-color: black;");
    this.setStyle("-fx-border-width: 0 0 0 2; -fx-border-color: black;");
//    this.setStyle("-fx-padding-left: 5");
//    this.setStyle("-fx-padding: 0 0 0 5");
//    this.setStyle("-fx-margin: 0 0 0 0");
//    this.setStyle("-fx-border-width: 5 10 20 50");
//    this.setStyle("-fx-margin: 0 0 0 0");
    this.setPadding(new Insets(0, 0, 0, 5));

    currentChoiceParametersPane = new StackPane();
    for (String choice : choices.keySet()) {
//      Pane parametersPane = createChoiceParametersPane(choice);
      Region parametersPane = createChoiceParametersPane(choice);
      parametersPane.setVisible(false);
      choicesParametersPane.put(choice, parametersPane);
      currentChoiceParametersPane.getChildren().add(parametersPane);
    }


    comboBox = new ComboBox<>(FXCollections.observableArrayList(choices.keySet()));
    comboBox.setOnAction(e -> {
//      currentChoiceParametersPane.getChildren().clear();
//      currentChoiceParametersPane.getChildren().add(choicesParametersPane.get(comboBox.getSelectionModel().getSelectedItem()));
//      for (Pane pane : choicesParametersPane.values()) {
      for (Region pane : choicesParametersPane.values()) {
        pane.setVisible(false);
      }
      String currentChoice = comboBox.getSelectionModel().getSelectedItem();
      choicesParametersPane.get(currentChoice).setVisible(true);
      updateParameterSetFromComponents();
    });
//    comboBox.setStyle("-fx-padding: 0 0 0 5");

    if (defaultChoice == null) {
      comboBox.getSelectionModel().selectFirst();
    }
    else {
      comboBox.getSelectionModel().select(defaultChoice);
    }
//    comboBox.fireEvent(new ActionEvent());

//    getChildren().addAll(comboBox, currentChoiceParametersPane);
    setTop(comboBox);
    setCenter(currentChoiceParametersPane);
//    setBottom(currentChoiceParametersPane);
  }

//  protected Pane createChoiceParametersPane(String choice) {
  protected Region createChoiceParametersPane(String choice) {
    ParameterSet parameterSet = choices.get(choice);
    GridPane paramsPane = new GridPane();
//    paramsPane.setStyle("-fx-border-color: orange");
//    paramsPane.setPrefWidth(0);
//    paramsPane.setStyle("-fx-border-style: none none none solid; -fx-border-width: 5; -fx-border-color: black;");
//    paramsPane.setStyle("-fx-border-width: 0 0 0 5; -fx-border-color: black;");

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
//      GridPane.setMargin(comp, new Insets(5.0, 0.0, 5.0, 0.0));
      GridPane.setMargin(comp, new Insets(5.0, 0.0, 5.0, 0.0));

      Object value = up.getValue();
      if (value != null) {
        up.setValueToComponent(comp, value);
      }

      addListenersToNode(comp);
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

    return paramsPane;
//    return new TitledPane(choice + "parameters", paramsPane);
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

   protected void addListenersToNode(Node node) {
    if (true) return;
    if (node instanceof TextField) {
      TextField textField = (TextField) node;
      textField.textProperty().addListener(((observable, oldValue, newValue) -> {
        updateParameterSetFromComponents();
      }));
    }
    if (node instanceof ComboBox) {
      ComboBox<?> comboComp = (ComboBox<?>) node;
      comboComp.valueProperty()
          .addListener(((observable, oldValue, newValue) -> updateParameterSetFromComponents()));
    }
    if (node instanceof ChoiceBox) {
      ChoiceBox<?> choiceBox = (ChoiceBox) node;
      choiceBox.valueProperty()
          .addListener(((observable, oldValue, newValue) -> updateParameterSetFromComponents())); 5
    }
    if (node instanceof CheckBox) {
      CheckBox checkBox = (CheckBox) node;
      checkBox.selectedProperty()
          .addListener(((observable, oldValue, newValue) -> updateParameterSetFromComponents()));
    }
    if (node instanceof Region) {
      Region panelComp = (Region)
          node;
      for (int i = 0; i < panelComp.getChildrenUnmodifiable().size(); i++) {
        Node child =
            panelComp.getChildrenUnmodifiable().get(i);
        if (!(child instanceof Control)) {
          continue;
        }
        addListenersToNode(child);
      }
    }
  }

  protected String getCurrentChoice() {
    return comboBox.getSelectionModel().getSelectedItem();
  }

  protected void setCurrentChoice(String choice) {
    comboBox.getSelectionModel().select(choice);
  }

  public void setValue(NestedCombo value) {
    comboBox.getSelectionModel().select(value.getCurrentChoice());
    choices = value.getChoices();


  }

  public NestedCombo getValue() {
    String currentChoice = comboBox.getSelectionModel().getSelectedItem();
    return new NestedCombo(choices, currentChoice);
  }

  public void setToolTipText(String toolTip) {
    comboBox.setTooltip(new Tooltip(toolTip));
  }

}
