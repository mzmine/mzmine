/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.EstimatedComponentHeightProvider;
import io.github.mzmine.parameters.FullColumnComponent;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TitledPane;

/**
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class AdvancedParametersComponent extends Accordion implements
    EstimatedComponentHeightProvider, FullColumnComponent {

  private final ParameterSetupPane paramPane;
  private final CheckBox checkBox;
  private final DoubleProperty estimatedHeightProperty = new SimpleDoubleProperty(0);

  public AdvancedParametersComponent(final ParameterSet parameters, String title, boolean state) {
    paramPane = new ParameterSetupPane(false, parameters, false, false, null, true, false);
    checkBox = new CheckBox(title);
    setSelected(state);

    TitledPane titledPane = new TitledPane("", paramPane.getParamsPane());
    titledPane.setGraphic(checkBox);
    expandedPaneProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        setEstimatedHeight(0);
      } else {
        setEstimatedHeight(paramPane.getParametersAndComponents().size());
      }
    });
    titledPane.setAnimated(false);

    checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
      paramPane.getParametersAndComponents().values()
          .forEach(node -> node.setDisable(!checkBox.isSelected()));
    });

    paramPane.getParametersAndComponents().values()
        .forEach(node -> node.setDisable(!checkBox.isSelected()));

    getPanes().add(titledPane);
  }

  @Override
  public DoubleProperty estimatedHeightProperty() {
    return estimatedHeightProperty;
  }

  public ParameterSet getValue() {
    return paramPane.updateParameterSetFromComponents();
  }

  public void setValue(final ParameterSet parameters) {
    paramPane.setParameterValuesToComponents();
  }

  public boolean isSelected() {
    return checkBox.isSelected();
  }

  public void setSelected(boolean state) {
    checkBox.setSelected(state);
  }
}
