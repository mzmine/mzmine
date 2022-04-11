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
