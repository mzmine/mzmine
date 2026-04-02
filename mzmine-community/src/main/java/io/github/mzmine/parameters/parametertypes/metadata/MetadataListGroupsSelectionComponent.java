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

import static io.github.mzmine.javafx.components.factories.FxLabels.newLabelNoWrap;

import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.util.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import javafx.util.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetadataListGroupsSelectionComponent extends GridPane {

  // auto completion is automatically bound to both fields
  private final MetadataGroupingComponent columnField = new MetadataGroupingComponent();
  private final FlowPane groupsPane;
  private final ListProperty<TextField> groupsTextFields = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ObjectProperty<TextField> lastField = new SimpleObjectProperty<>(null);
  private final ObjectProperty<Subscription> lastFieldSubscription = new SimpleObjectProperty<>(
      null);
  private final String tooltip = "Enter one metadata group name per text field. Together this forms a list of groups.";

  public MetadataListGroupsSelectionComponent(@NotNull MetadataListGroupsSelection value) {
    super();
    setHgap(FxLayout.DEFAULT_SPACE);
    setVgap(FxLayout.DEFAULT_SPACE);

    groupsPane = new FlowPane(2, FxLayout.DEFAULT_SPACE);
    FxLayout.applyGrid2Col(this,
        // children
        newLabelNoWrap("Metadata column"), columnField, //
        newLabelNoWrap("Groups"), groupsPane //
    );

    lastFieldSubscription.subscribe((old, nv) -> {
      if (old != null) {
        old.unsubscribe();
      }
    });

    lastField.subscribe((old, nv) -> {
      if (old != null) {
        addAutoRemoveListenerOnEmpty(old);
      }

      if (nv != null) {
        final Subscription subscription = PropertyUtils.onChangeDelayedSubscription(() -> {
          if (!StringUtils.isBlank(nv.getText())) {
            addLastEmptyField();
          }
        }, Duration.millis(500), nv.textProperty());
        lastFieldSubscription.set(subscription);
      }
    });

    groupsTextFields.subscribe((nv) -> {
      List<Node> nodes = new ArrayList<>();
      for (int i = 0; i < nv.size(); i++) {
        nodes.add(nv.get(i));
        if (i < nv.size() - 1) {
          nodes.add(new Label(", "));
        }
      }

      groupsPane.getChildren().setAll(nodes);
    });

    setValue(value);
  }

  private void setGroupsTextFields(@NotNull Collection<String> groups) {
    List<TextField> fields = new ArrayList<>();
    for (String group : groups) {
      final TextField field = columnField.createLinkedGroupCombo(tooltip);
      field.setText(group);
      fields.add(field);

      // remove text field if text is empty
      addAutoRemoveListenerOnEmpty(field);
    }

    groupsTextFields.setAll(fields);

    addLastEmptyField();
  }

  private void addLastEmptyField() {
    final TextField emptyLastField = columnField.createLinkedGroupCombo(tooltip);
    groupsTextFields.add(emptyLastField);
    lastField.set(emptyLastField);
  }

  private void addAutoRemoveListenerOnEmpty(TextField field) {
    PropertyUtils.onChangeDelayedSubscription(() -> {
      if (StringUtils.isBlank(field.getText())) {
        groupsTextFields.remove(field);
      }
    }, Duration.millis(3500), field.textProperty());
  }

  @NotNull
  public MetadataListGroupsSelection getValue() {
    final String column = columnField.getValue();

    final List<String> groups = groupsTextFields.stream().map(TextField::getText).map(String::trim)
        .filter(StringUtils::hasValue).toList();

    if (column == null || groups.isEmpty()) {
      return MetadataListGroupsSelection.NONE;
    }

    return new MetadataListGroupsSelection(column.trim(), groups);
  }

  public void setValue(@Nullable MetadataListGroupsSelection value) {
    if (value == null) {
      groupsTextFields.clear();
      addLastEmptyField();
      columnField.setValue("");
      return;
    }

    columnField.setValue(value.columnName());
    setGroupsTextFields(value.groups());
  }

}
