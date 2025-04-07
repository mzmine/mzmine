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

package io.github.mzmine.parameters.parametertypes.metadata;

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
import javafx.geometry.Insets;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;
import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.Nullable;

public class MetadataGroupingComponent extends FlowPane implements
    ValuePropertyComponent<MetadataColumn<?>> {

  private final List<AvailableTypes> availableTypes;

  private final ObjectProperty<@Nullable MetadataColumn<?>> columnProperty = new SimpleObjectProperty<>();
  private final TextField text;

  public MetadataGroupingComponent() {
    this(AvailableTypes.values());
  }

  public MetadataGroupingComponent(AvailableTypes[] availableTypes) {
    this(List.of(availableTypes));
  }

  public MetadataGroupingComponent(List<AvailableTypes> types) {
    super();
    setHgap(FxLayout.DEFAULT_SPACE);
    setPadding(Insets.EMPTY);

    text = new TextField();

    var icon = FxIconUtil.newIconButton(FxIcons.METADATA_TABLE,
        "Open project metadata. (Import data files and metadata to then modify project metadata)",
        () -> {
          MZmineCore.getDesktop().addTab(new ProjectMetadataTab());
        });

    getChildren().addAll(text, icon);

    this.availableTypes = types;
    final String[] columns = ProjectService.getMetadata().getColumns().stream()
        .filter(col -> availableTypes.contains(col.getType())).map(MetadataColumn::getTitle)
        .toArray(String[]::new);
    TextFields.bindAutoCompletion(text, columns);

    Bindings.bindBidirectional(text.textProperty(), valueProperty(),
        new StringConverter<MetadataColumn<?>>() {
          @Override
          public String toString(MetadataColumn<?> object) {
            return object != null ? object.getTitle() : "";
          }

          @Override
          public MetadataColumn<?> fromString(String string) {
            return ProjectService.getMetadata().getColumnByName(string);
          }
        });
  }

  @Override
  public Property<MetadataColumn<?>> valueProperty() {
    return columnProperty;
  }

  public void setValue(String value) {
    text.setText(value);
  }

  public String getValue() {
    return text.getText();
  }
}
