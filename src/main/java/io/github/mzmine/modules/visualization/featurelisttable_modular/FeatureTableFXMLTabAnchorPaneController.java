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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.massql.MassQLQuery;
import io.github.mzmine.modules.tools.massql.MassQLTextField;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.webapi.MassQLUtils;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class FeatureTableFXMLTabAnchorPaneController {

  private static final Logger logger = Logger
      .getLogger(FeatureTableFXMLTabAnchorPaneController.class.getName());

  private static ParameterSet param;

  @FXML
  private SplitPane pnFilters;
  @FXML
  private SplitPane pnTablePreviewSplit;
  @FXML
  private FeatureTableFX featureTable;

  private TextField mzSearchField;
  private TextField rtSearchField;
  private TextField anySearchField;
  private ComboBox<DataType> typeComboBox;

  // filter table by mass QL commands
  private MassQLTextField massQLSearchField;

  public void initialize() {
    param = MZmineCore.getConfiguration().getModuleParameters(FeatureTableFXModule.class);

    // Filters hbox
    HBox filtersRow = new HBox();
    filtersRow.setAlignment(Pos.CENTER_LEFT);
    filtersRow.setSpacing(10.0);
    Separator separator = new Separator(Orientation.VERTICAL);

    // Filter icon
    ImageView filterIcon
        = new ImageView(FxIconUtil.loadImageFromResources("icons/filtericon.png"));

    // Search fields
    mzSearchField = new TextField();
    //mzSearchField.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
    rtSearchField = new TextField();
    // Add filter text fields listeners to filter on air
    mzSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterRows());
    rtSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterRows());
    HBox mzFilter = new HBox(new Label("m/z: "), mzSearchField);
    mzFilter.setAlignment(filtersRow.getAlignment());
    HBox rtFilter = new HBox(new Label("RT: "), rtSearchField);
    rtFilter.setAlignment(filtersRow.getAlignment());

    filtersRow.getChildren().addAll(filterIcon, mzFilter, separator, rtFilter);

    typeComboBox = new ComboBox<>();
    /*typeComboBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(DataType object) {
        if (object == null) {
          return "";
        }
        return object.getHeaderString();
      }

      @Override
      public DataType fromString(String string) {
        return null;
      }
    });*/

    typeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> filterRows());
    featureTable.featureListProperty().addListener(((observable, oldValue, newValue) -> {
      typeComboBox.setItems(
          FXCollections.observableArrayList(newValue.getRowTypes().values()));
      if (!typeComboBox.getItems().isEmpty()) {
        typeComboBox.setValue(typeComboBox.getItems().get(0));
      }
    }));
    anySearchField = new TextField();
    anySearchField.textProperty().addListener((observable, oldValue, newValue) -> filterRows());
    filtersRow.getChildren()
        .addAll(new Separator(Orientation.VERTICAL), typeComboBox, new Label(": "), anySearchField);

    // add massql filter
    massQLSearchField = new MassQLTextField();
    massQLSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterRows());
    HBox massQLFilter = new HBox(new Label("MassQL: "), massQLSearchField);
    massQLFilter.setAlignment(filtersRow.getAlignment());

    VBox pnSouth = new VBox(2, filtersRow, massQLFilter);
    pnSouth.setFillWidth(true);
    HBox.setHgrow(massQLSearchField, Priority.ALWAYS);
    pnFilters.getItems().add(pnSouth);

    featureTable.getSelectionModel().selectedItemProperty()
        .addListener(((obs, o, n) -> selectedRowChanged()));
  }

  private void filterRows() {
    // Parse input text fields
    Range<Double> mzFilter = parseNumericFilter(mzSearchField, 5e-5);
    Range<Double> rtFilter = parseNumericFilter(rtSearchField, 5e-3);

    // Filter strings are invalid, do nothing
    if (RangeUtils.isNaNRange(mzFilter) || RangeUtils.isNaNRange(rtFilter)) {
      return;
    }

    final String anyFilterString =
        anySearchField.getText().isBlank() ? null : anySearchField.getText().toLowerCase().trim();
    DataType<?> type = typeComboBox.getValue();

    // filter by MassQL
    final MassQLQuery massQlFilter = massQLSearchField.getQuery();

    // Filter rows
    featureTable.getFilteredRowItems().setPredicate(item -> {
      ModularFeatureListRow row = item.getValue();
      boolean anyFilterOk = true;
      if (anyFilterString != null && type != null) {
        Property<?> property = row.get(type);
        anyFilterOk =
            property != null && type.getFormattedString(property.getValue()).toLowerCase().trim()
                .contains(anyFilterString);
      }

      return mzFilter.contains(row.getAverageMZ())
             && rtFilter.contains((double) row.getAverageRT())
             && anyFilterOk
             && massQlFilter.accept(row);
    });

    // Update rows in feature table
    featureTable.getRoot().getChildren().clear();
    featureTable.getRoot().getChildren().addAll(featureTable.getFilteredRowItems());
  }

  /**
   * Parses string of the given filter text field and returns a range of values satisfying the
   * filter. Examples: "5.34" -> [5.34 - epsilon, 5.34 + epsilon] "2.37 - 6" -> [2.37 - epsilon,
   * 6.00 + epsilon]
   *
   * @param textField Text field
   * @param epsilon   Precision of the filter
   * @return Range of values satisfying the filter or RangeUtils.DOUBLE_NAN_RANGE if the filter
   * string is invalid
   */
  private Range<Double> parseNumericFilter(TextField textField, double epsilon) {
    textField.setStyle("-fx-control-inner-background: #ffffff;");
    String filterStr = textField.getText();
    filterStr = filterStr.replace(" ", "");

    if (filterStr.isEmpty()) { // Empty filter
      textField.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
      return RangeUtils.DOUBLE_INFINITE_RANGE;
    } else if (filterStr.contains("-") && filterStr.indexOf("-") > 0) { // Filter by range
      try {
        Range<Double> parsedRange = RangeUtils.parseDoubleRange(filterStr);
        return Range
            .closed(parsedRange.lowerEndpoint() - epsilon, parsedRange.upperEndpoint() + epsilon);
      } catch (Exception exception) {
        textField.setStyle("-fx-control-inner-background: #ffcccb;");
        return RangeUtils.DOUBLE_NAN_RANGE;
      }
    } else { // Filter by single value
      try {
        return RangeUtils.getRangeToCeilDecimal(filterStr);
      } catch (Exception exception) {
        textField.setStyle("-fx-control-inner-background: #ffcccb;");
        return RangeUtils.DOUBLE_NAN_RANGE;
      }
    }
  }

  @FXML
  public void miParametersOnAction(ActionEvent event) {
    Platform.runLater(() -> {
      ExitCode exitCode = param.showSetupDialog(true);
      if (exitCode == ExitCode.OK) {
        updateWindowToParameterSetValues();
      }
    });
  }

  /**
   * In case the parameters are changed in the setup dialog, they are applied to the window.
   */
  void updateWindowToParameterSetValues() {
    featureTable.updateColumnsVisibilityParameters(
        param.getParameter(FeatureTableFXParameters.showRowTypeColumns).getValue(),
        param.getParameter(FeatureTableFXParameters.showFeatureTypeColumns).getValue());
  }

  public FeatureList getFeatureList() {
    return featureTable.getFeatureList();
  }

  public void setFeatureList(FeatureList featureList) {
    if (!(featureList instanceof ModularFeatureList flist)) {
      return;
    }
    featureTable.setFeatureList(flist);

    try {
      // Fill filters text fields with a prompt values
      NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
      Range<Double> mzRange = featureList.getRowsMZRange();
      if (mzRange != null) {
        mzSearchField.setPromptText(mzFormat.format(mzRange.lowerEndpoint()) + " - "
                                    + mzFormat.format(mzRange.upperEndpoint()));
      }
      mzSearchField.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");

      NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
      Range<Float> rtRange = featureList.getRowsRTRange();
      if (rtRange != null) {
        rtSearchField.setPromptText(rtFormat.format(rtRange.lowerEndpoint()) + " - "
                                    + rtFormat.format(rtRange.upperEndpoint()));
      }
      rtSearchField.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Error in table visualization", ex);
    }
  }

  void selectedRowChanged() {
    TreeItem<ModularFeatureListRow> selectedItem = featureTable.getSelectionModel()
        .getSelectedItem();
//    featureTable.getColumns().forEach(c -> logger.info(c.getText()));
    if (!featureTable.getSelectionModel().getSelectedCells().isEmpty()) {
      logger.info(
          "selected: " + featureTable.getSelectionModel().getSelectedCells().get(0).getTableColumn()
              .getText());
    }

    if (selectedItem == null) {
      return;
    }

    FeatureListRow selectedRow = selectedItem.getValue();
    if (selectedRow == null) {
      return;
    }
  }
}
