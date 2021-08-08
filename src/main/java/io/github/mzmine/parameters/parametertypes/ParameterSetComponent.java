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
