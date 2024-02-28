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

package io.github.mzmine.gui.mainwindow.tasksview;

import io.github.mzmine.gui.framework.fx.components.LabeledProgressBar;
import io.github.mzmine.gui.framework.fx.components.MenuItems;
import io.github.mzmine.gui.framework.fx.mvci.FxViewBuilder;
import javafx.beans.binding.Bindings;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * MVCI view that uses a builder to generate a MiniTaskView on demand
 */
public class MiniTaskView extends FxViewBuilder<TasksViewModel> {

  MiniTaskView(final TasksViewModel model) {
    super(model);
  }

  @Override
  public Region build() {
    LabeledProgressBar batchPane = createBatchPane();
    LabeledProgressBar taskPane = createTaskPane();

    setupContextMenus(taskPane, batchPane);

    HBox hBox = new HBox(5, batchPane, taskPane);
    hBox.setFillHeight(true);
    HBox.setHgrow(batchPane, Priority.ALWAYS);
    HBox.setHgrow(taskPane, Priority.ALWAYS);
    return hBox;
  }

  private LabeledProgressBar createTaskPane() {
    return new LabeledProgressBar( //
        model.allTasksProgressProperty(),
        Bindings.createStringBinding(this::computeTasksText, model.getTasks()));
  }

  private LabeledProgressBar createBatchPane() {
    var pane = new LabeledProgressBar( //
        model.batchProgressProperty(), model.batchDescriptionProperty());
    pane.visibleProperty().bind(model.batchIsRunningProperty());
    return pane;
  }

  private String computeTasksText() {
    var size = model.getTasks().size();
    if (size == 0) {
      return "0 tasks";
    }
    return "%d tasks (%.0f %%)".formatted(size, model.getAllTasksProgress() * 100.0);
  }

  private void setupContextMenus(final LabeledProgressBar taskPane,
      final LabeledProgressBar batchPane) {
    // task pane
    ContextMenu taskMenu = new ContextMenu(
        MenuItems.create("Show tasks view", model.onShowTasksViewProperty()),
        MenuItems.create("Cancel all tasks", model.onCancelAllTasksProperty()));
    taskPane.setContextMenu(taskMenu);

    EventHandler<MouseEvent> openTaskView = e -> {
      if (e.getButton() == MouseButton.PRIMARY) {
        var onShowTasksView = model.getOnShowTasksView();
        if (onShowTasksView != null) {
          onShowTasksView.handle(null);
        }
      }
    };
    taskPane.getLabel().setOnMousePressed(openTaskView);
    taskPane.getProgressBar().setOnMousePressed(openTaskView);

    // batch
    ContextMenu taskBatch = new ContextMenu(
        MenuItems.create("Cancel batch", model.onCancelBatchTaskProperty()));
    batchPane.setContextMenu(taskBatch);
  }

}
