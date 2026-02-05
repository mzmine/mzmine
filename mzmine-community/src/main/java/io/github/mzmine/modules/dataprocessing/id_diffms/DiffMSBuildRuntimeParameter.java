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
import io.github.mzmine.taskcontrol.AbstractTask;
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
    HBox buildBox = new HBox(10);
    buildBox.setAlignment(Pos.CENTER_LEFT);
    
    Button buildButton = new Button(buttonText);
    buildButton.setTooltip(new Tooltip(description));
    
    Label statusLabel = new Label();
    updateStatus(statusLabel, main);

    buildButton.setOnAction(e -> {
      TaskService.getController().addTask(new DiffMSBuildRuntimeTask(() -> FxThread.runLater(() -> updateStatus(statusLabel, main))), TaskPriority.HIGH);
    });
    
    Label descLabel = new Label(description);
    descLabel.setWrapText(true);
    HBox.setHgrow(descLabel, Priority.ALWAYS);
    
    buildBox.getChildren().addAll(buildButton, descLabel);
    main.getChildren().addAll(buildBox, statusLabel);
    return main;
  }

  private void updateStatus(Label label, VBox main) {
    File py = DiffMSRuntimeManager.getUsablePython(DiffMSRuntimeManager.Variant.CPU);
    if (py != null) {
      label.setText("Status: Found usable Python at " + py.getAbsolutePath());
      label.setTextFill(Color.GREEN);
      // Remove any install button if it exists
      main.getChildren().removeIf(node -> node instanceof HBox && ((HBox)node).getId() != null && ((HBox)node).getId().equals("install-box"));
    } else {
      if (DiffMSRuntimeManager.anyPackExists()) {
        label.setText("Status: Runtime pack found but not installed.");
        label.setTextFill(Color.ORANGE);
        
        // Add install button if not already there
        if (main.getChildren().stream().noneMatch(node -> node instanceof HBox && ((HBox)node).getId() != null && ((HBox)node).getId().equals("install-box"))) {
          HBox installBox = new HBox(10);
          installBox.setId("install-box");
          installBox.setAlignment(Pos.CENTER_LEFT);
          Button installButton = new Button("Install Found Runtime");
          installButton.setOnAction(e -> {
            TaskService.getController().addTask(new AbstractTask(null, java.time.Instant.now()) {
              @Override
              public String getTaskDescription() { return "Extracting DiffMS runtime"; }
              @Override
              public double getFinishedPercentage() { return 0; }
              @Override
              public void run() {
                setStatus(io.github.mzmine.taskcontrol.TaskStatus.PROCESSING);
                try {
                  DiffMSRuntimeManager.ensureRuntimeAndGetPython(DiffMSRuntimeManager.Variant.CPU, this::isCanceled);
                  setStatus(io.github.mzmine.taskcontrol.TaskStatus.FINISHED);
                  FxThread.runLater(() -> updateStatus(label, main));
                } catch (Exception ex) {
                  setStatus(io.github.mzmine.taskcontrol.TaskStatus.ERROR);
                  error("Failed to extract runtime: " + ex.getMessage(), ex);
                }
              }
            }, TaskPriority.HIGH);
          });
          Label infoLabel = new Label("A local runtime pack was detected. Click to extract and initialize it.");
          infoLabel.setWrapText(true);
          installBox.getChildren().addAll(installButton, infoLabel);
          main.getChildren().add(installBox);
        }
      } else {
        label.setText("Status: No Python runtime found in user directory.");
        label.setTextFill(Color.RED);
        main.getChildren().removeIf(node -> node instanceof HBox && ((HBox)node).getId() != null && ((HBox)node).getId().equals("install-box"));
      }
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
