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

package io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences;

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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;
import javafx.util.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Editor for the ordered tag labels. The last empty field grows the list when the user enters a
 * label, matching the interaction of the metadata group list editor.
 */
public class TagLabelsComponent extends FlowPane {

  private static final String TOOLTIP = "Enter one label for each tag checkbox.";

  private final ListProperty<TextField> labelFields = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ObjectProperty<TextField> lastField = new SimpleObjectProperty<>(null);
  private final ObjectProperty<Subscription> lastFieldSubscription = new SimpleObjectProperty<>(
      null);

  public TagLabelsComponent(@NotNull final List<String> labels) {
    super(2, FxLayout.DEFAULT_SPACE);

    lastFieldSubscription.subscribe((old, _) -> {
      if (old != null) {
        old.unsubscribe();
      }
    });

    lastField.subscribe((old, current) -> {
      if (old != null) {
        addAutoRemoveListenerOnEmpty(old);
      }
      if (current != null) {
        final Subscription subscription = PropertyUtils.onChangeDelayedSubscription(() -> {
          if (StringUtils.hasValue(current.getText())) {
            addLastEmptyField();
          }
        }, Duration.millis(500), current.textProperty());
        lastFieldSubscription.set(subscription);
      }
    });

    labelFields.subscribe(fields -> {
      final List<Node> nodes = new ArrayList<>();
      for (int i = 0; i < fields.size(); i++) {
        nodes.add(fields.get(i));
        if (i < fields.size() - 1) {
          nodes.add(new Label(", "));
        }
      }
      getChildren().setAll(nodes);
    });

    setValue(labels);
  }

  private @NotNull TextField createField() {
    final TextField field = new TextField();
    field.setPrefColumnCount(10);
    field.setTooltip(new Tooltip(TOOLTIP));
    return field;
  }

  private void setLabelFields(@NotNull final Collection<String> labels) {
    final List<TextField> fields = new ArrayList<>();
    for (final String label : labels) {
      final TextField field = createField();
      field.setText(label);
      fields.add(field);
      addAutoRemoveListenerOnEmpty(field);
    }
    labelFields.setAll(fields);
    addLastEmptyField();
  }

  private void addLastEmptyField() {
    final TextField emptyLastField = createField();
    emptyLastField.setPromptText("New tag");
    labelFields.add(emptyLastField);
    lastField.set(emptyLastField);
  }

  private void addAutoRemoveListenerOnEmpty(@NotNull final TextField field) {
    PropertyUtils.onChangeDelayedSubscription(() -> {
      if (StringUtils.isBlank(field.getText())) {
        labelFields.remove(field);
      }
    }, Duration.millis(3500), field.textProperty());
  }

  public @NotNull List<String> getValue() {
    return labelFields.stream().map(TextField::getText).map(String::trim)
        .filter(StringUtils::hasValue).toList();
  }

  public void setValue(@Nullable final List<String> labels) {
    setLabelFields(labels == null ? List.of() : labels);
  }
}
