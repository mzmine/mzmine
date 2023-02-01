/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.util.javafx;

import io.github.mzmine.taskcontrol.TaskController;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class MiniTaskView extends StackPane {

  private final ProgressBar progressBar = new ProgressBar(0.0d);
  private ContextMenu contextMenu = null;
  private final Label label = new Label();

  public MiniTaskView() {
    this.getChildren().add(progressBar);
    this.getChildren().add(label);
    label.setMouseTransparent(true);

    widthProperty().addListener(
        ((observableValue, oldValue, newValue) -> progressBar.setMinWidth(newValue.doubleValue())));
    label.setText("%d tasks".formatted(0));
  }

  public void refresh(TaskController controller) {
    if (controller == null) {
      label.setText("ERROR: No task controller.");
      return;
    }
    final int percent = controller.getTaskQueue().getTotalPercentComplete();
    final int tasks = controller.getTaskQueue().getQueueSnapshot().length;

    progressBar.setProgress(percent * 0.01);
    label.setText(tasks != 0 ? "%d tasks (%d %%)".formatted(tasks, percent) : "%d tasks".formatted(tasks));
  }

  public ContextMenu getProgressBarContextMenu() {
    if (contextMenu == null) {
      contextMenu = new ContextMenu();
      progressBar.setContextMenu(contextMenu);
    }
    return contextMenu;
  }

  public void setOnProgressBarClicked(EventHandler<? super MouseEvent> e) {
    progressBar.setOnMousePressed(e);
  }
}
