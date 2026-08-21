/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates a table column that has a combo box in it's header, which allows selection of a metadata
 * column.
 *
 * @param <S>
 * @param <T>
 */
public class MetadataHeaderColumn<S, T> extends TreeTableColumn<S, T> {

  private final ObjectProperty<@Nullable MetadataColumn<?>> selectedColumn = new SimpleObjectProperty<>();
  /**
   * State of the optional header check box. Only meaningful if a check box label was given and
   * {@link #normVisibleProperty()} is true.
   */
  private final BooleanProperty normActive = new SimpleBooleanProperty(true);
  private final BooleanProperty normVisible = new SimpleBooleanProperty(false);

  public MetadataHeaderColumn(@NotNull DataType<?> dataType,
      @Nullable MetadataColumn<?> defaultColumn, @NotNull AbundanceMeasure rawAbundanceMeasure) {
    super();

    setUserData(dataType);
    setSortable(true);
    if (dataType.getPrefColumnWidth() > 0) {
      setPrefWidth(dataType.getPrefColumnWidth());
    }

    final VBox header = FxLayout.newVBox(Pos.CENTER);
    header.setFillWidth(true);

    final ButtonBase toggle = FxIconUtil.newIconButton(FxIcons.TOGGLE_SWITCH,
        FxIconUtil.LIST_ICON_SIZE, () -> normActive.set(!normActive.get()));
    final Label title = FxLabels.newLabel(normActive.and(normVisible).map(
        normalized -> (normalized ? "Normalized " + rawAbundanceMeasure.toString().toLowerCase()
            : rawAbundanceMeasure.toString())));

    toggle.visibleProperty().bind(normVisible);
    toggle.managedProperty().bind(normVisible);

    header.getChildren().add(FxLayout.newHBox(Pos.CENTER, Insets.EMPTY, 4, toggle, title));

    // using a combobox here causes the virtual flow to fail. This lead to the feature table not
    // jumping to/selecting the row if a loading was selected in the stats dashboard scores plot
    final ChoiceBox<MetadataColumn<?>> metadataColumnBox = new ChoiceBox<>();
    metadataColumnBox.getItems().addAll(ProjectService.getMetadata().getColumns());
    metadataColumnBox.getSelectionModel().selectFirst();
    metadataColumnBox.valueProperty().bindBidirectional(selectedColumn);
    metadataColumnBox.setMaxWidth(Double.MAX_VALUE);
    header.getChildren().add(metadataColumnBox);
    VBox.setVgrow(metadataColumnBox, Priority.ALWAYS);

    setGraphic(header);
    selectedColumn.set(defaultColumn);
  }

  public @Nullable MetadataColumn<?> getSelectedColumn() {
    return selectedColumn.get();
  }

  public ObjectProperty<@Nullable MetadataColumn<?>> selectedColumnProperty() {
    return selectedColumn;
  }

  public BooleanProperty normActiveProperty() {
    return normActive;
  }

  public boolean isNormActiveSelected() {
    return normActive.get();
  }

  public BooleanProperty normVisibleProperty() {
    return normVisible;
  }

  public boolean isNormVisible() {
    return normVisible.get();
  }

  public void setNormVisible(final boolean visible) {
    normVisible.set(visible);
  }
}
