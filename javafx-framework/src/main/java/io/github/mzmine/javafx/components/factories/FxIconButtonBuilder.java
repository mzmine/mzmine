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

import io.github.mzmine.javafx.components.animations.FxFlashingAnimation;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.IconCodeSupplier;
import java.util.Objects;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.javafx.FontIcon;

public class FxIconButtonBuilder<T extends ButtonBase> {


  public enum EventHandling {
    /**
     * Consume events and do not pass them to the next parent object. Great for button that is on a
     * menu item for example
     */
    CONSUME_EVENTS,
    /**
     * Pass event to parent like default behavior
     */
    DEFAULT_PASS;
  }

  private final EventHandling eventHandling;
  private final FontIcon icon;
  private final T button;
  private BooleanExpression flashingProperty;

  public FxIconButtonBuilder(@NotNull final T button, @NotNull IconCodeSupplier iconCode) {
    this(button, iconCode, EventHandling.DEFAULT_PASS);
  }

  public FxIconButtonBuilder(@NotNull final T button, @NotNull IconCodeSupplier iconCode,
      @NotNull EventHandling eventHandling) {
    this(button, iconCode.getIconCode(), eventHandling);
  }

  public FxIconButtonBuilder(@NotNull final T button, @NotNull String iconCode) {
    this(button, iconCode, EventHandling.DEFAULT_PASS);
  }

  public FxIconButtonBuilder(@NotNull final T button, @NotNull String iconCode,
      @NotNull EventHandling eventHandling) {
    icon = new FontIcon(iconCode);
    this.eventHandling = eventHandling;
    icon.setIconSize(FxIconUtil.DEFAULT_ICON_SIZE);
    this.button = button;
    button.getStyleClass().add("icon-button");
  }

  public T build() {
    if (flashingProperty != null) {
      FxFlashingAnimation.animate(icon, flashingProperty);
    }

    button.setGraphic(icon);
    return button;
  }

  //
  public static FxIconButtonBuilder<Button> ofIconButton(IconCodeSupplier iconCode) {
    return ofIconButton(iconCode, EventHandling.DEFAULT_PASS);
  }

  public static FxIconButtonBuilder<Button> ofIconButton(IconCodeSupplier iconCode,
      @NotNull EventHandling eventHandling) {
    return new FxIconButtonBuilder<>(new Button(), iconCode.getIconCode(), eventHandling);
  }

  public static FxIconButtonBuilder<ToggleButton> ofToggleIconButton(IconCodeSupplier iconCode) {
    return new FxIconButtonBuilder<>(new ToggleButton(), iconCode.getIconCode());
  }

  public static FxIconButtonBuilder ofMenuIconButton(IconCodeSupplier iconCode) {
    return new FxIconButtonBuilder(new MenuButton(), iconCode.getIconCode());
  }

  public static FxIconButtonBuilder<Button> ofIconButton(String iconCode) {
    return new FxIconButtonBuilder<>(new Button(), iconCode);
  }

  public static FxIconButtonBuilder<ToggleButton> ofToggleIconButton(String iconCode) {
    return new FxIconButtonBuilder<>(new ToggleButton(), iconCode);
  }

  public FxIconButtonBuilder<T> tooltip(String tooltip) {
    if (tooltip != null) {
      button.setTooltip(new Tooltip(tooltip));
    }
    return this;
  }

  public FxIconButtonBuilder<T> size(int size) {
    icon.setIconSize(size);
    return this;
  }

  public FxIconButtonBuilder<T> onAction(Runnable onAction) {
    if (onAction != null) {
      button.setOnAction(e -> {
        onAction.run();
        if (Objects.requireNonNull(eventHandling) == EventHandling.CONSUME_EVENTS) {
          e.consume();
        }
      });
    }
    return this;
  }

  public FxIconButtonBuilder<T> onAction(EventHandler<ActionEvent> onAction) {
    if (onAction != null) {
      if (Objects.requireNonNull(eventHandling) == EventHandling.CONSUME_EVENTS) {
        button.setOnAction(e -> {
          onAction.handle(e);
          e.consume();
        });
      }
      button.setOnAction(onAction);
    }
    return this;
  }

  public FxIconButtonBuilder<T> bindDisable(final ObservableValue<? extends Boolean> disableWhen) {
    if (disableWhen != null) {
      button.disableProperty().bind(disableWhen);
    }
    return this;
  }

  public FxIconButtonBuilder<T> flashingProperty(final BooleanExpression flashingProperty) {
    this.flashingProperty = flashingProperty;
    return this;
  }
}
