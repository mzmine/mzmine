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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import io.github.mzmine.javafx.components.util.FxLayout;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.StackPane;

public class InChartInfoPane extends StackPane {

  protected final double DEFAULT_WIDTH = 150;
  protected final double DEFAULT_HEIGHT = 150;

  private final BooleanProperty fixedVisible = new SimpleBooleanProperty(false);

  public InChartInfoPane() {
    setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    getStyleClass().add("region-match-chart-bg");
    this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  public void bindVisible(final BooleanExpression visible) {
    BooleanBinding combined = fixedVisible.or(visible);
    visibleProperty().bind(combined);
    managedProperty().bind(combined);
  }

  public void setSize(final double w, final double h) {
    setMaxSize(w, h);
    setPrefSize(w, h);
  }

  public boolean isFixedVisible() {
    return fixedVisible.get();
  }

  public BooleanProperty fixedVisibleProperty() {
    return fixedVisible;
  }

  public void setFixedVisible(final boolean fixedVisible) {
    this.fixedVisible.set(fixedVisible);
  }
}
