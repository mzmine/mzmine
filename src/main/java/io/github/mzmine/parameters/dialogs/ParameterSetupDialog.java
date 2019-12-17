/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.dialogs;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import com.google.common.base.Strings;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.util.ExitCode;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * This class represents the parameter setup dialog to set the values of SimpleParameterSet. Each
 * Parameter is represented by a component. The component can be obtained by calling
 * getComponentForParameter(). Type of component depends on parameter type:
 *
 * TODO: parameter setup dialog should show the name of the module in the title
 *
 */
public class ParameterSetupDialog extends Stage {

  private ExitCode exitCode = ExitCode.UNKNOWN;

  protected final URL helpURL;

  /**
   * Help window for this setup dialog. Initially null, until the user clicks the Help button.
   */
  protected HelpWindow helpWindow = null;

  // Parameters and their representation in the dialog
  protected final ParameterSet parameterSet;
  protected final Map<String, Node> parametersAndComponents = new HashMap<>();

  // If true, the dialog won't allow the OK button to proceed, unless all
  // parameters pass the value check. This is undesirable in the BatchMode
  // setup dialog, where some parameters need to be set in advance according
  // to values that are not yet imported etc.
  private final boolean valueCheckRequired;

  // Buttons
  protected final Button btnOK, btnCancel, btnHelp;

  // Button panel - added here so it is possible to move buttons as a whole,
  // if needed.
  protected final ButtonBar pnlButtons;

  // Footer message
  protected final String footerMessage;

  /**
   * This single panel contains a grid of all the components of this dialog. Row number 100 contains
   * all the buttons of the dialog. Derived classes may add their own components such as previews to
   * the unused cells of the grid.
   */
  protected final GridPane mainPanel;

  /**
   * Constructor
   */
  public ParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    this(valueCheckRequired, parameters, null);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public ParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters, String message) {

    this.valueCheckRequired = valueCheckRequired;
    this.parameterSet = parameters;
    this.helpURL = parameters.getClass().getResource("help/help.html");
    this.footerMessage = message;

    // Main panel which holds all the components in a grid
    mainPanel = new GridPane();
    Scene scene = new Scene(mainPanel);
    setScene(scene);

    int rowCounter = 0;
    int vertWeightSum = 0;

    // Create labels and components for each parameter
    for (Parameter<?> p : parameterSet.getParameters()) {

      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;

      Node comp = up.createEditingComponent();
      if (comp instanceof Control) {
        ((Control) comp).setTooltip(new Tooltip(up.getDescription()));
      }
      // Set the initial value
      Object value = up.getValue();
      if (value != null)
        up.setValueToComponent(comp, value);

      // Add listeners so we are notified about any change in the values
      // addListenersToComponent(comp);

      // By calling this we make sure the components will never be resized
      // smaller than their optimal size
      // comp.setMinimumSize(comp.getPreferredSize());

      // comp.setToolTipText(up.getDescription());

      Label label = new Label(p.getName());
      mainPanel.add(label, 0, rowCounter);
      label.setLabelFor(comp);

      parametersAndComponents.put(p.getName(), comp);

      // TODO: Multiple selection will be expandable, other components not
      /*
       * JComboBox t = new JComboBox(); int comboh = t.getPreferredSize().height; int comph =
       * comp.getPreferredSize().height; int verticalWeight = comph > 2 * comboh ? 1 : 0;
       * vertWeightSum += verticalWeight;
       */

      mainPanel.add(comp, 1, rowCounter, 1, 1);

      rowCounter++;

    }

    // Add a single empty cell to the 99th row. This cell is expandable
    // (weightY is 1), therefore the other components will be
    // aligned to the top, which is what we want
    // JComponent emptySpace = (JComponent) Box.createVerticalStrut(1);
    // mainPanel.add(emptySpace, 0, 99, 3, 1, 0, 1);


    btnOK = new Button("OK");
    btnOK.setOnAction(e -> {
      closeDialog(ExitCode.OK);
    });
    ButtonBar.setButtonData(btnOK, ButtonData.OK_DONE);

    btnCancel = new Button("Cancel");
    btnCancel.setOnAction(e -> {
      closeDialog(ExitCode.CANCEL);
    });
    ButtonBar.setButtonData(btnCancel, ButtonData.CANCEL_CLOSE);

    // Add buttons to the ButtonBar
    pnlButtons = new ButtonBar();
    pnlButtons.getButtons().addAll(btnOK, btnCancel);

    if (helpURL != null) {
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

    mainPanel.add(pnlButtons, 0, 100, 2, 1);

    /*
     * Last row in the table will be occupied by the buttons. We set the row number to 100 and width
     * to 3, spanning the 3 component columns defined above.
     */
    if (vertWeightSum == 0) {
      // mainPanel.add(Box.createGlue(), 0, 99, 3, 1, 1, 1);
    }

    if (!Strings.isNullOrEmpty(footerMessage)) {

      // Footer
      // WebView webView = new WebView();
      NotificationPane notificationPane = new NotificationPane(mainPanel);
      notificationPane.setText(footerMessage);
      notificationPane.setShowFromTop(false);
      notificationPane.getActions().add(new Action("Close", e -> notificationPane.hide()));


      /*
       * JEditorPane editorPane = GUIUtils.addEditorPane(footerMessage);
       * editorPane.addHyperlinkListener(new HyperlinkListener() {
       *
       * @Override public void hyperlinkUpdate(HyperlinkEvent e) { if
       * (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) { try {
       * Desktop.getDesktop().browse(e.getURL().toURI()); } catch (Exception ex) {
       * ex.printStackTrace(); } } } });
       */

      // This line is important on Windows, where resizing the dialog has
      // unexpected consequences on
      // some components
      // editorPane.setMinimumSize(editorPane.getPreferredSize());
      // mainPanel.add(editorPane, 0, 98, 3, 1);

      // mainPanel.addCenter(pnlButtons, 0, 100, 3, 1);
    }

    // Add some space around the widgets
    // GUIUtils.addMargin(mainPanel, 10);

    // Add the main panel as the only component of this dialog
    // add(mainPanel);

    // pack();

    setTitle("Please set the parameters");
    setMinWidth(400.0);
    setMinHeight(400.0);
    sizeToScene();
    centerOnScreen();

  }

  /**
   * This method must be called each time when a component is added to mainPanel. It will ensure the
   * minimal size of the dialog is set to the minimum size of the mainPanel plus a little extra, so
   * user cannot resize the dialog window smaller.
   */
  /*
   * public void updateMinimumSize() { Dimension panelSize = mainPanel.getMinimumSize(); Dimension
   * minimumSize = new Dimension(panelSize.width + 50, panelSize.height + 50);
   * setMinimumSize(minimumSize); }
   */



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
  protected void updateParameterSetFromComponents() {
    for (Parameter<?> p : parameterSet.getParameters()) {
      if (!(p instanceof UserParameter) && !(p instanceof HiddenParameter))
        continue;
      UserParameter up;
      if (p instanceof UserParameter)
        up = (UserParameter) p;
      else
        up = (UserParameter) ((HiddenParameter) p).getEmbeddedParameter();

      Node component = parametersAndComponents.get(p.getName());

      // if a parameter is a HiddenParameter it does not necessarily have
      // component
      if (component != null)
        up.setValueFromComponent(component);
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

  /*
   * protected void addListenersToComponent(JComponent comp) { if (comp instanceof JTextComponent) {
   * JTextComponent textComp = (JTextComponent) comp;
   * textComp.getDocument().addDocumentListener(this); } if (comp instanceof JComboBox) {
   * JComboBox<?> comboComp = (JComboBox<?>) comp; comboComp.addActionListener(this); } if (comp
   * instanceof JCheckBox) { JCheckBox checkComp = (JCheckBox) comp;
   * checkComp.addActionListener(this); } if (comp instanceof JPanel) { JPanel panelComp = (JPanel)
   * comp; for (int i = 0; i < panelComp.getComponentCount(); i++) { Component child =
   * panelComp.getComponent(i); if (!(child instanceof JComponent)) continue;
   * addListenersToComponent((JComponent) child); } } }
   *
   * @Override public void changedUpdate(DocumentEvent event) { parametersChanged(); }
   *
   * @Override public void insertUpdate(DocumentEvent event) { parametersChanged(); }
   *
   * @Override public void removeUpdate(DocumentEvent event) { parametersChanged(); }
   */

  public boolean isValueCheckRequired() {
    return valueCheckRequired;
  }

}
