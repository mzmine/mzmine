/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import org.w3c.dom.Element;

/**
 * Batch queue parameter.
 */
public class BatchQueueParameter implements UserParameter<BatchQueue, AnchorPane> {

  private static final Logger logger = Logger.getLogger(BatchQueueParameter.class.getName());
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
  public Priority getComponentVgrowPriority() {
    return Priority.ALWAYS;
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

      List<String> newErrors = new ArrayList<>();
      // Check each step.
      for (final MZmineProcessingStep<?> batchStep : value) {

        // Check step's parameters.
        final ParameterSet params = batchStep.getParameterSet();
        if (params == null) {
          continue;
        }

        if (!params.checkParameterValues(newErrors, true)) {
          allParamsOK = false;
        }

        if (!newErrors.isEmpty()) {
          // add module name and
          errorMessages.add("\n%s (step %d):".formatted(batchStep.getModule().getName(),
              value.indexOf(batchStep) + 1));
          errorMessages.addAll(newErrors);
          newErrors.clear();
        }
      }

      // do meta checks through all parameters
      // check min samples filter
      String warning = BatchUtils.checkBatchParameters(value);
      if (warning != null) {
        if (DesktopService.isGUI()) {
          final boolean continueAnyway = DialogLoggerUtil.showDialogYesNo("Warning", """
              %s
              Continue anyway?""".formatted(warning));

          allParamsOK = allParamsOK && continueAnyway;
        } else {
          // only add warning if not GUI as GUI otherwise shows two dialogs
          // in CLI mode we will just log the warning but still continue with the batch
          logger.warning(warning);
          errorMessages.add(warning);
        }
      }
    }

    return allParamsOK;
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {
    List<String> errorMessages = new ArrayList<>();
    // if modules are missing (false) fail it and do not set the parameters
    value = BatchQueue.loadFromXml(xmlElement, errorMessages, false);
    // do not log warnings here it's called on startup of mzmine
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
