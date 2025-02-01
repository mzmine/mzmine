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

import io.github.mzmine.parameters.EmbeddedParameterComponentProvider;
import io.github.mzmine.parameters.EstimatedComponentHeightProvider;
import io.github.mzmine.parameters.EstimatedComponentWidthProvider;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class OptionalModuleComponent extends BorderPane implements EstimatedComponentHeightProvider,
    EstimatedComponentWidthProvider, EmbeddedParameterComponentProvider {

  // null if shown in dialog
  protected final @Nullable ParameterSetupPane paramPane;
  protected final @NotNull CheckBox checkBox;
  private final Button setButton;
  private final BooleanProperty hidden = new SimpleBooleanProperty(true);
  private final DoubleProperty estimatedHeightProperty = new SimpleDoubleProperty(0);
  private final DoubleProperty estimatedWidthProperty = new SimpleDoubleProperty(0);
  protected final FlowPane topPane;


  public OptionalModuleComponent(ParameterSet embeddedParameters,
      EmbeddedComponentOptions viewOption, boolean alwaysActive) {
    this(embeddedParameters, viewOption, "", alwaysActive, alwaysActive);
  }

  public OptionalModuleComponent(ParameterSet embeddedParameters,
      EmbeddedComponentOptions viewOption, String title, boolean alwaysActive, boolean active) {
    super();
    checkBox = new CheckBox(title);
    setSelected(active);
    checkBox.selectedProperty().addListener((ob, ov, nv) -> applyCheckBoxState());
    if (viewOption == EmbeddedComponentOptions.VIEW_IN_WINDOW) {
      paramPane = null;
      setButton = new Button("Setup...");
      setButton.setOnAction(e -> embeddedParameters.showSetupDialog(false));
    } else {
      // use internal parameter pane
      paramPane = ParameterSetupPane.createEmbedded(true, embeddedParameters, null);

      setButton = new Button("Show");
      setButton.setOnAction(e -> {
        boolean toggledHidden = !hidden.get();
        // change text
        setButton.setText(toggledHidden ? "Show" : "Hide");
        setCenter(toggledHidden ? null : paramPane);
        // events
        hidden.set(toggledHidden);

        // estimate new height
        var params =
            toggledHidden ? 0 : getEmbeddedParameterPane().getParametersAndComponents().size();
        setEstimatedHeight(params);

        setEstimatedDefaultWidth(params == 0);

        onViewStateChange(toggledHidden);
      });
      setButton.setDisable(!active);
    }
    topPane = new FlowPane();
    topPane.setHgap(5d);
    topPane.setVgap(5d);

    // just leave out checkbox if always active
    if (alwaysActive) {
      topPane.getChildren().addAll(setButton);
    } else {
      topPane.getChildren().addAll(checkBox, setButton);
    }

    setTop(topPane);
    applyCheckBoxState();
  }

  public void onViewStateChange(final boolean hidden) {

  }

  public ParameterSetupPane getEmbeddedParameterPane() {
    return paramPane;
  }

  public boolean isSelected() {
    return checkBox.isSelected();
  }

  public void setSelected(boolean state) {
    checkBox.setSelected(state);
  }

  public void setToolTipText(String toolTip) {
    checkBox.setTooltip(new Tooltip(toolTip));
  }

  public CheckBox getCheckbox() {
    return checkBox;
  }

  protected void applyCheckBoxState() {
    setButton.setDisable(!checkBox.isSelected());
    if (paramPane != null) {
      paramPane.getParametersAndComponents().values()
          .forEach(node -> node.setDisable(!checkBox.isSelected()));
    }
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

  @Override
  public @Nullable Map<String, Node> getParametersAndComponents() {
    return getEmbeddedParameterPane() != null
        ? getEmbeddedParameterPane().getParametersAndComponents() : null;
  }
}
