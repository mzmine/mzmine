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

package io.github.mzmine.javafx.components.factories;

import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Still needs more styling to be viable
 */
public class FxPopOvers {

  public static PopOver newPopOver(@Nullable Node content) {
    return newPopOver(content, ArrowLocation.TOP_CENTER);
  }

  public static PopOver newPopOver(@Nullable Node content, @NotNull ArrowLocation arrowLocation) {
    var popOver = new PopOver(content);
    popOver.setAutoHide(true);
    popOver.setAutoFix(true);
    popOver.setHideOnEscape(true);
    popOver.setDetachable(false);
    popOver.setAnimated(false);
    popOver.setDetached(false);
    popOver.setArrowLocation(arrowLocation);
//    popOver.show(owner);
    return popOver;
  }

  public static void install(@NotNull ButtonBase button, @NotNull PopOver popOver) {
    button.setOnAction(_ -> {
      if (popOver.showingProperty().get()) {
        popOver.hide();
      } else {
        popOver.show(button);
      }
    });
  }
}
