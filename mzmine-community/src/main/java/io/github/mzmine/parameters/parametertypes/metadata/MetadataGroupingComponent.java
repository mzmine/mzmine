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

package io.github.mzmine.parameters.parametertypes.metadata;

import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataTab;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ValuePropertyComponent;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetadataGroupingComponent extends HBox implements
    ValuePropertyComponent<MetadataColumn<?>> {

  private final List<AvailableTypes> availableTypes;

  private final ObjectProperty<@Nullable MetadataColumn<?>> columnProperty = new SimpleObjectProperty<>();
  private final TextField columnField;
  private final StringProperty columnName = new SimpleStringProperty("");
  private final @NotNull ObservableList<String> uniqueColumnValues = FXCollections.observableArrayList();

  public MetadataGroupingComponent() {
    this(AvailableTypes.values());
  }

  public MetadataGroupingComponent(AvailableTypes[] availableTypes) {
    this(List.of(availableTypes));
  }

  public MetadataGroupingComponent(List<AvailableTypes> types) {
    super();
    setSpacing(FxLayout.DEFAULT_SPACE);
    setPadding(Insets.EMPTY);
    FxLayout.applyDefaults(this, Insets.EMPTY);
    this.availableTypes = types;

    // find all columns at time of creation
    final List<String> columns = ProjectService.getMetadata().getColumns().stream()
        .filter(col -> availableTypes.contains(col.getType())).map(MetadataColumn::getTitle)
        .toList();

    // better use a textfield for this
    // editable combobox behaves weird when entered values that are not within the items
    columnField = FxTextFields.newAutoGrowTextField(columnName, "",
        "Define a column in the sample metadata.", 4, 20);
    FxTextFields.bindAutoCompletion(columnField, columns);

    var icon = FxIconUtil.newIconButton(FxIcons.METADATA_TABLE,
        "Open project metadata. (Import data files and metadata to then modify project metadata)",
        () -> {
          MZmineCore.getDesktop().addTab(new ProjectMetadataTab());
        });

    getChildren().addAll(columnField, icon);

    Bindings.bindBidirectional(columnField.textProperty(), valueProperty(),
        new StringConverter<>() {
          @Override
          public String toString(MetadataColumn<?> object) {
            return object != null ? object.getTitle() : "";
          }

          @Override
          public MetadataColumn<?> fromString(String string) {
            return ProjectService.getMetadata().getColumnByName(string);
          }
        });

    // find unique values in column
    valueProperty().subscribe(nv -> {
      if (nv == null) {
        uniqueColumnValues.clear();
        return;
      }
      final List<?> uniqueValues = ProjectService.getMetadata().getDistinctColumnValues(nv);
      final List<String> uniqueStrings = uniqueValues.stream().map(Object::toString).toList();
      uniqueColumnValues.setAll(uniqueStrings);
    });
  }


  /**
   * @return a combobox with auto completion of the selected column groups
   */
  public TextField createLinkedGroupCombo(String tooltip) {
    // better use a textfield for this
    // editable combobox behaves weird when entered values that are not within the items
    final TextField subField = FxTextFields.newAutoGrowTextField(null, "", tooltip, 4, 20);
    FxTextFields.bindAutoCompletion(subField, uniqueColumnValues);
    return subField;
  }

  @Override
  public Property<MetadataColumn<?>> valueProperty() {
    return columnProperty;
  }

  public void setValue(String value) {
    columnField.setText(value);
  }

  public String getValue() {
    return columnField.getText();
  }

  /**
   * @return list of unique values in selected column or empty list
   */
  public @NotNull ObservableList<String> uniqueColumnValuesProperty() {
    return uniqueColumnValues;
  }

}
