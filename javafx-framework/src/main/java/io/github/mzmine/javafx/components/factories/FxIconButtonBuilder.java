/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.javafx.FontIcon;

public class FxIconButtonBuilder {

  private final FontIcon icon;
  private final ButtonBase button;
  private BooleanExpression flashingProperty;

  public FxIconButtonBuilder(@NotNull final ButtonBase button, @NotNull IconCodeSupplier iconCode) {
    this(button, iconCode.getIconCode());
  }

  public FxIconButtonBuilder(@NotNull final ButtonBase button, @NotNull String iconCode) {
    icon = new FontIcon(iconCode);
    icon.setIconSize(FxIconUtil.DEFAULT_ICON_SIZE);
    this.button = button;
    button.getStyleClass().add("icon-button");
  }


  public ButtonBase build() {
    if (flashingProperty != null) {
      FxFlashingAnimation.animate(icon, flashingProperty);
    }

    button.setGraphic(icon);
    return button;
  }

  //
  public static FxIconButtonBuilder ofIconButton(IconCodeSupplier iconCode) {
    return new FxIconButtonBuilder(new Button(), iconCode.getIconCode());
  }

  public static FxIconButtonBuilder ofMenuIconButton(IconCodeSupplier iconCode) {
    return new FxIconButtonBuilder(new MenuButton(), iconCode.getIconCode());
  }

  public static FxIconButtonBuilder ofToggleIconButton(IconCodeSupplier iconCode) {
    return new FxIconButtonBuilder(new ToggleButton(), iconCode.getIconCode());
  }

  public static FxIconButtonBuilder ofIconButton(String iconCode) {
    return new FxIconButtonBuilder(new Button(), iconCode);
  }

  public static FxIconButtonBuilder ofToggleIconButton(String iconCode) {
    return new FxIconButtonBuilder(new ToggleButton(), iconCode);
  }

  public FxIconButtonBuilder tooltip(String tooltip) {
    if (tooltip != null) {
      button.setTooltip(new Tooltip(tooltip));
    }
    return this;
  }

  public FxIconButtonBuilder size(int size) {
    icon.setIconSize(size);
    return this;
  }

  public FxIconButtonBuilder onAction(Runnable onAction) {
    if (onAction != null) {
      button.setOnAction(_ -> onAction.run());
    }
    return this;
  }

  public FxIconButtonBuilder bindDisable(final ObservableValue<? extends Boolean> disableWhen) {
    if (disableWhen != null) {
      button.disableProperty().bind(disableWhen);
    }
    return this;
  }

  public FxIconButtonBuilder flashingProperty(final BooleanExpression flashingProperty) {
    this.flashingProperty = flashingProperty;
    return this;
  }
}
