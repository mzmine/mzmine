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

import static io.github.mzmine.javafx.components.factories.FxTextFlows.newTextFlow;
import static io.github.mzmine.javafx.components.util.FxLayout.DEFAULT_SPACE;
import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;
import static io.github.mzmine.javafx.components.util.FxLayout.newStackPane;
import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxLabels.Styles;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.collections.CollectionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.Nullable;

public class GroupedParameterSetupPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(GroupedParameterSetupPane.class.getName());
  private final List<? extends Parameter<?>> fixedParameters;
  private final List<ParameterGroup> groups;
  private final ParameterSetupPane parentPane;
  private final List<ParameterGroupGrid> groupedParameterPanes = new ArrayList<>();
  private final ObjectProperty<GroupView> viewType = new SimpleObjectProperty<>(GroupView.GROUPED);
  private final TextField searchField;
  private final BorderPane centerPane;
  private final BooleanProperty showSummary = new SimpleBooleanProperty();
  private final TextFlow summary;
  private int minimumParametersForGrouped = 7;

  private final PauseTransition reLayoutDelay = new PauseTransition(Duration.millis(200));

  public GroupedParameterSetupPane(@Nullable List<? extends Parameter<?>> fixedParameters,
      List<ParameterGroup> groups, ParameterSetupPane parentPane, GroupView view,
      boolean useAutoCompleteFilter) {
    assert !groups.isEmpty() : "Groups cannot be empty";

    // need to map parameters to actual parameters to not use static instances
    this.fixedParameters = fixedParameters == null ? null
        : ParameterUtils.mapToActualParameters(parentPane.getParameterSet(), fixedParameters);
    this.groups = groups.stream().map(group -> group.remap(parentPane.getParameterSet())).toList();
    this.parentPane = parentPane;
    this.viewType.set(view);

    reLayoutDelay.setOnFinished(_ -> applyViewLayout(viewType.get()));

    // search bar
    searchField = TextFields.createClearableTextField();

    // only in some cases useful. Default is not to use auto complete as the filter is directly responding and showing options
    if (useAutoCompleteFilter) {
      final List<String> autoOptions = Arrays.stream(parentPane.getParameterSet().getParameters())
          .filter(UserParameter.class::isInstance).map(Parameter::getName).toList();
      FxTextFields.bindAutoCompletion(searchField, autoOptions);
    }
    searchField.setPromptText("Search...");

    final ToggleButton groupViewButton = FxIconUtil.newToggleIconButton(
        "Either show parameters as groups or in one list. Click to toggle.",
        selected -> selected ? FxIcons.GROUPED_PARAMETERS : FxIcons.LIST);

    // both set each other
    this.viewType.subscribe((nv) -> groupViewButton.setSelected(nv == GroupView.GROUPED));
    groupViewButton.selectedProperty()
        .subscribe((_, nv) -> viewType.set(nv ? GroupView.GROUPED : GroupView.SINGLE_LIST));

    final HBox searchBar = newHBox(groupViewButton, searchField);
    HBox.setHgrow(searchField, Priority.ALWAYS);

    // summary just under search bar
    summary = newTextFlow();
    final var summaryFlow = newStackPane(new Insets(DEFAULT_SPACE, DEFAULT_SPACE, 8, DEFAULT_SPACE),
        summary);

    showSummary.subscribe((_, _) -> updateSummary());
    final BooleanBinding hasMessage = Bindings.isNotEmpty(summary.getChildren()).and(showSummary);
    summaryFlow.visibleProperty().bind(hasMessage);
    summaryFlow.managedProperty().bind(hasMessage);
//    topPane.bottomProperty()
//        .bind(Bindings.when(showSummary).then(summaryFlow).otherwise((StackPane) null));

    centerPane = new BorderPane();
    VBox centerFlow = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, searchBar, summaryFlow,
        centerPane);
    setCenter(centerFlow);

    createParameterComponents();
    this.viewType.subscribe((_) -> reLayoutDelay.playFromStart());

    // final check
    checkAllParametersCovered();
  }

  private void applyViewLayout(GroupView view) {
    centerPane.setCenter(null);
    switch (view) {
      case GROUPED -> {
        // only if enough parameters after filtering
        final int components = groupedParameterPanes.stream().map(ParameterGroupGrid::grid)
            .mapToInt(ParameterGridLayout::getNumComponents).sum();

        if (components < minimumParametersForGrouped) {
          logger.fine(
              "RELAYOUT Not enough parameters to show grouped view. Showing single list instead.");
          applyViewLayout(GroupView.SINGLE_LIST);
          return;
        }
        logger.fine("RELAYOUT GROUPED");

        final Accordion accordion = new Accordion();
        for (ParameterGroupGrid group : groupedParameterPanes) {
          if (!group.grid.hasComponents()) {
            // skip empty
            continue;
          }

          final TitledPane pane = FxLayout.newTitledPane(group.name(), group.grid());
          pane.getStyleClass().add("large-title-pane");
          accordion.getPanes().add(pane);
        }
        centerPane.setCenter(accordion);
        if (!accordion.getPanes().isEmpty()) {
          accordion.setExpandedPane(accordion.getPanes().getFirst());
        }
        centerPane.setCenter(accordion);
      }
      case SINGLE_LIST -> {
        logger.fine("RELAYOUT SINGLE");
        List<Node> list = new ArrayList<>();
        for (ParameterGroupGrid group : groupedParameterPanes) {
          ParameterGridLayout grid = group.grid();
          if (!grid.hasComponents()) {
            continue;
          }
          list.add(new Separator(Orientation.HORIZONTAL));
          list.add(FxLabels.styled(group.name(), Styles.BOLD_SEMI_TITLE));
          list.add(grid);
          // is false in accordion so need to reset
          grid.setVisible(true);
          grid.setManaged(true);
        }
        final Node[] nodes = list.toArray(Node[]::new);
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
  private void createParameterComponents() {
    if (!fixedParameters.isEmpty()) {
      final GridPane fixedGrid = parentPane.createParameterPane(fixedParameters);
      setTop(fixedGrid);
    }

    if (!groups.isEmpty()) {
      // for now just create the grouped view - so that components are initialized
      for (ParameterGroup group : groups) {
        final ParameterGridLayout grid = parentPane.createParameterPane(group.parameters());
        grid.setPadding(new Insets(DEFAULT_SPACE, DEFAULT_SPACE, DEFAULT_SPACE, DEFAULT_SPACE * 3));
        grid.searchTextProperty().bind(searchField.textProperty());
        groupedParameterPanes.add(new ParameterGroupGrid(group.name(), group.parameters(), grid));

        // listen for changes in optional selection state
        addSummaryListeners(grid);

        // add layout listeners
        grid.numComponentsProperty().subscribe((_, _) -> reLayoutDelay.playFromStart());
      }
      // layout is added later
    }
  }

  private void addSummaryListeners(ParameterGridLayout grid) {
    for (ParameterAndComponent pc : grid.getComponents().values()) {
      final BooleanProperty selectedProperty = ParameterUtils.getSelectedProperty(pc.component());
      if (selectedProperty != null) {
        selectedProperty.subscribe((_) -> updateSummary());
      }
    }
  }


  /**
   * concat all optional parameter values
   */
  private void updateSummary() {
    if (!showSummary.get()) {
      summary.getChildren().clear();
      return;
    }

    List<Parameter<?>> selected = new ArrayList<>();
    List<Text> texts = new ArrayList<>();
    texts.add(FxTexts.styledText("Summary: ", Styles.BOLD_SEMI_TITLE));

    for (ParameterGroupGrid group : groupedParameterPanes) {
      for (ParameterAndComponent pc : group.grid().getComponents().values()) {
        final UserParameter up = pc.parameter();
        final Node comp = pc.component();

        final BooleanProperty selectedProperty = ParameterUtils.getSelectedProperty(comp);
        if (selectedProperty != null && selectedProperty.get()) {
          selected.add(up);
          // make text clickable to auto filter
          final Text text = FxTexts.styledText(up.getName(), Styles.BOLD_SEMI_TITLE);
          text.getStyleClass().add("text-hover");
          text.setOnMouseClicked(_ -> setSearchFilter(up.getName()));
          if (selected.size() > 1) {
            texts.add(FxTexts.styledText(", ", Styles.BOLD_SEMI_TITLE));
          }
          texts.add(text);
        }
      }
    }

    summary.getChildren().setAll(texts);
  }


  /**
   * All {@link UserParameter} require an entry in both allParameters and in the fixed or grouped
   * parameters
   */
  private void checkAllParametersCovered() {
    final List<String> issues = new ArrayList<>();

    final var allParametersList = Arrays.stream(parentPane.getParameterSet().getParameters())
        .filter(UserParameter.class::isInstance).toList();
    final Set<Parameter<?>> allParametersSet = new HashSet<>(allParametersList);

    if (allParametersSet.size() != allParametersList.size()) {
      var duplicates = CollectionUtils.findDuplicates(allParametersList);
      issues.add("Duplicate parameters found in ParameterSet: " + StringUtils.join(duplicates, "\n",
          Parameter::getName));
    }

    Set<Parameter<?>> allComponentParameters = new HashSet<>();

    if (fixedParameters != null) {
      for (Parameter<?> fixed : fixedParameters) {
        if (!allParametersSet.contains(fixed)) {
          issues.add(
              "Fixed parameter " + fixed.getName() + " not found in ParameterSet with nparams="
                  + parentPane.getNumberOfParameters());
        }
        // already added? = duplicate
        if (!allComponentParameters.add(fixed)) {
          issues.add("Duplicate fixed parameter " + fixed.getName());
        }
      }
    }

    // need to check the actual parameter here
    final Parameter[] grouped = groups.stream().map(ParameterGroup::parameters)
        .flatMap(Collection::stream).toArray(Parameter[]::new);

    for (Parameter<?> groupedParam : grouped) {
      if (!allParametersSet.contains(groupedParam)) {
        issues.add("Grouped parameter " + groupedParam.getName()
            + " not found in ParameterSet with nparams=" + parentPane.getNumberOfParameters());
      }
      // already added? = duplicate
      if (!allComponentParameters.add(groupedParam)) {
        issues.add("Duplicate grouped parameter " + groupedParam.getName());
      }
    }

    // now check reverse if all user parameters from allParameters are actual visual as component
    for (Parameter<?> param : allParametersList) {
      if (!allComponentParameters.contains(param)) {
        issues.add("Parameter " + param.getName()
            + " from ParameterSet is not found in the grouped or fixed parameters");
      }
    }

    if (!issues.isEmpty()) {
      throw new IllegalArgumentException(String.join("\n", issues));
    }
  }

  public void setShowSummary(boolean showSummary) {
    this.showSummary.set(showSummary);
  }

  public enum GroupView {
    GROUPED, SINGLE_LIST
  }

  private record ParameterGroupGrid(String name, List<? extends Parameter<?>> parameters,
                                    ParameterGridLayout grid) {

  }

  public record ParameterGroup(String name, List<? extends Parameter<?>> parameters) {

    public ParameterGroup(String name, Parameter<?>... parameters) {
      this(name, List.of(parameters));
    }

    public ParameterGroup remap(ParameterSet paramset) {
      final List<? extends Parameter<?>> actual = ParameterUtils.mapToActualParameters(paramset,
          parameters);
      return new ParameterGroup(name, actual);
    }
  }
}
