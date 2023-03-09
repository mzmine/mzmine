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

package io.github.mzmine.parameters.dialogs;

import com.google.common.base.Strings;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersComponent;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.scene.Node;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a basic pane for parameter setup when the {@link ParameterSetupDialog} is
 * not used. All added parameters and their components in {@link #parametersAndComponents}
 * <p>
 */
@SuppressWarnings("rawtypes")
public class ParameterSetupPane extends BorderPane {

  public static final Logger logger = Logger.getLogger(ParameterSetupPane.class.getName());
  protected final URL helpURL;
  // Parameters and their representation in the dialog
  protected final ParameterSet parameterSet;
  protected final Map<String, Node> parametersAndComponents = new HashMap<>();
  protected final Button btnHelp;
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
  protected Button btnCancel;
  // Buttons
  protected Button btnOK;
  /**
   * Help window for this setup dialog. Initially null, until the user clicks the Help button.
   */
  protected HelpWindow helpWindow = null;
  private GridPane paramsPane;

  public ParameterSetupPane(boolean valueCheckRequired, boolean addOkButton,
      ParameterSet parameters) {
    this(valueCheckRequired, parameters, addOkButton, false, null, true, true);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  public ParameterSetupPane(boolean valueCheckRequired, ParameterSet parameters,
      boolean addOkButton, String message) {
    this(valueCheckRequired, parameters, addOkButton, false, message, true, true);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  public ParameterSetupPane(boolean valueCheckRequired, ParameterSet parameters,
      boolean addOkButton, boolean addCancelButton, String message, boolean addParamComponents) {
    this(valueCheckRequired, parameters, addOkButton, addCancelButton, message, addParamComponents,
        true);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  public ParameterSetupPane(boolean valueCheckRequired, ParameterSet parameters,
      boolean addOkButton, boolean addCancelButton, String message, boolean addParamComponents,
      boolean addHelp) {
    this.valueCheckRequired = valueCheckRequired;
    this.parameterSet = parameters;
    this.helpURL = parameters.getClass().getResource("help/help.html");
    this.footerMessage = message;

    // Main panel which holds all the components in a grid
    mainPane = this;

    // Use main CSS
    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    centerPane = new BorderPane();

    mainScrollPane = new ScrollPane(centerPane);
    // mainScrollPane.setStyle("-fx-border-color: red;");
    mainScrollPane.setFitToWidth(true);
    mainScrollPane.setFitToHeight(true);
    mainScrollPane.setPadding(new Insets(10.0));
    mainPane.setCenter(mainScrollPane);

    // Add buttons to the ButtonBar
    pnlButtons = new ButtonBar();
    pnlButtons.setPadding(new Insets(1));
    if (addOkButton) {
      btnOK = new Button("OK");
      btnOK.setOnAction(e -> callOkButton());
      pnlButtons.getButtons().addAll(btnOK);
      ButtonBar.setButtonData(btnOK, ButtonData.OK_DONE);
    }
    if (addCancelButton) {
      btnCancel = new Button("Cancel");
      btnCancel.setOnAction(e -> callCancelButton());
      pnlButtons.getButtons().addAll(btnCancel);
      ButtonBar.setButtonData(btnCancel, ButtonData.CANCEL_CLOSE);
    }

    if (addHelp) {
      if (parameters.getOnlineHelpUrl() != null) { // if we have online docs, use those
        btnHelp = new Button("Help");
        btnHelp.setOnAction(
            e -> MZmineCore.getDesktop().openWebPage(parameters.getOnlineHelpUrl()));

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

    if (addParamComponents) {
      paramsPane = createParameterPane(parameterSet.getParameters());
      centerPane.setCenter(paramsPane);
    }
//    setMinWidth(500.0);
//    setMinHeight(400.0);
  }

  public BorderPane getCenterPane() {
    return centerPane;
  }

  public Map<String, Node> getParametersAndComponents() {
    return parametersAndComponents;
  }

  public ButtonBar getButtonBar() {
    return pnlButtons;
  }

  /**
   * ok button was clicked
   */
  protected void callOkButton() {
  }

  /**
   * check parameters button was clicked
   */
  protected void callCheckParametersButton() {
  }

  /**
   * cancel button was clicked
   */
  protected void callCancelButton() {
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
    paramsPane.setPadding(new Insets(5));
    // paramsPane.setStyle("-fx-border-color: blue;");

    /*
     * Adding an empty ColumnConstraints object for column2 has the effect of not setting any
     * constraints, leaving the GridPane to compute the column's layout based solely on its
     * content's size preferences and constraints.
     */
    ColumnConstraints column1 = new ColumnConstraints();
    ColumnConstraints column2 = new ColumnConstraints();
    column2.setFillWidth(true);
    column2.setHgrow(Priority.ALWAYS);
    paramsPane.getColumnConstraints().addAll(column1, column2);
    int rowCounter = 0;

    // Create labels and components for each parameter
    for (Parameter<?> p : parameters) {

      if (!(p instanceof UserParameter up)) {
        continue;
      }

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

      if (!up.getDescription().isEmpty()) {
        final Tooltip tooltip = new Tooltip(up.getDescription());
        tooltip.setShowDuration(new Duration(20_000));
        label.setTooltip(tooltip);
      }

      label.setStyle("-fx-font-weight: bold");
      label.setLabelFor(comp);

      parametersAndComponents.put(p.getName(), comp);

      // TODO: Multiple selection will be expandable, other components not
      /*
       * JComboBox t = new JComboBox(); int comboh = t.getPreferredSize().height; int comph =
       * comp.getPreferredSize().height; int verticalWeight = comph > 2 * comboh ? 1 : 0;
       * vertWeightSum += verticalWeight;
       */

      RowConstraints rowConstraints = new RowConstraints();
      rowConstraints.setVgrow(up.getComponentVgrowPriority());
      if (comp instanceof AdvancedParametersComponent) {
        paramsPane.add(comp, 0, rowCounter, 2, 1);
        rowConstraints.setVgrow(Priority.SOMETIMES);
      } else {
        paramsPane.add(label, 0, rowCounter);
        paramsPane.add(comp, 1, rowCounter, 1, 1);
      }
      paramsPane.getRowConstraints().add(rowConstraints);
      rowCounter++;
    }
    return paramsPane;
  }

  @SuppressWarnings("unchecked")
  public <ComponentType extends Node> ComponentType getComponentForParameter(
      UserParameter<?, ComponentType> p) {
    return (ComponentType) parametersAndComponents.get(p.getName());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public ParameterSet updateParameterSetFromComponents() {
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
    return parameterSet;
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
   * This method does nothing, but it is called whenever user changes the parameters. It can be
   * overridden in extending classes to update the preview components, for example.
   */
  protected void parametersChanged() {
  }

  public GridPane getParamsPane() {
    return paramsPane;
  }

  public ParameterSet getParameterSet() {
    return parameterSet;
  }

  protected void addListenersToNode(Node node) {
    if (node instanceof TextField textField) {
      textField.textProperty()
          .addListener(((observable, oldValue, newValue) -> parametersChanged()));
    }
    if (node instanceof ComboBox<?> comboComp) {
      comboComp.valueProperty()
          .addListener(((observable, oldValue, newValue) -> parametersChanged()));
    }
    if (node instanceof ChoiceBox) {
      ChoiceBox<?> choiceBox = (ChoiceBox) node;
      choiceBox.valueProperty()
          .addListener(((observable, oldValue, newValue) -> parametersChanged()));
    }
    if (node instanceof CheckBox checkBox) {
      checkBox.selectedProperty()
          .addListener(((observable, oldValue, newValue) -> parametersChanged()));
    }
    if (node instanceof Region panelComp) {
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
    if (node instanceof Region panelComp) {
      for (int i = 0; i < panelComp.getChildrenUnmodifiable().size(); i++) {
        Node child = panelComp.getChildrenUnmodifiable().get(i);
        addToolTipToControls(child, toolTipText);
      }
    }
  }

  /**
   * Adds a button to check the parameter
   */
  public void addCheckParametersButton() {
    Button btnCheck = new Button("Check");
    btnCheck.setOnAction(e -> callCheckParametersButton());
    pnlButtons.getButtons().addAll(btnCheck);
    // this way its always right next to OK button
    ButtonBar.setButtonData(btnCheck, ButtonData.OK_DONE);
  }
}
