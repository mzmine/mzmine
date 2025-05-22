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

package io.github.mzmine.parameters.parametertypes.metadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

public class MetadataGroupSelectionComponent extends VBox {

  private static final int inputSize = 20;
  private final TextField columnField = new TextField();
  private final TextField groupField = new TextField();
  private final PauseTransition pauseAfterColChange = new PauseTransition(Duration.millis(200));

  public MetadataGroupSelectionComponent() {
    super();
    columnField.setPrefColumnCount(inputSize);
    final AutoCompletionBinding<MetadataColumn<?>> colBinding = TextFields.bindAutoCompletion(
        columnField, MZmineCore.getProjectMetadata().getColumns());

    groupField.setPrefColumnCount(inputSize);

    final AutoCompletionBinding<String> newBinding = TextFields.bindAutoCompletion(groupField,
        iSuggestionRequest -> {
          final String input = iSuggestionRequest.getUserText().toLowerCase();

          final MetadataTable metadata = MZmineCore.getProjectMetadata();
          final MetadataColumn<?> column = metadata.getColumnByName(columnField.getText());
          if (column == null) {
            return List.of();
          }

          // get all possible values
          final RawDataFile[] files = ProjectService.getProject().getDataFiles();
          final List<?> distinctValues = Arrays.stream(files)
              .map(file -> metadata.getValue(column, file)).distinct().toList();

          // get all values that match the current input
          return distinctValues.stream().map(Object::toString).filter(Objects::nonNull)
              .map(String::toLowerCase).filter(str -> str.contains(input)).sorted().toList();
        });

    setSpacing(5);
    getChildren().addAll(columnField, groupField);
  }

  public MetadataGroupSelection getValue() {
    final String column = columnField.textProperty().getValue();
    final String group = groupField.textProperty().getValue();

    if (column == null || group == null) {
      return MetadataGroupSelection.NONE;
    }

    return new MetadataGroupSelection(column.trim(), group.trim());
  }

  public void setValue(MetadataGroupSelection value) {
    if (value == null) {
      groupField.setText("");
      columnField.setText("");
      return;
    }

    groupField.setText(value.groupStr());
    columnField.setText(value.columnName());
  }
}
