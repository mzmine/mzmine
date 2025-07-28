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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.parameters.Parameter;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.Nullable;

public class GroupedParameterSetupPane extends BorderPane {

  private final List<Parameter<?>> fixedParameters;
  private final List<ParameterGroup> groups;
  private final ParameterSetupPane parentPane;
  private final List<ParameterGroupGrid> groupedParameterPanes = new ArrayList<>();
  private final ObjectProperty<GroupView> viewType = new SimpleObjectProperty<>(GroupView.GROUPED);
  private final TextField searchField;
  private final BorderPane centerPane;

  public GroupedParameterSetupPane(@Nullable List<Parameter<?>> fixedParameters,
      List<ParameterGroup> groups, ParameterSetupPane parentPane, GroupView viewType) {
    assert !groups.isEmpty() : "Groups cannot be empty";
    this.fixedParameters = fixedParameters;
    this.groups = groups;
    this.parentPane = parentPane;
    this.viewType.set(viewType);

    // search bar
    searchField = TextFields.createClearableTextField();
    FxTextFields.bindAutoCompletion(searchField,
        List.of(parentPane.getParameterSet().getParameters()));
    searchField.setPromptText("Search...");

    final ToggleButton groupViewButton = FxIconUtil.newToggleIconButton(
        "Either show parameters as groups or in one list. Click to toggle.",
        selected -> selected ? FxIcons.FOLDER : FxIcons.LIST);
    groupViewButton.selectedProperty().bind(this.viewType.map(type -> type == GroupView.GROUPED));

    centerPane = new BorderPane();

    final HBox searchBar = new HBox(5, groupViewButton, searchField);
    VBox centerFlow = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, searchBar, centerPane);
    setCenter(centerFlow);

    createInitialLayout();
    this.viewType.subscribe((nv) -> applyViewLayout(nv));
  }

  private void applyViewLayout(GroupView view) {
    // TODO need to remove from parent first?
//    for (GridPane pane : groupedParameterPanes) {
//      if (pane.getParent() instanceof TitledPane parent) {
//        parent.setContent(null);
//      }
//      if (pane.getParent() instanceof Pane parent) {
//        parent.getChildren().remove(pane);
//      }
//    }
    centerPane.setCenter(null);
    switch (view) {
      case GROUPED -> {
        final Accordion accordion = new Accordion();
        for (ParameterGroupGrid group : groupedParameterPanes) {
          accordion.getPanes().add(FxLayout.newTitledPane(group.name(), group.grid()));
        }
        centerPane.setCenter(accordion);
        accordion.setExpandedPane(accordion.getPanes().getFirst());
        centerPane.setCenter(accordion);
      }
      case SINGLE_LIST -> {
        final GridPane[] nodes = groupedParameterPanes.stream().map(ParameterGroupGrid::grid)
            .toArray(GridPane[]::new);
        final VBox singleFlow = FxLayout.newVBox(Pos.TOP_LEFT,
            new Insets(0, FxLayout.DEFAULT_SPACE, 0, FxLayout.DEFAULT_SPACE), true, nodes);
        centerPane.setCenter(singleFlow);
      }
    }
  }

  public void setSearchFilter(@Nullable String filter) {
    searchField.setText(requireNonNullElse(filter, ""));
  }

  /**
   * Only called once to initialize everything
   */
  private void createInitialLayout() {
    if (!fixedParameters.isEmpty()) {
      final GridPane fixedGrid = parentPane.createParameterPane(fixedParameters);
      setTop(fixedGrid);
    } else if (!groups.isEmpty()) {
      // for now just create the grouped view - so that components are initialized
      for (ParameterGroup group : groups) {
        final GridPane grid = parentPane.createParameterPane(group.parameters());
        groupedParameterPanes.add(new ParameterGroupGrid(group.name(), group.parameters(), grid));
      }
      // layout is added later
    }
  }


  public enum GroupView {
    GROUPED, SINGLE_LIST;
  }

  private record ParameterGroupGrid(String name, List<Parameter<?>> parameters, GridPane grid) {

  }

  public record ParameterGroup(String name, List<Parameter<?>> parameters) {

  }
}
