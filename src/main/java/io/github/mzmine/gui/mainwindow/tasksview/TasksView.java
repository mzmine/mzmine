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

import static io.github.mzmine.util.javafx.TableViewUtils.createColumn;

import io.github.mzmine.gui.framework.fx.components.LabeledProgressBarCell;
import io.github.mzmine.gui.framework.fx.components.MenuItems;
import io.github.mzmine.gui.framework.fx.mvci.FxViewBuilder;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.util.javafx.TableViewUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * MVCI view that uses a builder to generate a TasksView on demand
 */
public class TasksView extends FxViewBuilder<TasksViewModel> {

  TasksView(final TasksViewModel model) {
    super(model);
  }

  @Override
  public Region build() {
    TableView<WrappedTaskModel> table = createTable();
    createContextMenu(table);

    return new StackPane(table);
  }

  private TableView<WrappedTaskModel> createTable() {
    TableView<WrappedTaskModel> table = new TableView<>(model.getTasks());
    var columns = table.getColumns();
    columns.add(createColumn("Task", 300, WrappedTaskModel::nameProperty));
    columns.add(createColumn("Status", 100, 100, WrappedTaskModel::statusProperty));
    columns.add(createColumn("Priority", 100, 100, WrappedTaskModel::priorityProperty));
    TableColumn<WrappedTaskModel, Number> progressCol = createColumn("Progress", 100,
        WrappedTaskModel::progressProperty);
    columns.add(progressCol);

    // progress bar
    progressCol.setCellFactory(__ -> new LabeledProgressBarCell<>());

    TableViewUtils.autoFitLastColumn(table);
    return table;
  }

  private void createContextMenu(final TableView<WrappedTaskModel> table) {
    Menu prio = new Menu("Set priority", null,
        MenuItems.create("High", event -> setTaskPriority(table, TaskPriority.HIGH)),
        MenuItems.create("Normal", event -> setTaskPriority(table, TaskPriority.NORMAL)));

    ContextMenu menu = new ContextMenu(
        MenuItems.create("Cancel selected tasks", event -> handleCancelTask(table, event)),
        MenuItems.create("Cancel all tasks", model.onCancelAllTasksProperty()),
        MenuItems.create("Cancel batch task", model.onCancelBatchTaskProperty()), //
        prio);

    table.setContextMenu(menu);
  }

  private void setTaskPriority(final TableView<WrappedTaskModel> table, TaskPriority prio) {
    var selectedTasks = table.getSelectionModel().getSelectedItems();
    for (WrappedTaskModel t : selectedTasks) {
      // TODO reflec this in the thread pool
      // there is a version of PriorityQueue that may be used
      t.getTask().setPriority(prio);
    }
  }

  public void handleCancelTask(final TableView<WrappedTaskModel> table, ActionEvent event) {
    var selectedTasks = table.getSelectionModel().getSelectedItems();
    for (WrappedTaskModel t : selectedTasks) {
      t.getTask().cancel();
    }
  }

}
