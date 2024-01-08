/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.util;

import io.github.mzmine.main.MZmineCore;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javafx.application.Platform;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

/**
 * GUI related utilities
 */
public class GUIUtils {

  /**
   * Registers a keyboard handler to a given component
   *
   * @param component Component to register the handler to
   * @param stroke Keystroke to activate the handler
   * @param listener ActionListener to handle the key press
   * @param actionCommand Action command string
   */
  public static void registerKeyHandler(JComponent component, KeyStroke stroke,
      final ActionListener listener, final String actionCommand) {
    registerKeyHandler(component, JComponent.WHEN_IN_FOCUSED_WINDOW, stroke, listener,
        actionCommand);
  }

  /**
   * Registers a keyboard handler to a given component
   *
   * @param component Component to register the handler to
   * @param condition see {@link JComponent} and {@link JComponent#WHEN_IN_FOCUSED_WINDOW}
   * @param stroke Keystroke to activate the handler
   * @param listener ActionListener to handle the key press
   * @param actionCommand Action command string
   */
  public static void registerKeyHandler(JComponent component, int condition, KeyStroke stroke,
      final ActionListener listener, final String actionCommand) {
    component.getInputMap(condition).put(stroke, actionCommand);
    component.getActionMap().put(actionCommand, new AbstractAction() {

      /**
           *
           */
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent event) {
        ActionEvent newEvent =
            new ActionEvent(event.getSource(), ActionEvent.ACTION_PERFORMED, actionCommand);
        listener.actionPerformed(newEvent);
      }
    });
  }

  /**
   * Close all open MZmine windows, except the main (project) window
   */
  public static void closeAllWindows() {
    if(MZmineCore.isHeadLessMode()) {
      // dont do anything in headless mode
      return;
    }
    // Close AWT windows
    SwingUtilities.invokeLater(() -> {
      for (java.awt.Window window : java.awt.Window.getWindows()) {
        window.dispose();
      }
    });

    // Close JavaFX windows
    Platform.runLater(() -> {
      for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
        if (window == MZmineCore.getDesktop().getMainWindow())
          continue;
        window.hide();
      }
    });

  }

  /**
   * Add a new menu item to a given menu
   *
   * @param menu Menu to add the item to
   * @param text Menu item text
   * @param listener Menu item's ActionListener or null
   * @return Created menu item
   */
  public static JMenuItem addMenuItem(Container menu, String text, ActionListener listener) {
    return addMenuItem(menu, text, listener, null, 0, false);
  }

  /**
   * Add a new menu item to a given menu
   *
   * @param menu Menu to add the item to
   * @param text Menu item text
   * @param listener Menu item's ActionListener or null
   * @param actionCommand Menu item's action command or null
   * @return Created menu item
   */
  public static JMenuItem addMenuItem(Container menu, String text, ActionListener listener,
      String actionCommand) {
    return addMenuItem(menu, text, listener, actionCommand, 0, false);
  }

  /**
   * Add a new menu item to a given menu
   *
   * @param menu Menu to add the item to
   * @param text Menu item text
   * @param listener Menu item's ActionListener or null
   * @param mnemonic Menu item's mnemonic (virtual key code) or 0
   * @return Created menu item
   */
  public static JMenuItem addMenuItem(Container menu, String text, ActionListener listener,
      int mnemonic) {
    return addMenuItem(menu, text, listener, null, mnemonic, false);
  }

  /**
   * Add a new menu item to a given menu
   *
   * @param menu Menu to add the item to
   * @param text Menu item text
   * @param listener Menu item's ActionListener or null
   * @param mnemonic Menu item's mnemonic (virtual key code) or 0
   * @param setAccelerator Indicates whether to set key accelerator to CTRL + the key specified by
   *        mnemonic parameter
   * @return Created menu item
   */
  public static JMenuItem addMenuItem(Container menu, String text, ActionListener listener,
      int mnemonic, boolean setAccelerator) {
    return addMenuItem(menu, text, listener, null, mnemonic, setAccelerator);
  }

  /**
   * Add a new menu item to a given menu
   *
   * @param menu Menu to add the item to
   * @param text Menu item text
   * @param listener Menu item's ActionListener or null
   * @param actionCommand Menu item's action command or null
   * @param mnemonic Menu item's mnemonic (virtual key code) or 0
   * @param setAccelerator Indicates whether to set key accelerator to CTRL + the key specified by
   *        mnemonic parameter
   * @return Created menu item
   */
  public static JMenuItem addMenuItem(Container menu, String text, ActionListener listener,
      String actionCommand, int mnemonic, boolean setAccelerator) {
    JMenuItem item = new JMenuItem(text);
    if (listener != null)
      item.addActionListener(listener);
    if (actionCommand != null)
      item.setActionCommand(actionCommand);
    if (mnemonic > 0)
      item.setMnemonic(mnemonic);
    if (setAccelerator)
      item.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.CTRL_MASK));
    if (menu != null)
      menu.add(item);
    return item;
  }

  /**
   * Add a new button to a given component
   *
   * @param component Component to add the button to
   * @param text Button's text or null
   * @param icon Button's icon or null
   * @param listener Button's ActionListener or null
   * @return Created button
   */
  public static JButton addButton(Container component, String text, Icon icon,
      ActionListener listener) {
    return addButton(component, text, icon, listener, null, 0, null);
  }

  /**
   * Add a new button to a given component
   *
   * @param component Component to add the button to
   * @param text Button's text or null
   * @param icon Button's icon or null
   * @param listener Button's ActionListener or null
   * @param actionCommand Button's action command or null
   * @return Created button
   */
  public static JButton addButton(Container component, String text, Icon icon,
      ActionListener listener, String actionCommand) {
    return addButton(component, text, icon, listener, actionCommand, 0, null);
  }

  /**
   * Add a new button to a given component
   *
   * @param component Component to add the button to
   * @param text Button's text or null
   * @param icon Button's icon or null
   * @param listener Button's ActionListener or null
   * @param actionCommand Button's action command or null
   * @param toolTip Button's tooltip text or null
   * @return Created button
   */
  public static JButton addButton(Container component, String text, Icon icon,
      ActionListener listener, String actionCommand, String toolTip) {
    return addButton(component, text, icon, listener, actionCommand, 0, toolTip);
  }

  /**
   * Add a new button to a given component
   *
   * @param component Component to add the button to
   * @param text Button's text or null
   * @param icon Button's icon or null
   * @param listener Button's ActionListener or null
   * @param actionCommand Button's action command or null
   * @param mnemonic Button's mnemonic (virtual key code) or 0
   * @param toolTip Button's tooltip text or null
   * @return Created button
   */
  public static JButton addButton(Container component, String text, Icon icon,
      ActionListener listener, String actionCommand, int mnemonic, String toolTip) {
    JButton button = new JButton(text, icon);
    if (listener != null)
      button.addActionListener(listener);
    if (actionCommand != null)
      button.setActionCommand(actionCommand);
    if (mnemonic > 0)
      button.setMnemonic(mnemonic);
    if (toolTip != null)
      button.setToolTipText(toolTip);
    if (component != null)
      component.add(button);
    return button;
  }

  /**
   * Add a new button to a JPanel and then add the panel to a given component
   *
   * @param component Component to add the button to
   * @param text Button's text or null
   * @param icon Button's icon or null
   * @param listener Button's ActionListener or null
   * @return Created button
   */
  public static JButton addButtonInPanel(Container component, String text,
      ActionListener listener) {
    return addButtonInPanel(component, text, listener, null);
  }

  /**
   * Add a new button to a JPanel and then add the panel to a given component
   *
   * @param component Component to add the button to
   * @param text Button's text or null
   * @param icon Button's icon or null
   * @param listener Button's ActionListener or null
   * @param actionCommand Button's action command or null
   * @return Created button
   */
  public static JButton addButtonInPanel(Container component, String text, ActionListener listener,
      String actionCommand) {
    JPanel panel = new JPanel();
    JButton button = new JButton(text);
    if (listener != null)
      button.addActionListener(listener);
    if (actionCommand != null)
      button.setActionCommand(actionCommand);
    panel.add(button);
    if (component != null)
      component.add(panel);
    return button;
  }

  /**
   * Add a new editorpane to a given component
   *
   * @param component Component to add the label to
   * @param text Label's text
   * @return Created EditorPane
   */
  public static JEditorPane addEditorPane(String text) {
    JEditorPane result = new JEditorPane("text/html", text);
    result.setEditable(false);
    return result;
  }

  /**
   * Add a new label to a given component
   *
   * @param component Component to add the label to
   * @param text Label's text
   * @return Created label
   */
  public static JLabel addLabel(Container component, String text) {
    return addLabel(component, text, null, JLabel.LEFT, null);
  }

  /**
   * Add a new label to a given component
   *
   * @param component Component to add the label to
   * @param text Label's text
   * @param horizontalAlignment Label's horizontal alignment (e.g. JLabel.LEFT)
   * @return Created label
   */
  public static JLabel addLabel(Container component, String text, int horizontalAlignment) {
    return addLabel(component, text, null, horizontalAlignment, null);
  }

  /**
   * Add a new label to a given component
   *
   * @param component Component to add the label to
   * @param text Label's text
   * @param horizontalAlignment Label's horizontal alignment (e.g. JLabel.LEFT)
   * @param font Label's font
   * @return Created label
   */
  public static JLabel addLabel(Container component, String text, int horizontalAlignment,
      Font font) {
    return addLabel(component, text, null, horizontalAlignment, font);
  }

  /**
   * Add a new label to a given component
   *
   * @param component Component to add the label to
   * @param text Label's text
   * @param icon Label's icon
   * @param horizontalAlignment Label's horizontal alignment (e.g. JLabel.LEFT)
   * @param font Label's font
   * @return Created label
   */
  public static JLabel addLabel(Container component, String text, Icon icon,
      int horizontalAlignment, Font font) {
    JLabel label = new JLabel(text, icon, horizontalAlignment);
    if (component != null)
      component.add(label);
    if (font != null)
      label.setFont(font);
    return label;
  }

  /**
   * Add a new label to a JPanel and then add the panel to a given component
   *
   * @param component Component to add the label to
   * @param text Label's text
   * @return Created label
   */
  public static JLabel addLabelInPanel(Container component, String text) {
    JPanel panel = new JPanel();
    component.add(panel);
    return addLabel(panel, text);
  }

  /**
   * Add a separator to a given component
   *
   * @param component Component to add the separator to
   * @return Created separator
   */
  public static JSeparator addSeparator(Container component) {
    return addSeparator(component, 0);
  }

  /**
   * Add a separator to a given component
   *
   * @param component Component to add the separator to
   * @param margin Margin around the separator
   * @return Created separator
   */
  public static JSeparator addSeparator(Container component, int margin) {
    JSeparator separator = new JSeparator();
    if (margin > 0)
      addMargin(separator, margin);
    if (component != null)
      component.add(separator);
    return separator;
  }

  /**
   * Add a margin to a given component
   *
   * @param component Component to add the margin to
   * @param margin Margin size
   * @return Created border
   */
  public static Border addMargin(JComponent component, int margin) {
    Border marginBorder = BorderFactory.createEmptyBorder(margin, margin, margin, margin);
    component.setBorder(marginBorder);
    return marginBorder;
  }

  /**
   * Add a margin and border to a given component
   *
   * @param component Component to add the margin to
   * @param margin Margin size
   * @return Created border
   */
  public static Border addMarginAndBorder(JComponent component, int margin) {
    Border marginBorder = BorderFactory.createEmptyBorder(margin, margin, margin, margin);
    Border etchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    Border compoundBorder = BorderFactory.createCompoundBorder(etchedBorder, marginBorder);
    component.setBorder(compoundBorder);
    return compoundBorder;
  }

  /**
   * This method creates a JPanel which layouts given components in a table of given rows/columns.
   * Last column is considered main, so its components will be expanded to fill extra space.
   */
  public static JPanel makeTablePanel(int rows, int cols, JComponent components[]) {
    return makeTablePanel(rows, cols, cols - 1, components);
  }

  /**
   * This method creates a JPanel which layouts given components in a table of given rows/columns.
   * Specified column is considered main, so its components will be expanded to fill extra space.
   */
  public static JPanel makeTablePanel(int rows, int cols, int mainColumn, JComponent components[]) {

    GridBagLayout layout = new GridBagLayout();
    JPanel panel = new JPanel(layout);
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(0, 0, 5, 5);

    for (int row = 0; row < rows; row++)
      for (int col = 0; col < cols; col++) {

        constraints.gridx = col;
        constraints.gridy = row;
        constraints.weightx = (col == mainColumn) ? 1 : 0;
        JComponent component = components[row * cols + col];
        panel.add(component);
        layout.setConstraints(component, constraints);
      }

    /*
     * Create one extra invisible component, which is vertically expandable. This will align the
     * whole table to the top.
     */
    JComponent space = (JComponent) Box.createGlue();
    constraints.gridx = 0;
    constraints.gridy = rows;
    constraints.weightx = 0;
    constraints.weighty = 1;
    panel.add(space);
    layout.setConstraints(space, constraints);

    return panel;
  }

  /**
   *
   * Add a new checkbox to given component
   *
   * @param container Component to add the checkbox to
   * @param text Checkbox' text
   * @param icon Checkbox' icon or null
   * @param listener Checkbox' listener or null
   * @param actionCommand Checkbox' action command or null
   * @param mnemonic Checkbox' mnemonic (virtual key code) or 0
   * @param toolTip Checkbox' tool tip or null
   * @param state Checkbox' state
   * @return Created checkbox
   */
  public static JCheckBox addCheckbox(Container component, String text, Icon icon,
      ActionListener listener, String actionCommand, int mnemonic, String toolTip, boolean state) {
    JCheckBox checkbox = new JCheckBox(text, icon, state);
    if (listener != null)
      checkbox.addActionListener(listener);
    if (actionCommand != null)
      checkbox.setActionCommand(actionCommand);
    if (mnemonic > 0)
      checkbox.setMnemonic(mnemonic);
    if (toolTip != null)
      checkbox.setToolTipText(toolTip);
    if (component != null)
      component.add(checkbox);
    return checkbox;
  }

  /**
   *
   * Add a new checkbox to given component
   *
   * @param component Component to add the checkbox to
   * @param text Checkbox' text
   * @param icon Checkbox' icon or null
   * @param listener Checkbox' listener or null
   * @param actionCommand Checkbox' action command or null
   * @param toolTip Checkbox' tool tip or null
   * @param state Checkbox' state
   * @return
   */
  public static JCheckBox addCheckbox(Container component, String text, Icon icon,
      ActionListener listener, String actionCommand, String toolTip, boolean state) {
    return addCheckbox(component, text, icon, listener, actionCommand, 0, toolTip, state);
  }

  /**
   *
   * Add a new checkbox to given component
   *
   * @param component Component to add the checkbox to
   * @param text Checkbox' text
   * @param icon Checkbox' icon or null
   * @param listener Checkbox' listener or null
   * @param actionCommand Checkbox' action command or null
   * @param toolTip Checkbox' tool tip or null
   * @return
   */
  public static JCheckBox addCheckbox(Container component, String text, Icon icon,
      ActionListener listener, String actionCommand, String toolTip) {
    return addCheckbox(component, text, icon, listener, actionCommand, 0, toolTip, false);
  }

  /**
   *
   * Add a new checkbox to given component
   *
   * @param component Component to add the checkbox to
   * @param text Checkbox' text
   * @param listener Checkbox' listener or null
   * @param actionCommand Checkbox' action command or null
   * @param toolTip Checkbox' tool tip or null
   * @return
   */
  public static JCheckBox addCheckbox(Container component, String text, ActionListener listener,
      String actionCommand, String toolTip) {
    return addCheckbox(component, text, null, listener, actionCommand, 0, toolTip, false);
  }

}
