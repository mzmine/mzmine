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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckListView;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A {@link UserParameter} that presents a list of {@link SweepMetric} choices in a
 * {@link CheckListView}. The parameter value is the list of currently checked (selected) metrics.
 * Items are identified by their {@link SweepMetric#name()} for display and XML serialisation.
 */
public class SweepMetricCheckListParameter implements
    UserParameter<List<SweepMetric>, CheckListView<SweepMetric>> {

  private final String name;
  private final String description;
  private final List<SweepMetric> choices;
  private List<SweepMetric> value;

  /**
   * @param name        parameter name shown in the UI
   * @param description tooltip/description
   * @param choices     all available metrics (used as checklist items)
   * @param value       initially selected metrics; pass a copy of {@code choices} to select all
   */
  public SweepMetricCheckListParameter(final String name, final String description,
      final List<SweepMetric> choices, final List<SweepMetric> value) {
    this.name = name;
    this.description = description;
    this.choices = List.copyOf(choices);
    this.value = new ArrayList<>(value);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Priority getComponentVgrowPriority() {
    return Priority.SOMETIMES;
  }

  @Override
  public CheckListView<SweepMetric> createEditingComponent() {
    final CheckListView<SweepMetric> view = new CheckListView<>(
        FXCollections.observableArrayList(choices));

    view.setCellFactory(listView -> {
      final CheckBoxListCell<SweepMetric> checkBoxListCell = new CheckBoxListCell<>(
          view::getItemBooleanProperty);
      checkBoxListCell.focusedProperty().addListener((o, ov, nv) -> {
        if (nv) {
          final Parent parent = checkBoxListCell.getParent();
          if (parent != null) {
            parent.requestFocus();
          }
        }
      });

      checkBoxListCell.setConverter(new StringConverter<>() {
        @Override
        public String toString(SweepMetric object) {
          if (object == null) {
            return "";
          }
          return object.name();
        }

        @Override
        public SweepMetric fromString(String string) {
          if (string == null) {
            return null;
          }
          return view.getItems().stream().filter(metric -> metric.name().equals(string))
              .findFirst().orElse(null);
        }
      });
      return checkBoxListCell;
    });

    view.setPrefHeight(150);
    return view;
  }

  @Override
  public List<SweepMetric> getValue() {
    return value;
  }

  @Override
  public void setValue(final List<SweepMetric> newValue) {
    this.value = new ArrayList<>(newValue);
  }

  @Override
  public void setValueFromComponent(final CheckListView<SweepMetric> component) {
    this.value = new ArrayList<>(component.getCheckModel().getCheckedItems());
  }

  @Override
  public void setValueToComponent(final CheckListView<SweepMetric> component,
      @Nullable final List<SweepMetric> newValue) {
    component.getCheckModel().clearChecks();
    if (newValue == null) {
      return;
    }
    for (SweepMetric selected : newValue) {
      component.getCheckModel().check(selected);
    }
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {
    final NodeList items = xmlElement.getElementsByTagName("item");
    final List<SweepMetric> loaded = new ArrayList<>();
    for (int i = 0; i < items.getLength(); i++) {
      final String itemName = items.item(i).getTextContent();
      choices.stream().filter(c -> c.name().equals(itemName)).findFirst().ifPresent(loaded::add);
    }
    if (!loaded.isEmpty()) {
      this.value = loaded;
    }
  }

  @Override
  public void saveValueToXML(final Element xmlElement) {
    if (value == null) {
      return;
    }
    final Document doc = xmlElement.getOwnerDocument();
    for (SweepMetric metric : value) {
      final Element item = doc.createElement("item");
      item.setTextContent(metric.name());
      xmlElement.appendChild(item);
    }
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {
    if (value == null || value.isEmpty()) {
      errorMessages.add(name + ": at least one metric must be selected.");
      return false;
    }

    return true;
  }

  @Override
  public SweepMetricCheckListParameter cloneParameter() {
    return new SweepMetricCheckListParameter(name, description, choices,
        value != null ? value : List.of());
  }
}
