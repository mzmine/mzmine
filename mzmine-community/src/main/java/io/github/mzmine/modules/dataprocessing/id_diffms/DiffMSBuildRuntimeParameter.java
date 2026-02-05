/*
 * Copyright (c) 2004-2026 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_diffms;

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.parameters.AbstractParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import java.io.File;
import java.util.Collection;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class DiffMSBuildRuntimeParameter extends AbstractParameter<Boolean, VBox> {

  private final String description;
  private final String buttonText;

  public DiffMSBuildRuntimeParameter(String name, String description, String buttonText) {
    super(name, description);
    this.description = description;
    this.buttonText = buttonText;
    setValue(false);
  }

  @Override
  public VBox createEditingComponent() {
    VBox main = new VBox(5);
    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER_LEFT);
    
    Button button = new Button(buttonText);
    button.setTooltip(new Tooltip(description));
    
    Label statusLabel = new Label();
    updateStatus(statusLabel);

    button.setOnAction(e -> {
      TaskService.getController().addTask(new DiffMSBuildRuntimeTask(() -> FxThread.runLater(() -> updateStatus(statusLabel))), TaskPriority.HIGH);
    });
    
    Label descLabel = new Label(description);
    descLabel.setWrapText(true);
    HBox.setHgrow(descLabel, Priority.ALWAYS);
    
    box.getChildren().addAll(button, descLabel);
    main.getChildren().addAll(box, statusLabel);
    return main;
  }

  private void updateStatus(Label label) {
    File py = DiffMSRuntimeManager.getUsablePython(DiffMSRuntimeManager.Variant.CPU);
    if (py != null) {
      label.setText("Status: Found usable Python at " + py.getAbsolutePath());
      label.setTextFill(Color.GREEN);
    } else {
      label.setText("Status: No Python runtime found in user directory.");
      label.setTextFill(Color.RED);
    }
  }

  @Override
  public void setValueToComponent(VBox component, @Nullable Boolean newValue) {
    // Action parameter, no state to reflect in component
  }

  @Override
  public void setValueFromComponent(VBox component) {
    // Action parameter, no state to read from component
  }

  @Override
  public @Nullable Boolean getValue() {
    return false;
  }

  @Override
  public void setValue(@Nullable Boolean value) {
  }

  @Override
  public DiffMSBuildRuntimeParameter cloneParameter() {
    return new DiffMSBuildRuntimeParameter(getName(), description, buttonText);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }
}
