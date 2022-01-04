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

package io.github.mzmine.parameters.dialogs;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeComponent;
import io.github.mzmine.util.RangeUtils;
import java.text.NumberFormat;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

/**
 * This class extends ParameterSetupDialog class, including a TICPlot. This is used to preview how
 * the selected raw data filters work.
 * <p>
 * Slightly modified to add the possibility of switching to TIC (versus Base Peak) preview.
 */
public abstract class ParameterSetupDialogWithChromatogramPreview extends
    ParameterSetupDialogWithPreview {

  // Dialog components
  private final BorderPane pnlPreviewFields = new BorderPane();
  private final ComboBox<RawDataFile> comboDataFileName;
  // TODO: FloatRangeComponent
  private final DoubleRangeComponent rtRangeBox = new DoubleRangeComponent(
      MZmineCore.getConfiguration().getRTFormat());
  private final DoubleRangeComponent mzRangeBox = new DoubleRangeComponent(
      MZmineCore.getConfiguration().getMZFormat());
  // Show as TIC
  private final ComboBox<TICPlotType> ticViewComboBox =
      new ComboBox<TICPlotType>(FXCollections.observableArrayList(TICPlotType.values()));
  private RawDataFile[] dataFiles;
  private RawDataFile previewDataFile;
  // XYPlot
  private TICPlot ticPlot;

  public ParameterSetupDialogWithChromatogramPreview(boolean valueCheckRequired,
      ParameterSet parameters) {
    super(valueCheckRequired, parameters);
    comboDataFileName = new ComboBox<>(FXCollections.observableList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentRawDataFiles()));

    dataFiles = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

    if (dataFiles.length > 0) {

      RawDataFile selectedFiles[] = MZmineCore.getDesktop().getSelectedDataFiles();

      if (selectedFiles.length > 0) {
        previewDataFile = selectedFiles[0];
      } else {
        previewDataFile = dataFiles[0];
      }
    }

    // Elements of pnlLab
    FlowPane pnlLab = new FlowPane(Orientation.VERTICAL);
    // pnlLab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    pnlLab.getChildren().add(new Label("Data file "));
    pnlLab.getChildren().add(new Label("Plot Type "));
    pnlLab.getChildren().add(new Label("RT range "));
    pnlLab.getChildren().add(new Label("m/z range "));

    // Elements of pnlFlds
    FlowPane pnlFlds = new FlowPane(Orientation.VERTICAL);

    comboDataFileName.getSelectionModel().select(previewDataFile);
    comboDataFileName.setOnAction(e -> {
      int ind = comboDataFileName.getSelectionModel().getSelectedIndex();
      if (ind >= 0) {
        previewDataFile = dataFiles[ind];
        parametersChanged();
      }
    });

    ticViewComboBox.getSelectionModel().select(TICPlotType.TIC);
    ticViewComboBox.setOnAction(e -> parametersChanged());

    // TODO: FloatRangeComponent
    rtRangeBox.setValue(Range
        .closed(previewDataFile.getDataRTRange(1).lowerEndpoint().doubleValue(),
            previewDataFile.getDataRTRange(1).lowerEndpoint().doubleValue()));
    mzRangeBox.setValue(previewDataFile.getDataMZRange(1));

    pnlFlds.getChildren().add(comboDataFileName);
    pnlFlds.getChildren().add(ticViewComboBox);
    pnlFlds.getChildren().add(rtRangeBox);
    pnlFlds.getChildren().add(mzRangeBox);

    // Put all together
    pnlPreviewFields.setLeft(pnlLab);
    pnlPreviewFields.setCenter(pnlFlds);
    pnlPreviewFields.setVisible(false);

    ticPlot = new TICPlot();

    previewWrapperPane.setBottom(pnlPreviewFields);
    previewWrapperPane.setCenter(ticPlot);
    setOnPreviewShown(() -> parametersChanged());
  }

  /**
   * Get the parameters related to the plot and call the function addRawDataFile() to add the data
   * file to the plot
   *
   * @param dataFile
   */
  protected abstract void loadPreview(TICPlot ticPlot, RawDataFile dataFile, Range<Float> rtRange,
      Range<Double> mzRange);

  private void updateTitle() {

    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    Range<Double> rtRange = rtRangeBox.getValue();
    Range<Double> mzRange = mzRangeBox.getValue();

    String title = "m/z: " + mzFormat.format(mzRange.lowerEndpoint()) + " - "
        + mzFormat.format(mzRange.upperEndpoint()) + ", RT: "
        + rtFormat.format(rtRange.lowerEndpoint()) + " - "
        + rtFormat.format(rtRange.upperEndpoint());

    // update plot title
    ticPlot.setTitle(previewDataFile.getName(), title);
  }


  public TICPlotType getPlotType() {
    return (ticViewComboBox.getSelectionModel().getSelectedItem());
  }

  public void setPlotType(TICPlotType plotType) {
    ticViewComboBox.getSelectionModel().select(plotType);
  }

  public RawDataFile getPreviewDataFile() {
    return this.previewDataFile;
  }

  @Override
  protected void parametersChanged() {

    // Update preview as parameters have changed
    if ((getPreviewCheckbox() == null) || (!getPreviewCheckbox().isSelected())) {
      return;
    }

    Range<Float> rtRange = RangeUtils.toFloatRange(rtRangeBox.getValue());
    Range<Double> mzRange = mzRangeBox.getValue();
    updateParameterSetFromComponents();

    loadPreview(ticPlot, previewDataFile, rtRange, mzRange);

    updateTitle();

  }

  public TICPlot getTicPlot() {
    return ticPlot;
  }

  public void setTicPlot(TICPlot ticPlot) {
    this.ticPlot = ticPlot;
  }

}
