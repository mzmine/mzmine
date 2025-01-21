/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.submodules;

import io.github.mzmine.javafx.components.factories.FxTooltips;
import io.github.mzmine.parameters.ParameterSet;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;


/**
 *
 */
public class SubModuleComponent extends FlowPane {

  /**
   *
   */
  private final Button setButton;

  public SubModuleComponent(ParameterSet embeddedParameters) {
    setButton = new Button("Setup...");
    setButton.setOnAction(e -> embeddedParameters.showSetupDialog(false));
    setButton.setDisable(false);
    getChildren().addAll(setButton);
  }

  public void setToolTipText(String toolTip) {
    setButton.setTooltip(FxTooltips.newTooltip(toolTip));
  }

}
