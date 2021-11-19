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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import java.io.IOException;
import java.util.Collection;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import org.w3c.dom.Element;

/**
 * Batch queue parameter.
 */
public class BatchQueueParameter implements UserParameter<BatchQueue, AnchorPane> {

  private BatchQueue value;

  private BatchComponentController controller;

  /**
   * Create the parameter.
   */
  public BatchQueueParameter() {
    value = null;
  }

  @Override
  public String getName() {
    return "Batch queue";
  }

  @Override
  public String getDescription() {
    return "Please add and configure individual batch steps";
  }

  @Override
  public AnchorPane createEditingComponent() {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("BatchComponent.fxml"));
    try {
      AnchorPane pane = loader.load();
      controller = loader.getController();
      return pane;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return new AnchorPane();
  }

  @Override
  public BatchQueue getValue() {
    return value;
  }

  @Override
  public void setValue(final BatchQueue newValue) {
    value = newValue;
  }

  @Override
  public void setValueFromComponent(final AnchorPane component) {
    if (controller == null) {
      return;
    }
    setValue(controller.getValue());
  }

  @Override
  public void setValueToComponent(final AnchorPane component, final BatchQueue newValue) {
    if (controller == null) {
      return;
    }
    controller.setValue(newValue);
  }

  @Override
  public BatchQueueParameter cloneParameter() {
    final BatchQueueParameter copy = new BatchQueueParameter();
    copy.setValue(value != null ? value.clone() : null);
    return copy;
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {

    boolean allParamsOK = true;
    if (value == null) {

      // Parameters not set.
      errorMessages.add(getName() + " is not set");
      allParamsOK = false;

    } else {

      // Check each step.
      for (final MZmineProcessingStep<?> batchStep : value) {

        // Check step's parameters.
        final ParameterSet params = batchStep.getParameterSet();
        if (params == null)
          continue;

        for (final Parameter<?> parameter : params.getParameters()) {

          // Ignore the raw data files and feature lists parameters
          if (!(parameter instanceof RawDataFilesParameter)
              && !(parameter instanceof FeatureListsParameter)
              && !parameter.checkValue(errorMessages)) {
            allParamsOK = false;

          }
        }
      }
    }

    return allParamsOK;
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {
    value = BatchQueue.loadFromXml(xmlElement);
  }

  @Override
  public void saveValueToXML(final Element xmlElement) {
    if (value != null) {
      value.saveToXml(xmlElement);
    }
  }

  public BatchComponentController getController() {
    return controller;
  }
}
