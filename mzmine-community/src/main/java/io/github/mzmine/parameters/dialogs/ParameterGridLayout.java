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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.parameters.FullColumnComponent;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.StringUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParameterGridLayout extends GridPane {

  /**
   * Parameter.getName to component
   */
  private final Map<String, ParameterAndComponent> components;
  private final StringProperty searchText = new SimpleStringProperty("");
  private final BooleanProperty hasComponents = new SimpleBooleanProperty(true);
  private final IntegerProperty numComponents = new SimpleIntegerProperty(0);

  public ParameterGridLayout(@NotNull Parameter<?>[] parameters) {
    setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    // setStyle("-fx-border-color: blue;");
    setVgap(FxLayout.DEFAULT_SPACE * 1.5);
    setHgap(FxLayout.DEFAULT_SPACE * 2);

    List<UserParameter> userParams = Arrays.stream(parameters)
        .filter(UserParameter.class::isInstance).map(UserParameter.class::cast).toList();
    // needs order
    components = LinkedHashMap.newLinkedHashMap(userParams.size());
    /*
     * Adding an empty ColumnConstraints object for column2 has the effect of not setting any
     * constraints, leaving the GridPane to compute the column's layout based solely on its
     * content's size preferences and constraints.
     */
    ColumnConstraints column1 = new ColumnConstraints();
    column1.setHgrow(Priority.SOMETIMES);
    column1.setMinWidth(USE_PREF_SIZE);
    column1.setPrefWidth(Region.USE_COMPUTED_SIZE);
    ColumnConstraints column2 = new ColumnConstraints();
    column2.setFillWidth(true);
    column2.setHgrow(Priority.ALWAYS);
    getColumnConstraints().addAll(column1, column2);
    int rowCounter = 0;

    // Create labels and components for each parameter
    for (UserParameter up : userParams) {
      Node comp = up.createEditingComponent();
      //      addToolTipToControls(comp, up.getDescription());
      if (comp instanceof Region) {
        double minWidth = ((Region) comp).getMinWidth();
        // if (minWidth > column2.getMinWidth()) column2.setMinWidth(minWidth);
        // setMinWidth(minWidth + 200);
      }
      // now added as Vgap and Hgap above
//      GridPane.setMargin(comp, new Insets(5.0, 0.0, 5.0, 0.0));

      // Set the initial value
      Object value = up.getValue();
      if (value != null) {
        up.setValueToComponent(comp, value);
      }

      // By calling this we make sure the components will never be resized
      // smaller than their optimal size
      // comp.setMinimumSize(comp.getPreferredSize());
      // comp.setToolTipText(up.getDescription());

      Label label = new Label(up.getName());
      label.minWidthProperty().bind(label.widthProperty());
//      label.setPadding(new Insets(0.0, 10.0, 0.0, 0.0));

      if (!up.getDescription().isEmpty()) {
        final Tooltip tooltip = new Tooltip(up.getDescription());
        tooltip.setShowDuration(new Duration(20_000));
        label.setTooltip(tooltip);
      }

      label.setStyle("-fx-font-weight: bold");
      label.setLabelFor(comp);

      // TODO: Multiple selection will be expandable, other components not
      /*
       * JComboBox t = new JComboBox(); int comboh = t.getPreferredSize().height; int comph =
       * comp.getPreferredSize().height; int verticalWeight = comph > 2 * comboh ? 1 : 0;
       * vertWeightSum += verticalWeight;
       */

      RowConstraints rowConstraints = new RowConstraints();
      rowConstraints.setVgrow(up.getComponentVgrowPriority());
      rowConstraints.setMinHeight(USE_PREF_SIZE);
      rowConstraints.setPrefHeight(USE_COMPUTED_SIZE);
      if (comp instanceof FullColumnComponent) {
        add(comp, 0, rowCounter, 2, 1);
//        rowConstraints.setVgrow(Priority.NEVER);
        components.put(up.getName(), new ParameterAndComponent<>(up, comp, null));
      } else {
        add(label, 0, rowCounter);
        add(comp, 1, rowCounter, 1, 1);
        components.put(up.getName(), new ParameterAndComponent<>(up, comp, label));
      }
      getRowConstraints().add(rowConstraints);
      rowCounter++;
    }

    // add search capability
    searchText.subscribe((filter) -> applyFilter(filter));
  }

  public void setSearchText(String text) {
    searchText.set(text);
  }

  public StringProperty searchTextProperty() {
    return searchText;
  }

  private void applyFilter(@Nullable String filter) {
    if (StringUtils.isBlank(filter)) {
      makeLayout(components.values());
      return;
    }

    final String lowerFilter = filter.toLowerCase().trim();
    final List<ParameterAndComponent> filtered = components.values().stream()
        .filter(comp -> comp.parameter().getName().toLowerCase().contains(lowerFilter)).toList();

    makeLayout(filtered);
  }

  private void makeLayout(Collection<ParameterAndComponent> filtered) {
    getChildren().clear();
    getRowConstraints().clear();

    int rowCounter = 0;
    for (ParameterAndComponent comp : filtered) {
      Label label = comp.label();
      Node component = comp.component();
      if (label == null) {
        add(component, 0, rowCounter, 2, 1);
      } else {
        add(label, 0, rowCounter);
        add(component, 1, rowCounter);
      }

      RowConstraints rowConstraints = new RowConstraints();
      rowConstraints.setVgrow(comp.parameter().getComponentVgrowPriority());
      rowConstraints.setMinHeight(USE_PREF_SIZE);
      rowConstraints.setPrefHeight(USE_COMPUTED_SIZE);
      getRowConstraints().add(rowConstraints);

      rowCounter++;
    }

    // empty
    boolean visible = rowCounter != 0;
    hasComponents.set(visible);
    numComponents.set(rowCounter);
  }

  public BooleanProperty hasComponentsProperty() {
    return hasComponents;
  }

  public boolean hasComponents() {
    return hasComponents.get();
  }

  public @NotNull Map<String, ParameterAndComponent> getComponents() {
    return components;
  }

  @Nullable
  public ParameterAndComponent getParameterAndComponent(@NotNull Parameter<?> parameter) {
    return components.get(parameter.getName());
  }

  public int getNumComponents() {
    return numComponents.get();
  }

  public IntegerProperty numComponentsProperty() {
    return numComponents;
  }
}
