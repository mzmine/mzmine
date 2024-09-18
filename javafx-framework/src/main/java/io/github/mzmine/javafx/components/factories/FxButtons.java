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

package io.github.mzmine.javafx.components.factories;

import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.util.IconCodeSupplier;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
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

  public static Button createButton(@Nullable String label, @NotNull IconCodeSupplier icon,
      @Nullable String tooltip, Runnable onAction) {
    return createButton(label, tooltip, FxIconUtil.getFontIcon(icon), _ -> onAction.run());
  }

  public static Button createButton(String label, String tooltip,
      EventHandler<ActionEvent> onAction) {
    return createButton(label, tooltip, null, onAction);
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
}
