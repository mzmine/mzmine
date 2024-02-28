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

import com.google.common.collect.Range;
import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramAxisParameters;
import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramExportParameters;
import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramExportTask;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
  private final Image EDIT_AXIS_RANGES_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnScaleAxis.png");

  //The plot.
  MassvoltammogramPlotPanel plotPanel;
  PlotCanvas plotCanvas;

  //The massvoltammogram.
  Massvoltammogram massvoltammogram;

  public MassvoltammogramToolBar(MassvoltammogramPlotPanel plotPanel,
      Massvoltammogram massvoltammogram) {

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
    final Button editAxisRangesButton = new Button(null, new ImageView(EDIT_AXIS_RANGES_ICON));
    editAxisRangesButton.setTooltip(new Tooltip("Edit the massvoltammograms axis ranges."));
    editAxisRangesButton.setOnAction(e -> editAxisRanges());
    editAxisRangesButton.setMinSize(35, 35);

    setOrientation(Orientation.VERTICAL);
    getItems().addAll(moveButton, rotateButton, resetButton, editAxisRangesButton, exportButton);
    setStyle("-fx-background-color: white;");
  }

  /**
   * Exports the massvoltammogram to different file formats chosen by the user.
   */
  private void exportMassvoltammogram() {

    MassvoltammogramExportParameters exportParameters = new MassvoltammogramExportParameters();

    if (exportParameters.showSetupDialog(true) != ExitCode.OK) {
      return;
    }

    new MassvoltammogramExportTask(massvoltammogram, exportParameters);
  }

  /**
   * Sets the massvoltammogram-plots axis ranges to values chosen by the user.
   */
  private void editAxisRanges() {

    //Getting the input from the user.
    final MassvoltammogramAxisParameters mzRangeParameter = new MassvoltammogramAxisParameters();
    if (mzRangeParameter.showSetupDialog(true) != ExitCode.OK) {
      return;
    }

    Range<Double> mzRange = mzRangeParameter.getEmbeddedParameterValueIfSelectedOrElse(
        MassvoltammogramAxisParameters.mzRange, null);
    Double maxIntensity = mzRangeParameter.getEmbeddedParameterValueIfSelectedOrElse(
        MassvoltammogramAxisParameters.maxIntensity, null);

    //Editing the massvoltammogram-plots ranges.
    if (mzRange != null) {
      massvoltammogram.editMzRange(mzRange);
    }
    if (maxIntensity != null) {
      massvoltammogram.scalePlotsIntensityAxis(maxIntensity);
    }
  }
}
