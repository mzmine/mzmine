/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.modules.dataprocessing.filter_lipidannotationcleanup.IonizationPreference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Table-based UI component for editing a list of {@link IonizationPreference} rules. Each row shows
 * the hierarchy scope (category, main class, lipid class) and the preferred ionization. An "Add"
 * button opens {@link IonizationPreferenceAddDialog}; a "Remove" button deletes selected rows.
 */
public class IonizationPreferenceComponent extends VBox {

  private final @NotNull ObservableList<IonizationPreference> items = FXCollections.observableArrayList();
  private final @NotNull TableView<IonizationPreference> table;

  public IonizationPreferenceComponent(final @Nullable List<IonizationPreference> initial) {
    super(4);
    setPadding(new Insets(2));

    table = new TableView<>(items);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPrefHeight(140);
    table.setPlaceholder(
        new javafx.scene.control.Label("No preferences — defaults to highest-score selection"));

    final TableColumn<IonizationPreference, String> categoryCol = new TableColumn<>("Category");
    categoryCol.setCellValueFactory(
        cd -> new SimpleStringProperty(cd.getValue().category().getAbbreviation()));

    final TableColumn<IonizationPreference, String> mainClassCol = new TableColumn<>("Main class");
    mainClassCol.setCellValueFactory(cd -> new SimpleStringProperty(
        cd.getValue().mainClass() != null ? cd.getValue().mainClass().getName() : "—"));

    final TableColumn<IonizationPreference, String> lipidClassCol = new TableColumn<>(
        "Lipid class");
    lipidClassCol.setCellValueFactory(cd -> new SimpleStringProperty(
        cd.getValue().lipidClass() != null ? cd.getValue().lipidClass().getAbbr() : "—"));

    final TableColumn<IonizationPreference, String> ionCol = new TableColumn<>("Preferred ion");
    ionCol.setCellValueFactory(
        cd -> new SimpleStringProperty(cd.getValue().ionizationType().toString()));

    //noinspection unchecked
    table.getColumns().addAll(categoryCol, mainClassCol, lipidClassCol, ionCol);

    final Button addBtn = new Button("Add");
    addBtn.setOnAction(_ -> {
      final IonizationPreferenceAddDialog dialog = new IonizationPreferenceAddDialog(
          List.copyOf(items));
      final Optional<IonizationPreference> result = dialog.showAndWait();
      result.ifPresent(items::add);
    });

    final Button removeBtn = new Button("Remove");
    removeBtn.setDisable(true);
    removeBtn.setOnAction(_ -> items.removeAll(table.getSelectionModel().getSelectedItems()));
    table.getSelectionModel().selectedItemProperty()
        .addListener((_, _, sel) -> removeBtn.setDisable(sel == null));

    final HBox buttons = new HBox(6, addBtn, removeBtn);
    VBox.setVgrow(table, Priority.ALWAYS);
    getChildren().addAll(table, buttons);

    if (initial != null) {
      items.setAll(initial);
    }
  }

  public @NotNull List<IonizationPreference> getValue() {
    return new ArrayList<>(items);
  }

  public void setValue(final @Nullable List<IonizationPreference> prefs) {
    items.clear();
    if (prefs != null) {
      items.addAll(prefs);
    }
  }
}
