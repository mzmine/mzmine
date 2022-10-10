/*
 * Copyright 2006-2022 The MZmine Development Team
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
package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TitledPane;

/**
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class AdvancedParametersComponent extends Accordion {

  private final ParameterSetupPane paramPane;
  private final CheckBox checkBox;

  public AdvancedParametersComponent(final ParameterSet parameters, String title, boolean state) {
    paramPane = new ParameterSetupPane(false, parameters, false, false, null, true, false);
    checkBox = new CheckBox(title);
    setSelected(state);

    TitledPane titledPane = new TitledPane("", paramPane.getParamsPane());
    titledPane.setGraphic(checkBox);

    getPanes().add(titledPane);
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
