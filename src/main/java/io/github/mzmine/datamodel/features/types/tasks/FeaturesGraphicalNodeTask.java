/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features.types.tasks;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Task for creating graphical nodes, having (ModularFeatureListRow row, AtomicDouble progress) constructor
 */
public class FeaturesGraphicalNodeTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(FeaturesGraphicalNodeTask.class.getName());

  Class<? extends Node> nodeClass;
  private StackPane pane;
  private ModularFeatureListRow row;
  private String collHeader;
  private AtomicDouble progress = new AtomicDouble(0d);
  private final int rowID;

  public FeaturesGraphicalNodeTask(Class<? extends Node> nodeClass, StackPane pane,
      ModularFeatureListRow row, String collHeader) {
    super(null, Instant.now()); // no new data stored -> null
    this.nodeClass = nodeClass;
    this.pane = pane;
    this.row = row;
    rowID = row.getID();
    this.collHeader = collHeader;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    Node n = null;
    try {
      // create instance of nodeClass node with (ModularFeatureListRow, AtomicDouble) constructor
      n = nodeClass.getConstructor(new Class[]{ModularFeatureListRow.class, AtomicDouble.class})
          .newInstance(row, progress);

      if (n != null) {
        final Node node = n;
        // save chart for later
        row.addBufferedColChart(collHeader, n);

        Platform.runLater(() -> {
          pane.getChildren().add(node);
        });
      } else {
        logger.log(Level.WARNING,
            () -> String.format("Cannot create graphical column for row %d and %s", rowID,
                nodeClass.toString()));
      }
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
        | InvocationTargetException e) {
      e.printStackTrace();
      logger.log(Level.SEVERE, e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      return;
    }

    setStatus(TaskStatus.FINISHED);
    progress.set(1d);
  }

  @Override
  public String getTaskDescription() {
    return "Creating a graphical column for col: " + collHeader
        + " in row: " + rowID;
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }
}
