/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
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

  public EmptyParameterSetupDialogBase(boolean valueCheckRequired, ParameterSet parameters) {
    this(valueCheckRequired, parameters, null);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  public EmptyParameterSetupDialogBase(boolean valueCheckRequired, ParameterSet parameters,
      String message) {
    this(valueCheckRequired, parameters, true, true, message);
  }

  public EmptyParameterSetupDialogBase(boolean valueCheckRequired, ParameterSet parameters,
      boolean addOkButton, boolean addCancelButton, String message) {
    super();
    Image mzmineIcon = FxIconUtil.loadImageFromResources("MZmineIcon.png");
    this.getIcons().add(mzmineIcon);

    final var thisDialog = this;
    paramPane = new ParameterSetupPane(valueCheckRequired, parameters, addOkButton, addCancelButton,
        message, false) {
      @Override
      protected void callOkButton() {
        closeDialog(ExitCode.OK);
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
    Scene scene = new Scene(mainPane);

    // Use main CSS
    scene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(scene);

    setTitle("Please set the parameters");
    setMinWidth(500.0);
    setMinHeight(400.0);

    centerOnScreen();
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
    return paramPane.createParameterPane(parameters);
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
    if (exitCode == ExitCode.OK) {
      // commit the changes to the parameter set
      updateParameterSetFromComponents();

      if (isValueCheckRequired()) {
        ArrayList<String> messages = new ArrayList<>();
        boolean allParametersOK = paramPane.getParameterSet().checkParameterValues(messages);

        if (!allParametersOK) {
          StringBuilder message = new StringBuilder("Please check the parameter settings:\n\n");
          for (String m : messages) {
            message.append(m);
            message.append("\n");
          }
          MZmineCore.getDesktop().displayMessage(null, message.toString());
          return;
        }
      }
    }
    this.exitCode = exitCode;
    hide();
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
