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

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * The MVCI model contains only observable properties to bind in the {@link TasksView} and is
 * updated in the {@link TasksViewInteractor}
 */
public class TasksViewModel {

  private final ObservableList<WrappedTaskModel> tasks = FXCollections.observableArrayList();
  private final DoubleProperty allTasksProgress = new SimpleDoubleProperty();

  // important to track if batch is actually running to show additional progress
  private final BooleanProperty batchIsRunning = new SimpleBooleanProperty();
  private final DoubleProperty batchProgress = new SimpleDoubleProperty();
  private final StringProperty batchDescription = new SimpleStringProperty();

  // interactions
  private final ObjectProperty<EventHandler<ActionEvent>> onCancelBatchTask = new SimpleObjectProperty<>();
  private final ObjectProperty<EventHandler<ActionEvent>> onCancelAllTasks = new SimpleObjectProperty<>();
  private final ObjectProperty<EventHandler<ActionEvent>> onShowTasksView = new SimpleObjectProperty<>();

  public EventHandler<ActionEvent> getOnCancelBatchTask() {
    return onCancelBatchTask.get();
  }

  public void setOnCancelBatchTask(final EventHandler<ActionEvent> onCancelBatchTask) {
    this.onCancelBatchTask.set(onCancelBatchTask);
  }

  public ObjectProperty<EventHandler<ActionEvent>> onCancelBatchTaskProperty() {
    return onCancelBatchTask;
  }

  public EventHandler<ActionEvent> getOnCancelAllTasks() {
    return onCancelAllTasks.get();
  }

  public void setOnCancelAllTasks(final EventHandler<ActionEvent> onCancelAllTasks) {
    this.onCancelAllTasks.set(onCancelAllTasks);
  }

  public ObjectProperty<EventHandler<ActionEvent>> onCancelAllTasksProperty() {
    return onCancelAllTasks;
  }

  public EventHandler<ActionEvent> getOnShowTasksView() {
    return onShowTasksView.get();
  }

  public void setOnShowTasksView(final EventHandler<ActionEvent> onShowTasksView) {
    this.onShowTasksView.set(onShowTasksView);
  }

  public ObjectProperty<EventHandler<ActionEvent>> onShowTasksViewProperty() {
    return onShowTasksView;
  }

  public ObservableList<WrappedTaskModel> getTasks() {
    return tasks;
  }

  public void addTasks(List<WrappedTaskModel> toAdd) {
    tasks.addAll(toAdd);
  }

  public double getAllTasksProgress() {
    return allTasksProgress.get();
  }

  public void setAllTasksProgress(final double allTasksProgress) {
    this.allTasksProgress.set(allTasksProgress);
  }

  public DoubleProperty allTasksProgressProperty() {
    return allTasksProgress;
  }

  public boolean isBatchIsRunning() {
    return batchIsRunning.get();
  }

  public void setBatchIsRunning(final boolean batchIsRunning) {
    this.batchIsRunning.set(batchIsRunning);
  }

  public BooleanProperty batchIsRunningProperty() {
    return batchIsRunning;
  }

  public double getBatchProgress() {
    return batchProgress.get();
  }

  public void setBatchProgress(final double batchProgress) {
    this.batchProgress.set(batchProgress);
  }

  public DoubleProperty batchProgressProperty() {
    return batchProgress;
  }

  public String getBatchDescription() {
    return batchDescription.get();
  }

  public void setBatchDescription(final String batchDescription) {
    this.batchDescription.set(batchDescription);
  }

  public StringProperty batchDescriptionProperty() {
    return batchDescription;
  }
}
