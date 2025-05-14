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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates a table column that has a combo box in it's header, which allows selection of a metadata column.
 * @param <S>
 * @param <T>
 */
public class MetadataHeaderColumn<S, T> extends TreeTableColumn<S, T> {

  private final ObjectProperty<@Nullable MetadataColumn<?>> selectedColumn = new SimpleObjectProperty<>();
  private final PauseTransition delay = new PauseTransition(new Duration(100));

  public MetadataHeaderColumn(@NotNull DataType<?> dataType,
      @Nullable MetadataColumn<?> defaultColumn) {

    setUserData(dataType);
    setSortable(true);
    if (dataType.getPrefColumnWidth() > 0) {
      setPrefWidth(dataType.getPrefColumnWidth());
    }
    final BorderPane header = new BorderPane();
    final Label title = new Label(dataType.getHeaderString());
    header.setTop(title);
    BorderPane.setAlignment(title, Pos.TOP_CENTER);

    final ComboBox<String> metadataColumnBox = new ComboBox<>();
    metadataColumnBox.setEditable(true);
    metadataColumnBox.getItems().addAll(ProjectService.getMetadata().getColumnTitles());
    metadataColumnBox.valueProperty().addListener((_, _, _) -> delay.playFromStart());

    delay.setOnFinished(_ -> {
      final String columnName = metadataColumnBox.getValue();
      final MetadataTable metadata = ProjectService.getMetadata();
      final MetadataColumn<?> column = metadata.getColumnByName(columnName);
      if (column != selectedColumn.get()) {
        selectedColumn.set(column);
      }
    });

    selectedColumn.addListener((_, _, newColumn) -> {
      if (newColumn != null) {
        if (!Objects.equals(metadataColumnBox.getValue(), newColumn.getTitle())) {
          metadataColumnBox.setValue(newColumn.getTitle());
        }
      } else {
        metadataColumnBox.setValue(null);
      }
    });

    selectedColumn.set(defaultColumn);

    header.setCenter(metadataColumnBox);
    setGraphic(header);
  }

  public @Nullable MetadataColumn<?> getSelectedColumn() {
    return selectedColumn.get();
  }

  public ObjectProperty<@Nullable MetadataColumn<?>> selectedColumnProperty() {
    return selectedColumn;
  }
}
