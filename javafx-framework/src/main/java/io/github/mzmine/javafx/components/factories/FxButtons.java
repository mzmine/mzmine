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

package io.github.mzmine.javafx.components.factories;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.components.util.FxControls;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.util.IconCodeSupplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxButtons {

  public static Button createButton(String label, Runnable onAction) {
    return createButton(label, null, onAction);
  }

  public static Button createButton(String label, @Nullable String tooltip, Runnable onAction) {
    return createButton(label, tooltip, null, onAction);
  }

  public static Button createDisabledButton(String label, @Nullable String tooltip,
      @Nullable ObservableBooleanValue disableBinding, Runnable onAction) {
    return createDisabledButton(label, null, tooltip, disableBinding, onAction);
  }

  public static Button createDisabledButton(String label, @Nullable IconCodeSupplier icon,
      @Nullable String tooltip, @Nullable ObservableBooleanValue disableBinding,
      Runnable onAction) {
    return disableIf(createButton(label, icon, tooltip, onAction), disableBinding);
  }

  public static Button createDisabledLabelButton(ObservableValue<String> label,
      @Nullable IconCodeSupplier icon, @Nullable String tooltip,
      @Nullable ObservableBooleanValue disableBinding, Runnable onAction) {
    return disableIf(createLabelButton(label, icon, tooltip, onAction), disableBinding);
  }

  public static Button createDisabledButton(Node icon, @Nullable String tooltip,
      @Nullable ObservableBooleanValue disableBinding, Runnable onAction) {
    return disableIf(createButton(null, tooltip, icon, onAction), disableBinding);
  }


  public static Button createButton(Node icon, Runnable onAction) {
    return createButton(icon, null, onAction);
  }

  public static Button createButton(Node icon, @Nullable String tooltip, Runnable onAction) {
    return createButton(null, tooltip, icon, onAction);
  }

  public static Button createButton(@Nullable String label, @Nullable String tooltip,
      @Nullable Node icon, Runnable onAction) {
    return createButton(label, tooltip, icon, _ -> onAction.run());
  }

  public static Button createButton(@Nullable String label, @Nullable IconCodeSupplier icon,
      @Nullable String tooltip, Runnable onAction) {
    return createButton(label, tooltip, icon == null ? null : FxIconUtil.getFontIcon(icon),
        _ -> onAction.run());
  }

  public static Button createLabelButton(@NotNull ObservableValue<String> label,
      @Nullable IconCodeSupplier icon, @Nullable String tooltip, Runnable onAction) {
    return createLabelButton(label, tooltip, icon == null ? null : FxIconUtil.getFontIcon(icon),
        _ -> onAction.run());
  }

  public static Button createButton(String label, String tooltip,
      EventHandler<ActionEvent> onAction) {
    return createButton(label, tooltip, null, onAction);
  }

  public static Button createLabelButton(@NotNull ObservableValue<String> label,
      @Nullable String tooltip, @Nullable Node icon, EventHandler<ActionEvent> onAction) {
    final Button button = createButton(null, tooltip, icon, onAction);
    button.textProperty().bind(label);
    return button;
  }

  public static Button createButton(@Nullable String label, @Nullable String tooltip,
      @Nullable Node icon, EventHandler<ActionEvent> onAction) {
    Button b = new Button(label, icon);
    b.setOnAction(onAction);
    if (tooltip != null) {
      Tooltip.install(b, new Tooltip(tooltip));
    }
    return b;
  }

  public static Button graphicButton(@NotNull Node icon, @Nullable String tooltip,
      EventHandler<ActionEvent> onAction) {
    Button b = new Button(null, icon);
    b.setOnAction(onAction);
    if (tooltip != null) {
      Tooltip.install(b, new Tooltip(tooltip));
    }
    b.getStyleClass().add("icon-button");
    return b;
  }

  public static Button createButton(final String label, final String tooltip,
      final ObjectProperty<EventHandler<ActionEvent>> handlerProperty) {
    Button b = new Button(label);
    b.onActionProperty().bind(handlerProperty);
    if (tooltip != null) {
      b.setTooltip(new Tooltip(tooltip));
    }
    return b;
  }

  public static Button createSaveButton(Runnable runnable) {
    return createSaveButton("Save", runnable);
  }

  public static Button createSaveButton(String text, Runnable runnable) {
    return createButton(text, null, FxIconUtil.getFontIcon(FxIcons.SAVE), runnable);
  }

  public static Button createLoadButton(Runnable runnable) {
    return createLoadButton("Load", runnable);
  }

  public static Button createLoadButton(String text, Runnable runnable) {
    return createButton(text, null, FxIconUtil.getFontIcon(FxIcons.LOAD), runnable);
  }

  public static Button createCancelButton(Runnable runnable) {
    return createCancelButton("Cancel", runnable);
  }

  public static Button createCancelButton(String text, Runnable runnable) {
    return createButton(text, null, FxIconUtil.getFontIcon(FxIcons.CANCEL), runnable);
  }

  /**
   * Add disableProperty binding
   */
  public static <T extends Control> T disableIf(T control,
      ObservableBooleanValue disableCondition) {
    return FxControls.disableIf(control, disableCondition);
  }


  public static Button createHelpButton(String url) {
    return FxButtons.createButton("Help", "Open the documentation",
        FxIconUtil.getFontIcon(FxIcons.QUESTIONMARK),
        () -> DesktopService.getDesktop().openWebPage(url));
  }

  public static ToggleButton createToggleButton(@Nullable String selectedLabel,
      @Nullable String unselectedLabel, @Nullable BooleanProperty selected) {
    return createToggleButton(selectedLabel, unselectedLabel, selected, null);
  }

  public static ToggleButton createToggleButton(@Nullable String selectedLabel,
      @Nullable String unselectedLabel, @Nullable BooleanProperty selected,
      @Nullable String tooltip) {
    final ToggleButton button = new ToggleButton();
    if (tooltip != null) {
      button.setTooltip(new Tooltip(tooltip));
    }
    button.selectedProperty().bindBidirectional(selected);

    if (selectedLabel != null && unselectedLabel != null) {
      button.textProperty().bind(
          button.selectedProperty().map(state -> state ? selectedLabel : unselectedLabel)
              .orElse(selectedLabel));
    } else if (selectedLabel != null ^ unselectedLabel != null) {
      throw new IllegalArgumentException(
          "Either both selected and unselected labels are set or null. Here one is null and one is set.");
    }

    return button;
  }
}
