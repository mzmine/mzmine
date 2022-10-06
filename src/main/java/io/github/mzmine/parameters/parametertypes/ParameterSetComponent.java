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
package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;

/**
 *
 * @author aleksandrsmirnov
 */
public class ParameterSetComponent extends GridPane {

  private final Label lblParameters;
  private final Button btnChange;
  private final ProgressBar progressBar;

  private ParameterSet parameters;

  public ParameterSetComponent(final ParameterSet parameters) {

    this.parameters = parameters;

    // this.setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

    lblParameters = new Label();
    lblParameters.setDisable(true);
    this.add(lblParameters, 0, 0);

    btnChange = new Button("Change");
    btnChange.setOnAction(e -> {
      if (parameters == null)
        return;

      ExitCode exitCode = parameters.showSetupDialog(true);
      if (exitCode != ExitCode.OK)
        return;
      updateLabel();

    });
    this.add(btnChange, 1, 0);

    progressBar = new ProgressBar();
    progressBar.setProgress(0.0);
    progressBar.setVisible(false);
    // progressBar.setStringPainted(true);
    this.add(progressBar, 0, 1, 2, 1);

    // if (process != null) {
    // SwingUtilities.invokeLater(new Runnable() {
    // public void run() {
    // int value = (int) Math.round(process.getFinishedPercentage());
    // if (0 < value && value < 100) {
    // progressBar.setValue(value);
    // progressBar.setVisible(true);
    // } else {
    // progressBar.setValue(0);
    // progressBar.setVisible(false);
    // }
    //
    // try {
    // Thread.sleep(5);
    // }
    // catch (InterruptedException e) {
    // progressBar.setValue(0);
    // progressBar.setVisible(false);
    // }
    // }
    // });
    // }
  }


  public ParameterSet getValue() {
    return parameters;
  }

  public void setValue(final ParameterSet parameters) {
    this.parameters = parameters;

    updateLabel();
  }

  private void updateLabel() {
    // Update text for lblParameters
    /*StringBuilder builder = new StringBuilder().append("<html>");
    Parameter[] params = parameters.getParameters();
    for (int i = 0; i < params.length; ++i) {
      builder.append(params[i].getName()).append(" = ").append(params[i].getValue());
      if (i < params.length - 1)
        builder.append("<br>");
    }
    builder.append("</html>");

    lblParameters.setText(builder.toString());*/
  }
}
