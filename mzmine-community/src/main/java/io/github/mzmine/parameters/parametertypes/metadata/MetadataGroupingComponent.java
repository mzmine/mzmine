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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.FlowPane;
import org.jetbrains.annotations.Nullable;

public class MetadataGroupingComponent extends FlowPane implements
    ValuePropertyComponent<MetadataColumn<?>> {

  private final List<AvailableTypes> availableTypes;

  private final ObjectProperty<@Nullable MetadataColumn<?>> columnProperty = new SimpleObjectProperty<>();
  private final ComboBox<String> combo;

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

    combo = new ComboBox<>();
    combo.setEditable(true);
    combo.setPromptText("Select metadata column");

    var icon = FxIconUtil.newIconButton(FxIcons.METADATA_TABLE,
        "Open project metadata. (Import data files and metadata to then modify project metadata)",
        () -> {
          MZmineCore.getDesktop().addTab(new ProjectMetadataTab());
        });

    getChildren().addAll(combo, icon);

    this.availableTypes = types;
    final List<String> columns = ProjectService.getMetadata().getColumns().stream()
        .filter(col -> availableTypes.contains(col.getType())).map(MetadataColumn::getTitle)
        .toList();
    combo.getItems().addAll(columns);

    // One-way listener: editor text → columnProperty. Never feed back the other way so that
    // a null lookup result (column not yet loaded) cannot wipe out the typed text.
    combo.getEditor().textProperty().addListener((obs, oldText, newText) ->
        columnProperty.set(ProjectService.getMetadata().getColumnByName(newText)));
  }

  @Override
  public Property<MetadataColumn<?>> valueProperty() {
    return columnProperty;
  }

  public void setValue(String value) {
    combo.getEditor().setText(value);
  }

  public String getValue() {
    return combo.getEditor().getText();
  }
}
