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

package io.github.mzmine.parameters.dialogs;

import com.google.common.base.Strings;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents an empty base parameter setup dialog. Implementations of this dialog should
 * list all added parameters and their components in {@link #parametersAndComponents}
 * <p>
 * TODO: parameter setup dialog should show the name of the module in the title
 */
public class EmptyParameterSetupDialogBase extends Stage {

  public static final Logger logger = Logger.getLogger(
      EmptyParameterSetupDialogBase.class.getName());
  protected final URL helpURL;
  // Parameters and their representation in the dialog
  protected final ParameterSet parameterSet;
  protected final Map<String, Node> parametersAndComponents = new HashMap<>();
  // Buttons
  protected final Button btnOK, btnCancel, btnHelp;
  // Button panel - added here so it is possible to move buttons as a whole,
  // if needed.
  protected final ButtonBar pnlButtons;
  // Footer message
  protected final String footerMessage;

  // the centerPane is empty and used as the main container for all parameter components
  protected final BorderPane mainPane;
  protected final ScrollPane mainScrollPane;
  protected final BorderPane centerPane;

  // If true, the dialog won't allow the OK button to proceed, unless all
  // parameters pass the value check. This is undesirable in the BatchMode
  // setup dialog, where some parameters need to be set in advance according
  // to values that are not yet imported etc.
  private final boolean valueCheckRequired;
  /**
   * Help window for this setup dialog. Initially null, until the user clicks the Help button.
   */
  protected HelpWindow helpWindow = null;
  private ExitCode exitCode = ExitCode.UNKNOWN;

  public EmptyParameterSetupDialogBase(boolean valueCheckRequired, ParameterSet parameters) {
    this(valueCheckRequired, parameters, null);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public EmptyParameterSetupDialogBase(boolean valueCheckRequired, ParameterSet parameters,
      String message) {

    Image mzmineIcon = FxIconUtil.loadImageFromResources("MZmineIcon.png");
    this.getIcons().add(mzmineIcon);

    this.valueCheckRequired = valueCheckRequired;
    this.parameterSet = parameters;
    this.helpURL = parameters.getClass().getResource("help/help.html");
    this.footerMessage = message;

    // Main panel which holds all the components in a grid
    mainPane = new BorderPane();
    Scene scene = new Scene(mainPane);

    // Use main CSS
    scene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(scene);

    centerPane = new BorderPane();

    mainScrollPane = new ScrollPane(centerPane);
    // mainScrollPane.setStyle("-fx-border-color: red;");
    mainScrollPane.setFitToWidth(true);
    mainScrollPane.setFitToHeight(true);
    mainScrollPane.setPadding(new Insets(10.0));
    mainPane.setCenter(mainScrollPane);

    btnOK = new Button("OK");
    btnOK.setOnAction(e -> closeDialog(ExitCode.OK));
    ButtonBar.setButtonData(btnOK, ButtonData.OK_DONE);

    btnCancel = new Button("Cancel");
    btnCancel.setOnAction(e -> closeDialog(ExitCode.CANCEL));
    ButtonBar.setButtonData(btnCancel, ButtonData.CANCEL_CLOSE);

    // Add buttons to the ButtonBar
    pnlButtons = new ButtonBar();
    pnlButtons.getButtons().addAll(btnOK, btnCancel);
    pnlButtons.setPadding(new Insets(10.0));

    if (parameters.getOnlineHelpUrl() != null) { // if we have online docs, use those
      btnHelp = new Button("Help");
      btnHelp.setOnAction(e -> {
        MZmineCore.getDesktop().openWebPage(parameters.getOnlineHelpUrl());
      });

      ButtonBar.setButtonData(btnHelp, ButtonData.HELP);
      pnlButtons.getButtons().add(btnHelp);
    } else if (helpURL != null) { // otherwise use old help url
      btnHelp = new Button("Help");
      btnHelp.setOnAction(e -> {
        if (helpWindow != null) {
          helpWindow.show();
          helpWindow.toFront();
        } else {
          helpWindow = new HelpWindow(helpURL.toString());
          helpWindow.show();
        }
      });

      ButtonBar.setButtonData(btnHelp, ButtonData.HELP);
      pnlButtons.getButtons().add(btnHelp);
    } else {
      btnHelp = null;
    }

    mainPane.setBottom(pnlButtons);

    if (!Strings.isNullOrEmpty(footerMessage)) {
      WebView label = new WebView();
      label.getEngine().loadContent(footerMessage);
      label.setMaxHeight(100.0);
      mainPane.setTop(label);
    }

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

  /**
   * Creating a grid pane with all the parameters and labels
   *
   * @param parameters parameters to fill the grid pane
   * @return a grid pane
   */
  @NotNull
  public GridPane createParameterPane(@NotNull Parameter<?>[] parameters) {
    GridPane paramsPane = new GridPane();
    // paramsPane.setStyle("-fx-border-color: blue;");

    /*
     * Adding an empty ColumnConstraints object for column2 has the effect of not setting any
     * constraints, leaving the GridPane to compute the column's layout based solely on its
     * content's size preferences and constraints.
     */
    ColumnConstraints column1 = new ColumnConstraints();
    ColumnConstraints column2 = new ColumnConstraints();
    paramsPane.getColumnConstraints().addAll(column1, column2);
    int rowCounter = 0;

    // Create labels and components for each parameter
    for (Parameter<?> p : parameters) {

      if (!(p instanceof UserParameter)) {
        continue;
      }
      UserParameter up = (UserParameter) p;

      Node comp = up.createEditingComponent();
      //      addToolTipToControls(comp, up.getDescription());
      if (comp instanceof Region) {
        double minWidth = ((Region) comp).getMinWidth();
        // if (minWidth > column2.getMinWidth()) column2.setMinWidth(minWidth);
        // paramsPane.setMinWidth(minWidth + 200);
      }
      GridPane.setMargin(comp, new Insets(5.0, 0.0, 5.0, 0.0));

      // Set the initial value
      Object value = up.getValue();
      if (value != null) {
        up.setValueToComponent(comp, value);
      }

      // Add listeners so we are notified about any change in the values
      addListenersToNode(comp);

      // By calling this we make sure the components will never be resized
      // smaller than their optimal size
      // comp.setMinimumSize(comp.getPreferredSize());
      // comp.setToolTipText(up.getDescription());

      Label label = new Label(p.getName());
      label.minWidthProperty().bind(label.widthProperty());
      label.setPadding(new Insets(0.0, 10.0, 0.0, 0.0));

      final Tooltip tooltip = new Tooltip(up.getDescription());
      tooltip.setShowDuration(new Duration(20_000));
      label.setTooltip(tooltip);

      label.setStyle("-fx-font-weight: bold");
      paramsPane.add(label, 0, rowCounter);
      label.setLabelFor(comp);

      parametersAndComponents.put(p.getName(), comp);

      // TODO: Multiple selection will be expandable, other components not
      /*
       * JComboBox t = new JComboBox(); int comboh = t.getPreferredSize().height; int comph =
       * comp.getPreferredSize().height; int verticalWeight = comph > 2 * comboh ? 1 : 0;
       * vertWeightSum += verticalWeight;
       */

      paramsPane.add(comp, 1, rowCounter, 1, 1);

      rowCounter++;
    }
    return paramsPane;
  }


  /**
   * Method for reading exit code
   */
  public ExitCode getExitCode() {
    return exitCode;
  }

  @SuppressWarnings("unchecked")
  public <ComponentType extends Node> ComponentType getComponentForParameter(
      UserParameter<?, ComponentType> p) {
    return (ComponentType) parametersAndComponents.get(p.getName());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void updateParameterSetFromComponents() {
    for (Parameter<?> p : parameterSet.getParameters()) {
      if (!(p instanceof UserParameter) && !(p instanceof HiddenParameter)) {
        continue;
      }
      UserParameter up;
      if (p instanceof UserParameter) {
        up = (UserParameter) p;
      } else {
        up = (UserParameter) ((HiddenParameter) p).getEmbeddedParameter();
      }

      Node component = parametersAndComponents.get(p.getName());

      // if a parameter is a HiddenParameter it does not necessarily have
      // component
      if (component != null) {
        up.setValueFromComponent(component);
      }
    }
  }

  public void setParameterValuesToComponents() {
    for (Parameter<?> p : parameterSet.getParameters()) {
      if (!(p instanceof UserParameter) && !(p instanceof HiddenParameter)) {
        continue;
      }
      UserParameter up;
      if (p instanceof UserParameter) {
        up = (UserParameter) p;
      } else {
        up = (UserParameter) ((HiddenParameter) p).getEmbeddedParameter();
      }

      Node component = parametersAndComponents.get(p.getName());

      // if a parameter is a HiddenParameter it does not necessarily have
      // component
      if (component != null) {
        up.setValueToComponent(component, up.getValue());
      }
    }
  }

  protected int getNumberOfParameters() {
    return parameterSet.getParameters().length;
  }

  /**
   * This method may be called by some of the dialog components, for example as a result of
   * double-click by user
   */
  public void closeDialog(ExitCode exitCode) {
    if (exitCode == ExitCode.OK) {
      // commit the changes to the parameter set
      updateParameterSetFromComponents();

      if (valueCheckRequired) {
        ArrayList<String> messages = new ArrayList<String>();
        boolean allParametersOK = parameterSet.checkParameterValues(messages);

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


  protected void addListenersToNode(Node node) {
    if (node instanceof TextField) {
      TextField textField = (TextField) node;
      textField.textProperty().addListener(((observable, oldValue, newValue) -> {
        parametersChanged();
      }));
    }
    if (node instanceof ComboBox) {
      ComboBox<?> comboComp = (ComboBox<?>) node;
      comboComp.valueProperty()
          .addListener(((observable, oldValue, newValue) -> parametersChanged()));
    }
    if (node instanceof ChoiceBox) {
      ChoiceBox<?> choiceBox = (ChoiceBox) node;
      choiceBox.valueProperty()
          .addListener(((observable, oldValue, newValue) -> parametersChanged()));
    }
    if (node instanceof CheckBox) {
      CheckBox checkBox = (CheckBox) node;
      checkBox.selectedProperty()
          .addListener(((observable, oldValue, newValue) -> parametersChanged()));
    }
    if (node instanceof Region) {
      Region panelComp = (Region) node;
      for (int i = 0; i < panelComp.getChildrenUnmodifiable().size(); i++) {
        Node child = panelComp.getChildrenUnmodifiable().get(i);
        addListenersToNode(child);
      }
    }
  }

  public boolean isValueCheckRequired() {
    return valueCheckRequired;
  }

  protected void addToolTipToControls(Node node, String toolTipText) {
    if (node instanceof Control) {
      ((Control) node).setTooltip(new Tooltip(toolTipText));
    }
    if (node instanceof Region) {
      Region panelComp = (Region) node;
      for (int i = 0; i < panelComp.getChildrenUnmodifiable().size(); i++) {
        Node child = panelComp.getChildrenUnmodifiable().get(i);
        addToolTipToControls(child, toolTipText);
      }
    }
  }

}
