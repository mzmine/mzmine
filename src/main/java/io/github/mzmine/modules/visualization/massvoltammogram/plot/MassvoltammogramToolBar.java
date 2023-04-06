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

package io.github.mzmine.modules.visualization.massvoltammogram.plot;

import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramExportTask;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.FxThreadUtil;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
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
    exportButton.setOnAction(e -> exportMassvoltammogram());
    exportButton.setMinSize(35, 35);

    //Creating a button to edit the m/z-range.
    final Button editMzRangeButton = new Button(null, new ImageView(EDIT_MZ_RANGE_ICON));
    editMzRangeButton.setTooltip(new Tooltip("Edit the massvoltammograms m/z-range."));
    editMzRangeButton.setOnAction(e -> massvoltammogram.editMzRange());
    editMzRangeButton.setMinSize(35, 35);

    setOrientation(Orientation.VERTICAL);
    getItems().addAll(moveButton, rotateButton, resetButton, exportButton, editMzRangeButton);
    setStyle("-fx-background-color: white;");
  }

  /**
   * Exports the massvoltammogram to different file formats chosen by the user.
   */
  private void exportMassvoltammogram() {

    //Initializing a file chooser and a file to save the selected path to.
    final FileChooser fileChooser = new FileChooser();
    final AtomicReference<File> chosenFile = new AtomicReference<>(null);

    //Generating the extension filters.
    final FileChooser.ExtensionFilter extensionFilterPNG = new ExtensionFilter(
        "Portable Network Graphics", ".png");
    final FileChooser.ExtensionFilter extensionFilterCSV = new ExtensionFilter("CSV-File", ".csv");
    final FileChooser.ExtensionFilter extensionFilterXLSX = new ExtensionFilter("Excel-File",
        ".xlsx");
    fileChooser.getExtensionFilters()
        .addAll(Arrays.asList(extensionFilterCSV, extensionFilterXLSX, extensionFilterPNG));

    //Opening dialog to choose the path to save the file to.
    FxThreadUtil.runOnFxThreadAndWait(() -> chosenFile.set(fileChooser.showSaveDialog(null)));

    if (chosenFile.get() != null) {
      new MassvoltammogramExportTask(massvoltammogram, chosenFile.get());
    }
  }
}
