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

package io.github.mzmine.parameters.parametertypes.submodules;

import io.github.mzmine.parameters.ParameterSet;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;


/**
 */
public class SubModuleComponent extends FlowPane {

  /**
   * 
   */
  private final Button setButton;

  public SubModuleComponent(ParameterSet embeddedParameters) {
    setButton = new Button("Setup..");
    setButton.setOnAction(e -> embeddedParameters.showSetupDialog(true));
    setButton.setDisable(false);
    getChildren().addAll(setButton);
  }

  public void setToolTipText(String toolTip) {
    setButton.setTooltip(new Tooltip(toolTip));
  }

}
