/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.dialogs;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.GridBagPanel;
import net.sf.mzmine.util.components.HelpButton;

/**
 * This class represents the parameter setup dialog to set the values of
 * SimpleParameterSet. Each Parameter is represented by a component. The
 * component can be obtained by calling getComponentForParameter(). Type of
 * component depends on parameter type:
 * 
 * TODO: parameter setup dialog should show the name of the module in the title
 * 
 */
public class ParameterSetupDialog extends JDialog
        implements ActionListener, DocumentListener {

    private static final long serialVersionUID = 1L;

    private ExitCode exitCode = ExitCode.UNKNOWN;

    private String helpID;

    // Parameters and their representation in the dialog
    protected ParameterSet parameterSet;
    private final Map<String, JComponent> parametersAndComponents;

    // If true, the dialog won't allow the OK button to proceed, unless all
    // parameters pass the value check. This is undesirable in the BatchMode
    // setup dialog, where some parameters need to be set in advance according
    // to values that are not yet imported etc.
    private final boolean valueCheckRequired;

    // Buttons
    private JButton btnOK, btnCancel, btnHelp;

    /**
     * This single panel contains a grid of all the components of this dialog
     * (see GridBagPanel). First three columns of the grid are title (JLabel),
     * parameter component (JFormattedTextField or other) and units (JLabel),
     * one row for each parameter. Row number 100 contains all the buttons of
     * the dialog. Derived classes may add their own components such as previews
     * to the unused cells of the grid.
     */
    protected GridBagPanel mainPanel;

    /**
     * Constructor
     */
    public ParameterSetupDialog(Window parent, boolean valueCheckRequired,
            ParameterSet parameters) {

        // 2015/12/15 Setting the parent to null, so the dialog always appears
        // in front (Tomas)
        super(null, "Please set the parameters",
                Dialog.ModalityType.DOCUMENT_MODAL);

        this.valueCheckRequired = valueCheckRequired;
        this.parameterSet = parameters;
        this.helpID = GUIUtils.generateHelpID(parameters);

        parametersAndComponents = new Hashtable<String, JComponent>();

        addDialogComponents();

        updateMinimumSize();
        pack();

        setLocationRelativeTo(parent);

    }

    /**
     * This method must be called each time when a component is added to
     * mainPanel. It will ensure the minimal size of the dialog is set to the
     * minimum size of the mainPanel plus a little extra, so user cannot resize
     * the dialog window smaller.
     */
    protected void updateMinimumSize() {
        Dimension panelSize = mainPanel.getMinimumSize();
        Dimension minimumSize = new Dimension(panelSize.width + 50,
                panelSize.height + 50);
        setMinimumSize(minimumSize);
    }

    /**
     * Constructs all components of the dialog
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void addDialogComponents() {

        // Main panel which holds all the components in a grid
        mainPanel = new GridBagPanel();

        int rowCounter = 0;
        int vertWeightSum = 0;

        // Create labels and components for each parameter
        for (Parameter p : parameterSet.getParameters()) {

            if (!(p instanceof UserParameter))
                continue;
            UserParameter up = (UserParameter) p;

            JComponent comp = up.createEditingComponent();
            comp.setToolTipText(up.getDescription());

            // Set the initial value
            Object value = up.getValue();
            if (value != null)
                up.setValueToComponent(comp, value);

            // Add listeners so we are notified about any change in the values
            addListenersToComponent(comp);

            // By calling this we make sure the components will never be resized
            // smaller than their optimal size
            comp.setMinimumSize(comp.getPreferredSize());

            comp.setToolTipText(up.getDescription());

            JLabel label = new JLabel(p.getName());
            mainPanel.add(label, 0, rowCounter);
            label.setLabelFor(comp);

            parametersAndComponents.put(p.getName(), comp);

            JComboBox t = new JComboBox();
            int comboh = t.getPreferredSize().height;
            int comph = comp.getPreferredSize().height;

            // Multiple selection will be expandable, other components not
            int verticalWeight = comph > 2 * comboh ? 1 : 0;
            vertWeightSum += verticalWeight;

            mainPanel.add(comp, 1, rowCounter, 1, 1, 1, verticalWeight,
                    GridBagConstraints.VERTICAL);

            rowCounter++;

        }

        // Add a single empty cell to the 99th row. This cell is expandable
        // (weightY is 1), therefore the other components will be
        // aligned to the top, which is what we want
        // JComponent emptySpace = (JComponent) Box.createVerticalStrut(1);
        // mainPanel.add(emptySpace, 0, 99, 3, 1, 0, 1);

        // Create a separate panel for the buttons
        JPanel pnlButtons = new JPanel();

        btnOK = GUIUtils.addButton(pnlButtons, "OK", null, this);
        btnCancel = GUIUtils.addButton(pnlButtons, "Cancel", null, this);

        if (helpID != null) {
            btnHelp = new HelpButton(helpID);
            pnlButtons.add(btnHelp);
        }

        /*
         * Last row in the table will be occupied by the buttons. We set the row
         * number to 100 and width to 3, spanning the 3 component columns
         * defined above.
         */
        if (vertWeightSum == 0) {
            mainPanel.add(Box.createGlue(), 0, 99, 3, 1, 1, 1);
        }
        mainPanel.addCenter(pnlButtons, 0, 100, 3, 1);

        // Add some space around the widgets
        GUIUtils.addMargin(mainPanel, 10);

        // Add the main panel as the only component of this dialog
        add(mainPanel);

        pack();
    }

    /**
     * Implementation for ActionListener interface
     */
    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnOK) {
            closeDialog(ExitCode.OK);
        }

        if (src == btnCancel) {
            closeDialog(ExitCode.CANCEL);
        }

        if ((src instanceof JCheckBox) || (src instanceof JComboBox)) {
            parametersChanged();
        }

    }

    /**
     * Method for reading exit code
     */
    public ExitCode getExitCode() {
        return exitCode;
    }

    public JComponent getComponentForParameter(Parameter<?> p) {
        return parametersAndComponents.get(p.getName());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void updateParameterSetFromComponents() {
        for (Parameter<?> p : parameterSet.getParameters()) {
            if (!(p instanceof UserParameter))
                continue;
            UserParameter up = (UserParameter) p;
            JComponent component = parametersAndComponents.get(p.getName());
            up.setValueFromComponent(component);
        }
    }

    protected int getNumberOfParameters() {
        return parameterSet.getParameters().length;
    }

    /**
     * This method may be called by some of the dialog components, for example
     * as a result of double-click by user
     */
    public void closeDialog(ExitCode exitCode) {
        if (exitCode == ExitCode.OK) {
            // commit the changes to the parameter set
            updateParameterSetFromComponents();

            if (valueCheckRequired) {
                ArrayList<String> messages = new ArrayList<String>();
                boolean allParametersOK = parameterSet
                        .checkParameterValues(messages);

                if (!allParametersOK) {
                    StringBuilder message = new StringBuilder(
                            "Please check the parameter settings:\n\n");
                    for (String m : messages) {
                        message.append(m);
                        message.append("\n");
                    }
                    MZmineCore.getDesktop().displayMessage(this,
                            message.toString());
                    return;
                }
            }
        }
        this.exitCode = exitCode;
        dispose();

    }

    /**
     * This method does nothing, but it is called whenever user changes the
     * parameters. It can be overridden in extending classes to update the
     * preview components, for example.
     */
    protected void parametersChanged() {

    }

    private void addListenersToComponent(JComponent comp) {
        if (comp instanceof JTextComponent) {
            JTextComponent textComp = (JTextComponent) comp;
            textComp.getDocument().addDocumentListener(this);
        }
        if (comp instanceof JComboBox) {
            JComboBox<?> comboComp = (JComboBox<?>) comp;
            comboComp.addActionListener(this);
        }
        if (comp instanceof JCheckBox) {
            JCheckBox checkComp = (JCheckBox) comp;
            checkComp.addActionListener(this);
        }
        if (comp instanceof JPanel) {
            JPanel panelComp = (JPanel) comp;
            for (int i = 0; i < panelComp.getComponentCount(); i++) {
                Component child = panelComp.getComponent(i);
                if (!(child instanceof JComponent))
                    continue;
                addListenersToComponent((JComponent) child);
            }
        }
    }

    @Override
    public void changedUpdate(DocumentEvent event) {
        parametersChanged();
    }

    @Override
    public void insertUpdate(DocumentEvent event) {
        parametersChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent event) {
        parametersChanged();
    }

    public boolean isValueCheckRequired() {
        return valueCheckRequired;
    }

}
