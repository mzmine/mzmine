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

package io.github.mzmine.modules.visualization.msms;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.annotation.Nonnull;
import org.jfree.chart.util.SortOrder;

public class MsMsVisualizerTab extends MZmineTab {

  private final ParameterSet parameters;
  private final MsMsChart chart;
  private RawDataFile dataFile;

  private static final Image POINTS_ICON =
      FxIconUtil.loadImageFromResources("icons/pointsicon.png");

  private static final Image Z_ASC_ICON =
      FxIconUtil.loadImageFromResources("icons/msms_points_asc.png");

  private static final Image Z_DESC_ICON =
      FxIconUtil.loadImageFromResources("icons/msms_points_desc.png");

  // Highlighted precursor mz range
  Double minMzValue, maxMzValue;

  public MsMsVisualizerTab(ParameterSet parameters) {
    super("MS/MS Visualizer", true, false);

    this.parameters = parameters;
    dataFile = parameters.getParameter(MsMsParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];

    BorderPane borderPane = new BorderPane();
    setContent(borderPane);

    // Chart
    chart = new MsMsChart(parameters);
    borderPane.setCenter(chart);
    chart.datasetStatusProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == TaskStatus.FINISHED) {
        assert MZmineCore.getDesktop() != null;
        MZmineCore.getDesktop().addTab(this);
      }
    });

    // Axes selection
    ComboBox<MsMsXYAxisType> xAxisTypes = new ComboBox<>();
    xAxisTypes.getItems().addAll(MsMsXYAxisType.values());
    xAxisTypes.setValue(parameters.getParameter(MsMsParameters.xAxisType).getValue());
    xAxisTypes.setOnAction(event -> {
      chart.setDomainAxisLabel(xAxisTypes.getValue().toString());
      chart.setXAxisType(xAxisTypes.getValue());
    });
    ComboBox<MsMsXYAxisType> yAxisTypes = new ComboBox<>();
    yAxisTypes.getItems().addAll(MsMsXYAxisType.values());
    yAxisTypes.setValue(parameters.getParameter(MsMsParameters.yAxisType).getValue());
    yAxisTypes.setOnAction(event -> {
      chart.setRangeAxisLabel(yAxisTypes.getValue().toString());
      chart.setYAxisType(yAxisTypes.getValue());
    });
    ComboBox<MsMsZAxisType> zAxisTypes = new ComboBox<>();
    zAxisTypes.getItems().addAll(MsMsZAxisType.values());
    zAxisTypes.setValue(parameters.getParameter(MsMsParameters.zAxisType).getValue());
    zAxisTypes.setOnAction(event -> {
      chart.setZAxisType(zAxisTypes.getValue());
    });
    HBox bottomBox = new HBox();
    bottomBox.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY,
        Insets.EMPTY)));
    bottomBox.setPadding(new Insets(5, 5, 5, 5));
    bottomBox.setAlignment(Pos.BASELINE_CENTER);
    Text xAxisText = new Text("  X axis: ");
    xAxisText.setStyle("-fx-font-weight: bold");
    Text yAxisText = new Text("  Y axis: ");
    yAxisText.setStyle("-fx-font-weight: bold");
    Text zAxisText = new Text("  Z axis: ");
    zAxisText.setStyle("-fx-font-weight: bold");
    bottomBox.getChildren().addAll(new Separator(Orientation.VERTICAL), xAxisText, xAxisTypes,
        new Text("   "), new Separator(Orientation.VERTICAL), yAxisText, yAxisTypes,
        new Text("   "), new Separator(Orientation.VERTICAL), zAxisText, zAxisTypes,
        new Text("   "), new Separator(Orientation.VERTICAL));
    borderPane.setBottom(bottomBox);

    // Tool bar
    ToolBar toolBar = new ToolBar();
    toolBar.setOrientation(Orientation.VERTICAL);

    Button highlightMzButton = new Button(null, new ImageView(POINTS_ICON));
    highlightMzButton.setTooltip(new Tooltip("Highlight points with specific precursor m/z"));
    highlightMzButton.setOnAction(event -> highlightMzOnAction());

    Button sortZValuesAscButton = new Button(null, new ImageView(Z_ASC_ICON));
    sortZValuesAscButton.setTooltip(new Tooltip("Sort points by Z axis in ascending order"));
    sortZValuesAscButton.setOnAction(event -> chart.sortZValues(SortOrder.ASCENDING));

    Button sortZValuesDescButton = new Button(null, new ImageView(Z_DESC_ICON));
    sortZValuesDescButton.setTooltip(new Tooltip("Sort points by Z axis in descending order"));
    sortZValuesDescButton.setOnAction(event -> chart.sortZValues(SortOrder.DESCENDING));

    toolBar.getItems().addAll(highlightMzButton, new Separator(Orientation.VERTICAL),
        sortZValuesAscButton, sortZValuesDescButton);

    borderPane.setRight(toolBar);
  }

  /**
   * Creates new popup stage with m/z range input fields, calls {@link MsMsChart#highlightPrecursorMz(Range)}
   * if the input m/z range is valid and the "Apply button is pressed".
   */
  private void highlightMzOnAction() {

    // Create stage and fields
    Stage highlightDialog = new Stage();
    HBox highlightDialogBox = new HBox();
    highlightDialogBox.setAlignment(Pos.CENTER_LEFT);
    TextField minMzField = new TextField();
    TextField maxMzField = new TextField();
    minMzField.setPrefColumnCount(6);
    maxMzField.setPrefColumnCount(6);
    minMzField.setText(Objects.toString(minMzValue, ""));
    maxMzField.setText(Objects.toString(maxMzValue, ""));

    // Add elements to the main VBox
    highlightDialogBox.getChildren().addAll(new Text("m/z range: "), minMzField, new Text(" - "), maxMzField);
    VBox box = new VBox(5);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(10, 10, 10, 10));
    highlightDialog.setScene(new Scene(box));

    // Define buttons behaviour
    Button btnApply = new Button("Apply");
    Button btnCancel = new Button("Cancel");
    btnApply.setOnAction(e -> {
      try {
        minMzValue = Double.parseDouble(minMzField.getText());
        maxMzValue = Double.parseDouble(maxMzField.getText());
      } catch (NumberFormatException exception) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Input error");
        alert.setHeaderText("m/z range input is invalid");
        alert.showAndWait();
        return;
      }

      // Highlight plot chart points according to the input m/z range
      chart.highlightPrecursorMz(Range.closed(minMzValue, maxMzValue));
      highlightDialog.hide();
    });
    btnCancel.setOnAction(e -> highlightDialog.hide());
    ButtonBar.setButtonData(btnApply, ButtonData.APPLY);
    ButtonBar.setButtonData(btnCancel, ButtonData.CANCEL_CLOSE);
    ButtonBar btnBar = new ButtonBar();
    btnBar.getButtons().addAll(btnApply, btnCancel);
    box.getChildren().addAll(highlightDialogBox, btnBar);

    // Setup stage
    highlightDialog.setTitle("Precursor m/z range to highlight");
    highlightDialog.setResizable(false);
    highlightDialog.initModality(Modality.APPLICATION_MODAL);
    highlightDialog.getIcons().add(new Image("MZmineIcon.png"));
    highlightDialog.show();
  }

  @Nonnull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.singletonList(dataFile);
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    if (rawDataFiles == null || rawDataFiles.isEmpty()) {
      return;
    }

    // Get first raw data file
    RawDataFile newFile = rawDataFiles.iterator().next();

    if (dataFile.equals(newFile)) {
      return;
    }

    dataFile = newFile;
    chart.setDataFile(newFile);
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featurelists) {

  }
}
