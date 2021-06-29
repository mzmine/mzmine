/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.parameters.dialogs;


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import javafx.geometry.Orientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

public class ParameterSetupDialogWithPreview extends ParameterSetupDialog {

  /*
   * Structure: <p></p> //
   * - mainPane <p></p> //
   *  -bottom <p></p> //
   *    - pnlButtons <p></p> //
   *  -center <p></p> //
   *    - mainScrollPane <p></p> //
   *      - paramPreviewSplit <p></p> //
   *        - previewWrapperPane (BorderPane) <p></p> //
   *        - previewPane <p></p> //
   */

  protected final SplitPane paramPreviewSplit;
  protected final BorderPane previewWrapperPane;
  protected final CheckBox cbShowPreview;
  private Runnable onPreviewShown;

  public ParameterSetupDialogWithPreview(boolean valueCheckRequired,
      ParameterSet parameters) {
    this(valueCheckRequired, parameters, "");
  }

  public ParameterSetupDialogWithPreview(boolean valueCheckRequired,
      ParameterSet parameters, String message) {
    super(valueCheckRequired, parameters, message);

    // Iterate over all OptionalModuleParameter's and add listeners to detect changes in their
    // embedded parameters. In case of any change call this.parametersChanged()
    for (Parameter<?> parameter : parameters.getParameters()) {
      if (parameter instanceof OptionalModuleParameter) {
        ((OptionalModuleParameter<?>) parameter).embeddedParametersChangeProperty()
            .addListener((observable, oldValue, newValue) -> parametersChanged());
      }
    }

    paramPreviewSplit = new SplitPane();
    paramPreviewSplit.getItems().add(mainScrollPane);
    paramPreviewSplit.setOrientation(Orientation.HORIZONTAL);
    mainPane.setCenter(paramPreviewSplit);

    previewWrapperPane = new BorderPane();
    cbShowPreview = new CheckBox();

    Separator separator = new Separator();
    separator.setMinHeight(15d);
    paramsPane.add(separator, 0, getNumberOfParameters() + 2, 2, 1);
    paramsPane.add(new Label("Show preview"), 0, getNumberOfParameters() + 3);
    paramsPane.add(cbShowPreview, 1, getNumberOfParameters() + 3);
    paramsPane.setHgap(7d);

    cbShowPreview.setDisable(true);
    cbShowPreview.selectedProperty()
        .addListener(((observable, oldValue, newValue) -> showPreview(newValue)));

  }

  protected void showPreview(boolean show) {
    if (show) {
      paramPreviewSplit.getItems().add(previewWrapperPane);
      mainPane.getScene().getWindow().sizeToScene();
      if (onPreviewShown != null) {
        try {
          onPreviewShown.run();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      paramPreviewSplit.setDividerPosition(0, 0.5);
    } else {
      paramPreviewSplit.getItems().remove(previewWrapperPane);
    }
  }

  protected BorderPane getPreviewWrapperPane() {
    return previewWrapperPane;
  }

  protected CheckBox getPreviewCheckbox() {
    return cbShowPreview;
  }

  public void setOnPreviewShown(Runnable onPreviewShown) {
    this.onPreviewShown = onPreviewShown;
    cbShowPreview.setDisable(false);
  }
}
