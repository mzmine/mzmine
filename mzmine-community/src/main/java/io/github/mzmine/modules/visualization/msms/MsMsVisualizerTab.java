/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.msms;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.parameters.ParameterSet;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.util.SortOrder;

public class MsMsVisualizerTab extends MZmineTab {

  private final ParameterSet parameters;
  private final MsMsChart chart;
  private RawDataFile dataFile;

  private static final Image POINTS_ICON =
      FxIconUtil.loadImageFromResources("icons/pointsicon.png");

  private static final Image Z_ASC_ICON = FxIconUtil.loadImageFromResources(
      "icons/msms_points_asc.png");

  private static final Image Z_DESC_ICON = FxIconUtil.loadImageFromResources(
      "icons/msms_points_desc.png");

  // Highlighted points utility variables
  Range<Double> range1, range2;
  MsMsXYAxisType valueType1, valueType2;

  public MsMsVisualizerTab(RawDataFile[] raws) {
    this(MsMsParameters.getDefaultMsMsPrecursorParameters(raws));
  }

  public MsMsVisualizerTab(ParameterSet parameters) {
    super("MS/MS scatter plot", true, false);

    this.parameters = parameters;
    dataFile = parameters.getParameter(MsMsParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];

    BorderPane borderPane = new BorderPane();
    setContent(borderPane);

    // Chart
    chart = new MsMsChart(parameters);
    borderPane.setCenter(chart);

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
    bottomBox.setBackground(
        new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
    bottomBox.setPadding(new Insets(5, 5, 5, 5));
    bottomBox.setAlignment(Pos.BASELINE_CENTER);
    Text xAxisText = new Text("  X axis: ");
    xAxisText.setStyle("-fx-font-weight: bold");
    Text yAxisText = new Text("  Y axis: ");
    yAxisText.setStyle("-fx-font-weight: bold");
    Text zAxisText = new Text("  Z axis: ");
    zAxisText.setStyle("-fx-font-weight: bold");
    bottomBox.getChildren()
        .addAll(xAxisText, xAxisTypes, new Text("   "), yAxisText, yAxisTypes, new Text("   "),
            zAxisText, zAxisTypes, new Text("   "));
    borderPane.setBottom(bottomBox);

    // Tool bar
    ToolBar toolBar = new ToolBar();
    toolBar.setOrientation(Orientation.VERTICAL);

    Button highlightButton = new Button(null, new ImageView(POINTS_ICON));
    highlightButton.setTooltip(new Tooltip("Highlight points with specific X and Y values"));
    highlightButton.setOnAction(event -> highlightPointsOnAction());

    Button sortZValuesAscButton = new Button(null, new ImageView(Z_ASC_ICON));
    sortZValuesAscButton.setTooltip(new Tooltip("Sort points by Z axis in ascending order"));
    sortZValuesAscButton.setOnAction(event -> chart.sortZValues(SortOrder.ASCENDING));

    Button sortZValuesDescButton = new Button(null, new ImageView(Z_DESC_ICON));
    sortZValuesDescButton.setTooltip(new Tooltip("Sort points by Z axis in descending order"));
    sortZValuesDescButton.setOnAction(event -> chart.sortZValues(SortOrder.DESCENDING));

    toolBar.getItems().addAll(highlightButton, new Separator(Orientation.VERTICAL),
        sortZValuesAscButton, sortZValuesDescButton);

    borderPane.setRight(toolBar);
  }

  /**
   * Creates new popup stage with input fields for 2 ranges, calls
   * {@link MsMsChart#highlightPoints(MsMsXYAxisType, Range, MsMsXYAxisType, Range)}
   * if the ranges are valid and the Apply button or Enter key are pressed.
   */
  private void highlightPointsOnAction() {

    // Create stage and fields
    Stage highlightDialog = new Stage();
    HBox hbox1 = new HBox();
    hbox1.setAlignment(Pos.CENTER_LEFT);
    HBox hbox2 = new HBox();
    hbox2.setAlignment(Pos.CENTER_LEFT);

    // Ranges test fields
    TextField minField1 = new TextField();
    TextField maxField1 = new TextField();
    TextField minField2 = new TextField();
    TextField maxField2 = new TextField();
    minField1.setPrefColumnCount(6);
    maxField1.setPrefColumnCount(6);
    minField2.setPrefColumnCount(6);
    maxField2.setPrefColumnCount(6);
    minField1.setText(range1 == null ? "" : Objects.toString(range1.lowerEndpoint()));
    maxField1.setText(range1 == null ? "" : Objects.toString(range1.upperEndpoint()));
    minField2.setText(range2 == null ? "" : Objects.toString(range2.lowerEndpoint()));
    maxField2.setText(range2 == null ? "" : Objects.toString(range2.upperEndpoint()));

    // Combo boxes
    ComboBox<MsMsXYAxisType> combo1 = new ComboBox<>();
    combo1.getItems().addAll(MsMsXYAxisType.values());
    combo1.setValue(valueType1 != null ? valueType1 : chart.getXAxisType());
    ComboBox<MsMsXYAxisType> combo2 = new ComboBox<>();
    combo2.getItems().addAll(MsMsXYAxisType.values());
    combo2.setValue(valueType2 != null ? valueType2 : chart.getYAxisType());

    // Clear ranges buttons
    Button clearBtn1 = new Button("Clear");
    Button clearBtn2 = new Button("Clear");
    clearBtn1.setOnAction(e -> {
      minField1.setText("");
      maxField1.setText("");
    });
    clearBtn2.setOnAction(e -> {
      minField2.setText("");
      maxField2.setText("");
    });

    // Set text fields frames colors according to the highlighting color
    minField1.setStyle("-fx-text-box-border: #ff2121; -fx-focus-color: #ff2121");
    maxField1.setStyle("-fx-text-box-border: #ff2121; -fx-focus-color: #ff2121");
    minField2.setStyle("-fx-text-box-border: #d01ff2; -fx-focus-color: #d01ff2");
    maxField2.setStyle("-fx-text-box-border: #d01ff2; -fx-focus-color: #d01ff2");

    // Add elements to the main VBox
    hbox1.getChildren().addAll(combo1, new Text(" "), minField1, new Text(" - "), maxField1,
        new Text(" "), clearBtn1);
    hbox2.getChildren().addAll(combo2, new Text(" "), minField2, new Text(" - "), maxField2,
        new Text(" "), clearBtn2);
    VBox box = new VBox(5);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(10, 10, 10, 10));
    highlightDialog.setScene(new Scene(box));

    // Create apply and cancel buttons and add them to the button bar
    Button applyBtn = new Button("Apply");
    Button cancelBtn = new Button("Cancel");
    applyBtn.setOnAction(e -> {

      // Try to parse fields, show alert window if input values are invalid or highlight plot chart
      // points if the values are valid
      try {
        if (minField1.getText().equals("") && maxField1.getText().equals("")) {
          valueType1 = null;
          range1 = null;
        } else {
          valueType1 = combo1.getValue();
          range1 = Range.closed(Double.parseDouble(minField1.getText()),
              Double.parseDouble(maxField1.getText()));
        }

        if (minField2.getText().equals("") && maxField2.getText().equals("")) {
          valueType2 = null;
          range2 = null;
        } else {
          valueType2 = combo2.getValue();
          range2 = Range.closed(Double.parseDouble(minField2.getText()),
              Double.parseDouble(maxField2.getText()));
        }
      } catch (NumberFormatException exception) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Input error");
        alert.setHeaderText("Invalid input");
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
        return;
      }

      chart.highlightPoints(valueType1, range1, valueType2, range2);
      highlightDialog.hide();
    });
    cancelBtn.setOnAction(e -> highlightDialog.hide());
    highlightDialog.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ENTER) {
        applyBtn.fire();
      }
    });
    ButtonBar.setButtonData(applyBtn, ButtonData.APPLY);
    ButtonBar.setButtonData(cancelBtn, ButtonData.CANCEL_CLOSE);
    ButtonBar btnBar = new ButtonBar();
    btnBar.getButtons().addAll(applyBtn, cancelBtn);
    box.getChildren().addAll(hbox1, hbox2, btnBar);

    // Setup stage
    highlightDialog.setTitle("Values ranges to highlight");
    highlightDialog.setResizable(false);
    highlightDialog.initModality(Modality.APPLICATION_MODAL);
    highlightDialog.getIcons().add(new Image("MZmineIcon.png"));
    highlightDialog.show();

    minField1.requestFocus();
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.singletonList(dataFile);
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @NotNull
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
      Collection<? extends FeatureList> featureLists) {

  }
}
