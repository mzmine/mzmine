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

package io.github.mzmine.datamodel.features.types.tasks;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.ModularFeature;
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
 * Task for creating graphical nodes, having (ModularFeature feature, AtomicDouble progress)
 * constructor
 */
public class FeatureGraphicalNodeTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(FeatureGraphicalNodeTask.class.getName());

  Class<? extends Node> nodeClass;
  private StackPane pane;
  private ModularFeature feature;
  private String collHeader;
  private AtomicDouble progress = new AtomicDouble(0d);

  public FeatureGraphicalNodeTask(Class<? extends Node> nodeClass, StackPane pane,
      ModularFeature feature,
      String collHeader) {
    super(null, Instant.now()); // no new data stored -> null, date irrelevant
    this.nodeClass = nodeClass;
    this.pane = pane;
    this.feature = feature;
    this.collHeader = collHeader;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    Node n = null;
    try {
      // create instance of nodeClass node with (ModularFeatureListRow, AtomicDouble) constructor
      n = nodeClass.getConstructor(new Class[]{ModularFeature.class, AtomicDouble.class})
          .newInstance(feature, progress);

      if (n != null) {
        final Node node = n;
        // save chart for later
        feature.addBufferedColChart(collHeader, n);
        Platform.runLater(() -> pane.getChildren().add(node));
      }

    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
        | InvocationTargetException e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE, e.getMessage(), e);
      return;
    }

    setStatus(TaskStatus.FINISHED);
    progress.set(1d);
  }

  @Override
  public String getTaskDescription() {
    return "Creating a graphical column for col: " + collHeader
        + " for m/z: " + feature.getMZ() + " in file: " + feature.getRawDataFile();
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }
}
