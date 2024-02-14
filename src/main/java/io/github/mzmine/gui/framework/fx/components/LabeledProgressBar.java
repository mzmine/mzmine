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

package io.github.mzmine.gui.framework.fx.components;

import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;

public class LabeledProgressBar extends StackPane {

  private final Label label;
  private final ProgressBar progressBar;

  private LabeledProgressBar(final StringExpression labelBinding) {
    progressBar = new ProgressBar(0.0d);
    progressBar.setOpacity(0.3);
    progressBar.setMaxWidth(Double.MAX_VALUE);
    label = new Label("");
    label.setMouseTransparent(true);

    this.getChildren().addAll();

    // bindings
    label.textProperty().bind(labelBinding);
    this.getChildren().addAll(progressBar, label);
  }

  /**
   * Creates a progress pane with overlayed text
   *
   * @param progressBinding progress binding needs to be from 0-1, e.g., use
   *                        {@link DoubleProperty#multiply(int)}
   * @param labelBinding    label binding will be displayed above the progress bar
   */
  public LabeledProgressBar(final ObservableValue<Double> progressBinding,
      final StringExpression labelBinding) {
    this(labelBinding);
    progressBar.progressProperty().bind(progressBinding);
  }

  /**
   * Creates a progress pane with overlayed text
   *
   * @param progressBinding progress binding needs to be from 0-1, e.g., use
   *                        {@link DoubleProperty#multiply(int)}
   * @param labelBinding    label binding will be displayed above the progress bar
   */
  public LabeledProgressBar(final DoubleExpression progressBinding,
      final StringExpression labelBinding) {
    this(labelBinding);
    progressBar.progressProperty().bind(progressBinding);
  }

  public Label getLabel() {
    return label;
  }

  public ProgressBar getProgressBar() {
    return progressBar;
  }

  public void setContextMenu(final ContextMenu taskMenu) {
    progressBar.setContextMenu(taskMenu);
    label.setContextMenu(taskMenu);
  }
}

