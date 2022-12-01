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

    setMinWidth(200);
    setMinHeight(200);

    centerOnScreen();
  }

  public GridPane getParamsPane() {
    return paramsPane;
  }
}
