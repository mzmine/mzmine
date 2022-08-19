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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.parameters.ParameterSet;
import javafx.scene.layout.GridPane;

/**
 * This class represents the parameter setup dialog to set the values of SimpleParameterSet. Each
 * Parameter is represented by a component. The component can be obtained by calling
 * getComponentForParameter(). Type of component depends on parameter type:
 * <p>
 * TODO: parameter setup dialog should show the name of the module in the title
 */
public class ParameterSetupDialog extends EmptyParameterSetupDialogBase {

  protected final GridPane paramsPane;

  public ParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    this(valueCheckRequired, parameters, null);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  public ParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters, String message) {
    this(valueCheckRequired, parameters, true, true, message);
  }

  public ParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      boolean addOkButton, boolean addCancelButton, String message) {
    super(valueCheckRequired, parameters, addOkButton, addCancelButton, message);

    this.paramsPane = createParameterPane(parameters.getParameters());
    centerPane.setCenter(paramsPane);

    setMinWidth(500.0);
    setMinHeight(400.0);

    centerOnScreen();
  }

  public GridPane getParamsPane() {
    return paramsPane;
  }
}
