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

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

/**
 * Creates PopOvers that can be shown by popOver.show(owner). Owner should not be a {@link ComboBox}
 * or other components that may consume clicks or lock in focus. Otherwise the popup becomes modal.
 * <p>
 * Just wrap ComboBox or similar into a pane and use that as the owner.
 */
public class FxPopOvers {

  public static PopOver newPopOver(Node content) {
    return newPopOver(content, ArrowLocation.BOTTOM_LEFT);
  }

  public static PopOver newPopOver(Node content, ArrowLocation arrowLocation) {
    final StackPane wrapper = new StackPane(content);
    wrapper.getStyleClass().add("outlined-panel");

    var popOver = new PopOver(wrapper);
    popOver.setAutoHide(true);
    popOver.setAutoFix(true);
    popOver.setHideOnEscape(true);
    popOver.setDetachable(true);
    popOver.setAnimated(true);
    popOver.setDetached(false);
    popOver.setArrowLocation(arrowLocation);
    popOver.setFadeInDuration(Duration.millis(500));
    popOver.setFadeOutDuration(Duration.millis(500));
//    popOver.show(owner);
    return popOver;
  }

}
