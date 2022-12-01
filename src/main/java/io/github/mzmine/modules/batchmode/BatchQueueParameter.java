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
        if (params == null) {
          continue;
        }

        for (final Parameter<?> parameter : params.getParameters()) {

          // Ignore the raw data files and feature lists parameters
          if (!(parameter instanceof RawDataFilesParameter)
              && !(parameter instanceof FeatureListsParameter) && !parameter.checkValue(
              errorMessages)) {
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
