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

package io.github.mzmine.modules.visualization.massvoltammogram;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.util.Collection;
import java.util.List;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

public class MassvoltammogramTab extends MZmineTab {

  private final Image MOVE_PLOT_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnMove.png");
  private final Image RESET_PLOT_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnReset.png");
  private final Image ROTATE_PLOT_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnRotate.png");
  private final Image EXPORT_PLOT_ICON = FxIconUtil.loadImageFromResources("icons/exporticon.png");
  private final Image EDIT_MZ_RANGE_ICON = FxIconUtil.loadImageFromResources(
      "icons/massvoltammogram/btnEditMzRange.png");

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return List.of();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return List.of();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return List.of();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  private ExtendedPlot3DPanel plot;

  public MassvoltammogramTab(String title, ExtendedPlot3DPanel plot, String filename) {
    super(title);

    this.plot = plot;

    final ExtendedPlotToolBar plotToolBar = plot.getExtendedPlotToolBar();
    final BorderPane mainPane = new BorderPane();

    //Converting the swing object to JavaFX.
    final SwingNode swingNodePlot = new SwingNode();
    swingNodePlot.setContent(this.plot);

    //Creating a button to move the plot.
    final ToggleButton moveButton = new ToggleButton(null, new ImageView(MOVE_PLOT_ICON));
    moveButton.setTooltip(new Tooltip("Move the massvoltammogram."));
    moveButton.setOnAction(e -> plotToolBar.moveButton.doClick());
    moveButton.setMinSize(35, 35);

    //Creating a Button to rotate the plot.
    final ToggleButton rotateButton = new ToggleButton(null, new ImageView(ROTATE_PLOT_ICON));
    rotateButton.setSelected(true);
    rotateButton.setTooltip(new Tooltip("Rotate the massvoltammogram."));
    rotateButton.setOnAction(e -> plotToolBar.rotateButton.doClick());
    rotateButton.setMinSize(35, 35);

    //Connecting the move and rotate buttons in a toggle group
    final ToggleGroup toggleGroup = new ToggleGroup();
    moveButton.setToggleGroup(toggleGroup);
    rotateButton.setToggleGroup(toggleGroup);

    //Creating a button to reset the zoom.
    final Button resetButton = new Button(null, new ImageView(RESET_PLOT_ICON));
    resetButton.setTooltip(new Tooltip("Reset the view."));
    resetButton.setOnAction(e -> plotToolBar.resetPlotButton.doClick());
    resetButton.setMinSize(35, 35);

    //Creating a button to export the plot.
    final Button exportButton = new Button(null, new ImageView(EXPORT_PLOT_ICON));
    exportButton.setTooltip(new Tooltip("Export the massvoltammogram."));
    exportButton.setOnAction(e -> MassvoltammogramExport.exportPlot(this.plot));
    exportButton.setMinSize(35, 35);

    //Creating a button to edit the m/z-range.
    Button editMzRangeButton = new Button(null, new ImageView(EDIT_MZ_RANGE_ICON));
    editMzRangeButton.setTooltip(new Tooltip("Edit the massvoltammograms m/z-range."));
    editMzRangeButton.setOnAction(e -> {
      //Extracting the new m/z range from the list of raw scans.
      editMzRange(plot);
      //Exchanging the old plot for the new one.
      swingNodePlot.setContent(this.plot);
    });
    editMzRangeButton.setMinSize(35, 35);

    //Creating a new toolbar and adding the buttons.
    final ToolBar toolbar = new ToolBar();
    toolbar.setOrientation(Orientation.VERTICAL);
    toolbar.getItems()
        .addAll(moveButton, rotateButton, resetButton, exportButton, editMzRangeButton);
    toolbar.setStyle("-fx-background-color: white;");

    //Adding lable to identify the different massvoltammograms
    Label fileNameLable = new Label(filename);
    fileNameLable.setStyle("-fx-background-color: white;");
    fileNameLable.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

    //Adding the toolbar and the plot to the pane.
    mainPane.setCenter(swingNodePlot);
    mainPane.setRight(toolbar);
    mainPane.setBottom(fileNameLable);

    //Setting the pane as the MassvoltammogramTabs content.
    setContent(mainPane);
  }

  /**
   * Opens up a dialog to enter a new m/z-range and exchanges the plots data with the new spectra
   * within the given range.
   *
   * @param plot The plot to be updated.
   */
  public void editMzRange(ExtendedPlot3DPanel plot) {

    //Getting user input for the new m/z-Range.
    final MassvoltammogramMzRangeParameter mzRangeParameter = new MassvoltammogramMzRangeParameter();
    if (mzRangeParameter.showSetupDialog(true) != ExitCode.OK) {
      return;
    }
    final Range<Double> newMzRange = mzRangeParameter.getValue(
        MassvoltammogramMzRangeParameter.mzRange);

    //Getting the raw data from the plot.
    final List<double[][]> scans = plot.getRawScans();

    //Processing the raw data.
    List<double[][]> spectra = MassvoltammogramUtils.extractMZRangeFromScan(scans, newMzRange);
    final double maxIntensity = MassvoltammogramUtils.getMaxIntensity(spectra);
    final List<double[][]> spectraWithoutNoise = MassvoltammogramUtils.removeNoise(spectra,
        maxIntensity);
    final List<double[][]> spectraWithoutZeros = MassvoltammogramUtils.removeExcessZeros(
        spectraWithoutNoise);

    //Adding the new list of scans to the plot for later export.
    plot.addRawScansInMzRange(spectra);

    //Getting the divisor and the min and max potential to set up the axis correctly.
    final double divisor = MassvoltammogramUtils.getDivisor(maxIntensity);
    final double[][] firstScan = scans.get(0);
    final double[][] lastScan = scans.get(scans.size() - 1);
    final double maxPotential = Math.max(firstScan[0][2], lastScan[0][2]);
    final double minPotential = Math.min(firstScan[0][2], lastScan[0][2]);

    //Removing the old line plots from the plot panel
    plot.removeAllPlots();

    //Adding the new plots and setting the axis up correctly.
    MassvoltammogramUtils.addSpectraToPlot(spectraWithoutZeros, divisor, plot);
    plot.setFixedBounds(0, newMzRange.lowerEndpoint(), newMzRange.upperEndpoint());
    plot.setFixedBounds(1, minPotential, maxPotential);

    this.plot = plot;
  }
}
