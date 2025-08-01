/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.submodules;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.parameters.EmbeddedParameterComponentProvider;
import io.github.mzmine.parameters.EstimatedComponentHeightProvider;
import io.github.mzmine.parameters.EstimatedComponentWidthProvider;
import io.github.mzmine.parameters.FullColumnComponent;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class ModuleOptionsEnumComponent<EnumType extends Enum<EnumType> & ModuleOptionsEnum> extends
    BorderPane implements EstimatedComponentHeightProvider, EstimatedComponentWidthProvider,
    FullColumnComponent, EmbeddedParameterComponentProvider {

  private final ComboBox<EnumType> combo;

  protected final Map<EnumType, ParameterSetupPane> paramPanesMap;
  protected @Nullable ParameterSetupPane paramPane;
  private final BooleanProperty hidden = new SimpleBooleanProperty(true);
  private final DoubleProperty estimatedHeightProperty = new SimpleDoubleProperty(0);
  private final DoubleProperty estimatedWidthProperty = new SimpleDoubleProperty(0);
  private final ObjectProperty<EnumType> selectedValue = new SimpleObjectProperty<>();
  protected final FlowPane topPane;
  @Nullable
  private Runnable onSubParametersChanged = null;

  public ModuleOptionsEnumComponent(String name,
      final EnumMap<EnumType, ParameterSet> parametersMap, final EnumType startValue,
      final boolean alwaysOpen) {
    super();
    hidden.set(!alwaysOpen);
    this.selectedValue.set(startValue);
    combo = FxComboBox.createComboBox("Options", parametersMap.keySet(), this.selectedValue);

    // create all parameter panes - this is important to keep listeners to components alive and stable
    paramPanesMap = parametersMap.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
        e -> ParameterSetupPane.createEmbedded(true, e.getValue(), () -> {
          if (onSubParametersChanged != null) {
            // option to pass change events up the chain
            onSubParametersChanged.run();
          }
        })));

    BorderPane paramHolder = new BorderPane();
    // selection changes creates a new parameter pane
    this.selectedValue.subscribe((newValue) -> {
      if (paramPane != null) {
        // reflect parameters values
        paramPane.updateParameterSetFromComponents();
      }

      // use internal parameter pane
      paramPane = paramPanesMap.get(newValue);
      paramHolder.setCenter(paramPane);

      // parameters have changed already due to change of selected option
      if (onSubParametersChanged != null) {
        onSubParametersChanged.run();
      }

      var parent = getParent();
      if (parent != null) {
        parent.layout();
      }
    });

    paramHolder.setBottom(new Separator(Orientation.HORIZONTAL));

    Button setButton = FxButtons.createButton("Show", () -> hidden.set(!hidden.get()));
    setButton.textProperty().bind(hidden.map(hidden -> hidden ? "Show" : "Hide"));
    // auto show paramPane
    centerProperty().bind(hidden.map(hidden -> hidden ? null : paramHolder));

    hidden.subscribe(newValue -> {
      var paramPane = getEmbeddedParameterPane();
      if (paramPane == null) {
        return;
      }
      // estimate new height
      var params = newValue ? 0 : paramPane.getParametersAndComponents().size();
      setEstimatedHeight(params);

      setEstimatedDefaultWidth(params == 0);

      onViewStateChange(newValue);
    });

    topPane = new FlowPane(5, 5, FxLabels.newBoldLabel(name), combo);
    if (!alwaysOpen) {
      topPane.getChildren().add(setButton);
    }

    setTop(topPane);
  }

  public void onViewStateChange(final boolean hidden) {

  }

  public ParameterSetupPane getEmbeddedParameterPane() {
    return paramPane;
  }

  public @NotNull Collection<Node> getComponents() {
    return paramPane.getParametersAndComponents().values();
  }

  @Override
  @Nullable
  public Map<String, Node> getParametersAndComponents() {
    return paramPane == null ? null : paramPanesMap.values().stream()
        .flatMap(pane -> pane.getParametersAndComponents().entrySet().stream())
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }


  public EnumType getValue() {
    return selectedValue.getValue();
  }

  public void setSelectedValue(EnumType selected) {
    selectedValue.setValue(selected);
  }

  public void setToolTipText(String toolTip) {
    combo.setTooltip(new Tooltip(toolTip));
  }

  public void setParameterValuesToComponents() {
    if (paramPane != null) {
      paramPane.setParameterValuesToComponents();
    }
  }

  public void updateParameterSetFromComponents() {
    if (paramPane != null) {
      paramPane.updateParameterSetFromComponents();
    }
  }

  @Override
  public DoubleProperty estimatedHeightProperty() {
    return estimatedHeightProperty;
  }

  @Override
  public DoubleProperty estimatedWidthProperty() {
    return estimatedWidthProperty;
  }

  public ObjectProperty<EnumType> selectedValueProperty() {
    return selectedValue;
  }

  public EnumType getSelectedValue() {
    return selectedValue.get();
  }

  /**
   * @param onChange is called on parameter changes through the embedded sub parameters
   */
  public void addSubParameterChangedListener(final Runnable onChange) {
    this.onSubParametersChanged = onChange;
  }
}
