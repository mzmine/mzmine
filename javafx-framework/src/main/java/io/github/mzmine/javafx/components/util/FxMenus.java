/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.javafx.components.util;

import io.github.mzmine.javafx.components.factories.FxLabels;
import javafx.scene.Node;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCodeCombination;
import org.jetbrains.annotations.Nullable;

public class FxMenus {

  public static <T> MenuItem newMenuItem(@Nullable Node graphicNode, @Nullable String text,
      @Nullable String tooltip, @Nullable Runnable onClick) {
    return newMenuItem(graphicNode, text, tooltip, null, onClick, null);
  }

  public static <T> MenuItem newMenuItem(@Nullable Node graphicNode, @Nullable String text,
      @Nullable String tooltip, @Nullable T userData, @Nullable Runnable onClick,
      @Nullable KeyCodeCombination accelerator) {
    if (graphicNode == null && text == null) {
      throw new IllegalArgumentException("Either graphicNode or text must be provided");
    }

    final MenuItem menuItem;
    final Node main;
    if (text != null) {
      // text and optional node on left
      main = FxLayout.newHBox(graphicNode, FxLabels.newLabel(text));
      menuItem = new CustomMenuItem(main, false);
    } else {
      // only show node
      menuItem = new CustomMenuItem(graphicNode, false);
      main = graphicNode;
    }
    if (tooltip != null) {
      Tooltip.install(main, new Tooltip(tooltip));
    }

    if (onClick != null) {
      menuItem.setOnAction(_ -> onClick.run());
    }
    if (userData != null) {
      menuItem.setUserData(userData);
    }
    menuItem.setAccelerator(accelerator);
    return menuItem;
  }
}
