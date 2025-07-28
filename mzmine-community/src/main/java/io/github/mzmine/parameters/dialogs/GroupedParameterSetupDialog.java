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
import static io.github.mzmine.javafx.components.util.FxLayout.newStackPane;

import impl.org.controlsfx.skin.PropertySheetSkin;
import io.github.mzmine.javafx.components.factories.FxLabels.Styles;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.collections.CollectionUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.control.PropertySheet.Mode;

/**
 * This class represents the parameter setup dialog to set the values of SimpleParameterSet. Each
 * Parameter is represented by a component. The component can be obtained by calling
 * getComponentForParameter(). Type of component depends on parameter type:
 * <p>
 * TODO: parameter setup dialog should show the name of the module in the title
 */
public class GroupedParameterSetupDialog extends EmptyParameterSetupDialogBase {


  private final ObservableList<Item> items;
  private final PropertySheet propertySheet;
  private final BooleanProperty showSummary = new SimpleBooleanProperty();
  /**
   * fixed parameters on top
   */
  private Parameter<?>[] fixedParameters;
  private final BorderPane topPane;
  private final TextFlow summary;

  public GroupedParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    this(valueCheckRequired, parameters, null);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public GroupedParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      Region message) {
    super(valueCheckRequired, parameters, message);

    items = FXCollections.observableArrayList();
    //    for (Parameter p : parameters.getParameters())
    //      items.add(new ParameterItem(p));

    propertySheet = new PropertySheet(items);
    propertySheet.setMode(Mode.CATEGORY);
    VBox.setVgrow(propertySheet, Priority.ALWAYS);

    propertySheet.setPropertyEditorFactory(param -> {
      if (param instanceof ParameterItem pitem
          && pitem.getClonedParameter() instanceof UserParameter up) {
        final Node editor = parametersAndComponents.get(up.getName());
        return new ParameterEditorWrapper(pitem, editor);
      } else {
        return null;
      }
    });

    centerPane.setCenter(propertySheet);

    // top is placeholder for fixedParam (center) and summary (bottom)
    topPane = new BorderPane();
    centerPane.setTop(topPane);

    summary = newTextFlow();
    final var summaryFlow = newStackPane(new Insets(DEFAULT_SPACE, DEFAULT_SPACE, 8, DEFAULT_SPACE),
        summary);
    topPane.bottomProperty()
        .bind(Bindings.when(showSummary).then(summaryFlow).otherwise((StackPane) null));

    setMinWidth(600);
    setMinHeight(400.0);

    centerOnScreen();
  }

  /**
   * concat all optional parameter values
   */
  private void updateSummary() {
    List<Parameter<?>> selected = new ArrayList<>();
    List<Text> texts = new ArrayList<>();
    texts.add(FxTexts.styledText("Summary: ", Styles.BOLD_SEMI_TITLE));

    for (Item item : items) {
      if (!(item instanceof ParameterItem pitem)
          || !(pitem.getClonedParameter() instanceof UserParameter up)) {
        continue;
      }
      final Node comp = getComponentForParameter(up);
      final BooleanProperty selectedProperty = ParameterUtils.getSelectedProperty(comp);
      if (selectedProperty != null && selectedProperty.get()) {
        selected.add(up);
        // make text clickable to auto filter
        final Text text = FxTexts.styledText(up.getName(), Styles.BOLD_SEMI_TITLE);
        text.setOnMouseClicked(event -> {
          setFilterText(up.getName());
        });
        if (selected.size() > 1) {
          texts.add(FxTexts.styledText(", ", Styles.BOLD_SEMI_TITLE));
        }
        texts.add(text);
      }
    }
    summary.getChildren().setAll(texts);
  }

  @Override
  public void showAndWait() {
    checkAllParametersCovered();
    super.showAndWait();
  }

  /**
   * All {@link UserParameter} require an entry in both allParameters and in the fixed or grouped
   * parameters
   */
  private void checkAllParametersCovered() {
    final List<String> issues = new ArrayList<>();

    final var allParametersList = Arrays.stream(getParamPane().getParameterSet().getParameters())
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
                  + getParamPane().getNumberOfParameters());
        }
        // already added? = duplicate
        if (!allComponentParameters.add(fixed)) {
          issues.add("Duplicate fixed parameter " + fixed.getName());
        }
      }
    }

    // need to check the actual parameter here
    Parameter[] grouped = items.stream().map(ParameterItem.class::cast)
        .map(ParameterItem::getClonedParameter).toArray(Parameter[]::new);
    grouped = mapToActualParameters(grouped);

    for (Parameter<?> groupedParam : grouped) {
      if (!allParametersSet.contains(groupedParam)) {
        issues.add("Grouped parameter " + groupedParam.getName()
            + " not found in ParameterSet with nparams=" + getParamPane().getNumberOfParameters());
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

  public PropertySheet getPropertySheet() {
    return propertySheet;
  }

  /**
   * Set the filter in the top of the property pane
   *
   * @param filter filters for the parameter titles
   */
  public void setFilterText(String filter) {
    if (propertySheet.getSkin() instanceof PropertySheetSkin skin) {
      try {
        Field searchField = PropertySheetSkin.class.getDeclaredField("searchField");
        searchField.setAccessible(true);
        TextField search = (TextField) searchField.get(skin);
        search.setText(filter);
      } catch (Exception e) {
        // silently fail if reflection fails and just set directly but this will not show in field
        propertySheet.setTitleFilter(filter);
      }
    }
  }

  public void addParameterGroup(String group, Parameter... parameters) {
    ParameterUtils.assertAllUserParameters(parameters);

    parameters = mapToActualParameters(parameters);
    for (Parameter p : parameters) {
      if (!(p instanceof UserParameter up)) {
        throw new IllegalArgumentException("Parameter " + p + " is not a UserParameter");
      }

      // create component once
      final Node editor = up.createEditingComponent();
      parametersAndComponents.put(up.getName(), editor);

      // listen to any optional parameter change
      final BooleanProperty selectedProperty = ParameterUtils.getSelectedProperty(editor);
      if (selectedProperty != null) {
        selectedProperty.subscribe((_) -> updateSummary());
      }
      items.add(new ParameterItem(up, group));
    }
  }

  /**
   * sets a fixed group on top of all other parameters
   */
  public void setFixedTopGroup(Parameter<?>... parameters) {
    ParameterUtils.assertAllUserParameters(parameters);

    // have to map all parameters to the actual parameters to not use the static params
    this.fixedParameters = mapToActualParameters(parameters);
    // automatically adds parameters to the parametersAndComponents map
    final GridPane paramPane = createParameterPane(parameters);
    topPane.setCenter(paramPane);
  }

  public void showSummaryOfSelectedParameters(boolean showSummary) {
    this.showSummary.set(showSummary);
  }

  /**
   * map to actual parameter instead of using another instance or the static instance.
   *
   * @throws IllegalArgumentException if a parameter is not a {@link UserParameter} only those have
   *                                  components to edit. Regular parameters can be passed into the
   *                                  ParameterSet but not into the fixed or grouped parameters.
   */
  private Parameter<?>[] mapToActualParameters(Parameter<?>[] parameters) {
    final Parameter<?>[] copy = new Parameter[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      // throws exception
      copy[i] = parameterSet.getParameter(parameters[i]);
      // just in case make sure it is not null
      if (copy[i] == null) {
        throw new IllegalArgumentException(
            "Parameter " + parameters[i] + " not found in " + parameterSet);
      }
    }
    return copy;
  }

}
