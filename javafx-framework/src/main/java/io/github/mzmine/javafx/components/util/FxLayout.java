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

package io.github.mzmine.javafx.components.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;

public class FxLayout {

  public static final int DEFAULT_SPACE = 5;
  public static final Insets DEFAULT_INSETS = new Insets(5);

  public static FlowPane newFlowPane() {
    return new FlowPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
  }

  public static FlowPane newFlowPane(Node... children) {
    return newFlowPane(DEFAULT_INSETS, children);
  }

  public static FlowPane newFlowPane(Insets insets, Node... children) {
    var flowPane = new FlowPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE, children);
    flowPane.setOpaqueInsets(insets);
    return flowPane;
  }

}
