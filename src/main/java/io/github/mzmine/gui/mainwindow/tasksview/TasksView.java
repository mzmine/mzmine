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

package io.github.mzmine.gui.mainwindow.tasksview;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class TasksView extends VBox {

  private final NumberFormat format = new DecimalFormat("0%");
  @FXML
  private TableColumn<WrappedTask, String> taskNameColumn;
  @FXML
  private TableColumn<WrappedTask, String> taskPriorityColumn;
  @FXML
  private TableColumn<WrappedTask, Double> taskProgressColumn;
  @FXML
  private TableColumn<WrappedTask, TaskStatus> taskStatusColumn;
  @FXML
  private TableView<WrappedTask> table;

  public TasksView() {
    setFillWidth(true);
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TasksView.fxml"));
    fxmlLoader.setController(this);

    try {
      table = fxmlLoader.load();
      getChildren().add(table);
      VBox.setVgrow(table, Priority.ALWAYS);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  public void initialize() {
    initTaskView();
  }

  private void initTaskView() {
    ObservableList<WrappedTask> tasksQueue = MZmineCore.getTaskController().getTaskQueue()
        .getTasks();
    table.setItems(tasksQueue);

    taskNameColumn.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getActualTask().getTaskDescription()));
    taskPriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));

    taskStatusColumn.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getActualTask().getStatus()));
    taskProgressColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
        cell.getValue().getActualTask().getFinishedPercentage()));
    taskProgressColumn.setCellFactory(column -> new TableCell<>() {

      @Override
      public void updateItem(Double value, boolean empty) {
        super.updateItem(value, empty);
        if (empty) {
          return;
        }
        ProgressBar progressBar = new ProgressBar(value);
        progressBar.setOpacity(0.3);
        progressBar.prefWidthProperty().bind(taskProgressColumn.widthProperty().subtract(20));
        String labelText = format.format(value);
        Label percentLabel = new Label(labelText);
        StackPane stack = new StackPane();
        stack.setManaged(true);
        stack.getChildren().addAll(progressBar, percentLabel);
        setGraphic(stack);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      }
    });
  }

  public TableView<WrappedTask> getTable() {
    return table;
  }

  @FXML
  public void handleCancelTask(Event event) {
    var selectedTasks = table.getSelectionModel().getSelectedItems();
    for (WrappedTask t : selectedTasks) {
      t.getActualTask().cancel();
    }
  }

  @FXML
  public void handleCancelAllTasks(Event event) {
    for (WrappedTask t : table.getItems()) {
      t.getActualTask().cancel();
    }
  }

  @FXML
  public void handleSetHighPriority(Event event) {
    TaskController taskController = MZmineCore.getTaskController();
    var selectedTasks = table.getSelectionModel().getSelectedItems();
    for (WrappedTask t : selectedTasks) {
      taskController.setTaskPriority(t.getActualTask(), TaskPriority.HIGH);
    }
  }

  @FXML
  public void handleSetNormalPriority(Event event) {
    TaskController taskController = MZmineCore.getTaskController();
    var selectedTasks = table.getSelectionModel().getSelectedItems();
    for (WrappedTask t : selectedTasks) {
      taskController.setTaskPriority(t.getActualTask(), TaskPriority.NORMAL);
    }
  }

}
