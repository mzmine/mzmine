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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.ValueChangeDecorator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.Nullable;

public class ComboWithInputComponent<EnumValue> extends HBox implements ValueChangeDecorator {

  private final ComboBox<EnumValue> comboBox;
  private final Node embeddedComponent;
  private final UserParameter<?, ? extends Node> embeddedParameter;
  private List<Runnable> changeListeners;

  public ComboWithInputComponent(final UserParameter<?, ? extends Node> embeddedParameter,
      final ObservableList<EnumValue> choices, final EnumValue inputTrigger,
      ComboWithInputValue<EnumValue, ?> defaultValue) {
    this.embeddedParameter = embeddedParameter;
    setSpacing(5);
    setAlignment(Pos.CENTER_LEFT);

    embeddedComponent = embeddedParameter.createEditingComponent();
    embeddedComponent.setDisable(true);

    if (embeddedComponent instanceof ValueChangeDecorator valueChangeDecorator) {
      valueChangeDecorator.addValueChangedListener(this::onValueChanged);
    }

    comboBox = new ComboBox<>();
    comboBox.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          embeddedComponent.setDisable(!Objects.equals(getSelectedOption(), inputTrigger));
          onValueChanged();
        });
    comboBox.setItems(choices);
    comboBox.setMinWidth(USE_PREF_SIZE);
    setValue(defaultValue);

    if (choices.contains(inputTrigger)) {
      super.getChildren().addAll(comboBox, embeddedComponent);
    } else {
      super.getChildren().add(comboBox);
    }
  }

  public Node getEmbeddedComponent() {
    return embeddedComponent;
  }

  public ComboBox<EnumValue> getComboBox() {
    return comboBox;
  }

  public void setToolTipText(String toolTip) {
    comboBox.setTooltip(new Tooltip(toolTip));
  }

  public EnumValue getSelectedOption() {
    return comboBox.getValue();
  }

  public void setValue(@Nullable final ComboWithInputValue<EnumValue, ?> newValue) {
    if (newValue == null) {
      comboBox.getSelectionModel().clearSelection();
      return;
    }

    comboBox.getSelectionModel().select(newValue.getSelectedOption());

    if (embeddedParameter.getValue() != null) {
      ((UserParameter) this.embeddedParameter).setValueToComponent(embeddedComponent,
          newValue.getEmbeddedValue());
    }
  }

  @Override
  public void addValueChangedListener(final Runnable onChange) {
    if (changeListeners == null) {
      changeListeners = new ArrayList<>();
    }
    changeListeners.add(onChange);
    comboBox.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> onValueChanged());
  }

  public void onValueChanged() {
    if (changeListeners == null) {
      return;
    }
    for (final Runnable onChange : changeListeners) {
      onChange.run();
    }
  }

}
