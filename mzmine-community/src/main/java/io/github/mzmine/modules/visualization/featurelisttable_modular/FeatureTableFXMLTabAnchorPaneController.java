/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import static io.github.mzmine.javafx.components.factories.FxLabels.newLabelNoWrap;
import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.RangeUtils;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class FeatureTableFXMLTabAnchorPaneController {

  private static final Logger logger = Logger.getLogger(
      FeatureTableFXMLTabAnchorPaneController.class.getName());

  private static ParameterSet param;

  @FXML
  private SplitPane pnFilters;
  @FXML
  private SplitPane pnTablePreviewSplit;
  @FXML
  private FeatureTableFX featureTable;
  private TextField idSearchField;
  private TextField mzSearchField;
  private TextField rtSearchField;
  private TextField anySearchField;
  private ComboBox<DataType> typeComboBox;


  public void initialize() {
    param = MZmineCore.getConfiguration().getModuleParameters(FeatureTableFXModule.class);

    // Filters hbox
    HBox filtersRow = newHBox(new Insets(0));
    filtersRow.setSpacing(0);
    filtersRow.setAlignment(Pos.CENTER_LEFT);
    Separator separator = new Separator(Orientation.VERTICAL);

    // Filter icon
    ImageView filterIcon = new ImageView(FxIconUtil.loadImageFromResources("icons/filtericon.png"));

    // Search fields
    mzSearchField = new TextField();
    rtSearchField = new TextField();
    idSearchField = new TextField();
    mzSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterRows());
    rtSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterRows());
    idSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterRows());

    HBox mzFilter = newHBox(newLabelNoWrap("m/z:"), mzSearchField);
    mzFilter.setAlignment(filtersRow.getAlignment());
    HBox rtFilter = newHBox(newLabelNoWrap("RT:"), rtSearchField);
    rtFilter.setAlignment(filtersRow.getAlignment());
    HBox idFilter = newHBox(newLabelNoWrap("ID:"), idSearchField);
    idFilter.setAlignment(filtersRow.getAlignment());

    filtersRow.getChildren().addAll(filterIcon, idFilter, mzFilter, separator, rtFilter);

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
      if(newValue == null) {
        return;
      }
      typeComboBox.setItems(FXCollections.observableArrayList(newValue.getRowTypes()));
      if (!typeComboBox.getItems().isEmpty()) {
        typeComboBox.setValue(typeComboBox.getItems().get(0));
      }
    }));
    anySearchField = new TextField();
    anySearchField.textProperty().addListener((observable, oldValue, newValue) -> filterRows());
    filtersRow.getChildren()
        .addAll(new Separator(Orientation.VERTICAL), typeComboBox, new Label(": "), anySearchField);

    pnFilters.getItems().add(filtersRow);

    featureTable.getSelectionModel().selectedItemProperty()
        .addListener(((obs, o, n) -> selectedRowChanged()));
  }

  public FeatureTableFX getFeatureTable() {
    return featureTable;
  }

  private void filterRows() {
    // Parse input text fields
    Set<String> idFilter = Arrays.stream(idSearchField.getText().split(","))
        .filter(s -> !s.isBlank()).collect(Collectors.toSet());
    Range<Double> mzFilter = parseNumericFilter(mzSearchField, 5e-5);
    Range<Double> rtFilter = parseNumericFilter(rtSearchField, 5e-3);

    // Filter strings are invalid, do nothing
    if (RangeUtils.isNaNRange(mzFilter) || RangeUtils.isNaNRange(rtFilter)) {
      return;
    }

    final String anyFilterString =
        anySearchField.getText().isBlank() ? null : anySearchField.getText().toLowerCase().trim();
    DataType<?> type = typeComboBox.getValue();

    // Filter rows
    featureTable.getFilteredRowItems().setPredicate(item -> {
      ModularFeatureListRow row = item.getValue();
      boolean anyFilterOk = true;
      if (anyFilterString != null && type != null) {
        Object value = row.get(type);
        anyFilterOk = value != null && type.getFormattedStringCheckType(value).toLowerCase().trim()
            .contains(anyFilterString);
      }
      if (idFilter.size() > 0 && !idFilter.contains(String.valueOf(row.getID()))) {
        return false;
      }

      Double mz = row.getAverageMZ();
      Float rt = row.getAverageRT();
      return (mz == null || mzFilter.contains(mz)) && (rt == null || rtFilter.contains(
          rt.doubleValue())) && anyFilterOk;
    });

    // Update rows in feature table
    featureTable.getRoot().getChildren().clear();
    featureTable.getRoot().getChildren().addAll(featureTable.getFilteredRowItems());
  }

  public TextField getIdSearchField() {
    return idSearchField;
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
    String filterStr = textField.getText();
    filterStr = filterStr.replace(" ", "");

    if (filterStr.isEmpty()) { // Empty filter
      textField.setStyle("-fx-prompt-text-fill: derive(-fx-text-fill, -50%);");
      return RangeUtils.DOUBLE_INFINITE_RANGE;
    } else if (filterStr.contains("-") && filterStr.indexOf("-") > 0) { // Filter by range
      try {
        Range<Double> parsedRange = RangeUtils.parseDoubleRange(filterStr);
        textField.setStyle("-fx-control-inner-background: " + FxColorUtil.colorToHex(
            MZmineCore.getConfiguration().getDefaultPaintScalePalette().getPositiveColor()));
        return Range.closed(parsedRange.lowerEndpoint() - epsilon,
            parsedRange.upperEndpoint() + epsilon);
      } catch (Exception exception) {
        textField.setStyle("-fx-control-inner-background: " + FxColorUtil.colorToHex(
            MZmineCore.getConfiguration().getDefaultPaintScalePalette().getNegativeColor()));
        return RangeUtils.DOUBLE_NAN_RANGE;
      }
    } else { // Filter by single value
      try {
        return RangeUtils.getRangeToCeilDecimal(filterStr);
      } catch (Exception exception) {
        textField.setStyle("-fx-control-inner-background: " + FxColorUtil.colorToHex(
            MZmineCore.getConfiguration().getDefaultPaintScalePalette().getNegativeColor()));
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
        mzSearchField.setPromptText(
            mzFormat.format(mzRange.lowerEndpoint()) + " - " + mzFormat.format(
                mzRange.upperEndpoint()));
      }
      mzSearchField.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");

      NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
      Range<Float> rtRange = featureList.getRowsRTRange();
      if (rtRange != null) {
        rtSearchField.setPromptText(
            rtFormat.format(rtRange.lowerEndpoint()) + " - " + rtFormat.format(
                rtRange.upperEndpoint()));
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

  public void close() {
    featureTable.closeTable();
  }

  public void setRtFilterString(String filterString) {
    rtSearchField.setText(filterString);
  }

  public void setMzFilterString(String filterString) {
    mzSearchField.setText(filterString);
  }
}
