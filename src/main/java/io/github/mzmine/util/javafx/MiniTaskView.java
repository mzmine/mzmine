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

import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import javafx.beans.NamedArg;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

public class MiniTaskView extends FlowPane {

  private final ProgressBar tasksProgressBar = new ProgressBar(0.0d);
  private final Label tasksLabel = new Label();

  private final ProgressBar batchProgressBar = new ProgressBar(0.0d);
  private final Label batchLabel = new Label();
  private final StackPane batchPane = new StackPane();
  private ContextMenu contextMenu = null;
  private ContextMenu batchContextMenu = null;

  public MiniTaskView(@NamedArg("progressBarOpacity") Double progressBarOpacity) {

    batchPane.getChildren().add(batchProgressBar);
    batchPane.getChildren().add(batchLabel);
    batchLabel.setMouseTransparent(true);
    batchPane.setVisible(false);
    getChildren().add(batchPane);

    var mainProgress = new StackPane();
    mainProgress.getChildren().add(tasksProgressBar);
    mainProgress.getChildren().add(tasksLabel);
    tasksLabel.setMouseTransparent(true);
    getChildren().add(mainProgress);

    widthProperty().addListener(((observableValue, oldValue, newValue) -> {
      tasksProgressBar.setMinWidth(Math.floor((getWidth() - getHgap()) / 2));
      batchProgressBar.setMinWidth(Math.floor((getWidth() - getHgap()) / 2));
    }));

    if (progressBarOpacity != null) {
      batchProgressBar.setOpacity(progressBarOpacity);
      tasksProgressBar.setOpacity(progressBarOpacity);
    }

    tasksLabel.setText("%d tasks".formatted(0));
  }

  public void refresh(TaskController controller) {
    if (controller == null) {
      tasksLabel.setText("ERROR: No task controller.");
      return;
    }
    final int percent = controller.getTaskQueue().getTotalPercentComplete();
    final WrappedTask[] queueSnapshot = controller.getTaskQueue().getQueueSnapshot();
    final int tasks = queueSnapshot.length;

    tasksProgressBar.setProgress(percent * 0.01);
    tasksLabel.setText(
        tasks != 0 ? "%d tasks (%d %%)".formatted(tasks, percent) : "%d tasks".formatted(tasks));

    if (controller.isTaskInstanceRunningOrQueued(BatchTask.class)) {
      if (!getChildren().contains(batchPane)) {
        batchPane.setVisible(true);
        getChildren().add(0, batchPane);
      }
      for (WrappedTask wrappedTask : queueSnapshot) {
        if (wrappedTask.getActualTask() instanceof BatchTask batchTask) {
          final double batchFinished = batchTask.getFinishedPercentage();
          batchProgressBar.setProgress(batchFinished);
          batchLabel.setText(batchTask.getTaskDescription());
        }
      }
    } else {
      getChildren().remove(batchPane);
      batchPane.setVisible(false);
      batchProgressBar.setProgress(0d);
      batchLabel.setText("No batch");
    }
  }

  public ContextMenu getProgressBarContextMenu() {
    if (contextMenu == null) {
      contextMenu = new ContextMenu();
      tasksProgressBar.setContextMenu(contextMenu);
    }
    return contextMenu;
  }

  public void setOnProgressBarClicked(EventHandler<? super MouseEvent> e) {
    tasksProgressBar.setOnMousePressed(e);
    batchProgressBar.setOnMousePressed(e);
  }

  public ContextMenu getBatchBarContextMenu() {
    if (batchContextMenu == null) {
      batchContextMenu = new ContextMenu();
      batchProgressBar.setContextMenu(batchContextMenu);
    }
    return batchProgressBar.getContextMenu();
  }
}
