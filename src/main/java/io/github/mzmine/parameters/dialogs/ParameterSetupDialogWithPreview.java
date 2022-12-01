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
import javafx.geometry.Orientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
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

  public ParameterSetupDialogWithPreview(boolean valueCheckRequired, ParameterSet parameters) {
    this(valueCheckRequired, parameters, "");
  }

  public ParameterSetupDialogWithPreview(boolean valueCheckRequired, ParameterSet parameters,
      String message) {
    super(valueCheckRequired, parameters, message);

    paramPreviewSplit = new SplitPane();
    paramPreviewSplit.getItems().add(getParamPane());
    paramPreviewSplit.setOrientation(Orientation.HORIZONTAL);
    mainPane.setCenter(paramPreviewSplit);

    previewWrapperPane = new BorderPane();
    cbShowPreview = new CheckBox();

    Label previewLabel = new Label("Show preview");
    previewLabel.setStyle("-fx-font-style: italic");
    paramsPane.add(previewLabel, 0, getNumberOfParameters() + 2);
    paramsPane.add(cbShowPreview, 1, getNumberOfParameters() + 2);
    paramsPane.setHgap(7d);
    paramsPane.setVgap(1d);

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
      mainPane.getScene().getWindow().sizeToScene();
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
  }
}
