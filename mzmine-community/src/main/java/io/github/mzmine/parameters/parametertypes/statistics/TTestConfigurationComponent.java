/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.SignificanceTests;
import io.github.mzmine.modules.dataanalysis.significance.UnivariateRowSignificanceTest;
import io.github.mzmine.parameters.ValuePropertyComponent;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingComponent;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

public class TTestConfigurationComponent extends GridPane implements
    ValuePropertyComponent<UnivariateRowSignificanceTestConfig> {

  private final ComboBox<SignificanceTests> samplingCombo;
  private final MetadataGroupingComponent metadataCombo;
  private final TextField groupACombo;
  private final TextField groupBCombo;


  /**
   * The value as selected in the gui. automatically updated on change. Value may be null, if an
   * invalid selection was made.
   */
  private final ObjectProperty<UnivariateRowSignificanceTestConfig> valueProperty = new SimpleObjectProperty<>();

  public TTestConfigurationComponent() {
    super(5, 5);

    samplingCombo = new ComboBox<>(
        FXCollections.observableList(List.of(SignificanceTests.univariateValues())));
    samplingCombo.getSelectionModel().select(0);

    // using an actual combo here. TTest is used for visualisation which is not available in headless mode.
    metadataCombo = new MetadataGroupingComponent();

    // todo check if this actually works or throws type exceptions.
    groupACombo = metadataCombo.createLinkedGroupCombo("Select first group (A) from column.");
    groupBCombo = metadataCombo.createLinkedGroupCombo("Select second group (B) from column.");

    metadataCombo.uniqueColumnValuesProperty()
        .addListener((ListChangeListener<? super String>) change -> {
          final ObservableList<? extends String> newOptions = change.getList();
          if (newOptions.size() < 2) {
            // this keeps the selection if an invalid metadata column was selected
            // necessary for the batch mode if no metadata was imported yet
            return;
          }

          groupACombo.setText(newOptions.getFirst());
          groupBCombo.setText(newOptions.getLast());
        });

    add(new Label("Metadata column"), 0, 0);
    add(metadataCombo, 1, 0);
    add(new Label("Test type"), 2, 0);
    add(samplingCombo, 3, 0);
    add(new Label("Group A"), 0, 1);
    add(groupACombo, 1, 1);
    add(new Label("Group B"), 2, 1);
    add(groupBCombo, 3, 1);

    // use a delay, because changes in the metadata column will automatically change the group
    // columns -> avoid multiple triggers of listeners to the value property.
    PropertyUtils.onChangeDelayedSubscription(this::updateValueProperty, Duration.millis(100),
        metadataCombo.valueProperty(), groupACombo.textProperty(), groupBCombo.textProperty(),
        samplingCombo.valueProperty());
  }

  private void updateValueProperty() {
    final String columnName = metadataCombo.getValue();
    final String a = groupACombo.getText();
    final String b = groupBCombo.getText();
    var sampling = samplingCombo.getValue();
    if (columnName == null || a == null || sampling == null || b == null) {
      return;
    }

    valueProperty.set(new UnivariateRowSignificanceTestConfig(sampling, columnName, a, b));
  }

  public UnivariateRowSignificanceTestConfig getValue() {
    updateValueProperty();
    return valueProperty.get();
  }

  @Override
  public ObjectProperty<UnivariateRowSignificanceTestConfig> valueProperty() {
    return valueProperty;
  }

  public void setTest(RowSignificanceTest test) {
    if (test == null) {
      setValue(null);
    } else if (test instanceof UnivariateRowSignificanceTest<?> uniTest) {
      setValue(uniTest.toConfiguration());
    } else {
      throw new IllegalArgumentException(
          "Unsupported test type in stats dashboard: " + test.getClass());
    }
  }

  public void setValue(UnivariateRowSignificanceTestConfig value) {
    if (value == null) {
      metadataCombo.setValue(null);
      groupACombo.setText(null);
      groupBCombo.setText(null);
      samplingCombo.setValue(null);
      return;
    }
    this.valueProperty.set(value);
    metadataCombo.setValue(value.column());
    groupACombo.setText(value.groupA());
    groupBCombo.setText(value.groupB());
    samplingCombo.setValue(value.samplingConfig());
  }
}
