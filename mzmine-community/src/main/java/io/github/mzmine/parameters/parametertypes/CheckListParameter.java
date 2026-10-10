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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import javafx.collections.FXCollections;
import javafx.scene.layout.Priority;
import org.controlsfx.control.CheckListView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A checklist of items with stable IDs, at least one has to be selected. The UI shows
 * {@link Object#toString()}, XML stores {@link UniqueIdSupplier#getUniqueID()}. Loading also
 * accepts {@link Object#toString()}, like {@link ComboParameter}.
 *
 * @param <T> item type
 */
public class CheckListParameter<T extends UniqueIdSupplier> implements
    UserParameter<List<T>, CheckListView<T>> {

  private final @NotNull String name;
  private final @NotNull String description;
  private final @NotNull List<T> choices;
  private @NotNull List<T> value;
  private @Nullable Predicate<T> visibleFilter;

  /**
   * @param choices all items, in display order
   * @param value   initially selected items
   */
  public CheckListParameter(@NotNull String name, @NotNull String description,
      @NotNull List<T> choices, @NotNull List<T> value) {
    this.name = name;
    this.description = description;
    this.choices = List.copyOf(choices);
    this.value = new ArrayList<>(value);
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public @NotNull String getDescription() {
    return description;
  }

  /**
   * Limits the items shown in the editing component and returned by {@link #getValue()}, e.g., to
   * the items that apply to the current context. Selected items that are hidden stay selected and
   * are saved to XML.
   *
   * @param filter accepts the visible items, null to show all
   */
  public void setVisibleFilter(@Nullable Predicate<T> filter) {
    this.visibleFilter = filter;
  }

  private @NotNull List<T> visibleChoices() {
    return visibleFilter == null ? choices : choices.stream().filter(visibleFilter).toList();
  }

  @Override
  public @NotNull Priority getComponentVgrowPriority() {
    return Priority.SOMETIMES;
  }

  @Override
  public @NotNull CheckListView<T> createEditingComponent() {
    final CheckListView<T> view = new CheckListView<>(
        FXCollections.observableArrayList(visibleChoices()));
    view.setPrefHeight(200);
    return view;
  }

  /**
   * @return the selected items that are visible, see {@link #setVisibleFilter(Predicate)}
   */
  @Override
  public @NotNull List<T> getValue() {
    final List<T> visible = new ArrayList<>(value);
    visible.retainAll(visibleChoices());
    return visible;
  }

  @Override
  public void setValue(@NotNull List<T> newValue) {
    this.value = new ArrayList<>(newValue);
  }

  @Override
  public void setValueFromComponent(@NotNull CheckListView<T> component) {
    final List<T> hidden = value.stream().filter(item -> !component.getItems().contains(item))
        .toList();
    final List<T> selected = new ArrayList<>(component.getCheckModel().getCheckedItems());
    selected.addAll(hidden);
    this.value = selected;
  }

  @Override
  public void setValueToComponent(@NotNull CheckListView<T> component, @Nullable List<T> newValue) {
    component.getCheckModel().clearChecks();
    if (newValue == null) {
      return;
    }
    for (final T selected : newValue) {
      // hidden items are not part of the component
      if (component.getItems().contains(selected)) {
        component.getCheckModel().check(selected);
      }
    }
  }

  @Override
  public void loadValueFromXML(@NotNull Element xmlElement) {
    final NodeList items = xmlElement.getElementsByTagName("item");
    final List<T> loaded = new ArrayList<>();
    for (int i = 0; i < items.getLength(); i++) {
      final String item = items.item(i).getTextContent();
      choices.stream().filter(c -> c.getUniqueID().equals(item) || c.toString().equals(item))
          .findFirst().ifPresent(loaded::add);
    }
    // decision: an empty saved selection stays empty instead of restoring the defaults
    this.value = loaded;
  }

  @Override
  public void saveValueToXML(@NotNull Element xmlElement) {
    final Document doc = xmlElement.getOwnerDocument();
    for (final T selected : value) {
      final Element item = doc.createElement("item");
      item.setTextContent(selected.getUniqueID());
      xmlElement.appendChild(item);
    }
  }

  @Override
  public boolean checkValue(@NotNull Collection<String> errorMessages) {
    if (value.isEmpty()) {
      errorMessages.add(name + ": select at least one item.");
      return false;
    }
    return true;
  }

  @Override
  public @NotNull CheckListParameter<T> cloneParameter() {
    return new CheckListParameter<>(name, description, choices, value);
  }
}
