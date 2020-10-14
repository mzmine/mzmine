/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data.types.tasks;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.lang.reflect.InvocationTargetException;
import javafx.application.Platform;
import javafx.beans.property.MapProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;

/**
 * Task for creating graphical nodes, having (ModularFeatureListRow row, AtomicDouble progress) constructor
 */
public class FeaturesGraphicalNodeTask extends AbstractTask{
  Class<? extends Node> nodeClass;
  private StackPane pane;
  private ModularFeatureListRow row;
  private TreeTableColumn<ModularFeatureListRow, MapProperty<RawDataFile, ModularFeature>> coll;
  private AtomicDouble progress = new AtomicDouble(0d);
  private int rowID = -1;

  public FeaturesGraphicalNodeTask(Class<? extends Node> nodeClass, StackPane pane, ModularFeatureListRow row,
      TreeTableColumn<ModularFeatureListRow, MapProperty<RawDataFile, ModularFeature>> coll) {
    super();
    this.nodeClass = nodeClass;
    this.pane = pane;
    this.row = row;
    this.coll = coll;
  }

  @Override
  public void run() {
    rowID = row.getID();

    setStatus(TaskStatus.PROCESSING);
    Node n = null;
    try {
      // create instance of nodeClass node with (ModularFeatureListRow, AtomicDouble) constructor
      n = nodeClass.getConstructor(new Class[]{ModularFeatureListRow.class, AtomicDouble.class})
          .newInstance(row, progress);
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
        | InvocationTargetException e) {
      e.printStackTrace();
    }
    final Node node = n;
    // save chart for later
    row.addBufferedColChart(coll.getText(), n);

    Platform.runLater(() -> {
      pane.getChildren().add(node);
    });
    setStatus(TaskStatus.FINISHED);
    progress.set(1d);
  }

  @Override
  public String getTaskDescription() {
    return "Creating a graphical column for col: " + coll.getText()
        + " in row: " + rowID;
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }
}