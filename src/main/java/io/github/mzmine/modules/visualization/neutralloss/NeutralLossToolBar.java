/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.neutralloss;

import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;


/**
 * Neutral loss visualizer's toolbar class
 */
class NeutralLossToolBar extends ToolBar {

  /**
   *
   */

  static final ImageView dataPointsIcon = new ImageView(new Image("icons/datapointsicon.png"));

  NeutralLossToolBar(NeutralLossVisualizerWindow masterFrame) {

    this.setOrientation(Orientation.VERTICAL);
    this.setFocusTraversable(false);
    setBackground(new Background(new BackgroundFill(
        Color.WHITE, new CornerRadii(0), new javafx.geometry.Insets(5))));
    Button button = new Button("", dataPointsIcon);
    button.setOnAction(event -> masterFrame.handleHighlight("HIGHLIGHT"));
    button.setTooltip(new Tooltip("Highlight selected precursor mass range"));
    this.getChildren().add(button);
  }

}
