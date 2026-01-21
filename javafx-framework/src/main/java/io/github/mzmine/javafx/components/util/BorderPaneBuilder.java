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

package io.github.mzmine.javafx.components.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class BorderPaneBuilder {

  private Node top;
  private Node bottom;
  private Node left;
  private Node right;
  private Node center;
  private Insets padding = Insets.EMPTY;

  public BorderPaneBuilder padding(Insets padding) {
    this.padding = padding;
    return this;
  }

  public BorderPaneBuilder defaultPadding() {
    this.padding = FxLayout.DEFAULT_PADDING_INSETS;
    return this;
  }

  public BorderPaneBuilder top(Node top) {
    this.top = top;
    return this;
  }

  public BorderPaneBuilder bottom(Node bottom) {
    this.bottom = bottom;
    return this;
  }

  public BorderPaneBuilder left(Node left) {
    this.left = left;
    return this;
  }

  public BorderPaneBuilder right(Node right) {
    this.right = right;
    return this;
  }

  public BorderPaneBuilder center(Node center) {
    this.center = center;
    return this;
  }

  public BorderPane build() {
    BorderPane borderPane = new BorderPane();
    borderPane.setTop(top);
    borderPane.setBottom(bottom);
    borderPane.setLeft(left);
    borderPane.setRight(right);
    borderPane.setCenter(center);
    if (padding != null) {
      borderPane.setPadding(padding);
    }
    return borderPane;
  }
}
