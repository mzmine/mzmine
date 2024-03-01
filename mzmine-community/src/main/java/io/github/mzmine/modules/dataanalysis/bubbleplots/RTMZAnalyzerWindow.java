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

package io.github.mzmine.modules.dataanalysis.bubbleplots;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScaleSetupDialogFX;
import javafx.application.Platform;
import org.jfree.data.xy.AbstractXYZDataset;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.WindowsMenu;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class RTMZAnalyzerWindow extends Stage {

  private static final Image axesIcon = FxIconUtil.loadImageFromResources("icons/axesicon.png");
  private static final Image colorbarIcon =
      FxIconUtil.loadImageFromResources("icons/colorbaricon.png");

  private final Scene mainScene;
  private final BorderPane mainPane;

  private final ToolBar toolbar;
  private final RTMZPlot plot;


  public RTMZAnalyzerWindow(AbstractXYZDataset dataset, FeatureList featureList,
      InterpolatingLookupPaintScale paintScale) {

    mainPane = new BorderPane();
    mainScene = new Scene(mainPane);

    // Use main CSS
    mainScene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(mainScene);

    setMinWidth(600.0);
    setMinHeight(500.0);

    toolbar = new ToolBar();
    toolbar.setOrientation(Orientation.VERTICAL);
    Button axesButton = new Button(null, new ImageView(axesIcon));
    axesButton.setTooltip(new Tooltip("Setup ranges for axes"));
    Button colorButton = new Button(null, new ImageView(colorbarIcon));
    colorButton.setTooltip(new Tooltip("Setup color palette"));
    toolbar.getItems().addAll(axesButton, colorButton);
    mainPane.setRight(toolbar);

    plot = new RTMZPlot(this, dataset, paintScale);
    mainPane.setCenter(plot);

    axesButton.setOnAction(e -> {
      AxesSetupDialog dialog = new AxesSetupDialog(this, plot.getChart().getXYPlot());
      dialog.showAndWait();
    });

    colorButton.setOnAction(e -> {
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            InterpolatingLookupPaintScaleSetupDialogFX colorDialog =
                    new InterpolatingLookupPaintScaleSetupDialogFX(plot.getPaintScale());
            colorDialog.showAndWait();

            if (colorDialog.getExitCode() == ExitCode.OK)
              plot.setPaintScale(colorDialog.getPaintScale());
          }
        });

      });

    String title = featureList.getName();
    title = title.concat(" : ");
    title = title.concat(dataset.toString());
    this.setTitle(title);

    // Add the Windows menu
    WindowsMenu.addWindowsMenu(mainScene);

  }


}
