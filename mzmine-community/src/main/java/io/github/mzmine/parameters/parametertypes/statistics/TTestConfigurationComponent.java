/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.statistics;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.ttest.TTestSamplingConfig;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ValuePropertyComponent;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

public class TTestConfigurationComponent extends GridPane implements
    ValuePropertyComponent<StorableTTestConfiguration> {

  private final ComboBox<TTestSamplingConfig> samplingCombo;
  private final ComboBox<String> metadataCombo;
  private final ComboBox<String> groupACombo;
  private final ComboBox<String> groupBCombo;

  private final PauseTransition delay = new PauseTransition(new Duration(100));

  /**
   * The value as selected in the gui. automatically updated on change. Value may be null, if an
   * invalid selection was made.
   */
  private final ObjectProperty<StorableTTestConfiguration> valueProperty = new SimpleObjectProperty<>();

  public TTestConfigurationComponent() {
    super(5, 5);

    samplingCombo = new ComboBox<>(
        FXCollections.observableList(List.of(TTestSamplingConfig.values())));
    samplingCombo.getSelectionModel().select(TTestSamplingConfig.UNPAIRED);

    // using an actual combo here. TTest is used for visualisation which is not available in headless mode.
    final MetadataTable metadata = MZmineCore.getProjectMetadata();
    metadataCombo = new ComboBox<>(FXCollections.observableList(
        metadata.getColumns().stream().map(MetadataColumn::getTitle).sorted().toList()));

    // todo check if this actually works or throws type exceptions.
    groupACombo = new ComboBox<>();
    groupBCombo = new ComboBox<>();
    metadataCombo.setEditable(true);
    groupACombo.setEditable(true);
    groupBCombo.setEditable(true);

    metadataCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, newColName) -> {
      final MetadataTable meta = MZmineCore.getProjectMetadata();
      var col = meta.getColumnByName(newColName);
      if (col == null) {
        final String a = groupACombo.getSelectionModel().getSelectedItem();
        final String b = groupBCombo.getSelectionModel().getSelectedItem();
        groupACombo.getSelectionModel().clearSelection();
        groupBCombo.getSelectionModel().clearSelection();
        groupACombo.setItems(FXCollections.observableList(List.of()));
        groupBCombo.setItems(FXCollections.observableList(List.of()));
        // this keeps the selection if an invalid metadata column was selected, but removes all the other choices
        // necessary for the batch mode if no metadata was imported yet
        groupACombo.getSelectionModel().select(a);
        groupBCombo.getSelectionModel().select(b);
        return;
      }
      final List<?> distinctColumnValues = meta.getDistinctColumnValues(col);

      final ObservableList<String> observableValues = FXCollections.observableList(
          distinctColumnValues.stream().map(Object::toString).toList());
      groupACombo.setItems(observableValues);
      groupBCombo.setItems(observableValues);
      groupACombo.getSelectionModel().selectFirst();
      groupBCombo.getSelectionModel().selectLast();
    });

    add(new Label("Metadata column"), 0, 0);
    add(metadataCombo, 1, 0);
    add(new Label("Sample type"), 2, 0);
    add(samplingCombo, 3, 0);
    add(new Label("Group A"), 0, 1);
    add(groupACombo, 1, 1);
    add(new Label("Group B"), 2, 1);
    add(groupBCombo, 3, 1);

    // use a delay, because changes in the metadata column will automatically change the group
    // columns -> avoid multiple triggers of listeners to the value property.
    delay.setOnFinished(__ -> updateValueProperty());

    // update value on change
    metadataCombo.valueProperty().addListener(__ -> delay.playFromStart());
    groupACombo.valueProperty().addListener(__ -> delay.playFromStart());
    groupBCombo.valueProperty().addListener(__ -> delay.playFromStart());
    samplingCombo.valueProperty().addListener(__ -> delay.playFromStart());
  }

  private void updateValueProperty() {
    final String columnName = metadataCombo.getValue();
    final String a = groupACombo.getValue();
    final String b = groupBCombo.getValue();
    var sampling = samplingCombo.getValue();
    if (columnName == null || a == null || sampling == null || b == null) {
      return;
    }

    valueProperty.set(new StorableTTestConfiguration(sampling, columnName, a, b));
  }

  public StorableTTestConfiguration getValue() {
    updateValueProperty();
    return valueProperty.get();
  }

  @Override
  public ObjectProperty<StorableTTestConfiguration> valueProperty() {
    return valueProperty;
  }

  public void setValue(StorableTTestConfiguration value) {
    this.valueProperty.set(value);
    metadataCombo.setValue(value.column());
    groupACombo.setValue(value.groupA());
    groupBCombo.setValue(value.groupB());
    samplingCombo.setValue(value.samplingConfig());
  }
}
