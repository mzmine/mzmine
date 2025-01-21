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

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxTooltips {

  public static @NotNull Tooltip newTooltip(@Nullable String text) {
    return newTooltip(text, Duration.seconds(30));
  }

  public static @NotNull Tooltip newTooltip(@Nullable String text, @Nullable Duration duration) {
    final Tooltip tooltip = new Tooltip(text);
    tooltip.setWrapText(true);
    tooltip.setMaxWidth(1000);
    tooltip.setShowDuration(duration);
    return tooltip;
  }

  public static @NotNull Tooltip install(@NotNull Tooltip tooltip, Node... nodes) {
    for (final Node node : nodes) {
      Tooltip.install(node, tooltip);
    }
    return tooltip;
  }

  public static @NotNull Tooltip uninstall(@NotNull Tooltip tooltip, Node... nodes) {
    for (final Node node : nodes) {
      Tooltip.uninstall(node, tooltip);
    }
    return tooltip;
  }


}
