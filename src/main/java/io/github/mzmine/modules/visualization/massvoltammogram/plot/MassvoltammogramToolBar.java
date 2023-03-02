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
 */

package io.github.mzmine.modules.visualization.massvoltammogram.plot;

import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramExport;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import io.github.mzmine.util.javafx.FxIconUtil;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.math.plot.PlotPanel;
import org.math.plot.canvas.Plot3DCanvas;
import org.math.plot.canvas.PlotCanvas;

public class MassvoltammogramToolBar extends ToolBar {

  //Icons.
  private final Image MOVE_PLOT_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnMove.png");
  private final Image RESET_PLOT_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnReset.png");
  private final Image ROTATE_PLOT_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnRotate.png");
  private final Image EXPORT_PLOT_ICON = FxIconUtil.loadImageFromResources("icons/exporticon.png");
  private final Image EDIT_MZ_RANGE_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnEditMzRange.png");

  //The plot.
  PlotPanel plotPanel;
  PlotCanvas plotCanvas;

  //The massvoltammogram.
  Massvoltammogram massvoltammogram;

  public MassvoltammogramToolBar(PlotPanel plotPanel, Massvoltammogram massvoltammogram) {

    //Setting fields.
    this.plotPanel = plotPanel;
    this.plotCanvas = plotPanel.plotCanvas;
    this.massvoltammogram = massvoltammogram;

    //Generating the plot toolbar.
    generateToolBar();
  }

  private void generateToolBar() {

    //Creating a button to move the plot.
    final ToggleButton moveButton = new ToggleButton(null, new ImageView(MOVE_PLOT_ICON));
    moveButton.setTooltip(new Tooltip("Move the massvoltammogram."));
    moveButton.setOnAction(e -> plotCanvas.ActionMode = PlotCanvas.TRANSLATION);
    moveButton.setMinSize(35, 35);

    //Creating a Button to rotate the plot.
    final ToggleButton rotateButton = new ToggleButton(null, new ImageView(ROTATE_PLOT_ICON));
    rotateButton.setSelected(true);
    rotateButton.setTooltip(new Tooltip("Rotate the massvoltammogram."));
    rotateButton.setOnAction(e -> plotCanvas.ActionMode = Plot3DCanvas.ROTATION);
    rotateButton.setMinSize(35, 35);

    //Connecting the move and rotate buttons in a toggle group
    final ToggleGroup toggleGroup = new ToggleGroup();
    moveButton.setToggleGroup(toggleGroup);
    rotateButton.setToggleGroup(toggleGroup);

    //Creating a button to reset the zoom.
    final Button resetButton = new Button(null, new ImageView(RESET_PLOT_ICON));
    resetButton.setTooltip(new Tooltip("Reset the view."));
    resetButton.setOnAction(e -> plotCanvas.resetBase());
    resetButton.setMinSize(35, 35);

    //Creating a button to export the plot.
    final Button exportButton = new Button(null, new ImageView(EXPORT_PLOT_ICON));
    exportButton.setTooltip(new Tooltip("Export the massvoltammogram."));
    exportButton.setOnAction(e -> MassvoltammogramExport.exportPlot(massvoltammogram));
    exportButton.setMinSize(35, 35);

    //Creating a button to edit the m/z-range.
    Button editMzRangeButton = new Button(null, new ImageView(EDIT_MZ_RANGE_ICON));
    editMzRangeButton.setTooltip(new Tooltip("Edit the massvoltammograms m/z-range."));
    editMzRangeButton.setOnAction(e -> massvoltammogram.editMzRange());
    editMzRangeButton.setMinSize(35, 35);

    setOrientation(Orientation.VERTICAL);
    getItems().addAll(moveButton, rotateButton, resetButton, exportButton, editMzRangeButton);
    setStyle("-fx-background-color: white;");
  }
}
