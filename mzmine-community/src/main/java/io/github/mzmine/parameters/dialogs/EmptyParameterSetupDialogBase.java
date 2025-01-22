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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.EmbeddedParameterComponentProvider;
import io.github.mzmine.parameters.EstimatedComponentHeightProvider;
import io.github.mzmine.parameters.EstimatedComponentWidthProvider;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.ExitCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents an empty base parameter setup dialog. Implementations of this dialog should
 * list all added parameters and their components in the pane
 * <p>
 * TODO: parameter setup dialog should show the name of the module in the title
 */
public class EmptyParameterSetupDialogBase extends Stage {

  public static final Logger logger = Logger.getLogger(
      EmptyParameterSetupDialogBase.class.getName());
  protected final ParameterSetupPane paramPane;
  protected ExitCode exitCode = ExitCode.UNKNOWN;
  // add everything to this panel
  protected BorderPane mainPane;
  // for ease of use extracted fields from param pane
  protected BorderPane centerPane;
  protected Map<String, Node> parametersAndComponents;
  protected ParameterSet parameterSet;
  // this value is incremented after a sub parmaeterset was expanded
  protected int widthExpandedBySubParameters;
  protected DoubleProperty maxExtraHeightExpanded = new SimpleDoubleProperty(0);
  protected DoubleProperty maxExtraWidthExpanded = new SimpleDoubleProperty(0);

  public EmptyParameterSetupDialogBase(boolean valueCheckRequired, ParameterSet parameters) {
    this(valueCheckRequired, parameters, null);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  public EmptyParameterSetupDialogBase(boolean valueCheckRequired, ParameterSet parameters,
      Region message) {
    this(valueCheckRequired, parameters, true, true, message);
  }

  public EmptyParameterSetupDialogBase(boolean valueCheckRequired, ParameterSet parameters,
      boolean addOkButton, boolean addCancelButton, Region message) {
    super();
    Image mzmineIcon = FxIconUtil.loadImageFromResources("mzmineIcon.png");
    this.getIcons().add(mzmineIcon);

    final var thisDialog = this;
    paramPane = new ParameterSetupPane(valueCheckRequired, parameters, addOkButton, addCancelButton,
        message, false) {
      @Override
      protected void callOkButton() {
        closeDialog(ExitCode.OK);
      }

      @Override
      protected void callCheckParametersButton() {
        checkParameterValues(true, true);
      }

      @Override
      protected void callCancelButton() {
        closeDialog(ExitCode.CANCEL);
      }

      @Override
      protected void parametersChanged() {
        thisDialog.parametersChanged();
      }
    };

    // for ease of use down stream
    centerPane = paramPane.getCenterPane();
    parametersAndComponents = paramPane.getParametersAndComponents();
    parameterSet = paramPane.getParameterSet();

    mainPane = new BorderPane(paramPane);
    Scene scene = new Scene(new StackPane(mainPane));
    scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        logger.finest("Escape pressed on dialog %s".formatted(parameters.getModuleNameAttribute()));
        event.consume();
        closeDialog(ExitCode.CANCEL);
      }
      if (event.getCode() == KeyCode.ENTER && event.isShortcutDown()) {
        event.consume();
        closeDialog(ExitCode.OK);
      }
    });

    // Use main CSS
    scene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(scene);

    setTitle(parameterSet.getModuleNameAttribute());

    setMinWidth(500.0);
    setMinHeight(400.0);

    centerOnScreen();
  }

  private static double calcMaxHeight() {
    return MZmineCore.getDesktop().getMainWindow().getScene().getHeight() * 0.95;
  }

  private static double calcMaxWidth() {
    return MZmineCore.getDesktop().getMainWindow().getScene().getWidth() * 0.95;
  }

  /**
   * Adds a button to check the parameter
   */
  public void addCheckParametersButton() {
    if (paramPane == null) {
      return;
    }
    paramPane.addCheckParametersButton();
  }

  private void addSizeChangeListeners(final Map<String, Node> parametersAndComponents) {
    final var heightProperties = streamComponents(parametersAndComponents.values()) //
        .filter(EstimatedComponentHeightProvider.class::isInstance)
        .map(EstimatedComponentHeightProvider.class::cast)
        .map(EstimatedComponentHeightProvider::estimatedHeightProperty)
        .toArray(DoubleProperty[]::new);

    // sum of extra heights
    maxExtraHeightExpanded.bind(Bindings.createDoubleBinding(
        (() -> Arrays.stream(heightProperties).mapToDouble(DoubleProperty::get).sum()),
        heightProperties));
    maxExtraHeightExpanded.addListener((_, oldValue, newValue) -> {
      double diff = newValue.doubleValue() - oldValue.doubleValue();
      setHeight(Math.min(calcMaxHeight(), getHeight() + diff));
    });

    // bind extra width to MAX of all widths
    final var widthProperties = streamComponents(parametersAndComponents.values()).filter(
            n -> n instanceof EstimatedComponentWidthProvider)
        .map(n -> ((EstimatedComponentWidthProvider) n).estimatedWidthProperty())
        .toArray(DoubleProperty[]::new);
    maxExtraWidthExpanded.bind(Bindings.createDoubleBinding(
        (() -> Arrays.stream(widthProperties).mapToDouble(DoubleProperty::get).max().orElse(0d)),
        widthProperties));
    maxExtraWidthExpanded.addListener((observable, oldValue, newValue) -> {
      double widthDiff = newValue.doubleValue() - oldValue.doubleValue();
      setWidth(Math.min(calcMaxWidth(), getWidth() + widthDiff));
    });
  }

  private static Stream<Node> streamComponents(final Collection<Node> nodes) {
    return nodes.stream().mapMulti(EmptyParameterSetupDialogBase::streamComponents);
  }

  private static void streamComponents(final Node node, final Consumer<Node> nodeConsumer) {
    nodeConsumer.accept(node);
    if (node instanceof EmbeddedParameterComponentProvider component) {
      component.getComponents().forEach(child -> {
        streamComponents(child, nodeConsumer);
      });
    }
  }

  @Override
  public void showAndWait() {
    if (MZmineCore.getDesktop() != null) {
      // this should prevent the main stage tool tips from bringing the main stage to the front.
      Stage mainStage = MZmineCore.getDesktop().getMainWindow();
      this.initOwner(mainStage);
    }
    super.showAndWait();
  }

  public ParameterSetupPane getParamPane() {
    return paramPane;
  }

  /**
   * Method for reading exit code
   */
  public ExitCode getExitCode() {
    return exitCode;
  }

  /**
   * Creating a grid pane with all the parameters and labels
   *
   * @param parameters parameters to fill the grid pane
   * @return a grid pane
   */
  @NotNull
  public GridPane createParameterPane(@NotNull Parameter<?>[] parameters) {
    var pane = paramPane.createParameterPane(parameters);
    // see if sizes change within components
    addSizeChangeListeners(parametersAndComponents);
    return pane;
  }

  public <ComponentType extends Node> ComponentType getComponentForParameter(
      UserParameter<?, ComponentType> p) {
    return paramPane.getComponentForParameter(p);
  }

  public ParameterSet updateParameterSetFromComponents() {
    return paramPane.updateParameterSetFromComponents();
  }

  public void setParameterValuesToComponents() {
    paramPane.setParameterValuesToComponents();
  }

  protected int getNumberOfParameters() {
    return paramPane.getNumberOfParameters();
  }

  public ButtonBar getButtonBar() {
    return paramPane.getButtonBar();
  }

  /**
   * This method may be called by some of the dialog components, for example as a result of
   * double-click by user
   */
  public void closeDialog(ExitCode exitCode) {
    boolean closeWindow = true;
    if (exitCode == ExitCode.OK) {
      // commit the changes to the parameter set
      updateParameterSetFromComponents();

      // ok? only close if value check not required or successful
      closeWindow = !isValueCheckRequired() || checkParameterValues(false, false);
    }
    if (closeWindow) {
      this.exitCode = exitCode;
      hide();
    }
  }

  /**
   * @return false if parameters are set incorrectly
   */
  public boolean checkParameterValues(boolean updateParametersFirst, boolean showSuccessDialog) {
    // commit the changes to the parameter set
    if (updateParametersFirst) {
      updateParameterSetFromComponents();
    }

    if (isValueCheckRequired()) {
      ArrayList<String> messages = new ArrayList<>();
      boolean allParametersOK = paramPane.getParameterSet().checkParameterValues(messages);

      if (!allParametersOK) {
        StringBuilder message = new StringBuilder("Please check the parameter settings:\n");
        for (String m : messages) {
          message.append(m).append("\n");
        }
        MZmineCore.getDesktop().displayMessage(null, message.toString());
        return false;
      }
      if (showSuccessDialog) {
        DialogLoggerUtil.showMessageDialogForTime("All parameter checks succeed.",
            "All parameters are set correctly", 3500);
      }
    }
    return true;
  }

  /**
   * This method does nothing, but it is called whenever user changes the parameters. It can be
   * overridden in extending classes to update the preview components, for example.
   */
  protected void parametersChanged() {
  }

  public boolean isValueCheckRequired() {
    return paramPane.isValueCheckRequired();
  }

}
